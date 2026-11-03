package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountErasureService {
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final VisitService visitService;

    @Transactional
    public void deleteMyAccount(Integer accountId, String password) {
        if (accountId == null || password == null || password.isBlank()) throw new ApiException("Enter your password to confirm account deletion");
        // Lock the row without triggering Hibernate's eager one-to-one profile join under PESSIMISTIC_WRITE.
        @SuppressWarnings("unchecked")
        List<Object[]> accountRows = entityManager.createNativeQuery("SELECT status, password_hash FROM account WHERE id = :id FOR UPDATE").setParameter("id", accountId).getResultList();
        if (accountRows.isEmpty() || !"ACTIVE".equals(String.valueOf(accountRows.get(0)[0])) || !passwordEncoder.matches(password, String.valueOf(accountRows.get(0)[1]))) throw new ApiException("Password confirmation failed");

        // A visit that belongs only to this account can be removed using the existing tested visit deletion path.
        List<Integer> ownedVisits = entityManager.createQuery("select v.id from Visit v where v.organizer.id = :accountId", Integer.class).setParameter("accountId", accountId).getResultList();
        for (Integer visitId : ownedVisits) {
            Number otherMembers = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM visit_member vm LEFT JOIN visitor_profile vp ON vp.id = vm.visitor_profile_id LEFT JOIN family_member fm ON fm.id = vm.family_member_id WHERE vm.visit_id = :visitId AND ((vp.account_id IS NOT NULL AND vp.account_id <> :accountId) OR (fm.owner_account_id IS NOT NULL AND fm.owner_account_id <> :accountId) OR (vm.visitor_profile_id IS NULL AND vm.family_member_id IS NULL))").setParameter("visitId", visitId).setParameter("accountId", accountId).getSingleResult();
            if (otherMembers.longValue() == 0) visitService.delete(accountId, visitId);
        }

        // Every preserved shared visit keeps its other members' history. The former organizer is replaced
        // by a disabled non-personal technical record; the original account itself is physically deleted.
        Account archival = new Account();
        archival.setName("Deleted account");
        archival.setEmail("deleted-" + UUID.randomUUID() + "@invalid.sikka.local");
        archival.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        archival.setRole("USER");
        archival.setStatus("BLOCKED");
        archival.setEmailVerified(false);
        entityManager.persist(archival);
        entityManager.flush();
        Integer archivalId = archival.getId();

        // Unlink third-party tokens/events before removing the account; no external account data is migrated.
        execute("DELETE FROM google_calendar_event WHERE account_id = :id", accountId);
        execute("DELETE FROM google_calendar_oauth_state WHERE account_id = :id", accountId);
        execute("DELETE FROM google_calendar_connection WHERE account_id = :id", accountId);

        // Remove the departing person's own notifications and notifications pointing to their sessions.
        execute("DELETE FROM notification WHERE account_id = :id OR activity_session_id IN (SELECT s.id FROM activity_session s JOIN visit_member m ON m.id = s.visit_member_id LEFT JOIN visitor_profile vp ON vp.id = m.visitor_profile_id LEFT JOIN family_member fm ON fm.id = m.family_member_id WHERE vp.account_id = :id OR fm.owner_account_id = :id)", accountId);

        // Personal activity data is deleted. Records completed by the departing organizer for somebody
        // else remain the other member's data, with their recorder's identity irreversibly severed.
        execute("DELETE FROM visit_record WHERE visit_member_id IN (SELECT m.id FROM visit_member m LEFT JOIN visitor_profile vp ON vp.id = m.visitor_profile_id LEFT JOIN family_member fm ON fm.id = m.family_member_id WHERE vp.account_id = :id OR fm.owner_account_id = :id)", accountId);
        executeWithReplacement("UPDATE visit_record SET recorded_by_account_id = :replacement WHERE recorded_by_account_id = :id", accountId, archivalId);
        execute("DELETE FROM activity_session WHERE visit_member_id IN (SELECT m.id FROM visit_member m LEFT JOIN visitor_profile vp ON vp.id = m.visitor_profile_id LEFT JOIN family_member fm ON fm.id = m.family_member_id WHERE vp.account_id = :id OR fm.owner_account_id = :id)", accountId);
        execute("DELETE FROM visit_meal_break WHERE visit_member_id IN (SELECT m.id FROM visit_member m LEFT JOIN visitor_profile vp ON vp.id = m.visitor_profile_id LEFT JOIN family_member fm ON fm.id = m.family_member_id WHERE vp.account_id = :id OR fm.owner_account_id = :id)", accountId);

        // Keep shared plan structures valid while breaking direct ownership references and clearing snapshots.
        execute("UPDATE visit_plan SET owner_visit_member_id = NULL WHERE owner_visit_member_id IN (SELECT m.id FROM visit_member m LEFT JOIN visitor_profile vp ON vp.id = m.visitor_profile_id LEFT JOIN family_member fm ON fm.id = m.family_member_id WHERE vp.account_id = :id OR fm.owner_account_id = :id)", accountId);
        execute("UPDATE visit_plan SET requested_by_visit_member_id = NULL WHERE requested_by_visit_member_id IN (SELECT m.id FROM visit_member m LEFT JOIN visitor_profile vp ON vp.id = m.visitor_profile_id LEFT JOIN family_member fm ON fm.id = m.family_member_id WHERE vp.account_id = :id OR fm.owner_account_id = :id)", accountId);
        execute("UPDATE visit_member m SET m.name = 'Deleted participant', m.birth_date = NULL, m.height_cm = NULL, m.weight_kg = NULL, m.gender = NULL, m.selected_rides = NULL, m.preferences = NULL, m.snapshot_at = NULL, m.checked_in_at = NULL, m.invited_at = NULL, m.visit_rating = NULL, m.visit_review = NULL, m.sikka_rating = NULL, m.sikka_review = NULL, m.rsvp_status = 'NOT_GOING', m.answer_status = 'SKIPPED', m.visitor_profile_id = NULL, m.family_member_id = NULL WHERE m.visitor_profile_id IN (SELECT id FROM visitor_profile WHERE account_id = :id) OR m.family_member_id IN (SELECT id FROM family_member WHERE owner_account_id = :id)", accountId);

        executeWithReplacement("UPDATE visit SET organizer_account_id = :replacement WHERE organizer_account_id = :id", accountId, archivalId);
        executeWithReplacement("UPDATE visit_group SET organizer_account_id = :replacement, invitations_enabled = FALSE, invite_code_hash = NULL WHERE organizer_account_id = :id", accountId, archivalId);
        executeWithReplacement("UPDATE plan_history SET account_id = :replacement, planning_input_snapshot = NULL, generation_result_snapshot = NULL, failure_reason = NULL WHERE account_id = :id", accountId, archivalId);
        executeWithReplacement("UPDATE payment_order SET buyer_account_id = :replacement WHERE buyer_account_id = :id", accountId, archivalId);
        execute("UPDATE plan_approval SET requested_by_account_id = NULL WHERE requested_by_account_id = :id", accountId);
        execute("UPDATE plan_approval SET required_approver_account_id = NULL WHERE required_approver_account_id = :id", accountId);
        execute("UPDATE plan_approval SET approved_by_account_id = NULL WHERE approved_by_account_id = :id", accountId);

        execute("DELETE FROM privacy_consent WHERE granted_by_account_id = :id OR visitor_profile_id IN (SELECT id FROM visitor_profile WHERE account_id = :id)", accountId);
        execute("DELETE FROM ride_preference WHERE visitor_profile_id IN (SELECT id FROM visitor_profile WHERE account_id = :id)", accountId);
        execute("DELETE FROM family_member WHERE owner_account_id = :id", accountId);
        execute("DELETE FROM visitor_profile WHERE account_id = :id", accountId);
        execute("DELETE FROM notification WHERE account_id = :id", accountId);
        entityManager.flush();
        entityManager.clear();
        execute("DELETE FROM account WHERE id = :id", accountId);
    }

    private void execute(String sql, Integer accountId) {
        entityManager.createNativeQuery(sql).setParameter("id", accountId).executeUpdate();
    }

    private void executeWithReplacement(String sql, Integer accountId, Integer archivalId) {
        entityManager.createNativeQuery(sql).setParameter("id", accountId).setParameter("replacement", archivalId).executeUpdate();
    }
}
