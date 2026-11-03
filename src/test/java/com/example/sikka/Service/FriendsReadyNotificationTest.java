package com.example.sikka.Service;

import com.example.sikka.Model.Account;
import com.example.sikka.Model.Notification;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Repository.NotificationRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;

class FriendsReadyNotificationTest {

    private NotificationEventService service(VisitMemberRepository members, NotificationRepository notices) {
        NotificationEventService result = mock(NotificationEventService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(result, "visitMemberRepository", members);
        ReflectionTestUtils.setField(result, "notificationRepository", notices);
        return result;
    }

    private Visit visit(String tripType, String togetherMode) {
        Account organizer = new Account();
        organizer.setId(1);
        organizer.setStatus("ACTIVE");
        Visit visit = new Visit();
        visit.setId(40);
        visit.setOrganizer(organizer);
        visit.setTripType(tripType);
        visit.setTogetherMode(togetherMode);
        return visit;
    }

    private VisitMember member(Visit visit, String status, String answers) {
        VisitMember member = new VisitMember();
        member.setVisit(visit);
        member.setRsvpStatus(status);
        member.setAnswerStatus(answers);
        return member;
    }

    @Test
    void togetherFriendsOnlyNeedConfirmedAttendance() {
        Visit visit = visit("FRIENDS", "TOGETHER");
        VisitMember leader = member(visit, "GOING", "PENDING");
        VisitMember friend = member(visit, "GOING", "PENDING");
        VisitMember absent = member(visit, "NOT_GOING", "PENDING");
        VisitMemberRepository members = mock(VisitMemberRepository.class);
        NotificationRepository notices = mock(NotificationRepository.class);
        when(members.findAllByVisit_Id(40)).thenReturn(List.of(leader, friend, absent));
        service(members, notices).notifyFriendsReady(visit);
        verify(notices, times(1)).save(any(Notification.class));
    }

    @Test
    void splitFriendsWaitForEveryAttendingMembersPlanningChoice() {
        Visit visit = visit("FRIENDS", "SPLIT");
        VisitMember leader = member(visit, "GOING", "ANSWERED");
        VisitMember friend = member(visit, "GOING", "PENDING");
        VisitMember absent = member(visit, "NOT_GOING", "PENDING");
        VisitMemberRepository members = mock(VisitMemberRepository.class);
        NotificationRepository notices = mock(NotificationRepository.class);
        when(members.findAllByVisit_Id(40)).thenReturn(List.of(leader, friend, absent));
        NotificationEventService service = service(members, notices);
        service.notifyFriendsReady(visit);
        verifyNoInteractions(notices);
        friend.setAnswerStatus("SKIPPED");
        service.notifyFriendsReady(visit);
        verify(notices, times(1)).save(any(Notification.class));
    }

    @Test
    void familyHasNoFriendsReadyNotification() {
        Visit visit = visit("FAMILY", "TOGETHER");
        VisitMemberRepository members = mock(VisitMemberRepository.class);
        NotificationRepository notices = mock(NotificationRepository.class);
        service(members, notices).notifyFriendsReady(visit);
        verifyNoInteractions(members, notices);
    }

    @Test
    void undecidedFriendCannotMakeGroupReady() {
        Visit visit = visit("FRIENDS", "TOGETHER");
        VisitMemberRepository members = mock(VisitMemberRepository.class);
        NotificationRepository notices = mock(NotificationRepository.class);
        when(members.findAllByVisit_Id(40)).thenReturn(List.of(member(visit,"GOING","ANSWERED"),member(visit,"INVITED","PENDING")));
        service(members, notices).notifyFriendsReady(visit);
        verifyNoInteractions(notices);
    }
}
