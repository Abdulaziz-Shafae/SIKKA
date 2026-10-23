package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.NotificationDtoIn;
import com.example.sikka.DtoIn.RespondDelayDtoIn;
import com.example.sikka.DtoIn.SuggestReplanDtoIn;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import com.example.sikka.DtoOut.NotificationDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.ActivitySession;
import com.example.sikka.Model.Notification;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.ActivitySessionRepository;
import com.example.sikka.Repository.NotificationRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final VisitPlanService visitPlanService;
    private final PlatformTransactionManager transactionManager;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final ActivitySessionRepository activitySessionRepository;
    private final JsonMapper responseJsonMapper = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    @Transactional(readOnly = true)
    public List<NotificationDtoOut> getMyNotifications(Integer accountId) {

        validateActiveAccount(accountId);

        List<Notification> notifications = notificationRepository.findAllByAccount_IdAndChannelAndStatusOrderByCreatedAtDescIdDesc(accountId, "IN_APP", "SENT");

        if (notifications.isEmpty()) {
            throw new ApiException("No notifications found");
        }

        List<NotificationDtoOut> results = new ArrayList<>();

        for (Notification notification : notifications) {
            results.add(convertNotificationToDtoOut(notification));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public NotificationDtoOut getNotificationDetails(Integer accountId, Integer notificationId) {

        validateActiveAccount(accountId);

        Notification notification = getRequiredInAppNotification(accountId, notificationId, false);

        return convertNotificationToDtoOut(notification);
    }

    @Transactional
    public void deleteNotification(Integer accountId, Integer notificationId) {

        validateActiveAccount(accountId);

        Notification notification = getRequiredInAppNotification(accountId, notificationId, true);

        if (notification.getEventKey() != null || notification.getActionType() != null) {
            throw new ApiException("Event notifications must be preserved; mark the notification as read instead");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void sendNotification(Integer accountId, Integer visitId, String channel, String type, String title, String message) {

        NotificationDtoIn dto = new NotificationDtoIn(visitId, channel, type, title, message, null);

        createNotification(accountId, dto);
    }

    @Transactional
    public void createNotification(Integer recipientAccountId, NotificationDtoIn dto) {

        Account account = validateActiveAccount(recipientAccountId);

        validateNotificationContent(dto);

        Visit visit = null;

        if (dto.getVisitId() != null) {

            if (dto.getVisitId() <= 0) {
                throw new ApiException("Visit ID must be positive");
            }

            visit = visitRepository.findVisitById(dto.getVisitId());

            if (visit == null) {
                throw new ApiException("Visit not found");
            }

            if (!isRecipientOfVisit(recipientAccountId, visit)) {
                throw new ApiException("The recipient does not belong to this visit");
            }
        }

        Notification notification = new Notification();
        notification.setAccount(account);
        notification.setVisit(visit);
        notification.setChannel(dto.getChannel());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setScheduledAt(dto.getScheduledAt());

        setInitialDeliveryStatus(notification);

        notificationRepository.save(notification);
    }

    @Transactional
    public NotificationDtoOut createActivityDelayNotification(Integer recipientAccountId, Integer activitySessionId, String title, String message) {

        Account account = validateActiveAccount(recipientAccountId);

        if (activitySessionId == null || activitySessionId <= 0) {
            throw new ApiException("A positive activity session ID is required");
        }

        NotificationDtoIn dto = new NotificationDtoIn(null, "IN_APP", "ACTIVITY_DELAY", title, message, null);

        validateNotificationContent(dto);

        ActivitySession session = activitySessionRepository.findActivitySessionByIdForUpdate(activitySessionId);

        if (session == null) {
            throw new ApiException("Activity session not found");
        }

        VisitMember member = session.getVisitMember();
        Visit visit = member.getVisit();

        validateSessionRecipient(recipientAccountId, session);

        if (!session.getPlanItem().getVisitPlan().getVisit().getId().equals(visit.getId())) {
            throw new ApiException("The activity does not belong to this visit");
        }

        String eventKey = "ACTIVITY_DELAY:" + visit.getId() + ":" + member.getId() + ":" + session.getPlanItem().getId();

        Notification existingNotification = notificationRepository.findNotificationByAccount_IdAndChannelAndEventKey(recipientAccountId, "IN_APP", eventKey);

        if (existingNotification != null) {

            if (existingNotification.getActivitySession() == null || !existingNotification.getActivitySession().getId().equals(session.getId())) {
                throw new ApiException("The delay event is already linked to another activity session");
            }

            return convertNotificationToDtoOut(existingNotification);
        }

        if (!"IN_PROGRESS".equals(visit.getStatus())) {
            throw new ApiException("Delay notifications require a visit in progress");
        }

        if (!"WAITING".equals(session.getStatus()) && !"ACTIVE".equals(session.getStatus())) {
            throw new ApiException("Delay notifications require a current activity session");
        }

        if (!Boolean.TRUE.equals(session.getPlanItem().getVisitPlan().getIsActive())) {
            throw new ApiException("Delay notifications require an active plan");
        }

        Notification notification = new Notification();
        notification.setAccount(account);
        notification.setVisit(visit);
        notification.setActivitySession(session);
        notification.setPlanItem(session.getPlanItem());
        notification.setChannel("IN_APP");
        notification.setType("ACTIVITY_DELAY");
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        notification.setActionType("RESPOND_DELAY");

        setInitialDeliveryStatus(notification);

        notificationRepository.save(notification);

        session.setLastAlertKey(eventKey);

        activitySessionRepository.save(session);

        return convertNotificationToDtoOut(notification);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public NotificationDtoOut respondToDelay(Integer accountId, Integer notificationId, RespondDelayDtoIn dto) {

        validateActiveAccount(accountId);

        if (dto == null || dto.getAdjustPlan() == null) {
            throw new ApiException("Adjust plan choice is required");
        }

        if (dto.getReason() != null && dto.getReason().length() > 500) {
            throw new ApiException("Delay reason must not exceed 500 characters");
        }


        String reason = dto.getReason() == null || dto.getReason().isBlank() ? null : dto.getReason().trim();
        Boolean adjustPlan = dto.getAdjustPlan();
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setTimeout(60);

        record DelayResponseStart(Integer visitId, NotificationDtoOut finished) {
        }

        DelayResponseStart start = template.execute(transaction -> {

            validateActiveAccount(accountId);
            Notification notification = getRequiredInAppNotification(accountId, notificationId, true);

            if (!"ACTIVITY_DELAY".equals(notification.getType()) || !"RESPOND_DELAY".equals(notification.getActionType())) {
                throw new ApiException("This notification does not accept a delay response");
            }

            if (notification.getRespondedAt() != null) {

                validateRepeatedDelayResponse(notification, reason, adjustPlan);

                return new DelayResponseStart(null, convertNotificationToDtoOut(notification));
            }

            if (notification.getActivitySession() == null || notification.getVisit() == null) {
                throw new ApiException("The delay notification is missing its activity reference");
            }

            ActivitySession session = activitySessionRepository.findActivitySessionByIdForUpdate(notification.getActivitySession().getId());

            if (session == null) {
                throw new ApiException("Activity session not found");
            }

            validateSessionRecipient(accountId, session);

            Visit visit = session.getVisitMember().getVisit();

            if (!notification.getVisit().getId().equals(visit.getId()) || !session.getPlanItem().getVisitPlan().getVisit().getId().equals(visit.getId())) {
                throw new ApiException("The delay notification does not match the activity visit");
            }

            if (notification.getPlanItem() != null && !notification.getPlanItem().getId().equals(session.getPlanItem().getId())) {
                throw new ApiException("The delay notification does not match the activity");
            }

            if (Boolean.TRUE.equals(adjustPlan)) {

                if (!"IN_PROGRESS".equals(visit.getStatus())) {
                    throw new ApiException("Replanning requires a visit in progress");
                }

                if (!Boolean.TRUE.equals(session.getPlanItem().getVisitPlan().getIsActive())) {
                    throw new ApiException("Replanning requires an active plan");
                }

                if (!"WAITING".equals(session.getStatus()) && !"ACTIVE".equals(session.getStatus())) {
                    throw new ApiException("This delay event is no longer current");
                }

                return new DelayResponseStart(visit.getId(), null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("reason", reason);
            response.put("adjustPlan", false);

            String responseJson;

            try {
                responseJson = responseJsonMapper.writeValueAsString(response);
            } catch (JacksonException exception) {
                throw new ApiException("The delay response could not be saved");
            }

            LocalDateTime respondedAt = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));

            notification.setResponse(responseJson);
            notification.setRespondedAt(respondedAt);

            session.setDelayReason(reason);
            session.setDelayRespondedAt(respondedAt);

            activitySessionRepository.save(session);
            notificationRepository.save(notification);

            return new DelayResponseStart(null, convertNotificationToDtoOut(notification));
        });

        if (start.finished() != null) {
            return start.finished();
        }

        SuggestReplanDtoIn replan = new SuggestReplanDtoIn();
        replan.setReason(reason == null ? "تعديل توقيت الأنشطة المتبقية بسبب التأخير في النشاط الحالي" : reason);
        replan.setSourceDelayNotificationId(notificationId);
        replan.setSourceDelayReason(reason);

        try {
            visitPlanService.suggestReplan(accountId, start.visitId(), replan);
        } catch (ApiException exception) {

            NotificationDtoOut repeated = template.execute(transaction -> {
                validateActiveAccount(accountId);
                Notification notification = getRequiredInAppNotification(accountId, notificationId, true);

                if (notification.getRespondedAt() == null) {
                    return null;
                }

                validateRepeatedDelayResponse(notification, reason, true);
                return convertNotificationToDtoOut(notification);
            });

            if (repeated != null) {
                return repeated;
            }

            throw exception;
        }

        return template.execute(transaction -> {
            validateActiveAccount(accountId);
            Notification notification = getRequiredInAppNotification(accountId, notificationId, true);
            validateRepeatedDelayResponse(notification, reason, true);
            return convertNotificationToDtoOut(notification);
        });
    }

    @Transactional
    public void markNotificationAsRead(Integer accountId, Integer notificationId) {

        validateActiveAccount(accountId);

        Notification notification = getRequiredInAppNotification(accountId, notificationId, true);

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now(ZoneId.of("Asia/Riyadh")));
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadNotificationsCount(Integer accountId) {

        validateActiveAccount(accountId);

        return notificationRepository.countByAccount_IdAndChannelAndStatusAndReadAtIsNull(accountId, "IN_APP", "SENT");
    }

    @Transactional
    public int publishDueInAppNotifications() {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));

        List<Notification> notifications = notificationRepository.findDueInAppNotificationsForUpdate(now, PageRequest.of(0, 100));

        int publishedCount = 0;

        for (Notification notification : notifications) {

            if (!"ACTIVE".equals(notification.getAccount().getStatus())) {
                notification.setStatus("FAILED");
                continue;
            }

            if (notification.getVisit() != null && !isRecipientOfVisit(notification.getAccount().getId(), notification.getVisit())) {
                notification.setStatus("FAILED");
                continue;
            }

            if ("ACTIVITY_DELAY".equals(notification.getType())) {

                ActivitySession session = notification.getActivitySession();

                if (session == null || notification.getVisit() == null || !"IN_PROGRESS".equals(notification.getVisit().getStatus())) {
                    notification.setStatus("FAILED");
                    continue;
                }

                if (!"WAITING".equals(session.getStatus()) && !"ACTIVE".equals(session.getStatus())) {
                    notification.setStatus("FAILED");
                    continue;
                }

                if (!Boolean.TRUE.equals(session.getPlanItem().getVisitPlan().getIsActive())) {
                    notification.setStatus("FAILED");
                    continue;
                }
            }

            notification.setStatus("SENT");
            notification.setSentAt(now);
            publishedCount++;
        }

        notificationRepository.saveAll(notifications);

        return publishedCount;
    }

    private Account validateActiveAccount(Integer accountId) {

        if (accountId == null || accountId <= 0) {
            throw new ApiException("A positive account ID is required");
        }

        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        return account;
    }

    private Notification getRequiredInAppNotification(Integer accountId, Integer notificationId, boolean lockForUpdate) {

        if (notificationId == null || notificationId <= 0) {
            throw new ApiException("A positive notification ID is required");
        }

        Notification notification = lockForUpdate ? notificationRepository.findNotificationByIdForUpdate(notificationId) : notificationRepository.findNotificationById(notificationId);

        if (notification == null) {
            throw new ApiException("Notification not found");
        }

        if (!notification.getAccount().getId().equals(accountId)) {
            throw new ApiException("This notification does not belong to you");
        }

        if (!"IN_APP".equals(notification.getChannel())) {
            throw new ApiException("This is not an in-app notification");
        }

        if (!"SENT".equals(notification.getStatus())) {
            throw new ApiException("This notification is not available yet");
        }

        return notification;
    }

    private void validateNotificationContent(NotificationDtoIn dto) {

        if (dto == null) {
            throw new ApiException("Notification details are required");
        }

        if (!"IN_APP".equals(dto.getChannel()) && !"EMAIL".equals(dto.getChannel()) && !"WHATSAPP".equals(dto.getChannel())) {
            throw new ApiException("Channel must be IN_APP, EMAIL or WHATSAPP");
        }

        if (dto.getType() == null || dto.getType().isBlank()) {
            throw new ApiException("Notification type is required");
        }

        if (dto.getType().length() > 60) {
            throw new ApiException("Notification type must not exceed 60 characters");
        }

        if (dto.getTitle() != null && dto.getTitle().length() > 180) {
            throw new ApiException("Title must not exceed 180 characters");
        }

        if (dto.getMessage() == null || dto.getMessage().isBlank()) {
            throw new ApiException("Notification message is required");
        }
    }

    private boolean isRecipientOfVisit(Integer accountId, Visit visit) {

        if (visit.getOrganizer().getId().equals(accountId)) {
            return true;
        }

        return visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visit.getId(), accountId) != null;
    }

    private void validateSessionRecipient(Integer accountId, ActivitySession session) {

        VisitMember member = session.getVisitMember();

        boolean isOwnSession = member.getVisitorProfile() != null && member.getVisitorProfile().getAccount().getId().equals(accountId);
        boolean isManagedFamilySession = member.getVisit().getOrganizer().getId().equals(accountId) && member.getVisitorProfile() == null && member.getFamilyMember() != null && member.getFamilyMember().getOwnerAccount().getId().equals(accountId);

        if (!isOwnSession && !isManagedFamilySession) {
            throw new ApiException("This activity session does not belong to the notification recipient");
        }
    }

    private void setInitialDeliveryStatus(Notification notification) {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Riyadh"));

        boolean isImmediate = notification.getScheduledAt() == null || !notification.getScheduledAt().isAfter(now);

        if ("IN_APP".equals(notification.getChannel()) && isImmediate) {
            notification.setStatus("SENT");
            notification.setSentAt(now);
        } else {
            notification.setStatus("PENDING");
            notification.setSentAt(null);
        }
    }

    private void validateRepeatedDelayResponse(Notification notification, String reason, Boolean adjustPlan) {

        if (notification.getResponse() == null) {
            throw new ApiException("The notification already has a response");
        }

        JsonNode response;

        try {
            response = responseJsonMapper.readTree(notification.getResponse());
        } catch (JacksonException exception) {
            throw new ApiException("The saved delay response is invalid");
        }

        if (response == null || !response.isObject() || !response.hasNonNull("adjustPlan") || !response.get("adjustPlan").isBoolean()) {
            throw new ApiException("The saved delay response is invalid");
        }

        String savedReason = null;

        if (response.hasNonNull("reason")) {

            if (!response.get("reason").isTextual()) {
                throw new ApiException("The saved delay response is invalid");
            }

            savedReason = response.get("reason").asText();
        }

        if (!Objects.equals(savedReason, reason) || response.get("adjustPlan").asBoolean() != adjustPlan.booleanValue()) {
            throw new ApiException("This notification has already been answered with different details");
        }
    }

    private NotificationDtoOut convertNotificationToDtoOut(Notification notification) {

        Integer visitId = notification.getVisit() == null ? null : notification.getVisit().getId();
        Integer activitySessionId = notification.getActivitySession() == null ? null : notification.getActivitySession().getId();
        Integer planItemId = notification.getPlanItem() == null ? null : notification.getPlanItem().getId();

        return new NotificationDtoOut(notification.getId(), visitId, activitySessionId, planItemId, notification.getChannel(), notification.getType(), notification.getTitle(), notification.getMessage(), notification.getStatus(), notification.getEventKey(), notification.getActionType(), notification.getResponse(), notification.getRespondedAt(), notification.getScheduledAt(), notification.getSentAt(), notification.getCreatedAt(), notification.getReadAt());
    }
}