package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.Visit;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FreeRepeatedVisitEditsTest {
    private Visit repeatedVisit() {
        Account organizer = new Account();
        organizer.setId(25);
        Visit visit = new Visit();
        visit.setId(99);
        visit.setOrganizer(organizer);
        visit.setReusedFromVisitId(8);
        return visit;
    }

    @Test
    void repeatMemberEditsDoNotRequirePaymentWhenFreeModeEnabled() {
        PaymentOrderService payments = mock(PaymentOrderService.class);
        when(payments.isFreeMode()).thenReturn(true);
        VisitMemberService service = mock(VisitMemberService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "paymentOrderService", payments);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "requirePaidRepeatedEdit", repeatedVisit()));
        verify(payments, never()).hasPaidVisit(anyInt(), anyInt());
    }

    @Test
    void repeatMealEditsDoNotRequirePaymentWhenFreeModeEnabled() {
        PaymentOrderService payments = mock(PaymentOrderService.class);
        when(payments.isFreeMode()).thenReturn(true);
        VisitMealBreakService service = mock(VisitMealBreakService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "paymentOrderService", payments);
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "requirePaidRepeatedEdit", repeatedVisit()));
        verify(payments, never()).hasPaidVisit(anyInt(), anyInt());
    }

    @Test
    void oldPaidModeStillRequiresVerifiedPaymentForRepeatedMembersAndMeals() {
        PaymentOrderService payments = mock(PaymentOrderService.class);
        when(payments.isFreeMode()).thenReturn(false);
        VisitMemberService members = mock(VisitMemberService.class, CALLS_REAL_METHODS);
        VisitMealBreakService meals = mock(VisitMealBreakService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(members, "paymentOrderService", payments);
        ReflectionTestUtils.setField(meals, "paymentOrderService", payments);
        assertThrows(ApiException.class, () -> ReflectionTestUtils.invokeMethod(members, "requirePaidRepeatedEdit", repeatedVisit()));
        assertThrows(ApiException.class, () -> ReflectionTestUtils.invokeMethod(meals, "requirePaidRepeatedEdit", repeatedVisit()));
    }
}
