package com.example.sikka.Service;

import com.example.sikka.Model.*;
import com.example.sikka.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationEventService {

    private final NotificationRepository notificationRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final VisitRepository visitRepository;
    private final VisitPlanRepository visitPlanRepository;
    private final PlanItemRepository planItemRepository;
    private final VisitMealBreakRepository visitMealBreakRepository;
    private final ActivitySessionRepository activitySessionRepository;
    private final VisitRecordRepository visitRecordRepository;
    private final N8nNotificationService n8nNotificationService;
    private final WhatsAppService whatsAppService;
    private final PrivacyConsentService privacyConsentService;
    private final PlatformTransactionManager transactionManager;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public void notifyPlanningReminder(VisitMember member) {
        boolean attendance = !"GOING".equals(member.getRsvpStatus());
        String message = attendance ? "أكد حضورك في الزيارة عشان نرتب طلعتكم." : "كمل اختياراتك عشان نرتب خطة طلعتكم.";
        long slot = java.time.Instant.now().getEpochSecond() / 600;
        Notification notice = save(member.getVisitorProfile().getAccount(), member.getVisit(), "PLANNING_REMINDER", attendance ? "تذكير بتأكيد حضورك" : "تذكير بإكمال اختياراتك", message, "PLANNING_REMINDER:" + member.getId() + ":" + attendance + ":" + slot, true, null);
        Account recipient = member.getVisitorProfile().getAccount();
        // WhatsApp reminders are disabled by the approved delivery policy.
    }

    // FRIENDS only: together requires RSVP, split also requires planning answers.
    public void notifyMemberRsvp(VisitMember member, boolean firstResponse) {
        notifyFriendsReady(member.getVisit());
    }

    public void notifyFriendsReady(Visit visit) {
        if (!"FRIENDS".equals(visit.getTripType())) return;
        List<VisitMember> members = visitMemberRepository.findAllByVisit_Id(visit.getId());
        if (members.isEmpty()) return;
        boolean split = "SPLIT".equals(visit.getTogetherMode());
        for (VisitMember candidate : members) {
            String rsvp = Objects.toString(candidate.getRsvpStatus(), "INVITED");
            if (!"GOING".equals(rsvp) && !"NOT_GOING".equals(rsvp)) return;
            if (split && "GOING".equals(rsvp)
                    && !"ANSWERED".equals(candidate.getAnswerStatus())
                    && !"SKIPPED".equals(candidate.getAnswerStatus())) return;
        }
        save(visit.getOrganizer(), visit, "ALL_MEMBERS_RESPONDED", "الكل جاهز لسكتكم! 🎢",
                split ? "كل الحاضرين أكدوا حضورهم وكملوا اختيارات الألعاب."
                      : "كل المدعوين ردوا على الدعوة وسكتكم جاهزة للتجهيز.",
                "ALL_MEMBERS_RESPONDED:" + visit.getId(), true, null);
    }

    // 3: generated means ready to review, not approved or ready to start.
    public void notifyPlanGenerated(PlanHistory history) {
        // Generated plans are already visible; no notification needed.
    }

    // 4: initial approval; replan requests have their own proposal key.
    public void notifyPlanApprovalRequested(PlanApproval approval) {
        // Initial approval occurs directly in the plan view.
    }

    // 5: only confirmed participants, with managed family members routed to organizer.
    public void notifyPlanApprovalResult(PlanApproval approval) {
        if (!"APPROVED".equals(approval.getStatus())) {
            return;
        }
        Visit visit = approval.getVisitPlan().getVisit();
        boolean approved = "APPROVED".equals(approval.getStatus());
        for (Account recipient : recipients(visit, null).values()) {
            if (recipient.getId().equals(visit.getOrganizer().getId())) continue;
            save(recipient, visit, "PLAN_APPROVAL_RESULT", approved ? "تم اعتماد الخطة" : "لم يتم اعتماد الخطة",
                    "الخطة رقم " + approval.getVisitPlan().getId()
                            + (approved ? " تم اعتمادها. راجع تفاصيلها في التطبيق." : " تم رفضها. انتظر تحديث المنظّم."),
                    "PLAN_APPROVAL_RESULT:" + approval.getId() + ":" + approval.getStatus(), true, null);
        }
    }

    // 6: call after saving proposalBaseSnapshot, before returning the proposal.
    public void notifyReplanRequested(VisitPlan plan, String proposalKey) {
        PlanApproval approval = plan.getPlanApproval();
        if (approval == null || !"PENDING".equals(approval.getStatus()) || plan.getProposedItems() == null) {
            return;
        }
        save(approval.getRequiredApproverAccount(), plan.getVisit(), "REPLAN_APPROVAL_REQUEST",
                "اقتراح تعديل الخطة بانتظار موافقتك",
                "يوجد اقتراح لتعديل الخطة رقم " + plan.getId()
                        + ". راجع الاقتراح في التطبيق؛ لم يتم تطبيقه تلقائيًا.",
                proposalEventKey("REPLAN_APPROVAL_REQUEST", plan.getId(), proposalKey), false, null);
    }

    // 7: pass affected IDs before the caller clears the proposal fields.
    public void notifyReplanResult(VisitPlan plan, String proposalKey, boolean approved,
                                  List<Integer> affectedMemberIds) {
        if (!approved) return;
        for (Account recipient : recipients(plan.getVisit(), new HashSet<>(affectedMemberIds)).values()) {
            save(recipient, plan.getVisit(), "REPLAN_RESULT", approved ? "تم تعديل الخطة" : "تم رفض تعديل الخطة",
                    "اقتراح تعديل الخطة رقم " + plan.getId()
                            + (approved ? " تم اعتماده. راجع مواعيدك الجديدة." : " تم رفضه؛ بقيت الخطة السابقة كما هي."),
                    proposalEventKey("REPLAN_RESULT", plan.getId(), proposalKey), false, null);
        }
    }

    // Visit completion is not emailed until the attendee submits both ratings.
    public void notifyVisitStatus(Visit visit) {
        // The visit recap is intentionally sent only after rating submission.
    }

    public void notifyVisitRatingSubmitted(VisitMember member) {
        Visit visit = member.getVisit();
        if (!"COMPLETED".equals(visit.getStatus()) || member.getVisitRating() == null || member.getSikkaRating() == null || !"GOING".equals(member.getRsvpStatus())) return;
        Account account = recipient(member);
        if (account == null) return;
        List<VisitRecord> playedRides = visitRecordRepository.findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(member.getId()).stream()
                .filter(record -> "COMPLETED".equals(record.getStatus()) && record.getRide() != null)
                .sorted(Comparator.comparing(VisitRecord::getCompletedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        StringBuilder message = new StringBuilder("يا سلام على سكتكم! 🎢\\nزيارة: ").append(visit.getName()).append("\\nالتاريخ: ").append(visit.getVisitDate());
        if (visit.getStartedAt() != null && visit.getCompletedAt() != null && !visit.getCompletedAt().isBefore(visit.getStartedAt())) {
            long duration = java.time.Duration.between(visit.getStartedAt(), visit.getCompletedAt()).toMinutes();
            message.append("\\nمدة الزيارة الفعلية: ").append(duration / 60).append(" ساعة و").append(duration % 60).append(" دقيقة");
        }
        message.append("\\nالألعاب المكتملة: ").append(playedRides.size());
        for (VisitRecord record : playedRides) {
            message.append("\\n- ").append(record.getRide().getName());
            if (record.getCompletedAt() != null) message.append(" — ").append(record.getCompletedAt().toLocalTime());
        }
        emailOnly(account, visit, "VISIT_COMPLETED", "يا سلام على سكتكم! 🚂🎉", message.toString(), "VISIT_COMPLETED:" + visit.getId() + ":" + member.getId());
    }

    // 10 + 11: for the future VERIFIED payment callback only; never trust client status.
    public void notifyPaymentResult(PaymentOrder payment) {
        if ((! "PAID".equals(payment.getStatus()) && !"FAILED".equals(payment.getStatus()))
                || ("PAID".equals(payment.getStatus()) && payment.getVerifiedAt() == null)) {
            return;
        }
        boolean paid = "PAID".equals(payment.getStatus());
        String type = paid ? "PAYMENT_PAID" : "PAYMENT_FAILED";
        emailOnly(payment.getBuyerAccount(), payment.getVisit(), type,
                paid ? "تم الدفع بنجاح" : "لم تكتمل عملية الدفع",
                "طلب الدفع رقم " + payment.getId() + " بمبلغ " + payment.getAmountSar()
                        + (paid ? " ريال تم تأكيده بنجاح." : " ريال لم يكتمل. راجع الطلب في التطبيق قبل المحاولة مجددًا."),
                type + ":" + payment.getId());
    }

    // Reuse the existing IN_APP row and its recipient; do not recreate delay/ride logic.
    public void forwardCriticalNotification(Notification notification) {
        // All during-visit alerts are in-app only.
    }

    // 12, 13, 14: scheduled reminders, re-read under the visit lock each time.
    public void notifyDueReminders(Integer visitId) {
        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);
        if (visit == null || (!"READY".equals(visit.getStatus()) && !"IN_PROGRESS".equals(visit.getStatus()))) {
            return;
        }
        LocalDateTime now = now();
        if ("READY".equals(visit.getStatus()) && visit.getVisitDate() != null && visit.getPlannedStartTime() != null) {
            LocalDateTime start = visit.getVisitDate().atTime(visit.getPlannedStartTime());
            // Check two short reminder windows, each with a distinct key for this arrival time.
            for (int hoursBefore : new int[]{24, 2}) {
                LocalDateTime due = start.minusHours(hoursBefore);
                if (now.isBefore(due) || !now.isBefore(due.plusMinutes(2))) {
                    continue;
                }
                for (Account target : recipients(visit, null).values()) {
                    emailOnly(target, visit, "VISIT_REMINDER", "تذكير بموعد زيارتك",
                            "باقي " + hoursBefore + " ساعة على زيارتك رقم " + visitId
                                    + "، موعد الوصول " + start.toLocalDate() + " الساعة "
                                    + start.toLocalTime() + " بتوقيت الرياض.",
                            "VISIT_REMINDER:" + visitId + ":" + start + ":" + hoursBefore);
                }
            }
        }
        if (!"IN_PROGRESS".equals(visit.getStatus())) {
            return;
        }
        for (VisitMealBreak meal : visitMealBreakRepository.findVisitMealBreaksByVisitId(visitId)) {
            PlanItem item = meal.getPlanItem();
            if (meal.getPlace() != null || item == null || item.getPlace() != null
                    || !Set.of("MEAL", "SNACK", "DEFERRED").contains(Objects.toString(meal.getMealChoice(), ""))
                    || !"GOING".equals(meal.getVisitMember().getRsvpStatus())
                    || !item.getVisitPlan().getVisit().getId().equals(visitId)
                    || !Boolean.TRUE.equals(item.getVisitPlan().getIsActive())
                    || !"GROUP".equals(item.getVisitPlan().getPlanKind())
                    || !"SCHEDULED".equals(item.getVisitPlan().getTimingMode())
                    || item.getStartsAt() == null || !item.getStartsAt().isAfter(now)
                    || !participantIds(item.getParticipantVisitMemberIds()).contains(meal.getVisitMember().getId())
                    || activityAlreadyStarted(meal.getVisitMember(), item)
                    || !penultimateRideCompletedBeforeMeal(meal.getVisitMember(), item)) {
                continue;
            }
            save(recipient(meal.getVisitMember()), visit, "MEAL_RESTAURANT_REMINDER", "وين ناكل بعد شوي؟ 🍔",
                    "بعد اللعبة الجاية عندكم استراحة أكل. اتفقوا على المطعم وأنتم بالطريق، وتقدرون تختارونه بعد اللعبة.",
                    "MEAL_RESTAURANT_REMINDER:" + item.getId() + ":" + item.getStartsAt(), false, item);
        }
    }

    private boolean penultimateRideCompletedBeforeMeal(VisitMember member, PlanItem mealItem) {
        if (mealItem.getSortOrder() == null || mealItem.getVisitPlan() == null) return false;
        List<PlanItem> precedingRides = planItemRepository.findAllByVisitPlan_Id(mealItem.getVisitPlan().getId())
                .stream()
                .filter(item -> "RIDE".equals(item.getActivityKind()) && item.getSortOrder() != null
                        && item.getSortOrder() < mealItem.getSortOrder()
                        && Objects.equals(item.getSubgroupKey(), mealItem.getSubgroupKey())
                        && participantIds(item.getParticipantVisitMemberIds()).contains(member.getId()))
                .sorted(Comparator.comparing(PlanItem::getSortOrder))
                .toList();
        if (precedingRides.size() < 2) return false;
        PlanItem prior = precedingRides.get(precedingRides.size() - 2);
        PlanItem next = precedingRides.get(precedingRides.size() - 1);
        if (activityAlreadyStarted(member, next)) return false;
        return visitRecordRepository.findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(member.getId())
                .stream().anyMatch(record -> "COMPLETED".equals(record.getStatus())
                        && record.getPlanItem() != null && prior.getId().equals(record.getPlanItem().getId()));
    }

    private boolean activityAlreadyStarted(VisitMember member, PlanItem item) {
        for (ActivitySession session : activitySessionRepository
                .findAllByVisitMember_IdAndPlanItem_IdOrderByIdAsc(member.getId(), item.getId())) {
            if (!"WAITING".equals(session.getStatus()) || session.getEnteredAreaAt() != null
                    || session.getQueueStartedAt() != null) {
                return true;
            }
        }
        return visitRecordRepository.findAllByVisitMember_IdOrderByCreatedAtDescIdDesc(member.getId())
                .stream().anyMatch(record -> record.getPlanItem() != null
                        && item.getId().equals(record.getPlanItem().getId()));
    }

    private Set<Integer> participantIds(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptySet();
        }
        try {
            JsonNode values = jsonMapper.readTree(json);
            if (values == null || !values.isArray()) {
                return Collections.emptySet();
            }
            Set<Integer> ids = new HashSet<>();
            for (JsonNode value : values) {
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() <= 0) {
                    return Collections.emptySet();
                }
                ids.add(value.asInt());
            }
            return ids;
        } catch (RuntimeException exception) {
            log.warn("Skipping notification with invalid participant IDs");
            return Collections.emptySet();
        }
    }

    private Account recipient(VisitMember member) {
        if (member.getVisitorProfile() != null) {
            return member.getVisitorProfile().getAccount();
        }
        Visit visit = member.getVisit();
        if (member.getFamilyMember() != null
                && member.getFamilyMember().getOwnerAccount().getId().equals(visit.getOrganizer().getId())) {
            return visit.getOrganizer();
        }
        return "FAMILY".equals(visit.getTripType()) && member.getSnapshotAt() != null
                ? visit.getOrganizer() : null;
    }

    private Map<Integer, Account> recipients(Visit visit, Set<Integer> selectedIds) {
        Map<Integer, Account> result = new LinkedHashMap<>();
        for (VisitMember member : visitMemberRepository.findAllByVisit_IdAndRsvpStatus(visit.getId(), "GOING")) {
            if (selectedIds != null && !selectedIds.contains(member.getId())) {
                continue;
            }
            Account account = recipient(member);
            if (account != null) {
                result.put(account.getId(), account);
            }
        }
        return result;
    }

    private String proposalEventKey(String type, Integer planId, String proposalKey) {
        if (proposalKey == null || proposalKey.isBlank()) {
            throw new IllegalArgumentException("A saved proposal key is required");
        }
        return type + ":" + planId + ":" + UUID.nameUUIDFromBytes(proposalKey.getBytes(StandardCharsets.UTF_8));
    }

    private Notification save(Account account, Visit visit, String type, String title,
                              String message, String eventKey, boolean email, PlanItem item) {
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            return null;
        }
        Notification existing = notificationRepository.findNotificationByAccount_IdAndChannelAndEventKey(
                account.getId(), "IN_APP", eventKey);
        if (existing != null) {
            return existing;
        }
        Notification notification = new Notification();
        notification.setAccount(account);
        notification.setVisit(visit);
        notification.setPlanItem(item);
        notification.setChannel("IN_APP");
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        notification.setStatus("SENT");
        notification.setSentAt(now());
        notificationRepository.save(notification);
        if (email) {
            queueEmail(notification);
        }
        return notification;
    }

    // Email-only events never create a visible IN_APP inbox entry.
    private void emailOnly(Account account, Visit visit, String type, String title, String message, String eventKey) {
        if (account == null || !"ACTIVE".equals(account.getStatus())) return;
        Notification source = new Notification();
        source.setAccount(account);
        source.setVisit(visit);
        source.setType(type);
        source.setTitle(title);
        source.setMessage(message);
        source.setEventKey(eventKey);
        queueEmail(source);
    }

    private void queueEmail(Notification source) {
        Account account = source.getAccount();
        if (account == null || !"ACTIVE".equals(account.getStatus()) || account.getEmail() == null
                || account.getEmail().isBlank()) {
            return;
        }
        String key = source.getEventKey() == null ? "NOTIFICATION:" + source.getId() : source.getEventKey();
        if (notificationRepository.findNotificationByAccount_IdAndChannelAndEventKey(
                account.getId(), "EMAIL", key) != null) {
            return;
        }
        Notification email = new Notification();
        email.setAccount(account);
        email.setVisit(source.getVisit());
        email.setActivitySession(source.getActivitySession());
        email.setPlanItem(source.getPlanItem());
        email.setChannel("EMAIL");
        email.setType(source.getType());
        email.setTitle(source.getTitle());
        email.setMessage(source.getMessage());
        email.setEventKey(key);
        email.setStatus("PENDING");
        notificationRepository.save(email);
        Integer emailId = email.getId();
        String address = account.getEmail();
        String type = email.getType();
        String title = email.getTitle();
        String message = email.getMessage();
        // Include recipient so n8n can later deduplicate the same event per recipient.
        String deliveryKey = "EMAIL:" + account.getId() + ":" + key;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                boolean sent = n8nNotificationService.sendNotification(deliveryKey, type, address, title, message);
                try {
                    TransactionTemplate template = new TransactionTemplate(transactionManager);
                    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    template.executeWithoutResult(transaction -> {
                        Notification saved = notificationRepository.findNotificationById(emailId);
                        if (saved != null && "PENDING".equals(saved.getStatus())) {
                            saved.setStatus(sent ? "SENT" : "FAILED");
                            saved.setSentAt(sent ? now() : null);
                            notificationRepository.save(saved);
                        }
                    });
                } catch (RuntimeException exception) {
                    log.warn("Could not record delivery result for notification {}", emailId);
                }
            }
        });
    }

    private void queueWhatsApp(Notification source) {
        String key = source.getEventKey();
        Account account = source.getAccount();
        if (notificationRepository.findNotificationByAccount_IdAndChannelAndEventKey(account.getId(), "WHATSAPP", key) != null) return;
        Notification delivery = new Notification();
        delivery.setAccount(account);
        delivery.setVisit(source.getVisit());
        delivery.setChannel("WHATSAPP");
        delivery.setType(source.getType());
        delivery.setTitle(source.getTitle());
        delivery.setMessage(source.getMessage());
        delivery.setEventKey(key);
        delivery.setStatus("PENDING");
        notificationRepository.save(delivery);
        Integer deliveryId = delivery.getId();
        String phone = account.getPhone();
        String message = source.getMessage();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                boolean sent = whatsAppService.sendMessage(phone, message);
                try {
                    TransactionTemplate template = new TransactionTemplate(transactionManager);
                    template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    template.executeWithoutResult(transaction -> {
                        Notification saved = notificationRepository.findNotificationById(deliveryId);
                        if (saved != null && "PENDING".equals(saved.getStatus())) {
                            saved.setStatus(sent ? "SENT" : "FAILED");
                            saved.setSentAt(sent ? now() : null);
                            notificationRepository.save(saved);
                        }
                    });
                } catch (RuntimeException exception) {
                    log.warn("Could not record WhatsApp delivery result for notification {}", deliveryId);
                }
            }
        });
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
    }
}
