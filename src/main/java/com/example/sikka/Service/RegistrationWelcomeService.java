package com.example.sikka.Service;

import com.example.sikka.Model.Account;
import com.example.sikka.Model.Notification;
import com.example.sikka.Repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationWelcomeService {
    private final NotificationRepository notificationRepository;
    private final VerificationService verificationService;
    private final AtomicReference<LocalDateTime> nextDue = new AtomicReference<>();

    @EventListener(ApplicationReadyEvent.class)
    public void restorePendingSchedule() {
        rememberNextPending();
    }

    @Transactional
    public void scheduleFollowup(Account account) {
        String key = "REGISTRATION_WELCOME_FOLLOWUP:" + account.getId();
        if (notificationRepository.findNotificationByAccount_IdAndChannelAndEventKey(account.getId(), "EMAIL", key) != null) {
            return;
        }
        Notification notification = new Notification();
        notification.setAccount(account);
        notification.setChannel("EMAIL");
        notification.setType("REGISTRATION_WELCOME_FOLLOWUP");
        notification.setTitle("أهلًا بك في سكة");
        notification.setMessage("رسالة متابعة الترحيب؛ يُنشأ رمز تفعيل جديد عند الإرسال للحساب غير المفعّل.");
        notification.setEventKey(key);
        notification.setStatus("PENDING");
        notification.setScheduledAt(now().plusMinutes(3));
        notificationRepository.save(notification);
        remember(notification.getScheduledAt());
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 15000)
    @Transactional
    public void sendDueWelcomeEmails() {
        LocalDateTime due = nextDue.get();
        if (due == null || due.isAfter(now())) {
            return;
        }
        nextDue.compareAndSet(due, null);
        for (Notification notification : notificationRepository.findDueRegistrationEmailsForUpdate(now(), PageRequest.of(0, 50))) {
            boolean sent = false;
            try {
                Account account = notification.getAccount();
                sent = account != null && "ACTIVE".equals(account.getStatus()) && verificationService.sendWelcomeFollowup(account);
            } catch (RuntimeException exception) {
                log.warn("Registration follow-up failed for notification {}: {}", notification.getId(), exception.getClass().getSimpleName());
            }
            notification.setStatus(sent ? "SENT" : "FAILED");
            notification.setSentAt(sent ? now() : null);
            notificationRepository.save(notification);
        }
        rememberNextPending();
    }

    private void rememberNextPending() {
        Notification notification = notificationRepository.findFirstByChannelAndTypeAndStatusOrderByScheduledAtAsc("EMAIL", "REGISTRATION_WELCOME_FOLLOWUP", "PENDING");
        if (notification != null && notification.getScheduledAt() != null) {
            remember(notification.getScheduledAt());
        }
    }

    private void remember(LocalDateTime due) {
        nextDue.updateAndGet(current -> current == null || due.isBefore(current) ? due : current);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Riyadh"));
    }
}
