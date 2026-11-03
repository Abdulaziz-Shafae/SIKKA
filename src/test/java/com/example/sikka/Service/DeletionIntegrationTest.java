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
    private AccountErasureService accountErasureService;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
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
    void eraseAccountWithOnlySoloVisitDeletesEntireVisitAndProfile() {
        Account owner = persistAccount("erase-solo-owner");
        owner.setPasswordHash(passwordEncoder.encode("SoloConfirm123!"));
        VisitorProfile profile = persistProfile(owner);
        Visit visit = persistVisit(owner);
        VisitMember member = persistMember(visit, profile);
        Integer ownerId = owner.getId(), profileId = profile.getId(), visitId = visit.getId(), memberId = member.getId();
        flushAndClear();
        accountErasureService.deleteMyAccount(ownerId, "SoloConfirm123!");
        flushAndClear();
        assertNull(entityManager.find(Account.class, ownerId));
        assertNull(entityManager.find(VisitorProfile.class, profileId));
        assertNull(entityManager.find(Visit.class, visitId));
        assertNull(entityManager.find(VisitMember.class, memberId));
    }

    @Test
    void eraseFriendAccountLeavesOrganizerVisitAndOtherParticipantsUnaffected() {
        Account organizer = persistAccount("erase-friend-owner");
        VisitorProfile organizerProfile = persistProfile(organizer);
        Visit visit = persistVisit(organizer);
        VisitMember ownerMember = persistMember(visit, organizerProfile);
        Account guest = persistAccount("erase-friend-guest");
        guest.setPasswordHash(passwordEncoder.encode("GuestConfirm123!"));
        VisitorProfile guestProfile = persistProfile(guest);
        VisitMember guestMember = persistMember(visit, guestProfile);
        guestMember.setName("Guest real name");
        guestMember.setPreferences("{\\"preferredRideIds\\":[1]}");
        Integer guestId = guest.getId(), guestProfileId = guestProfile.getId(), organizerId = organizer.getId(), visitId = visit.getId(), ownerMemberId = ownerMember.getId(), guestMemberId = guestMember.getId();
        flushAndClear();
        accountErasureService.deleteMyAccount(guestId, "GuestConfirm123!");
        flushAndClear();
        assertNull(entityManager.find(Account.class, guestId));
        assertNull(entityManager.find(VisitorProfile.class, guestProfileId));
        assertEquals(organizerId, entityManager.find(Visit.class, visitId).getOrganizer().getId());
        assertNotNull(entityManager.find(VisitMember.class, ownerMemberId));
        VisitMember formerGuest = entityManager.find(VisitMember.class, guestMemberId);
        assertEquals("Deleted participant", formerGuest.getName());
        assertNull(formerGuest.getPreferences());
        assertNull(formerGuest.getVisitorProfile());
    }

    @Test
    void deleteOwnAccountWithoutDeletingAnotherMemberFromSharedVisit() {
        Account organizer = persistAccount("self-erasure-owner");
        organizer.setPasswordHash(passwordEncoder.encode("Confirm123!"));
        VisitorProfile organizerProfile = persistProfile(organizer);
        Account friend = persistAccount("self-erasure-friend");
        VisitorProfile friendProfile = persistProfile(friend);
        Visit visit = persistVisit(organizer);
        VisitMember ownerMember = persistMember(visit, organizerProfile);
        ownerMember.setName("Personal owner name");
        ownerMember.setSelectedRides("[{\"rideId\":1}]");
        VisitMember friendMember = persistMember(visit, friendProfile);
        friendMember.setName("Friend still here");
        Integer organizerId = organizer.getId(), friendId = friend.getId(), visitId = visit.getId(), ownerMemberId = ownerMember.getId(), friendMemberId = friendMember.getId(), profileId = organizerProfile.getId();
        flushAndClear();
        accountErasureService.deleteMyAccount(organizerId, "Confirm123!");
        flushAndClear();
        assertNull(entityManager.find(Account.class, organizerId));
        assertNull(entityManager.find(VisitorProfile.class, profileId));
        assertNotNull(entityManager.find(Account.class, friendId));
        assertNotNull(entityManager.find(VisitMember.class, friendMemberId));
        assertEquals("Friend still here", entityManager.find(VisitMember.class, friendMemberId).getName());
        Visit shared = entityManager.find(Visit.class, visitId);
        assertNotNull(shared);
        assertNotEquals(organizerId, shared.getOrganizer().getId());
        assertEquals("BLOCKED", shared.getOrganizer().getStatus());
        VisitMember former = entityManager.find(VisitMember.class, ownerMemberId);
        assertNull(former.getVisitorProfile());
        assertNull(former.getSelectedRides());
        assertNull(former.getBirthDate());
        assertEquals("Deleted participant", former.getName());
        assertEquals("NOT_GOING", former.getRsvpStatus());
    }

    @Test
    void refuseSelfErasureOnWrongPasswordWithoutChangingVisit() {
        Account owner = persistAccount("self-erasure-reject");
        owner.setPasswordHash(passwordEncoder.encode("Correct123!"));
        Visit visit = persistVisit(owner);
        Integer ownerId = owner.getId(), visitId = visit.getId();
        flushAndClear();
        assertThrows(ApiException.class, () -> accountErasureService.deleteMyAccount(ownerId, "incorrect"));
        flushAndClear();
        assertNotNull(entityManager.find(Account.class, ownerId));
        assertEquals(ownerId, entityManager.find(Visit.class, visitId).getOrganizer().getId());
    }

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
    void deleteVisitMembersAndGroupButKeepAccountsAndProfiles() {
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
        visitService.delete(accountId, visitId);
        flushAndClear();
        assertNull(entityManager.find(Visit.class, visitId));
        assertNull(entityManager.find(VisitMember.class, memberId));
        assertNotNull(entityManager.find(Account.class, accountId));
        assertNotNull(entityManager.find(VisitorProfile.class, profileId));
        assertNull(entityManager.find(VisitGroup.class, groupId));
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
    void deleteVisitAndPreservePaymentHistory() {
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
        visitService.delete(account.getId(), visitId);
        flushAndClear();
        assertNull(entityManager.find(Visit.class, visitId));
        PaymentOrder retainedPayment = entityManager.find(PaymentOrder.class, paymentId);
        assertNotNull(retainedPayment);
        assertNull(retainedPayment.getVisit());
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

    @Test
    void rejectVisitDeletionByAnotherUser() {
        Account owner = persistAccount("delete-owner");
        Account stranger = persistAccount("delete-stranger");
        Visit visit = persistVisit(owner);
        Integer visitId = visit.getId();
        Integer strangerId = stranger.getId();
        flushAndClear();
        assertThrows(ApiException.class, () -> visitService.delete(strangerId, visitId));
        assertNotNull(entityManager.find(Visit.class, visitId));
    }

    @Test
    void preserveUsedFreeQuotaAndClearReuseSourceAfterDeletion() {
        Account account = persistAccount("delete-quota");
        Visit source = persistVisit(account);
        source.setFreeSlotReserved(true);
        source.setStartedAt(java.time.LocalDateTime.now());
        Visit retained = persistVisit(account);
        PlanHistory used = new PlanHistory();
        used.setAccount(account);
        used.setVisit(source);
        used.setRequestKey("delete-quota-used");
        used.setOperationType("INITIAL");
        used.setBillingMode("FREE");
        used.setStatus("SUCCEEDED");
        used.setCountsTowardQuota(true);
        entityManager.persist(used);
        PlanHistory reused = new PlanHistory();
        reused.setAccount(account);
        reused.setVisit(retained);
        reused.setSourceVisit(source);
        reused.setRequestKey("delete-quota-reused");
        reused.setOperationType("REUSE");
        reused.setBillingMode("FREE");
        reused.setStatus("FAILED");
        entityManager.persist(reused);
        VisitPlan plan = new VisitPlan();
        plan.setVisit(source);
        plan.setPlanKind("GROUP");
        plan.setGeneratedByHistory(used);
        entityManager.persist(plan);
        Integer accountId = account.getId();
        Integer visitId = source.getId();
        Integer retainedId = retained.getId();
        Integer reusedId = reused.getId();
        Integer usedId = used.getId();
        Integer planId = plan.getId();
        flushAndClear();
        visitService.delete(accountId, visitId);
        flushAndClear();
        assertNull(entityManager.find(Visit.class, visitId));
        assertNull(entityManager.find(VisitPlan.class, planId));
        assertNull(entityManager.find(PlanHistory.class, usedId));
        assertEquals(1L, entityManager.find(Account.class, accountId).getArchivedFreeVisits());
        assertNotNull(entityManager.find(Visit.class, retainedId));
        assertNull(entityManager.find(PlanHistory.class, reusedId).getSourceVisit());
    }

    @Test
    void rejectVisitDeletionDuringGeneration() {
        Account account = persistAccount("delete-processing");
        Visit visit = persistVisit(account);
        PlanHistory history = new PlanHistory();
        history.setAccount(account);
        history.setVisit(visit);
        history.setRequestKey("delete-processing-request");
        history.setOperationType("INITIAL");
        history.setBillingMode("FREE");
        entityManager.persist(history);
        Integer accountId = account.getId();
        Integer visitId = visit.getId();
        flushAndClear();
        assertThrows(ApiException.class, () -> visitService.delete(accountId, visitId));
        assertNotNull(entityManager.find(Visit.class, visitId));
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
