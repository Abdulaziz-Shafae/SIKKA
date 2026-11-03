package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.*;
import com.example.sikka.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RideNotificationActionService {
    private final NotificationRepository notificationRepository;
    private final VisitRepository visitRepository;
    private final VisitPlanRepository visitPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final ActivitySessionRepository activitySessionRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final RideRepository rideRepository;
    private final MapService mapService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public String applyRideNotificationAction(Integer accountId, Integer notificationId, String action) {
        Notification notification = notificationRepository.findNotificationByIdForUpdate(notificationId);
        if (notification == null || notification.getAccount() == null || !accountId.equals(notification.getAccount().getId()) || !"IN_APP".equals(notification.getChannel())) throw new ApiException("Notification not found");
        if (!Set.of("RIDE_STATUS_CHANGED", "RIDE_WAIT_CHANGED").contains(notification.getType())) throw new ApiException("This notification does not support a ride action");
        if (notification.getRespondedAt() != null) throw new ApiException("This ride alert has already been answered");
        if (notification.getPlanItem() == null || notification.getVisit() == null) throw new ApiException("Notification has no scheduled ride");
        Visit visit = visitRepository.findVisitByIdForUpdate(notification.getVisit().getId());
        if (visit == null || !"IN_PROGRESS".equals(visit.getStatus())) throw new ApiException("Ride actions require a visit in progress");
        if (!visit.getOrganizer().getId().equals(accountId)) throw new ApiException("Only the visit organizer can adjust the shared schedule");
        PlanItem target = entityManager.find(PlanItem.class, notification.getPlanItem().getId(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (target == null || target.getRide() == null || !target.getVisitPlan().getVisit().getId().equals(visit.getId())) throw new ApiException("Ride is not part of this visit");
        VisitPlan plan = visitPlanRepository.findVisitPlanByIdForUpdate(target.getVisitPlan().getId());
        if (plan == null || !"GROUP".equals(plan.getPlanKind()) || !Boolean.TRUE.equals(plan.getIsActive()) || !"SCHEDULED".equals(plan.getTimingMode()) || plan.getProposedItems() != null || plan.getPendingScope() != null || plan.getProposalBaseSnapshot() != null || plan.getPlanApproval() == null || !"APPROVED".equals(plan.getPlanApproval().getStatus())) throw new ApiException("Finish existing proposals before changing the approved schedule");
        if (Boolean.TRUE.equals(target.getPinned()) || alreadyStarted(target)) throw new ApiException("An active or pinned ride cannot be changed automatically");
        if (!Set.of("DEFER", "REPLACE", "KEEP", "ADJUST").contains(action)) throw new ApiException("Unknown ride action");
        if ("DEFER".equals(action) || "REPLACE".equals(action)) {
            if (!"RIDE_STATUS_CHANGED".equals(notification.getType()) || "OPEN".equals(target.getRide().getStatus())) throw new ApiException("This ride is no longer reported closed");
        } else if (!"RIDE_WAIT_CHANGED".equals(notification.getType())) {
            throw new ApiException("This action requires a wait-time alert");
        }
        if ("KEEP".equals(action) || "ADJUST".equals(action)) {
            if (!"RIDE_WAIT_MAJOR".equals(notification.getActionType())) throw new ApiException("This action requires at least 30 minutes of additional wait");
        }

        List<PlanItem> items = new ArrayList<>(planItemRepository.findAllByVisitPlan_Id(plan.getId()));
        items.sort(Comparator.comparing(PlanItem::getSortOrder));
        int index = items.indexOf(target);
        if (index < 0 || target.getStartsAt() == null || target.getEndsAt() == null) throw new ApiException("The scheduled ride could not be found");
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
        if (target.getEndsAt().isBefore(now)) throw new ApiException("The scheduled ride has passed");

        List<PlanItem> changed = new ArrayList<>(items);
        int originalDuration = visit.getDurationMinutes();
        LocalDateTime oldEnd = items.get(items.size() - 1).getEndsAt();
        if (oldEnd == null) throw new ApiException("An activity has no scheduled end");
        String result;

        if ("DEFER".equals(action)) {
            // Never move an uncompleted ride across a fixed prayer/meal/meeting or another subgroup.
            if (items.subList(index + 1, items.size()).stream().anyMatch(item -> Boolean.TRUE.equals(item.getPinned()) || !"RIDE".equals(item.getActivityKind()) || !Objects.equals(item.getSubgroupKey(), target.getSubgroupKey()) || alreadyStarted(item))) throw new ApiException("Cannot safely defer this ride past fixed activities; choose an alternative");
            changed.remove(target);
            changed.add(target);
            result = "تم تأجيل اللعبة إلى نهاية الأنشطة المتبقية";
        } else if ("REPLACE".equals(action)) {
            Ride replacement = chooseVerifiedReplacement(target, items, visit);
            target.setRide(replacement);
            target.setTitle(replacement.getName());
            target.setWaitingMinutes(replacement.getWaitMinutes());
            target.setActivityMinutes(replacement.getRideDurationMinutes() + (replacement.getEntryExitBufferMinutes() == null ? 0 : replacement.getEntryExitBufferMinutes()));
            result = "تم استبدال اللعبة بلعبة مفتوحة ومناسبة: " + replacement.getName();
        } else {
            Ride ride = target.getRide();
            if (ride.getWaitMinutes() == null || ride.getWaitObservedAt() == null || ride.getWaitObservedAt().isBefore(now.minusMinutes(10))) throw new ApiException("Updated wait-time reading is not fresh");
            int increase = ride.getWaitMinutes() - target.getWaitingMinutes();
            if (increase < 30) throw new ApiException("The waiting time no longer exceeds the plan by 30 minutes");
            target.setWaitingMinutes(ride.getWaitMinutes());
            if ("ADJUST".equals(action)) {
                PlanItem lessImportant = changed.stream().filter(item -> item.getSortOrder() > target.getSortOrder() && "RIDE".equals(item.getActivityKind()) && !Boolean.TRUE.equals(item.getPinned()) && !alreadyStarted(item) && !"MUST_RIDE".equals(item.getPriority())).min(Comparator.comparingInt(item -> "OPTIONAL".equals(item.getPriority()) ? 0 : 1)).orElseThrow(() -> new ApiException("No lower-priority future ride can be removed safely"));
                changed.remove(lessImportant);
                // Keep the old row for history only if execution has not started; deletion never removes actual visit records.
                planItemRepository.delete(lessImportant);
                result = "تم الاحتفاظ باللعبة المهمة واستبعاد نشاط أقل أولوية";
            } else {
                result = "تم الاحتفاظ بجميع الألعاب مع تحديث وقت نهاية الزيارة";
            }
        }

        recalculateFutureSchedule(visit, items, changed, index, now, oldEnd, "KEEP".equals(action));
        notification.setResponse("{\"action\":\"" + action + "\"}");
        notification.setRespondedAt(now);
        notification.setReadAt(now);
        notificationRepository.save(notification);
        return result;
    }

    private boolean alreadyStarted(PlanItem item) {
        if (item.getExecutionReleasedAt() != null) return true;
        for (ActivitySession session : activitySessionRepository.findAllByPlanItem_Id(item.getId())) {
            if (!"WAITING".equals(session.getStatus()) || session.getEnteredAreaAt() != null || session.getQueueStartedAt() != null) return true;
        }
        return visitRecordRepository.existsByPlanItem_Id(item.getId());
    }

    private Ride chooseVerifiedReplacement(PlanItem target, List<PlanItem> items, Visit visit) {
        Set<Integer> excluded = new HashSet<>();
        for (PlanItem item : items) if (item.getRide() != null) excluded.add(item.getRide().getId());
        List<VisitMember> members = visitMemberRepository.findAllByVisit_Id(visit.getId());
        Ride anchor = target.getRide();
        if (anchor.getMapX() == null || anchor.getMapY() == null) throw new ApiException("Original ride has no verified map coordinates");
        return rideRepository.findAll().stream().filter(ride -> !excluded.contains(ride.getId()) && "OPEN".equals(ride.getStatus()) && ride.getWaitMinutes() != null && ride.getWaitObservedAt() != null && !ride.getWaitObservedAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Riyadh")).minusMinutes(10)) && ride.getRideDurationMinutes() != null && ride.getMinHeightCm() != null && ride.getMapX() != null && ride.getMapY() != null)
                .filter(ride -> members.stream().filter(member -> "GOING".equals(member.getRsvpStatus())).allMatch(member -> member.getVisitorProfile() != null && member.getVisitorProfile().getHeightCm() != null && member.getVisitorProfile().getHeightCm().compareTo(ride.getMinHeightCm()) >= 0))
                .min(Comparator.comparingDouble(ride -> Math.pow(ride.getMapX().doubleValue() - anchor.getMapX().doubleValue(),2) + Math.pow(ride.getMapY().doubleValue() - anchor.getMapY().doubleValue(),2)))
                .orElseThrow(() -> new ApiException("No verified, open and eligible alternative ride is available"));
    }

    private void recalculateFutureSchedule(Visit visit, List<PlanItem> original, List<PlanItem> revised, int index, LocalDateTime now, LocalDateTime oldEnd, boolean allowExtension) {
        // Existing history and fixed activities must never be moved to manufacture a feasible schedule.
        PlanItem previous = index == 0 ? null : revised.get(index - 1);
        LocalDateTime cursor = previous == null ? original.get(0).getStartsAt() : previous.getEndsAt();
        if (cursor == null) throw new ApiException("Previous activity timing is missing");
        cursor = cursor.isBefore(now) ? now : cursor;
        for (int i = index; i < revised.size(); i++) {
            PlanItem current = revised.get(i);
            if (alreadyStarted(current) || Boolean.TRUE.equals(current.getPinned())) {
                if (current.getStartsAt() == null || cursor.isAfter(current.getStartsAt())) throw new ApiException("Change would move an already started or pinned activity");
                cursor = current.getEndsAt();
                previous = current;
                continue;
            }
            int walking = walkingMinutesBetween(previous, current);
            int waiting = current.getWaitingMinutes() == null ? 0 : current.getWaitingMinutes();
            int activity = current.getActivityMinutes() == null ? 0 : current.getActivityMinutes();
            if (waiting < 0 || activity <= 0) throw new ApiException("Cannot schedule an activity with missing timing data");
            LocalDateTime end = cursor.plusMinutes(walking + waiting + activity);
            if (!allowExtension && end.isAfter(oldEnd)) throw new ApiException("The proposed change exceeds the original visit end time");
            current.setWalkingMinutes(walking);
            current.setStartsAt(cursor);
            current.setEndsAt(end);
            if (visit.getPlannedStartTime() != null) {
                LocalDateTime base = visit.getVisitDate().atTime(visit.getPlannedStartTime());
                current.setRelativeStartMinutes(Math.toIntExact(java.time.Duration.between(base, cursor).toMinutes()));
                current.setRelativeEndMinutes(Math.toIntExact(java.time.Duration.between(base, end).toMinutes()));
            }
            current.setSortOrder(i);
            planItemRepository.save(current);
            cursor = end;
            previous = current;
        }
        if (allowExtension && cursor.isAfter(oldEnd)) {
            LocalDateTime base = visit.getVisitDate().atTime(visit.getPlannedStartTime());
            int duration = Math.toIntExact(java.time.Duration.between(base, cursor).toMinutes());
            ParkOperatingHours.validateVisitWindow(visit.getVisitDate(), visit.getPlannedStartTime(), duration);
            visit.setDurationMinutes(duration);
            visit.setPlannedEndTime(LocalTime.from(cursor));
            visitRepository.save(visit);
        }
    }

    private int walkingMinutesBetween(PlanItem from, PlanItem to) {
        if (to.getRide() == null && to.getPlace() == null) return 0;
        String toType = to.getRide() == null ? "PLACE" : "RIDE";
        Integer toId = to.getRide() == null ? to.getPlace().getId() : to.getRide().getId();
        com.example.sikka.DtoOut.MapRouteDtoOut route;
        if (from == null || from.getRide() == null && from.getPlace() == null) route = mapService.getEntranceRoute(toType, toId);
        else route = mapService.getRoute(from.getRide() == null ? "PLACE" : "RIDE", from.getRide() == null ? from.getPlace().getId() : from.getRide().getId(), toType, toId);
        if (route == null || route.getDistanceMeters() == null || route.getPoints() == null || route.getPoints().isEmpty()) throw new ApiException("Verified walking route is missing for the new order");
        return Math.toIntExact((long)Math.ceil(route.getDistanceMeters() / 70d));
    }
}
