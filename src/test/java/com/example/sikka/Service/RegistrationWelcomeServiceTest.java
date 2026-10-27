package com.example.sikka.Service;

import com.example.sikka.Model.Account;
import com.example.sikka.Model.Notification;
import com.example.sikka.Repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RegistrationWelcomeServiceTest {
    private Account account() {
        Account account = new Account();
        account.setId(12);
        account.setStatus("ACTIVE");
        return account;
    }

    @Test
    void savesOneFollowupThreeMinutesLaterWithoutSendingEarly() {
        NotificationRepository repository = mock(NotificationRepository.class);
        VerificationService verification = mock(VerificationService.class);
        RegistrationWelcomeService service = new RegistrationWelcomeService(repository, verification);
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Riyadh")).plusMinutes(3);
        service.scheduleFollowup(account());
        org.mockito.ArgumentCaptor<Notification> saved = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(saved.capture());
        assertEquals("PENDING", saved.getValue().getStatus());
        assertEquals("REGISTRATION_WELCOME_FOLLOWUP:12", saved.getValue().getEventKey());
        assertFalse(saved.getValue().getScheduledAt().isBefore(before));
        service.sendDueWelcomeEmails();
        verify(repository, never()).findDueRegistrationEmailsForUpdate(any(), any());
        verifyNoInteractions(verification);
    }

    @Test
    void doesNotQueueDuplicateFollowup() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.findNotificationByAccount_IdAndChannelAndEventKey(12, "EMAIL", "REGISTRATION_WELCOME_FOLLOWUP:12")).thenReturn(new Notification());
        RegistrationWelcomeService service = new RegistrationWelcomeService(repository, mock(VerificationService.class));
        service.scheduleFollowup(account());
        verify(repository, never()).save(any());
    }

    @Test
    void restoresPersistedDueWorkAndStopsPollingAfterDelivery() {
        NotificationRepository repository = mock(NotificationRepository.class);
        VerificationService verification = mock(VerificationService.class);
        Notification notification = new Notification();
        notification.setAccount(account());
        notification.setScheduledAt(LocalDateTime.now(ZoneId.of("Asia/Riyadh")).minusMinutes(1));
        when(repository.findFirstByChannelAndTypeAndStatusOrderByScheduledAtAsc("EMAIL", "REGISTRATION_WELCOME_FOLLOWUP", "PENDING")).thenReturn(notification, null);
        when(repository.findDueRegistrationEmailsForUpdate(any(), any())).thenReturn(List.of(notification));
        when(verification.sendWelcomeFollowup(notification.getAccount())).thenReturn(true);
        RegistrationWelcomeService service = new RegistrationWelcomeService(repository, verification);
        service.restorePendingSchedule();
        service.sendDueWelcomeEmails();
        service.sendDueWelcomeEmails();
        assertEquals("SENT", notification.getStatus());
        assertNotNull(notification.getSentAt());
        verify(verification, times(1)).sendWelcomeFollowup(notification.getAccount());
        verify(repository, times(1)).findDueRegistrationEmailsForUpdate(any(), any());
    }
}
