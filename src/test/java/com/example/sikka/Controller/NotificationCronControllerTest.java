package com.example.sikka.Controller;

import com.example.sikka.Service.NotificationReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationCronControllerTest {
    private NotificationCronController createCronController(NotificationReminderService reminders) {
        NotificationCronController controller = new NotificationCronController(reminders);
        ReflectionTestUtils.setField(controller, "cronKey", "example-staging-cron-secret");
        return controller;
    }

    @Test
    void rejectMissingOrIncorrectCronKeyWithoutScanningVisitData() {
        NotificationReminderService reminders = mock(NotificationReminderService.class);
        NotificationCronController controller = createCronController(reminders);
        assertEquals(404, controller.runDueReminderScan(null).getStatusCode().value());
        assertEquals(404, controller.runDueReminderScan("wrong").getStatusCode().value());
        verifyNoInteractions(reminders);
    }

    @Test
    void runDueRemindersOnlyWithCorrectSecret() {
        NotificationReminderService reminders = mock(NotificationReminderService.class);
        when(reminders.dispatchDueReminders()).thenReturn(3);
        NotificationCronController controller = createCronController(reminders);
        assertEquals(200, controller.runDueReminderScan("example-staging-cron-secret").getStatusCode().value());
        verify(reminders, times(1)).dispatchDueReminders();
    }
}
