package com.example.sikka.Service;

import com.example.sikka.Repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageRequest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NightBeforeVisitReminderServiceTest {
    @Test void windowUsesRiyadhTime() {
        ZoneId riyadh = ZoneId.of("Asia/Riyadh");
        assertTrue(NightBeforeVisitReminderService.isNightlyWindow(
                ZonedDateTime.of(2026, 11, 7, 20, 0, 0, 0, riyadh)));
        assertTrue(NightBeforeVisitReminderService.isNightlyWindow(
                ZonedDateTime.of(2026, 11, 7, 20, 29, 59, 0, riyadh)));
        assertFalse(NightBeforeVisitReminderService.isNightlyWindow(
                ZonedDateTime.of(2026, 11, 7, 19, 59, 59, 0, riyadh)));
        assertFalse(NightBeforeVisitReminderService.isNightlyWindow(
                ZonedDateTime.of(2026, 11, 7, 20, 30, 0, 0, riyadh)));
        assertTrue(NightBeforeVisitReminderService.isNightlyWindow(
                ZonedDateTime.of(2026, 11, 7, 17, 0, 0, 0, ZoneId.of("UTC"))));
    }

    @Test void disabledRunnerCannotQueryVisitsOrSend() {
        VisitRepository visits = mock(VisitRepository.class);
        NotificationEventService sender = mock(NotificationEventService.class);
        NightBeforeVisitReminderService service = new NightBeforeVisitReminderService(visits, sender);
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThrows(IllegalStateException.class, service::sendTomorrowReminders);
        verifyNoInteractions(visits, sender);
    }

    @Test void onlyExplicitlyEnabledRunnerCanConsiderWork() {
        VisitRepository visits = mock(VisitRepository.class);
        NotificationEventService sender = mock(NotificationEventService.class);
        NightBeforeVisitReminderService service = new NightBeforeVisitReminderService(visits, sender);
        assertFalse(service.isEnabled());
        ReflectionTestUtils.setField(service, "enabled", true);
        assertTrue(service.isEnabled());
    }
}
