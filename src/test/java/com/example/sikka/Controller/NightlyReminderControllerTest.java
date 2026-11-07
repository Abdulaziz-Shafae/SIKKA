package com.example.sikka.Controller;

import com.example.sikka.Service.NightBeforeVisitReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NightlyReminderControllerTest {
    @Test void disabledFeatureReturns404AndNeverDispatches() {
        NightBeforeVisitReminderService reminders = mock(NightBeforeVisitReminderService.class);
        NightlyReminderController controller = new NightlyReminderController(reminders);
        ReflectionTestUtils.setField(controller, "key", "a".repeat(40));
        assertEquals(404, controller.run("a".repeat(40)).getStatusCode().value());
        verify(reminders).isEnabled();
        verifyNoMoreInteractions(reminders);
    }

    @Test void missingShortOrIncorrectKeysCannotDispatch() {
        NightBeforeVisitReminderService reminders = mock(NightBeforeVisitReminderService.class);
        when(reminders.isEnabled()).thenReturn(true);
        NightlyReminderController controller = new NightlyReminderController(reminders);
        ReflectionTestUtils.setField(controller, "key", "a".repeat(40));
        assertEquals(403, controller.run(null).getStatusCode().value());
        assertEquals(403, controller.run("b".repeat(40)).getStatusCode().value());
        ReflectionTestUtils.setField(controller, "key", "short");
        assertEquals(403, controller.run("short").getStatusCode().value());
        verify(reminders, never()).sendTomorrowReminders();
    }
}
