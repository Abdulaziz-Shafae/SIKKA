package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.JoinDtoIn;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitGroup;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.VisitGroupRepository;
import com.example.sikka.Repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationAccessReviewTest {
    @Test
    void guestsWithoutVerifiedEmailCannotJoinFriendVisits() {
        AccountRepository accounts = mock(AccountRepository.class);
        VisitMemberService members = mock(VisitMemberService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(members, "accountRepository", accounts);
        Account guest = new Account();
        guest.setId(8);
        guest.setStatus("ACTIVE");
        guest.setEmailVerified(false);
        when(accounts.findAccountById(8)).thenReturn(guest);
        assertThrows(ApiException.class, () -> members.join(8, new JoinDtoIn()));
        verify(accounts).findAccountById(8);
    }

    @Test
    void organizerCannotRotateInviteCodeAfterVisitMembersLock() {
        VisitRepository visits = mock(VisitRepository.class);
        VisitGroupRepository groups = mock(VisitGroupRepository.class);
        VisitGroupService service = mock(VisitGroupService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "visitRepository", visits);
        ReflectionTestUtils.setField(service, "visitGroupRepository", groups);
        Account organizer = new Account();
        organizer.setId(3);
        Visit visit = new Visit();
        visit.setId(10);
        visit.setOrganizer(organizer);
        visit.setStatus("READY");
        visit.setMembersLocked(true);
        VisitGroup group = new VisitGroup();
        group.setVisit(visit);
        visit.setGroup(group);
        when(visits.findVisitById(10)).thenReturn(visit);
        assertThrows(ApiException.class, () -> service.regenerateCode(3,10));
        verifyNoInteractions(groups);
    }
}
