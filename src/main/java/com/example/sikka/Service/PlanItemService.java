package com.example.sikka.Service;

import com.example.sikka.Api.ApiException;
import com.example.sikka.DtoIn.PlanItemDtoIn;
import com.example.sikka.DtoOut.PlanItemDtoOut;
import com.example.sikka.Model.Account;
import com.example.sikka.Model.PlanItem;
import com.example.sikka.Model.Visit;
import com.example.sikka.Model.VisitMember;
import com.example.sikka.Model.VisitPlan;
import com.example.sikka.Repository.AccountRepository;
import com.example.sikka.Repository.PlanItemRepository;
import com.example.sikka.Repository.VisitMemberRepository;
import com.example.sikka.Repository.VisitPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanItemService {

    private final PlanItemRepository planItemRepository;
    private final VisitPlanRepository visitPlanRepository;
    private final AccountRepository accountRepository;
    private final VisitMemberRepository visitMemberRepository;

    @Transactional(readOnly = true)
    public List<PlanItemDtoOut> getPlanItems(Integer accountId, Integer visitPlanId) {
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
                    throw new ApiException("You cannot view another member's individual activities");
                }
            }
        }

        List<PlanItem> planItems = planItemRepository.findAllByVisitPlan_Id(visitPlanId);

        if (planItems.isEmpty()) {
            throw new ApiException("No activities found for this plan");
        }

        planItems.sort(Comparator.comparing(PlanItem::getSortOrder).thenComparing(PlanItem::getId));

        List<PlanItemDtoOut> planItemDtoOuts = new ArrayList<>();

        for (PlanItem planItem : planItems) {
            planItemDtoOuts.add(convertPlanItemToDtoOut(planItem));
        }

        return planItemDtoOuts;
    }

    @Transactional(readOnly = true)
    public PlanItemDtoOut getPlanItemDetails(Integer accountId, Integer planItemId) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        PlanItem planItem = planItemRepository.findPlanItemById(planItemId);

        if (planItem == null) {
            throw new ApiException("Plan item not found");
        }

        VisitPlan visitPlan = planItem.getVisitPlan();
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
                    throw new ApiException("You cannot view another member's individual activities");
                }
            }
        }

        return convertPlanItemToDtoOut(planItem);
    }

    @Transactional
    public void updatePlanItemPreferences(Integer accountId, PlanItemDtoIn planItemDtoIn) {
        Account account = accountRepository.findAccountById(accountId);

        if (account == null) {
            throw new ApiException("Account not found");
        }

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new ApiException("Account is not active");
        }

        if (planItemDtoIn == null || planItemDtoIn.getPlanItemId() == null || planItemDtoIn.getPlanItemId() <= 0) {
            throw new ApiException("A positive plan item ID is required");
        }

        PlanItem planItem = planItemRepository.findPlanItemById(planItemDtoIn.getPlanItemId());

        if (planItem == null) {
            throw new ApiException("Plan item not found");
        }

        VisitPlan visitPlan = visitPlanRepository.findVisitPlanByIdForUpdate(planItem.getVisitPlan().getId());

        if (visitPlan == null) {
            throw new ApiException("Visit plan not found");
        }

        Visit visit = visitPlan.getVisit();

        if ("GROUP".equals(visitPlan.getPlanKind())) {
            if (!visit.getOrganizer().getId().equals(accountId)) {
                throw new ApiException("Only the visit organizer can change group activity preferences");
            }
        } else if ("INDIVIDUAL".equals(visitPlan.getPlanKind())) {
            VisitMember owner = visitPlan.getOwnerVisitMember();

            if (owner == null) {
                throw new ApiException("Individual plan owner not found");
            }

            if (!owner.getVisitorProfile().getAccount().getId().equals(accountId)) {
                throw new ApiException("Only the plan owner can change individual activity preferences");
            }

            if (!"GOING".equals(owner.getRsvpStatus())) {
                throw new ApiException("The plan owner must be GOING");
            }

            if (visitPlanRepository.existsByVisit_IdAndPlanKind(visit.getId(), "GROUP")) {
                throw new ApiException("Use replanning to change preferences after the group plan is created");
            }
        } else {
            throw new ApiException("Invalid plan kind");
        }

        if (!"PLANNING".equals(visit.getStatus())) {
            throw new ApiException("Activity preferences cannot be changed in the current visit status");
        }

        if (Boolean.TRUE.equals(visitPlan.getIsActive())) {
            throw new ApiException("Active plans must be changed through replanning");
        }

        if (visitPlan.getPlanApproval() != null) {
            throw new ApiException("Use the approval flow before changing a submitted plan");
        }

        if (visitPlan.getProposedItems() != null) {
            throw new ApiException("A plan with a pending proposal cannot be edited directly");
        }

        if (planItemDtoIn == null) {
            throw new ApiException("Activity preferences are required");
        }

        String priority = planItemDtoIn.getPriority();

        if (priority != null && !"MUST_RIDE".equals(priority) && !"PREFERRED".equals(priority) && !"OPTIONAL".equals(priority)) {
            throw new ApiException("Priority must be MUST_RIDE, PREFERRED or OPTIONAL");
        }

        if (priority != null && !"RIDE".equals(planItem.getActivityKind())) {
            throw new ApiException("Priority can only be assigned to ride activities");
        }

        if (planItemDtoIn.getPinned() == null) {
            throw new ApiException("Pinned status is required");
        }

        planItem.setPriority(priority);
        planItem.setPinned(planItemDtoIn.getPinned());
        planItemRepository.save(planItem);
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