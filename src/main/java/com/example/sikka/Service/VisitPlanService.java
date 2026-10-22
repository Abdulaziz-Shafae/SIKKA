package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoOut.*;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.PlanItem;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitPlan;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.PaymentOrderRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitPlanRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitPlanService {

    private final VisitPlanRepository visitPlanRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final JsonMapper participantJsonMapper = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    @Transactional(readOnly = true)
    public List<VisitPlanDtoOut> getVisitPlans(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can view all plans");
        }

        List<VisitPlan> visitPlans = visitPlanRepository.findAllByVisit_Id(visitId);

        if (visitPlans.isEmpty()) {
            throw new ApiException("No plans found for this visit");
        }

        List<VisitPlanDtoOut> visitPlanDtoOuts = new ArrayList<>();

        for (VisitPlan visitPlan : visitPlans) {
            visitPlanDtoOuts.add(convertVisitPlanToDtoOut(visitPlan));
        }

        return visitPlanDtoOuts;
    }

    @Transactional(readOnly = true)
    public VisitPlanDetailsDtoOut getVisitPlanDetails(Integer accountId, Integer visitPlanId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanById(visitPlanId);

        if (visitPlan == null) {
            throw new ApiException("Visit plan not found");
        }

        Visit visit = visitPlan.getVisit();

        if (!visit.getOrganizer().getId().equals(accountId)) {
            VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visit.getId(), accountId);

            if (visitMember == null) {
                throw new ApiException("You are not a member of this visit");
            }

            if ("INDIVIDUAL".equals(visitPlan.getPlanKind())) {
                if (visitPlan.getOwnerVisitMember() == null) {
                    throw new ApiException("Individual plan owner not found");
                }

                if (!visitPlan.getOwnerVisitMember().getId().equals(visitMember.getId())) {
                    throw new ApiException("You cannot view another member's individual plan");
                }
            }
        }

        List<PlanItemDtoOut> planItemDtoOuts = new ArrayList<>();

        if (visitPlan.getPlanItems() != null) {
            List<PlanItem> planItems = new ArrayList<>(visitPlan.getPlanItems());
            planItems.sort(Comparator.comparing(PlanItem::getSortOrder).thenComparing(PlanItem::getId));

            for (PlanItem planItem : planItems) {
                planItemDtoOuts.add(convertPlanItemToDtoOut(planItem));
            }
        }

        String approvalStatus = null;

        if (visitPlan.getPlanApproval() != null) {
            approvalStatus = visitPlan.getPlanApproval().getStatus();
        }

        return new VisitPlanDetailsDtoOut(convertVisitPlanToDtoOut(visitPlan), approvalStatus, planItemDtoOuts);
    }

    @Transactional
    public void createIndividualVisitPlan(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Plans cannot be created in the current visit status");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can start planning");
        }

        VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (visitMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        if (!"GOING".equals(visitMember.getRsvpStatus())) {
            throw new ApiException("Individual plans require a GOING member");
        }

        if (!paymentOrderRepository.existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(visitId, "PAID")) {
            throw new ApiException("A verified payment is required before planning");
        }

        if (visitPlanRepository.existsByVisit_IdAndOwnerVisitMember_IdAndPlanKind(visitId, visitMember.getId(), "INDIVIDUAL")) {
            throw new ApiException("You already have an individual plan for this visit");
        }

        visit.setMembersLocked(true);
        VisitPlan visitPlan = new VisitPlan();
        visitPlan.setVisit(visit);
        visitPlan.setOwnerVisitMember(visitMember);
        visitPlan.setPlanKind("INDIVIDUAL");
        visitPlan.setIsActive(false);
        visitPlanRepository.save(visitPlan);
    }

    @Transactional
    public void createGroupVisitPlan(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can create the group plan");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Plans cannot be created in the current visit status");
        }

        if (!paymentOrderRepository.existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(visitId, "PAID")) {
            throw new ApiException("A verified payment is required before planning");
        }

        if (visitPlanRepository.existsByVisit_IdAndPlanKind(visitId, "GROUP")) {
            throw new ApiException("This visit already has a group plan");
        }

        List<VisitMember> visitMembers = visitMemberRepository.findAllByVisit_IdAndRsvpStatus(visitId, "GOING");

        if (visitMembers.isEmpty()) {
            throw new ApiException("The visit must have at least one GOING member");
        }

        for (VisitMember visitMember : visitMembers) {
            if (!visitPlanRepository.existsByVisit_IdAndOwnerVisitMember_IdAndPlanKind(visitId, visitMember.getId(), "INDIVIDUAL")) {
                throw new ApiException("Every GOING member must have an individual plan first");
            }
        }

        visit.setMembersLocked(true);
        VisitPlan visitPlan = new VisitPlan();
        visitPlan.setVisit(visit);
        visitPlan.setOwnerVisitMember(null);
        visitPlan.setPlanKind("GROUP");
        visitPlan.setIsActive(false);
        visitPlanRepository.save(visitPlan);
    }

    @Transactional
    public void deleteVisitPlan(Integer accountId, Integer visitPlanId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanById(visitPlanId);

        if (visitPlan == null) {
            throw new ApiException("Visit plan not found");
        }

        Visit visit = visitRepository.findVisitByIdForUpdate(visitPlan.getVisit().getId());

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can delete plans");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Plans cannot be deleted in the current visit status");
        }

        if (Boolean.TRUE.equals(visitPlan.getIsActive())) {
            throw new ApiException("An active plan cannot be deleted");
        }

        if (visitPlan.getPlanApproval() != null) {
            throw new ApiException("A plan with an approval record cannot be deleted");
        }

        if (visitPlan.getProposedItems() != null) {
            throw new ApiException("A plan with a pending proposal cannot be deleted");
        }

        if (visitPlan.getPlanItems() != null && !visitPlan.getPlanItems().isEmpty()) {
            throw new ApiException("Remove the plan items before deleting the plan");
        }

        if ("INDIVIDUAL".equals(visitPlan.getPlanKind()) && visitPlanRepository.existsByVisit_IdAndPlanKind(visit.getId(), "GROUP")) {
            throw new ApiException("An individual plan cannot be deleted while a group plan exists");
        }

        visitPlanRepository.delete(visitPlan);
    }

    @Transactional(readOnly = true)
    public String getPlanStatus(Integer visitId) {
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null) {
            throw new ApiException("Visit not found");
        }
        if ("CANCELLED".equals(visit.getStatus())) {
            return "NOT_STARTED";
        }
        for (VisitPlan plan : visitPlanRepository.findAllByVisit_Id(visitId)) {
            if ("GROUP".equals(plan.getPlanKind()) && plan.getPlanItems() != null && !plan.getPlanItems().isEmpty()) {
                for (PlanItem item : plan.getPlanItems()) {
                    if (item.getStartsAt() == null || item.getEndsAt() == null || !item.getEndsAt().isAfter(item.getStartsAt())) {
                        return "NOT_STARTED";
                    }
                }
                if (Boolean.TRUE.equals(plan.getIsActive()) && plan.getPlanApproval() != null && "APPROVED".equals(plan.getPlanApproval().getStatus())) {
                    return "APPROVED";
                }
                return "GENERATED";
            }
        }
        return "NOT_STARTED";
    }

    private VisitPlanDtoOut convertVisitPlanToDtoOut(VisitPlan visitPlan) {
        Integer ownerVisitMemberId = null;
        String ownerName = null;

        if (visitPlan.getOwnerVisitMember() != null) {
            ownerVisitMemberId = visitPlan.getOwnerVisitMember().getId();
            ownerName = visitPlan.getOwnerVisitMember().getVisitorProfile().getName();
        }

        return new VisitPlanDtoOut(visitPlan.getId(), visitPlan.getVisit().getId(), visitPlan.getPlanKind(), ownerVisitMemberId, ownerName, visitPlan.getIsActive(), visitPlan.getCreatedAt());
    }

    private PlanItemDtoOut convertPlanItemToDtoOut(PlanItem planItem) {
        Integer rideId = null;
        String rideName = null;
        Integer placeId = null;
        String placeName = null;

        if (planItem.getRide() != null) {
            rideId = planItem.getRide().getId();
            rideName = planItem.getRide().getName();
        }

        if (planItem.getPlace() != null) {
            placeId = planItem.getPlace().getId();
            placeName = planItem.getPlace().getName();
        }

        return new PlanItemDtoOut(planItem.getId(), planItem.getActivityKind(), planItem.getTitle(), rideId, rideName, placeId, placeName, planItem.getStartsAt(), planItem.getEndsAt(), planItem.getSortOrder(), planItem.getPriority(), planItem.getPinned(), planItem.getParticipantVisitMemberIds(), planItem.getSubgroupKey());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void preparePlanning(Integer accountId, Integer visitId) {
        if (accountId == null || accountId <= 0) {
            throw new ApiException("A valid account ID is required");
        }

        if (visitId == null || visitId <= 0) {
            throw new ApiException("A valid visit ID is required");
        }

        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can start planning");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Planning can only start while the visit is in PLANNING status");
        }

        if (!paymentOrderRepository.existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(visitId, "PAID")) {
            throw new ApiException("A verified payment is required before planning");
        }

        List<VisitMember> goingMembers = visitMemberRepository.findAllByVisit_IdAndRsvpStatus(visitId, "GOING");

        if (goingMembers.isEmpty()) {
            throw new ApiException("The visit must have at least one GOING member");
        }

        if (!Boolean.TRUE.equals(visit.getMembersLocked())) {
            visit.setMembersLocked(true);
            visitRepository.save(visit);
        }

    }

    @Transactional(readOnly = true)
    public VisitPlanDetailsDtoOut getMyIndividualVisitPlan(Integer accountId, Integer visitId) {
        if (accountId == null || accountId <= 0) {
            throw new ApiException("A valid account ID is required");
        }

        if (visitId == null || visitId <= 0) {
            throw new ApiException("A valid visit ID is required");
        }

        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (visitMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanByVisit_IdAndOwnerVisitMember_IdAndPlanKind(visitId, visitMember.getId(), "INDIVIDUAL");

        if (visitPlan == null) {
            throw new ApiException("Your individual plan has not been created yet");
        }

        return getVisitPlanDetails(accountId, visitPlan.getId());
    }

    @Transactional(readOnly = true)
    public VisitPlanDetailsDtoOut getApprovedGroupVisitPlan(Integer accountId, Integer visitId) {
        if (accountId == null || accountId <= 0) {
            throw new ApiException("A valid account ID is required");
        }

        if (visitId == null || visitId <= 0) {
            throw new ApiException("A valid visit ID is required");
        }

        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

            if (visitMember == null) {
                throw new ApiException("You are not a member of this visit");
            }
        }

        List<VisitPlan> groupPlans = visitPlanRepository.findAllByVisit_IdAndPlanKind(visitId, "GROUP");
        VisitPlan approvedGroupPlan = null;

        for (VisitPlan groupPlan : groupPlans) {
            if (Boolean.TRUE.equals(groupPlan.getIsActive()) && groupPlan.getPlanApproval() != null && "APPROVED".equals(groupPlan.getPlanApproval().getStatus())) {
                if (approvedGroupPlan != null) {
                    throw new ApiException("Multiple active approved group plans were found");
                }

                approvedGroupPlan = groupPlan;
            }
        }

        if (approvedGroupPlan == null) {
            throw new ApiException("No approved group plan is available yet");
        }

        return getVisitPlanDetails(accountId, approvedGroupPlan.getId());
    }

    @Transactional(readOnly = true)
    public VisitPlanDetailsDtoOut getPendingGroupVisitPlan(Integer accountId, Integer visitId) {
        if (accountId == null || accountId <= 0) {
            throw new ApiException("A valid account ID is required");
        }

        if (visitId == null || visitId <= 0) {
            throw new ApiException("A valid visit ID is required");
        }

        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can preview the pending group plan");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Initial plan preview requires a visit in PLANNING status");
        }

        List<VisitPlan> groupPlans = visitPlanRepository.findAllByVisit_IdAndPlanKind(visitId, "GROUP");
        VisitPlan pendingGroupPlan = null;

        for (VisitPlan groupPlan : groupPlans) {
            if (!Boolean.TRUE.equals(groupPlan.getIsActive()) && (groupPlan.getPlanApproval() == null || "PENDING".equals(groupPlan.getPlanApproval().getStatus()))) {
                if (groupPlan.getPlanItems() == null || groupPlan.getPlanItems().isEmpty()) {
                    continue;
                }

                if (pendingGroupPlan != null) {
                    throw new ApiException("Multiple pending group plans were found");
                }

                pendingGroupPlan = groupPlan;
            }
        }

        if (pendingGroupPlan == null) {
            throw new ApiException("No group plan is available for preview");
        }

        return getVisitPlanDetails(accountId, pendingGroupPlan.getId());
    }

    @Transactional(readOnly = true)
    public List<PlanItemDtoOut> getVisitMeetingPoints(Integer accountId, Integer visitId) {
        VisitPlanDetailsDtoOut approvedGroupPlan = getApprovedGroupVisitPlan(accountId, visitId);
        List<PlanItemDtoOut> meetingPoints = new ArrayList<>();

        for (PlanItemDtoOut planItem : approvedGroupPlan.getPlanItems()) {
            if ("MEETING_POINT".equals(planItem.getActivityKind())) {
                meetingPoints.add(planItem);
            }
        }

        return meetingPoints;
    }

    private HashSet<Integer> parseActivityParticipantIds(String participantVisitMemberIds) {
        if (participantVisitMemberIds == null || participantVisitMemberIds.isBlank()) {
            throw new ApiException("Activity participant IDs are missing");
        }

        JsonNode participantIdsNode;

        try {
            participantIdsNode = participantJsonMapper.readTree(participantVisitMemberIds);
        } catch (JacksonException exception) {
            throw new ApiException("Activity participant IDs contain invalid JSON");
        }

        if (participantIdsNode == null || !participantIdsNode.isArray()) {
            throw new ApiException("Activity participant IDs must be a JSON array");
        }

        HashSet<Integer> participantIds = new HashSet<>();

        for (JsonNode participantIdNode : participantIdsNode) {
            if (!participantIdNode.isIntegralNumber() || !participantIdNode.canConvertToInt()) {
                throw new ApiException("Activity participant IDs must be integers");
            }

            int participantId = participantIdNode.intValue();

            if (participantId <= 0) {
                throw new ApiException("Activity participant IDs must be positive");
            }

            if (!participantIds.add(participantId)) {
                throw new ApiException("Activity participant IDs must not contain duplicates");
            }
        }

        return participantIds;
    }

    @Transactional(readOnly = true)
    public HashSet<Integer> getMyPlannedRideIds(Integer accountId, Integer visitId) {
        VisitPlanDetailsDtoOut approvedGroupPlan = getApprovedGroupVisitPlan(accountId, visitId);
        Visit visit = visitRepository.findVisitById(visitId);

        if (!"READY".equals(visit.getStatus()) && !"IN_PROGRESS".equals(visit.getStatus()) && !"COMPLETED".equals(visit.getStatus())) {
            throw new ApiException("Progress is not available in the current visit status");
        }

        VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (visitMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        HashSet<Integer> visitMemberIds = new HashSet<>();

        for (VisitMember member : visitMemberRepository.findAllByVisit_Id(visitId)) {
            visitMemberIds.add(member.getId());
        }

        HashSet<Integer> plannedRideIds = new HashSet<>();

        for (PlanItemDtoOut planItem : approvedGroupPlan.getPlanItems()) {
            if (!"RIDE".equals(planItem.getActivityKind())) {
                continue;
            }

            if (planItem.getRideId() == null) {
                throw new ApiException("A ride activity must reference a ride");
            }

            HashSet<Integer> participantIds = parseActivityParticipantIds(planItem.getParticipantVisitMemberIds());

            if (!visitMemberIds.containsAll(participantIds)) {
                throw new ApiException("An activity contains participants from outside this visit");
            }

            if (participantIds.contains(visitMember.getId())) {
                plannedRideIds.add(planItem.getRideId());
            }
        }

        return plannedRideIds;
    }

    @Transactional(readOnly = true)
    public NextActivityDtoOut getMyNextActivity(Integer accountId, Integer visitId) {
        VisitPlanDetailsDtoOut approvedGroupPlan = getApprovedGroupVisitPlan(accountId, visitId);
        Visit visit = visitRepository.findVisitById(visitId);

        if (!"IN_PROGRESS".equals(visit.getStatus())) {
            throw new ApiException("The visit must be in progress to view the next activity");
        }

        VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (visitMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        if (!"GOING".equals(visitMember.getRsvpStatus())) {
            throw new ApiException("Only GOING members can view their next activity");
        }

        VisitPlan groupPlan = visitPlanRepository.findVisitPlanById(approvedGroupPlan.getPlan().getId());

        if (groupPlan == null) {
            throw new ApiException("Group plan not found");
        }

        List<PlanItem> planItems = new ArrayList<>(groupPlan.getPlanItems());
        HashSet<Integer> visitMemberIds = new HashSet<>();

        for (VisitMember member : visitMemberRepository.findAllByVisit_Id(visitId)) {
            visitMemberIds.add(member.getId());
        }

        for (PlanItem planItem : planItems) {
            if (planItem.getStartsAt() == null || planItem.getEndsAt() == null || !planItem.getEndsAt().isAfter(planItem.getStartsAt())) {
                throw new ApiException("The group plan contains invalid activity times");
            }
        }

        planItems.sort(Comparator.comparing(PlanItem::getStartsAt).thenComparing(PlanItem::getSortOrder).thenComparing(PlanItem::getId));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));

        for (PlanItem planItem : planItems) {
            if (!planItem.getEndsAt().isAfter(now)) {
                continue;
            }

            HashSet<Integer> participantIds = parseActivityParticipantIds(planItem.getParticipantVisitMemberIds());

            if (!visitMemberIds.containsAll(participantIds)) {
                throw new ApiException("An activity contains participants from outside this visit");
            }

            if (!participantIds.contains(visitMember.getId())) {
                continue;
            }

            if ("RIDE".equals(planItem.getActivityKind())) {
                if (planItem.getRide() == null) {
                    throw new ApiException("A ride activity must reference a ride");
                }

                if (!"OPEN".equals(planItem.getRide().getStatus())) {
                    continue;
                }
            }

            String activityStatus = "UPCOMING";

            if (!planItem.getStartsAt().isAfter(now)) {
                activityStatus = "CURRENT";
            }

            return new NextActivityDtoOut(visitId, activityStatus, convertPlanItemToDtoOut(planItem));
        }

        return new NextActivityDtoOut(visitId, "NONE", null);
    }

    @Transactional(readOnly = true)
    public MySubgroupDtoOut getMyCurrentSubgroup(Integer accountId, Integer visitId) {
        NextActivityDtoOut nextActivity = getMyNextActivity(accountId, visitId);

        if (!"CURRENT".equals(nextActivity.getActivityStatus())) {
            return new MySubgroupDtoOut(visitId, "NONE", null, null, new ArrayList<>());
        }

        PlanItemDtoOut currentActivity = nextActivity.getActivity();
        HashSet<Integer> participantIds = parseActivityParticipantIds(currentActivity.getParticipantVisitMemberIds());
        List<VisitMember> visitMembers = visitMemberRepository.findAllByVisit_Id(visitId);
        List<SubgroupMemberDtoOut> subgroupMembers = new ArrayList<>();

        visitMembers.sort(Comparator.comparing(VisitMember::getId));

        for (VisitMember visitMember : visitMembers) {
            if (participantIds.contains(visitMember.getId())) {
                subgroupMembers.add(new SubgroupMemberDtoOut(visitMember.getId(), visitMember.getVisitorProfile().getName()));
            }
        }

        return new MySubgroupDtoOut(visitId, "CURRENT", currentActivity.getSubgroupKey(), currentActivity, subgroupMembers);
    }


}
