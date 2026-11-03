package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.PlanHistory;
import com.example.sikka.Model.Visit;
import com.example.sikka.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FreeModePlanningTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final PlanHistoryRepository histories = mock(PlanHistoryRepository.class);
    private final PaymentOrderRepository payments = mock(PaymentOrderRepository.class);

    private PaymentOrderService service(boolean paymentsEnabled) {
        PaymentOrderService service = new PaymentOrderService(mock(jakarta.persistence.EntityManager.class), payments, accounts, visits, histories, mock(MoyasarService.class));
        ReflectionTestUtils.setField(service, "paymentsEnabled", paymentsEnabled);
        return service;
    }

    private Visit visit(String type, boolean reused) {
        Account owner = new Account();
        owner.setId(1);
        owner.setStatus("ACTIVE");
        owner.setArchivedFreeVisits(100L);
        Visit visit = new Visit();
        visit.setId(77);
        visit.setOrganizer(owner);
        visit.setTripType(type);
        visit.setStatus("DRAFT");
        if (reused) visit.setReusedFromVisitId(4);
        when(accounts.findAccountById(1)).thenReturn(owner);
        when(visits.findVisitById(77)).thenReturn(visit);
        when(visits.findVisitByIdForUpdate(77)).thenReturn(visit);
        when(histories.findAccountByIdForGenerationUpdate(1)).thenReturn(owner);
        return visit;
    }

    @Test
    void allVisitTypesAreFreeRegardlessOfPastQuotaOrReuse() {
        for (String type : new String[]{"SOLO", "FRIENDS", "FAMILY"}) {
            Visit current = visit(type, true);
            var access = service(false).getPlanningAccess(1, 77);
            assertFalse(access.getPaymentRequired());
            assertTrue(access.getGenerationAllowed());
            assertEquals("FREE_UNLIMITED", access.getEntitlementStatus());
            assertEquals(BigDecimal.ZERO, access.getPriceSar());
            assertEquals(0, access.getFreeRemaining());
            verifyNoInteractions(payments);
            reset(accounts, visits, histories, payments);
        }
    }

    @Test
    void repeatedFreeVisitReservesGenerationWithoutPaymentOrTwoVisitQuota() {
        Visit current = visit("FAMILY", true);
        PlanHistoryRepository historyRepository = histories;
        when(historyRepository.save(any(PlanHistory.class))).thenAnswer(invocation -> {
            PlanHistory saved = invocation.getArgument(0);
            saved.setId(99);
            return saved;
        });
        Integer historyId = service(false).reserveEntitlement(1, 77, "free-generation", "REGENERATE");
        assertEquals(99, historyId);
        assertFalse(Boolean.TRUE.equals(current.getFreeSlotReserved()));
        org.mockito.ArgumentCaptor<PlanHistory> captured = org.mockito.ArgumentCaptor.forClass(PlanHistory.class);
        verify(histories).save(captured.capture());
        assertEquals("FREE", captured.getValue().getBillingMode());
        verifyNoInteractions(payments);
    }

    @Test
    void moyasarOrdersRemainDisabledUntilPaidModeIsReenabled() {
        PaymentOrderService free = service(false);
        assertTrue(free.isFreeMode());
        assertThrows(ApiException.class, () -> free.addPaymentOrder(1, 77));
        assertThrows(ApiException.class, () -> free.confirmPayment(1, 9, "payment-test"));
        verifyNoInteractions(payments);
        assertFalse(service(true).isFreeMode());
    }
}
