package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.PaymentOrder;
import com.example.sikka.Model.Visit;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.PaymentOrderRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentOrderService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;

    public List<PaymentOrder> getMyPaymentOrders(Integer accountId){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        return paymentOrderRepository.findPaymentOrdersByBuyerAccountId(accountId);
    }

    public PaymentOrder getPaymentOrderById(Integer accountId, Integer paymentOrderId){
        PaymentOrder paymentOrder = paymentOrderRepository.findPaymentOrderById(paymentOrderId);

        if (paymentOrder == null){
            throw new ApiException("Payment order not found");
        }

        if (!paymentOrder.getBuyerAccount().getId().equals(accountId)){
            throw new ApiException("You cannot access another user's payment order");
        }

        return paymentOrder;
    }

    public List<PaymentOrder> getVisitPaymentOrders(Integer accountId, Integer visitId){
        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null){
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)){
            throw new ApiException("Only the visit organizer can view its payment orders");
        }

        return paymentOrderRepository.findPaymentOrdersByVisitId(visitId);
    }

    @Transactional
    public PaymentOrder addPaymentOrder(Integer accountId, Integer visitId){
        Account account = accountRepository.findAccountById(accountId);

        if (account == null){
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }
        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);

        if (visit == null){
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)){
            throw new ApiException("Only the visit organizer can pay for the plan");
        }

        if (visit.getVisitDate() == null || visit.getVisitDate().isBefore(LocalDate.now())){
            throw new ApiException("Cannot pay for a past visit");
        }

        if (!"DRAFT".equals(visit.getStatus())){
            throw new ApiException("Payment is only allowed for draft visits");
        }

        List<PaymentOrder> paymentOrders = paymentOrderRepository.findPaymentOrdersByVisitId(visitId);

        PaymentOrder latestPaymentOrder = null;

        for (PaymentOrder paymentOrder : paymentOrders){
            if ("PAID".equals(paymentOrder.getStatus())){
                throw new ApiException("This visit plan has already been paid for");
            }

            if ("PENDING".equals(paymentOrder.getStatus())){
                throw new ApiException("A pending payment order already exists for this visit");
            }

            if (!"FAILED".equals(paymentOrder.getStatus())){
                throw new ApiException("An existing payment order has an unsupported status");
            }

            if (latestPaymentOrder == null || paymentOrder.getId() > latestPaymentOrder.getId()){
                latestPaymentOrder = paymentOrder;
            }
        }

        PaymentOrder newPaymentOrder = new PaymentOrder();

        if (latestPaymentOrder == null){
            newPaymentOrder.setOrderGroupKey(UUID.randomUUID().toString());
            newPaymentOrder.setAttemptNo(1);
            newPaymentOrder.setKind("INITIAL");
        } else {
            newPaymentOrder.setOrderGroupKey(latestPaymentOrder.getOrderGroupKey());
            newPaymentOrder.setAttemptNo(latestPaymentOrder.getAttemptNo() + 1);
            newPaymentOrder.setKind("RETRY");
        }

        newPaymentOrder.setAmountSar(new BigDecimal("19.00"));
        newPaymentOrder.setStatus("PENDING");
        newPaymentOrder.setVisit(visit);
        newPaymentOrder.setBuyerAccount(account);

        return paymentOrderRepository.save(newPaymentOrder);
    }
}