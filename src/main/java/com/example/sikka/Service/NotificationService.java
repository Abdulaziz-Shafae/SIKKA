package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.NotificationDtoIn;
import com.example.sikka.DtoOut.NotificationDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Notification;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.NotificationRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;

    @Transactional(readOnly = true)
    public List<NotificationDtoOut> getMyNotifications(Integer accountId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        List<Notification> notifications = notificationRepository.findAllByAccount_IdAndChannelAndStatusOrderByCreatedAtDescIdDesc(accountId, "IN_APP", "SENT");

        if (notifications.isEmpty()) {
            throw new ApiException("No notifications found");
        }

        List<NotificationDtoOut> notificationDtoOuts = new ArrayList<>();

        for (Notification notification : notifications) {
            Integer visitId = null;

            if (notification.getVisit() != null) {
                visitId = notification.getVisit().getId();
            }

            notificationDtoOuts.add(new NotificationDtoOut(notification.getId(), visitId, notification.getChannel(), notification.getType(), notification.getTitle(), notification.getMessage(), notification.getStatus(), notification.getScheduledAt(), notification.getCreatedAt()));
        }

        return notificationDtoOuts;
    }

    @Transactional(readOnly = true)
    public NotificationDtoOut getNotificationDetails(Integer accountId, Integer notificationId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Notification notification = notificationRepository.findNotificationById(notificationId);

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

        Integer visitId = null;

        if (notification.getVisit() != null) {
            visitId = notification.getVisit().getId();
        }

        return new NotificationDtoOut(notification.getId(), visitId, notification.getChannel(), notification.getType(), notification.getTitle(), notification.getMessage(), notification.getStatus(), notification.getScheduledAt(), notification.getCreatedAt());
    }

    @Transactional
    public void deleteNotification(Integer accountId, Integer notificationId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Notification notification = notificationRepository.findNotificationById(notificationId);

        if (notification == null) {
            throw new ApiException("Notification not found");
        }

        if (!notification.getAccount().getId().equals(accountId)) {
            throw new ApiException("This notification does not belong to you");
        }

        if (!"IN_APP".equals(notification.getChannel())) {
            throw new ApiException("Only in-app notifications can be deleted");
        }

        if (!"SENT".equals(notification.getStatus())) {
            throw new ApiException("Only published notifications can be deleted");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void sendNotification(Integer accountId, Integer visitId, String channel, String type, String title, String message) {
        NotificationDtoIn dto = new NotificationDtoIn(visitId, channel, type, title, message, null);
        createNotification(accountId, dto);
    }

    @Transactional
    public void createNotification(Integer recipientAccountId, NotificationDtoIn notificationDtoIn) {
        Account account = accountRepository.findAccountById(recipientAccountId);

        if (account == null) {
            throw new ApiException("Recipient account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Recipient account is not active");
        }

        if (notificationDtoIn == null) {
            throw new ApiException("Notification details are required");
        }

        String channel = notificationDtoIn.getChannel();

        if (!"IN_APP".equals(channel) && !"EMAIL".equals(channel) && !"WHATSAPP".equals(channel)) {
            throw new ApiException("Channel must be IN_APP, EMAIL or WHATSAPP");
        }

        if (notificationDtoIn.getType() == null || notificationDtoIn.getType().isBlank()) {
            throw new ApiException("Notification type is required");
        }

        if (notificationDtoIn.getType().length() > 60) {
            throw new ApiException("Notification type must not exceed 60 characters");
        }

        if (notificationDtoIn.getTitle() != null && notificationDtoIn.getTitle().length() > 180) {
            throw new ApiException("Title must not exceed 180 characters");
        }

        if (notificationDtoIn.getMessage() == null || notificationDtoIn.getMessage().isBlank()) {
            throw new ApiException("Notification message is required");
        }

        Visit visit = null;

        if (notificationDtoIn.getVisitId() != null) {
            visit = visitRepository.findVisitById(notificationDtoIn.getVisitId());

            if (visit == null) {
                throw new ApiException("Visit not found");
            }

            if (!visit.getOrganizer().getId().equals(recipientAccountId)) {
                VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visit.getId(), recipientAccountId);

                if (visitMember == null) {
                    throw new ApiException("The recipient does not belong to this visit");
                }
            }
        }

        Notification notification = new Notification();
        notification.setAccount(account);
        notification.setVisit(visit);
        notification.setChannel(channel);
        notification.setType(notificationDtoIn.getType());
        notification.setTitle(notificationDtoIn.getTitle());
        notification.setMessage(notificationDtoIn.getMessage());
        notification.setScheduledAt(notificationDtoIn.getScheduledAt());
        notification.setStatus("PENDING");
        notificationRepository.save(notification);
    }
}