package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.PaymentOrderRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitEntitlementService {
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    public String resolveType(String requested, boolean hasGroup) {
        String type = requested == null ? (hasGroup ? "FRIENDS" : "SOLO") : requested;
        if (!type.matches("SOLO|FRIENDS|FAMILY")) throw new ApiException("Invalid visit type");
        if ("SOLO".equals(type) && hasGroup) throw new ApiException("Group visit cannot be SOLO");
        return type;
    }

    public boolean isPaid(Visit visit) {
        return paymentOrderRepository.existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(visit.getId(), "PAID");
    }

    @Transactional
    public void requirePlanningAccess(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountByIdForUpdate(accountId);
        if (account == null || !"ACTIVE".equals(account.getStatus())) throw new ApiException("Account is not active");
        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);
        if (visit == null || !visit.getOrganizer().getId().equals(accountId)) throw new ApiException("Visit organizer mismatch");
        if (isPaid(visit)) return;
        if (Boolean.TRUE.equals(visit.getFreeGenerationUsed())) throw new ApiException("Regeneration requires verified payment");
        if (Boolean.TRUE.equals(visit.getFreeSlotReserved())) return;
        List<Visit> visits = visitRepository.findAllByOrganizer_Id(accountId);
        long used = visits.stream().filter(v -> Boolean.TRUE.equals(v.getFreeSlotReserved()) && !"CANCELLED".equals(v.getStatus()) && !isPaid(v)).count();
        if (used >= 2) throw new ApiException("Free visits exhausted; verified payment required");
        visit.setFreeSlotReserved(true);
        visitRepository.save(visit);
    }

    @Transactional
    public void recordSuccessfulGeneration(Integer visitId) {
        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);
        if (visit == null) throw new ApiException("Visit not found");
        if (isPaid(visit) || Boolean.TRUE.equals(visit.getFreeGenerationUsed())) return;
        if (!Boolean.TRUE.equals(visit.getFreeSlotReserved())) throw new ApiException("Free reservation missing");
        boolean hasActivities = visit.getVisitPlans() != null && visit.getVisitPlans().stream().anyMatch(p -> p.getPlanItems() != null && !p.getPlanItems().isEmpty());
        if (!hasActivities) throw new ApiException("Cannot count empty plan as generated");
        visit.setFreeGenerationUsed(true);
        visitRepository.save(visit);
    }

    public void verifyStartLocked(Visit visit) {
        if (!LocalDate.now(ZoneId.of("Asia/Riyadh")).equals(visit.getVisitDate())) throw new ApiException("Visit can start only on its scheduled date");
        if (!isPaid(visit) && !Boolean.TRUE.equals(visit.getFreeSlotReserved())) throw new ApiException("Free slot or verified payment required");
        // Plan approval already verifies that the scheduled plan contains activities.
    }
}
