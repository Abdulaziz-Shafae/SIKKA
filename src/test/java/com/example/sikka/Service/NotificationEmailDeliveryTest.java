package com.example.sikka.Service;

import com.example.sikka.Model.Account;
import com.example.sikka.Model.Notification;
import com.example.sikka.Repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationEmailDeliveryTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final AtomicReference<Notification> savedEmail = new AtomicReference<>();
    private NotificationEventService service;

    @BeforeEach
    void prepareNotificationDelivery() {
        service = new NotificationEventService(notifications, mock(VisitMemberRepository.class), mock(VisitRepository.class), mock(VisitPlanRepository.class), mock(PlanItemRepository.class), mock(VisitMealBreakRepository.class), mock(ActivitySessionRepository.class), mock(VisitRecordRepository.class), emailService, mock(WhatsAppService.class), mock(PrivacyConsentService.class), transactions);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(notifications.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) notification.setId(42);
            savedEmail.set(notification);
            return notification;
        });
        when(notifications.findNotificationById(42)).thenAnswer(invocation -> savedEmail.get());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void sendsEmailOnlyAfterCommitAndRecordsSuccessfulDelivery() {
        when(emailService.sendEmail("recipient@example.test", "Activity reminder", "Test message")).thenReturn(true);
        service.forwardCriticalNotification(createCriticalNotification());
        verifyNoInteractions(emailService);
        assertEquals("PENDING", savedEmail.get().getStatus());
        commitNotificationTransaction();
        verify(emailService).sendEmail("recipient@example.test", "Activity reminder", "Test message");
        assertEquals("SENT", savedEmail.get().getStatus());
        assertNotNull(savedEmail.get().getSentAt());
    }

    @Test
    void recordsRejectedDeliveryAsFailedWithoutClaimingSuccess() {
        service.forwardCriticalNotification(createCriticalNotification());
        commitNotificationTransaction();
        verify(emailService).sendEmail("recipient@example.test", "Activity reminder", "Test message");
        assertEquals("FAILED", savedEmail.get().getStatus());
        assertNull(savedEmail.get().getSentAt());
    }

    @Test
    void doesNotSendEmailWhenSourceTransactionRollsBack() {
        service.forwardCriticalNotification(createCriticalNotification());
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        verifyNoInteractions(emailService);
    }

    @Test
    void doesNotQueueOrResendAnExistingRecipientEvent() {
        when(notifications.findNotificationByAccount_IdAndChannelAndEventKey(7, "EMAIL", "ACTIVITY_DELAY:11")).thenReturn(new Notification());
        service.forwardCriticalNotification(createCriticalNotification());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verify(notifications, never()).save(any());
        verifyNoInteractions(emailService);
    }

    private Notification createCriticalNotification() {
        Account account = new Account();
        account.setId(7);
        account.setStatus("ACTIVE");
        account.setEmail("recipient@example.test");
        Notification source = new Notification();
        source.setId(11);
        source.setAccount(account);
        source.setChannel("IN_APP");
        source.setStatus("SENT");
        source.setType("ACTIVITY_DELAY");
        source.setTitle("Activity reminder");
        source.setMessage("Test message");
        source.setEventKey("ACTIVITY_DELAY:11");
        return source;
    }

    private void commitNotificationTransaction() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }
}
