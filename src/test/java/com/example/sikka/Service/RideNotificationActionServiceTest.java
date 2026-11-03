package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.*;
import com.example.sikka.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RideNotificationActionServiceTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final VisitPlanRepository plans = mock(VisitPlanRepository.class);
    private final PlanItemRepository items = mock(PlanItemRepository.class);
    private final ActivitySessionRepository sessions = mock(ActivitySessionRepository.class);
    private final VisitRecordRepository records = mock(VisitRecordRepository.class);
    private final VisitMemberRepository members = mock(VisitMemberRepository.class);
    private final RideRepository rides = mock(RideRepository.class);
    private final MapService map = mock(MapService.class);

    private RideNotificationActionService service() {
        RideNotificationActionService service = new RideNotificationActionService(notifications, visits, plans, items, sessions, records, members, rides, map);
        ReflectionTestUtils.setField(service, "entityManager", mock(jakarta.persistence.EntityManager.class));
        return service;
    }

    @Test
    void rejectRideActionFromParticipantWhoIsNotOrganizer() {
        Account owner = new Account();owner.setId(1);
        Account participant = new Account();participant.setId(2);
        Visit visit = new Visit();visit.setId(3);visit.setStatus("IN_PROGRESS");visit.setOrganizer(owner);
        Notification notice = new Notification();notice.setId(20);notice.setAccount(participant);notice.setChannel("IN_APP");notice.setType("RIDE_STATUS_CHANGED");notice.setVisit(visit);
        PlanItem item = new PlanItem();item.setId(22);notice.setPlanItem(item);
        when(notifications.findNotificationByIdForUpdate(20)).thenReturn(notice);
        when(visits.findVisitByIdForUpdate(3)).thenReturn(visit);
        assertThrows(ApiException.class, () -> service().applyRideNotificationAction(2, 20, "DEFER"));
        verifyNoInteractions(map);
    }

    @Test
    void rejectChangingAlreadyAnsweredRideNotification() {
        Account owner = new Account();owner.setId(1);
        Notification notice = new Notification();notice.setId(20);notice.setAccount(owner);notice.setChannel("IN_APP");notice.setType("RIDE_STATUS_CHANGED");notice.setRespondedAt(LocalDateTime.now());
        when(notifications.findNotificationByIdForUpdate(20)).thenReturn(notice);
        assertThrows(ApiException.class, () -> service().applyRideNotificationAction(1, 20, "REPLACE"));
        verifyNoInteractions(visits,plans,map);
    }

    @Test
    void rejectUnknownRideActionBeforeChangingSchedule() {
        Account owner = new Account();owner.setId(1);
        Visit visit = new Visit();visit.setId(3);visit.setOrganizer(owner);visit.setStatus("IN_PROGRESS");
        Notification notice = new Notification();notice.setId(20);notice.setAccount(owner);notice.setVisit(visit);notice.setChannel("IN_APP");notice.setType("RIDE_STATUS_CHANGED");
        PlanItem item = new PlanItem();item.setId(22);item.setRide(new Ride());
        VisitPlan plan = new VisitPlan();plan.setVisit(visit);plan.setPlanKind("GROUP");plan.setIsActive(true);plan.setTimingMode("SCHEDULED");
        PlanApproval approval = new PlanApproval();approval.setStatus("APPROVED");plan.setPlanApproval(approval);item.setVisitPlan(plan);notice.setPlanItem(item);
        when(notifications.findNotificationByIdForUpdate(20)).thenReturn(notice);
        when(visits.findVisitByIdForUpdate(3)).thenReturn(visit);
        when(plans.findVisitPlanByIdForUpdate(null)).thenReturn(plan);
        RideNotificationActionService service=service();
        jakarta.persistence.EntityManager em = (jakarta.persistence.EntityManager)ReflectionTestUtils.getField(service,"entityManager");
        when(em.find(eq(PlanItem.class),eq(22),eq(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE))).thenReturn(item);
        assertThrows(ApiException.class, () -> service.applyRideNotificationAction(1,20,"SILENTLY_DELETE_PLAN"));
        verify(items, never()).save(any());
    }
}
