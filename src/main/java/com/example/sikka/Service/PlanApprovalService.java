package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.PlanApprovalDtoIn;
import com.example.sikka.DtoOut.PlanApprovalDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.PlanApproval;
import com.example.sikka.Model.PlanItem;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitPlan;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.PlanApprovalRepository;
import com.example.sikka.Repository.VisitPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlanApprovalService {

    private final PlanApprovalRepository planApprovalRepository;
    private final VisitPlanRepository visitPlanRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public PlanApprovalDtoOut getPlanApproval(Integer accountId, Integer visitPlanId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanById(visitPlanId);

        if (visitPlan == null) {
            throw new ApiException("Visit plan not found");
        }

        if (!visitPlan.getVisit().getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can view this approval");
        }

        PlanApproval planApproval = planApprovalRepository.findPlanApprovalByVisitPlan_Id(visitPlanId);

        if (planApproval == null) {
            throw new ApiException("Plan approval not found");
        }

        return new PlanApprovalDtoOut(planApproval.getId(), planApproval.getVisitPlan().getId(), planApproval.getApprovedByAccount().getId(), planApproval.getStatus(), planApproval.getRespondedAt());

    }

    @Transactional
    public void createPlanApproval(Integer accountId, Integer visitPlanId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanByIdForUpdate(visitPlanId);

        if (visitPlan == null) {
            throw new ApiException("Visit plan not found");
        }

        Visit visit = visitPlan.getVisit();

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can submit the group plan");
        }

        if (!"GROUP".equals(visitPlan.getPlanKind())) {
            throw new ApiException("Approval is only required for group plans");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("The visit must be PLANNING before submitting its plan");
        }

        if (Boolean.TRUE.equals(visitPlan.getIsActive())) {
            throw new ApiException("This plan is already active");
        }

        if (visitPlan.getProposedItems() != null) {
            throw new ApiException("Replanning proposals must use the replanning flow");
        }

        if (visitPlan.getPlanItems() == null || visitPlan.getPlanItems().isEmpty()) {
            throw new ApiException("Generate the group activities before submitting the plan");
        }

        PlanApproval existingApproval = planApprovalRepository.findPlanApprovalByVisitPlan_Id(visitPlanId);

        if (existingApproval != null) {
            throw new ApiException("This plan already has an approval record");
        }

        PlanApproval planApproval = new PlanApproval();
        planApproval.setVisitPlan(visitPlan);
        planApproval.setApprovedByAccount(account);
        planApproval.setStatus("PENDING");
        planApproval.setRespondedAt(null);
        planApprovalRepository.save(planApproval);
        visitPlan.setPlanApproval(planApproval);
    }

    @Transactional
    public void respondToPlanApproval(Integer accountId, PlanApprovalDtoIn planApprovalDtoIn) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        if (planApprovalDtoIn == null || planApprovalDtoIn.getVisitPlanId() == null) {
            throw new ApiException("Visit plan ID is required");
        }

        String status = planApprovalDtoIn.getStatus();

        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new ApiException("Response must be APPROVED or REJECTED");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanByIdForUpdate(planApprovalDtoIn.getVisitPlanId());

        if (visitPlan == null) {
            throw new ApiException("Visit plan not found");
        }

        Visit visit = visitPlan.getVisit();

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can respond to the approval");
        }

        if (!"GROUP".equals(visitPlan.getPlanKind())) {
            throw new ApiException("Approval is only required for group plans");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("The visit must be PLANNING before responding");
        }

        if (visitPlan.getProposedItems() != null) {
            throw new ApiException("Replanning proposals must use the replanning flow");
        }

        if (Boolean.TRUE.equals(visitPlan.getIsActive())) {
            throw new ApiException("This plan is already active");
        }

        PlanApproval planApproval = planApprovalRepository.findPlanApprovalByVisitPlan_Id(visitPlan.getId());

        if (planApproval == null) {
            throw new ApiException("Plan approval not found");
        }

        if (!"PENDING".equals(planApproval.getStatus())) {
            throw new ApiException("This approval has already been answered");
        }

        if ("APPROVED".equals(status)) {
            if (visitPlan.getPlanItems() == null || visitPlan.getPlanItems().isEmpty()) {
                throw new ApiException("An empty plan cannot be approved");
            }

            for (PlanItem planItem : visitPlan.getPlanItems()) {
                if (planItem.getStartsAt() == null || planItem.getEndsAt() == null) {
                    throw new ApiException("All activities must have start and end times");
                }

                if (!planItem.getEndsAt().isAfter(planItem.getStartsAt())) {
                    throw new ApiException("Activity end time must be after its start time");
                }

                if ("RIDE".equals(planItem.getActivityKind())) {
                    if (planItem.getRide() == null) {
                        throw new ApiException("A ride activity must reference a ride");
                    }

                    if (!"OPEN".equals(planItem.getRide().getStatus())) {
                        throw new ApiException("The plan contains a ride that is not open");
                    }
                }

                if ("PLACE".equals(planItem.getActivityKind()) || "MEETING_POINT".equals(planItem.getActivityKind())) {
                    if (planItem.getPlace() == null) {
                        throw new ApiException("A place activity must reference a place");
                    }
                }
            }

            visitPlan.setIsActive(true);
            visit.setStatus("READY");
        }

        planApproval.setStatus(status);
        planApproval.setApprovedByAccount(account);
        planApproval.setRespondedAt(LocalDateTime.now());
        planApprovalRepository.save(planApproval);
    }


}
