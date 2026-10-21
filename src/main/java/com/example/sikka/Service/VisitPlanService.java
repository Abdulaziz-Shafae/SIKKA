package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoOut.PlanItemDtoOut;
import com.example.sikka.DtoOut.VisitPlanDetailsDtoOut;
import com.example.sikka.DtoOut.VisitPlanDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.PlanItem;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitPlan;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.PaymentOrderRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitPlanRepository;
import com.example.sikka.Repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitPlanService {

    private final VisitPlanRepository visitPlanRepository;
    private final AccountRepository accountRepository;
    private final VisitRepository visitRepository;
    private final VisitMemberRepository visitMemberRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    @Transactional(readOnly = true)
    public List<VisitPlanDtoOut> getVisitPlans(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitById(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can view all plans");
        }

        List<VisitPlan> visitPlans = visitPlanRepository.findAllByVisit_Id(visitId);

        if (visitPlans.isEmpty()) {
            throw new ApiException("No plans found for this visit");
        }

        List<VisitPlanDtoOut> visitPlanDtoOuts = new ArrayList<>();

        for (VisitPlan visitPlan : visitPlans) {
            visitPlanDtoOuts.add(convertVisitPlanToDtoOut(visitPlan));
        }

        return visitPlanDtoOuts;
    }

    @Transactional(readOnly = true)
    public VisitPlanDetailsDtoOut getVisitPlanDetails(Integer accountId, Integer visitPlanId) {
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

        Visit visit = visitPlan.getVisit();

        if (!visit.getOrganizer().getId().equals(accountId)) {
            VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visit.getId(), accountId);

            if (visitMember == null) {
                throw new ApiException("You are not a member of this visit");
            }

            if ("INDIVIDUAL".equals(visitPlan.getPlanKind())) {
                if (visitPlan.getOwnerVisitMember() == null) {
                    throw new ApiException("Individual plan owner not found");
                }

                if (!visitPlan.getOwnerVisitMember().getId().equals(visitMember.getId())) {
                    throw new ApiException("You cannot view another member's individual plan");
                }
            }
        }

        List<PlanItemDtoOut> planItemDtoOuts = new ArrayList<>();

        if (visitPlan.getPlanItems() != null) {
            List<PlanItem> planItems = new ArrayList<>(visitPlan.getPlanItems());
            planItems.sort(Comparator.comparing(PlanItem::getSortOrder).thenComparing(PlanItem::getId));

            for (PlanItem planItem : planItems) {
                planItemDtoOuts.add(convertPlanItemToDtoOut(planItem));
            }
        }

        String approvalStatus = null;

        if (visitPlan.getPlanApproval() != null) {
            approvalStatus = visitPlan.getPlanApproval().getStatus();
        }

        return new VisitPlanDetailsDtoOut(convertVisitPlanToDtoOut(visitPlan), approvalStatus, planItemDtoOuts);
    }

    @Transactional
    public void createIndividualVisitPlan(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Plans cannot be created in the current visit status");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can start planning");
        }

        VisitMember visitMember = visitMemberRepository.findVisitMemberByVisit_IdAndVisitorProfile_Account_Id(visitId, accountId);

        if (visitMember == null) {
            throw new ApiException("You are not a member of this visit");
        }

        if (!"GOING".equals(visitMember.getRsvpStatus())) {
            throw new ApiException("Individual plans require a GOING member");
        }

        if (!paymentOrderRepository.existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(visitId, "PAID")) {
            throw new ApiException("A verified payment is required before planning");
        }

        if (visitPlanRepository.existsByVisit_IdAndOwnerVisitMember_IdAndPlanKind(visitId, visitMember.getId(), "INDIVIDUAL")) {
            throw new ApiException("You already have an individual plan for this visit");
        }

        visit.setMembersLocked(true);
        VisitPlan visitPlan = new VisitPlan();
        visitPlan.setVisit(visit);
        visitPlan.setOwnerVisitMember(visitMember);
        visitPlan.setPlanKind("INDIVIDUAL");
        visitPlan.setIsActive(false);
        visitPlanRepository.save(visitPlan);
    }

    @Transactional
    public void createGroupVisitPlan(Integer accountId, Integer visitId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        Visit visit = visitRepository.findVisitByIdForUpdate(visitId);

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can create the group plan");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Plans cannot be created in the current visit status");
        }

        if (!paymentOrderRepository.existsByVisit_IdAndStatusAndVerifiedAtIsNotNull(visitId, "PAID")) {
            throw new ApiException("A verified payment is required before planning");
        }

        if (visitPlanRepository.existsByVisit_IdAndPlanKind(visitId, "GROUP")) {
            throw new ApiException("This visit already has a group plan");
        }

        List<VisitMember> visitMembers = visitMemberRepository.findAllByVisit_IdAndRsvpStatus(visitId, "GOING");

        if (visitMembers.isEmpty()) {
            throw new ApiException("The visit must have at least one GOING member");
        }

        for (VisitMember visitMember : visitMembers) {
            if (!visitPlanRepository.existsByVisit_IdAndOwnerVisitMember_IdAndPlanKind(visitId, visitMember.getId(), "INDIVIDUAL")) {
                throw new ApiException("Every GOING member must have an individual plan first");
            }
        }

        visit.setMembersLocked(true);
        VisitPlan visitPlan = new VisitPlan();
        visitPlan.setVisit(visit);
        visitPlan.setOwnerVisitMember(null);
        visitPlan.setPlanKind("GROUP");
        visitPlan.setIsActive(false);
        visitPlanRepository.save(visitPlan);
    }

    @Transactional
    public void deleteVisitPlan(Integer accountId, Integer visitPlanId) {
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

        Visit visit = visitRepository.findVisitByIdForUpdate(visitPlan.getVisit().getId());

        if (visit == null) {
            throw new ApiException("Visit not found");
        }

        if (!visit.getOrganizer().getId().equals(accountId)) {
            throw new ApiException("Only the visit organizer can delete plans");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Plans cannot be deleted in the current visit status");
        }

        if (Boolean.TRUE.equals(visitPlan.getIsActive())) {
            throw new ApiException("An active plan cannot be deleted");
        }

        if (visitPlan.getPlanApproval() != null) {
            throw new ApiException("A plan with an approval record cannot be deleted");
        }

        if (visitPlan.getProposedItems() != null) {
            throw new ApiException("A plan with a pending proposal cannot be deleted");
        }

        if (visitPlan.getPlanItems() != null && !visitPlan.getPlanItems().isEmpty()) {
            throw new ApiException("Remove the plan items before deleting the plan");
        }

        if ("INDIVIDUAL".equals(visitPlan.getPlanKind()) && visitPlanRepository.existsByVisit_IdAndPlanKind(visit.getId(), "GROUP")) {
            throw new ApiException("An individual plan cannot be deleted while a group plan exists");
        }

        visitPlanRepository.delete(visitPlan);
    }

    @Transactional(readOnly = true)
    public String getPlanStatus(Integer visitId) {
        Visit visit = visitRepository.findVisitById(visitId);
        if (visit == null) {
            throw new ApiException("Visit not found");
        }
        if ("CANCELLED".equals(visit.getStatus())) {
            return "NOT_STARTED";
        }
        for (VisitPlan plan : visitPlanRepository.findAllByVisit_Id(visitId)) {
            if ("GROUP".equals(plan.getPlanKind()) && plan.getPlanItems() != null && !plan.getPlanItems().isEmpty()) {
                for (PlanItem item : plan.getPlanItems()) {
                    if (item.getStartsAt() == null || item.getEndsAt() == null || !item.getEndsAt().isAfter(item.getStartsAt())) {
                        return "NOT_STARTED";
                    }
                }
                if (Boolean.TRUE.equals(plan.getIsActive()) && plan.getPlanApproval() != null && "APPROVED".equals(plan.getPlanApproval().getStatus())) {
                    return "APPROVED";
                }
                return "GENERATED";
            }
        }
        return "NOT_STARTED";
    }

    private VisitPlanDtoOut convertVisitPlanToDtoOut(VisitPlan visitPlan) {
        Integer ownerVisitMemberId = null;
        String ownerName = null;

        if (visitPlan.getOwnerVisitMember() != null) {
            ownerVisitMemberId = visitPlan.getOwnerVisitMember().getId();
            ownerName = visitPlan.getOwnerVisitMember().getVisitorProfile().getName();
        }

        return new VisitPlanDtoOut(visitPlan.getId(), visitPlan.getVisit().getId(), visitPlan.getPlanKind(), ownerVisitMemberId, ownerName, visitPlan.getIsActive(), visitPlan.getCreatedAt());
    }

    private PlanItemDtoOut convertPlanItemToDtoOut(PlanItem planItem) {
        Integer rideId = null;
        String rideName = null;
        Integer placeId = null;
        String placeName = null;

        if (planItem.getRide() != null) {
            rideId = planItem.getRide().getId();
            rideName = planItem.getRide().getName();
        }

        if (planItem.getPlace() != null) {
            placeId = planItem.getPlace().getId();
            placeName = planItem.getPlace().getName();
        }

        return new PlanItemDtoOut(planItem.getId(), planItem.getActivityKind(), planItem.getTitle(), rideId, rideName, placeId, placeName, planItem.getStartsAt(), planItem.getEndsAt(), planItem.getSortOrder(), planItem.getPriority(), planItem.getPinned(), planItem.getParticipantVisitMemberIds(), planItem.getSubgroupKey());
    }
}