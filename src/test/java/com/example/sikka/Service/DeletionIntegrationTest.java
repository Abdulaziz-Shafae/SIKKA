package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DeletionIntegrationTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AccountService accountService;
    @Autowired
    private VisitorProfileService visitorProfileService;
    @Autowired
    private VisitService visitService;
    @Autowired
    private VisitGroupService visitGroupService;
    @Autowired
    private VisitMemberService visitMemberService;
    @Autowired
    private VisitPlanService visitPlanService;

    @Test
    void deleteUnlinkedAccountAndItsProfile() {
        Account account = persistAccount("empty-account");
        VisitorProfile profile = persistProfile(account);
        Integer accountId = account.getId();
        Integer profileId = profile.getId();
        flushAndClear();
        accountService.delete(accountId);
        flushAndClear();
        assertNull(entityManager.find(Account.class, accountId));
        assertNull(entityManager.find(VisitorProfile.class, profileId));
    }

    @Test
    void deleteStandaloneProfileAndKeepAccount() {
        Account account = persistAccount("standalone-profile");
        VisitorProfile profile = persistProfile(account);
        Integer profileId = profile.getId();
        Integer accountId = account.getId();
        flushAndClear();
        visitorProfileService.delete(profileId);
        flushAndClear();
        assertNull(entityManager.find(VisitorProfile.class, profileId));
        assertNotNull(entityManager.find(Account.class, accountId));
    }

    @Test
    void deleteVisitAndMembersButKeepGroupAccountsAndProfiles() {
        Account organizer = persistAccount("visit-delete");
        VisitorProfile profile = persistProfile(organizer);
        VisitGroup group = persistGroup(organizer);
        Visit visit = persistVisit(organizer);
        visit.setGroup(group);
        VisitMember member = persistMember(visit, profile);
        Integer visitId = visit.getId();
        Integer memberId = member.getId();
        Integer groupId = group.getId();
        Integer profileId = profile.getId();
        Integer accountId = organizer.getId();
        flushAndClear();
        visitService.delete(visitId);
        flushAndClear();
        assertNull(entityManager.find(Visit.class, visitId));
        assertNull(entityManager.find(VisitMember.class, memberId));
        assertNotNull(entityManager.find(Account.class, accountId));
        assertNotNull(entityManager.find(VisitorProfile.class, profileId));
        assertNull(entityManager.find(VisitGroup.class, groupId).getVisit());
    }

    @Test
    void deleteGroupWithoutDeletingItsVisit() {
        Account organizer = persistAccount("group-delete");
        VisitGroup group = persistGroup(organizer);
        Visit visit = persistVisit(organizer);
        visit.setGroup(group);
        Integer groupId = group.getId();
        Integer visitId = visit.getId();
        flushAndClear();
        visitGroupService.delete(groupId);
        flushAndClear();
        assertNull(entityManager.find(VisitGroup.class, groupId));
        assertNull(entityManager.find(Visit.class, visitId).getGroup());
    }

    @Test
    void leaveVisitDeletesOnlyTheGuestMembership() {
        Account organizer = persistAccount("leave-organizer");
        Account guest = persistAccount("leave-guest");
        VisitorProfile profile = persistProfile(guest);
        Visit visit = persistVisit(organizer);
        VisitMember member = persistMember(visit, profile);
        Integer memberId = member.getId();
        Integer guestId = guest.getId();
        Integer visitId = visit.getId();
        flushAndClear();
        visitMemberService.leave(guestId, visitId);
        flushAndClear();
        assertNull(entityManager.find(VisitMember.class, memberId));
        assertNotNull(entityManager.find(Account.class, guestId));
        assertNotNull(entityManager.find(Visit.class, visitId));
    }

    @Test
    void rejectProfileDeletionWhileItIsAMember() {
        Account account = persistAccount("linked-profile");
        VisitorProfile profile = persistProfile(account);
        persistMember(persistVisit(account), profile);
        Integer profileId = profile.getId();
        flushAndClear();
        ApiException exception = assertThrows(ApiException.class, () -> visitorProfileService.delete(profileId));
        assertTrue(exception.getMessage().contains("memberships"));
        assertNotNull(entityManager.find(VisitorProfile.class, profileId));
    }

    @Test
    void rejectAccountDeletionInsteadOfCascadingThroughSharedVisit() {
        Account account = persistAccount("linked-account");
        Visit visit = persistVisit(account);
        Integer accountId = account.getId();
        Integer visitId = visit.getId();
        flushAndClear();
        assertThrows(ApiException.class, () -> accountService.delete(accountId));
        assertNotNull(entityManager.find(Visit.class, visitId));
    }

    @Test
    void rejectVisitDeletionWithPaymentHistory() {
        Account account = persistAccount("paid-visit");
        Visit visit = persistVisit(account);
        PaymentOrder payment = new PaymentOrder();
        payment.setVisit(visit);
        payment.setBuyerAccount(account);
        payment.setOrderGroupKey("payment-delete-test");
        payment.setAmountSar(new BigDecimal("10.00"));
        entityManager.persist(payment);
        Integer visitId = visit.getId();
        Integer paymentId = payment.getId();
        flushAndClear();
        assertThrows(ApiException.class, () -> visitService.delete(visitId));
        assertNotNull(entityManager.find(PaymentOrder.class, paymentId));
    }

    @Test
    void deleteEmptyInactivePlanDuringPlanning() {
        Account account = persistAccount("empty-plan");
        Visit visit = persistVisit(account);
        visit.setStatus("PLANNING");
        VisitPlan plan = new VisitPlan();
        plan.setVisit(visit);
        plan.setPlanKind("GROUP");
        entityManager.persist(plan);
        Integer planId = plan.getId();
        Integer accountId = account.getId();
        flushAndClear();
        visitPlanService.deleteVisitPlan(accountId, planId);
        flushAndClear();
        assertNull(entityManager.find(VisitPlan.class, planId));
    }

    private Account persistAccount(String name) {
        Account account = new Account();
        account.setName(name);
        account.setEmail(name + "@example.com");
        account.setPasswordHash("test-password");
        account.setRole("USER");
        account.setStatus("ACTIVE");
        entityManager.persist(account);
        return account;
    }

    private VisitorProfile persistProfile(Account account) {
        VisitorProfile profile = new VisitorProfile();
        profile.setName(account.getName());
        profile.setAccount(account);
        entityManager.persist(profile);
        return profile;
    }

    private Visit persistVisit(Account organizer) {
        Visit visit = new Visit();
        visit.setOrganizer(organizer);
        visit.setVisitDate(LocalDate.now().plusDays(1));
        visit.setMaxParticipants(5);
        visit.setStatus("DRAFT");
        entityManager.persist(visit);
        return visit;
    }

    private VisitGroup persistGroup(Account organizer) {
        VisitGroup group = new VisitGroup();
        group.setName("Test group");
        group.setOrganizer(organizer);
        entityManager.persist(group);
        return group;
    }

    private VisitMember persistMember(Visit visit, VisitorProfile profile) {
        VisitMember member = new VisitMember();
        member.setVisit(visit);
        member.setVisitorProfile(profile);
        member.setRsvpStatus("GOING");
        entityManager.persist(member);
        return member;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
