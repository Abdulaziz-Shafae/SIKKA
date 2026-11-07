package com.example.sikka.Service;

import com.example.sikka.Config.VisitMonitorGate;
import com.example.sikka.Repository.ActivitySessionRepository;
import com.example.sikka.Repository.PlanItemRepository;
import com.example.sikka.Repository.VisitRepository;
import com.example.sikka.Repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

class ServerlessIdleMonitorsTest {
    @Test void rideAndDelayMonitorsSkipAllSqlWhenIdle() {
        VisitMonitorGate gate=mock(VisitMonitorGate.class);
        when(gate.shouldMonitor()).thenReturn(false);
        PlanItemRepository plans=mock(PlanItemRepository.class);
        ActivitySessionRepository sessions=mock(ActivitySessionRepository.class);
        NotificationService notifications=mock(NotificationService.class);

        RideChangeMonitorService rides=new RideChangeMonitorService(plans,notifications,gate);
        ReflectionTestUtils.setField(rides,"monitorEnabled",true);
        rides.checkPlannedRideChanges();

        ActivityDelayMonitorService delays=new ActivityDelayMonitorService(sessions,notifications,gate);
        ReflectionTestUtils.setField(delays,"monitorEnabled",true);
        delays.checkCurrentActivityDelays();

        verifyNoInteractions(plans,sessions,notifications);
    }

    @Test void reminderMonitorSkipsSqlWhenIdle() {
        VisitMonitorGate gate=mock(VisitMonitorGate.class);
        when(gate.shouldMonitor()).thenReturn(false);
        VisitRepository visits=mock(VisitRepository.class);
        NotificationEventService notifications=mock(NotificationEventService.class);
        NotificationReminderService reminders=new NotificationReminderService(visits,notifications);
        ReflectionTestUtils.setField(reminders,"enabled",true);
        ReflectionTestUtils.setField(reminders,"visitMonitorGate",gate);
        reminders.sendDueReminders();
        verifyNoInteractions(visits,notifications);
    }

    @Test void queueAnalyticsSkipsExternalTrafficAndSqlWhenIdle() {
        VisitMonitorGate gate=mock(VisitMonitorGate.class);
        when(gate.shouldMonitor()).thenReturn(false);
        RideRepository rides=mock(RideRepository.class);
        QueueAnalyticsSyncService sync=new QueueAnalyticsSyncService(
                rides,tools.jackson.databind.json.JsonMapper.builder().build());
        ReflectionTestUtils.setField(sync,"enabled",true);
        ReflectionTestUtils.setField(sync,"visitMonitorGate",gate);
        sync.synchronizeAnalyticsReadings();
        verifyNoInteractions(rides);
    }
}
