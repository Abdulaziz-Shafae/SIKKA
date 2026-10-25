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

    private final jakarta.persistence.EntityManager entityManager;
    private final PaymentOrderRepository paymentOrderRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final com.example.sikka.Repository.PlanHistoryRepository planHistoryRepository;

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

        if (visit.getVisitDate() == null || visit.getVisitDate().isBefore(LocalDate.now(java.time.ZoneId.of("Asia/Riyadh")))){
            throw new ApiException("Cannot pay for a past visit");
        }

        if (!java.util.Set.of("DRAFT", "PLANNING", "READY").contains(visit.getStatus()) || visit.getStartedAt() != null){
            throw new ApiException("Payment is only allowed for draft visits");
        }

        List<PaymentOrder> paymentOrders = paymentOrderRepository.findPaymentOrdersByVisitId(visitId);

        PaymentOrder latestPaymentOrder = null;

        for (PaymentOrder paymentOrder : paymentOrders){
            if ("PAID".equals(paymentOrder.getStatus())){
                if (paymentOrder.getConsumedAt() == null) {
                    throw new ApiException("An unused paid generation is already available");
                }
                continue;
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

    @Transactional(readOnly = true)
    public com.example.sikka.DtoOut.PlanningAccessDtoOut getPlanningAccess(Integer accountId, Integer visitId) {
        Visit visit = getPlanningVisit(accountId, visitId);
        long reservedFree = planHistoryRepository.findAllByAccount_IdOrderByStartedAtDescIdDesc(accountId).stream().filter(history -> "FREE".equals(history.getBillingMode()) && ("PROCESSING".equals(history.getStatus()) || "SUCCEEDED".equals(history.getStatus()) && Boolean.TRUE.equals(history.getCountsTowardQuota()))).count();
        Account account = accountRepository.findAccountById(accountId);
        entityManager.refresh(account);
        long archivedFree = account.getArchivedFreeGenerations() == null ? 0L : account.getArchivedFreeGenerations();
        int freeRemaining = (int) Math.max(0L, 2L - reservedFree - archivedFree);
        PaymentOrder payment = findAvailableGenerationPayment(accountId, visitId);
        boolean paymentRequired = freeRemaining == 0 && payment == null;
        boolean allowed = visit.getStartedAt() == null && java.util.Set.of("DRAFT", "PLANNING", "READY").contains(visit.getStatus());
        return new com.example.sikka.DtoOut.PlanningAccessDtoOut(freeRemaining, paymentRequired, new BigDecimal("19.00"), freeRemaining > 0 ? "FREE" : payment == null ? "PAYMENT_REQUIRED" : "PAID", allowed && !paymentRequired);
    }

    @Transactional
    public Integer reserveEntitlement(Integer accountId, Integer visitId, String requestKey, String operationType) {
        Account account = planHistoryRepository.findAccountByIdForGenerationUpdate(accountId);
        Visit visit = getPlanningVisit(accountId, visitId);
        if (account == null || requestKey == null || requestKey.isBlank() || requestKey.length() > 80 || !java.util.Set.of("INITIAL", "REGENERATE").contains(operationType)) {
            throw new ApiException("Valid generation request details are required");
        }
        if (visit.getStartedAt() != null || !java.util.Set.of("DRAFT", "PLANNING", "READY").contains(visit.getStatus())) {
            throw new ApiException("Generation entitlement is only available before the visit starts");
        }
        com.example.sikka.Model.PlanHistory previous = planHistoryRepository.findPlanHistoryByAccount_IdAndRequestKey(accountId, requestKey);
        if (previous != null) {
            if (!previous.getVisit().getId().equals(visitId) || !previous.getOperationType().equals(operationType)) {
                throw new ApiException("Request key is already used for another generation operation");
            }
            return previous.getId();
        }
        com.example.sikka.DtoOut.PlanningAccessDtoOut access = getPlanningAccess(accountId, visitId);
        if (!Boolean.TRUE.equals(access.getGenerationAllowed())) {
            throw new ApiException("A server-verified payment is required before generating another plan");
        }
        com.example.sikka.Model.PlanHistory history = new com.example.sikka.Model.PlanHistory();
        history.setAccount(account);
        history.setVisit(visit);
        history.setRequestKey(requestKey);
        history.setOperationType(operationType);
        history.setBillingMode(access.getFreeRemaining() > 0 ? "FREE" : "PAID");
        if ("PAID".equals(history.getBillingMode())) {
            history.setPaymentOrder(findAvailableGenerationPayment(accountId, visitId));
        }
        return planHistoryRepository.save(history).getId();
    }

    @Transactional
    public void consumeEntitlement(Integer accountId, Integer planHistoryId) {
        planHistoryRepository.findAccountByIdForGenerationUpdate(accountId);
        com.example.sikka.Model.PlanHistory history = planHistoryRepository.findPlanHistoryByIdForUpdate(planHistoryId);
        if (history == null || !history.getAccount().getId().equals(accountId) || !"PROCESSING".equals(history.getStatus())) {
            throw new ApiException("Processing generation request not found");
        }
        if ("PAID".equals(history.getBillingMode())) {
            PaymentOrder payment = history.getPaymentOrder();
            if (!isVerifiedGenerationPayment(payment, accountId, history.getVisit().getId()) || payment.getConsumedAt() != null) {
                throw new ApiException("The reserved payment is unavailable");
            }
            payment.setConsumedByPlanHistory(history);
            payment.setConsumedAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Riyadh")));
            paymentOrderRepository.save(payment);
        }
    }

    @Transactional
    public void releaseEntitlement(Integer accountId, Integer planHistoryId) {
        planHistoryRepository.findAccountByIdForGenerationUpdate(accountId);
        com.example.sikka.Model.PlanHistory history = planHistoryRepository.findPlanHistoryByIdForUpdate(planHistoryId);
        if (history == null || !history.getAccount().getId().equals(accountId) || "SUCCEEDED".equals(history.getStatus())) {
            throw new ApiException("Only an unsuccessful request can release its reservation");
        }
        history.setStatus("FAILED");
        history.setCountsTowardQuota(false);
        planHistoryRepository.save(history);
    }

    private Visit getPlanningVisit(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);
        Visit visit = visitRepository.findVisitById(visitId);
        if (account == null || !"ACTIVE".equals(account.getStatus()) || visit == null || !visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("An active visit organizer is required");
        }
        return visit;
    }

    private boolean isVerifiedGenerationPayment(PaymentOrder payment, Integer accountId, Integer visitId) {
        return payment != null && payment.getVisit() != null && payment.getBuyerAccount().getId().equals(accountId) && payment.getVisit().getId().equals(visitId) && "PAID".equals(payment.getStatus()) && payment.getVerifiedAt() != null && payment.getMoyasarPaymentId() != null && !payment.getMoyasarPaymentId().isBlank() && payment.getAmountSar() != null && payment.getAmountSar().compareTo(new BigDecimal("19.00")) == 0;
    }

    private PaymentOrder findAvailableGenerationPayment(Integer accountId, Integer visitId) {
        List<com.example.sikka.Model.PlanHistory> histories = planHistoryRepository.findAllByAccount_IdOrderByStartedAtDescIdDesc(accountId);
        for (PaymentOrder payment : paymentOrderRepository.findPaymentOrdersByVisitId(visitId)) {
            if (!isVerifiedGenerationPayment(payment, accountId, visitId) || payment.getConsumedAt() != null) {
                continue;
            }
            boolean reserved = histories.stream().anyMatch(history -> history.getPaymentOrder() != null && history.getPaymentOrder().getId().equals(payment.getId()) && java.util.Set.of("PROCESSING", "SUCCEEDED").contains(history.getStatus()));
            if (!reserved) {
                return payment;
            }
        }
        return null;
    }

}