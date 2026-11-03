package com.example.sikka.Service;

import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import com.example.sikka.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreeModePlanningTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final PlanHistoryRepository histories = mock(PlanHistoryRepository.class);
    private final PaymentOrderRepository payments = mock(PaymentOrderRepository.class);

    private PaymentOrderService service() {
        PaymentOrderService service = new PaymentOrderService(mock(jakarta.persistence.EntityManager.class), payments, accounts, visits, histories, mock(MoyasarService.class), mock(NotificationEventService.class));
        ReflectionTestUtils.setField(service, "paymentsEnabled", false);
        return service;
    }

    @Test
    void unlimitedFreePreviewRequiresNoPaymentEvenAfterPastFreeVisits() {
        Account owner = new Account();
        owner.setId(1);
        owner.setStatus("ACTIVE");
        owner.setArchivedFreeVisits(100L);
        Visit visit = new Visit();
        visit.setId(77);
        visit.setOrganizer(owner);
        visit.setTripType("FRIENDS");
        visit.setStatus("DRAFT");
        when(accounts.findAccountById(1)).thenReturn(owner);
        when(visits.findVisitById(77)).thenReturn(visit);
        var access = service().getPlanningAccess(1,77);
        assertFalse(access.getPaymentRequired());
        assertEquals(BigDecimal.ZERO, access.getPriceSar());
        assertEquals("FREE_UNLIMITED", access.getEntitlementStatus());
        assertTrue(access.getGenerationAllowed());
        verifyNoInteractions(payments);
    }

    @Test
    void disabledPaymentIntegrationDoesNotOpenACharge() {
        assertTrue(service().isFreeMode());
        assertThrows(com.example.sikka.Api.ApiException.class, () -> service().addPaymentOrder(1,77));
        verifyNoInteractions(payments);
    }
}
