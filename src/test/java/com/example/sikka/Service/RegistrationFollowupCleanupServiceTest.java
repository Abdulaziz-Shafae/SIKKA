package com.example.sikka.Service;

import com.example.sikka.Repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RegistrationFollowupCleanupServiceTest {
    @Test
    void cancelsOnlyTheLegacyFollowupQueueOnStartup() {
        NotificationRepository repository = mock(NotificationRepository.class);
        new RegistrationFollowupCleanupService(repository).cancelLegacyPendingFollowups();
        verify(repository).cancelPendingRegistrationFollowups(anyString());
        verifyNoMoreInteractions(repository);
    }
}
