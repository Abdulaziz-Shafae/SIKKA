package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.GeneratePlanDtoIn;
import com.example.sikka.DtoIn.SuggestReplanDtoIn;
import com.example.sikka.DtoOut.GenerateVisitPlansDtoOut;
import com.example.sikka.Service.VisitPlanService;
import com.example.sikka.Service.MapService;
import com.example.sikka.DtoOut.VisitPlanDetailsDtoOut;
import com.example.sikka.DtoOut.PlanItemDtoOut;
import com.example.sikka.DtoOut.MapRouteDtoOut;
import java.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.example.sikka.Model.Account;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-plan")
public class VisitPlanController {

    private final VisitPlanService visitPlanService;
    private final MapService mapService;

    @GetMapping("/get/map-route/{visitPlanId}")
    public ResponseEntity<?> getVisitPlanMapRoute(@AuthenticationPrincipal Account account, @PathVariable Integer visitPlanId) {
        VisitPlanDetailsDtoOut details = visitPlanService.getVisitPlanDetails(account.getId(), visitPlanId);
        List<PlanItemDtoOut> ordered = new ArrayList<>(details.getPlanItems());
        ordered.sort(Comparator.comparing(PlanItemDtoOut::getRelativeStartMinutes).thenComparing(PlanItemDtoOut::getRelativeEndMinutes));
        List<Map<String, Object>> segments = new ArrayList<>();
        PlanItemDtoOut previous = null;
        for (PlanItemDtoOut item : ordered) {
            String currentType = "RIDE".equals(item.getActivityKind()) ? "RIDE" : item.getPlaceId() != null ? "PLACE" : null;
            if (currentType == null) continue;
            if (previous != null) {
                String previousType = "RIDE".equals(previous.getActivityKind()) ? "RIDE" : "PLACE";
                Integer fromId = "RIDE".equals(previousType) ? previous.getRideId() : previous.getPlaceId();
                Integer toId = "RIDE".equals(currentType) ? item.getRideId() : item.getPlaceId();
                try {
                    MapRouteDtoOut route = mapService.getRoute(previousType, fromId, currentType, toId);
                    segments.add(Map.of("fromPlanItemId", previous.getId(), "toPlanItemId", item.getId(), "route", route, "available", true));
                } catch (com.example.sikka.Api.ApiException routeUnavailable) {
                    segments.add(Map.of("fromPlanItemId", previous.getId(), "toPlanItemId", item.getId(), "available", false, "reason", "NO_VERIFIED_ROUTE"));
                }
            }
            previous = item;
        }
        return ResponseEntity.ok(Map.of("visitPlanId", visitPlanId, "planItems", ordered, "segments", segments));
    }

    @GetMapping("/get/plans/{visitId}")
    public ResponseEntity<?> getVisitPlans(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitPlans(account.getId(), visitId));
    }

    @GetMapping("/get/details/{visitPlanId}")
    public ResponseEntity<?> getVisitPlanDetails(@AuthenticationPrincipal Account account, @PathVariable Integer visitPlanId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitPlanDetails(account.getId(), visitPlanId));
    }

    @PostMapping("/create/individual/{visitId}")
    public ResponseEntity<?> createIndividualVisitPlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        visitPlanService.createIndividualVisitPlan(account.getId(), visitId);
        return ResponseEntity.status(201).body(new ApiResponse("Individual visit plan created successfully"));
    }

    @PostMapping("/create/group/{visitId}")
    public ResponseEntity<?> createGroupVisitPlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        visitPlanService.createGroupVisitPlan(account.getId(), visitId);
        return ResponseEntity.status(201).body(new ApiResponse("Group visit plan created successfully"));
    }

    @DeleteMapping("/delete/{visitPlanId}")
    public ResponseEntity<?> deleteVisitPlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitPlanId) {
        visitPlanService.deleteVisitPlan(account.getId(), visitPlanId);
        return ResponseEntity.status(200).body(new ApiResponse("Visit plan deleted successfully"));
    }

    @GetMapping("/get/my-individual-plan/{visitId}")
    public ResponseEntity<?> getMyIndividualVisitPlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getMyIndividualVisitPlan(account.getId(), visitId));
    }

    @GetMapping("/get/approved-group-plan/{visitId}")
    public ResponseEntity<?> getApprovedGroupVisitPlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getApprovedGroupVisitPlan(account.getId(), visitId));
    }

    @GetMapping("/get/pending-group-plan/{visitId}")
    public ResponseEntity<?> getPendingGroupVisitPlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getPendingGroupVisitPlan(account.getId(), visitId));
    }

    @GetMapping("/meeting-points/{visitId}")
    public ResponseEntity<?> getVisitMeetingPoints(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitMeetingPoints(account.getId(), visitId));
    }

    @GetMapping("/next-activity/{visitId}")
    public ResponseEntity<?> getMyNextActivity(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getMyNextActivity(account.getId(), visitId));
    }

    @GetMapping("/my-subgroup/{visitId}")
    public ResponseEntity<?> getMyCurrentSubgroup(@AuthenticationPrincipal Account account, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getMyCurrentSubgroup(account.getId(), visitId));
    }

    @PostMapping("/generate/{visitId}")
    public ResponseEntity<?> generatePlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId, @RequestBody @Valid GeneratePlanDtoIn dto) {
        GenerateVisitPlansDtoOut result = visitPlanService.generatePlan(account.getId(), visitId, dto);
        return ResponseEntity.status("PROCESSING".equals(result.getRequestStatus()) ? 202 : 200).body(result);
    }

    @PostMapping("/regenerate/group/{visitId}")
    public ResponseEntity<?> regeneratePlan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId, @RequestBody @Valid GeneratePlanDtoIn dto) {

        GenerateVisitPlansDtoOut result = visitPlanService.regeneratePlan(account.getId(), visitId, dto);

        if ("PROCESSING".equals(result.getRequestStatus())) {
            return ResponseEntity.status(202).body(result);
        }

        return ResponseEntity.status(200).body(result);
    }

    @PostMapping("/suggest-replan/{visitId}")
    public ResponseEntity<?> suggestReplan(@AuthenticationPrincipal Account account, @PathVariable Integer visitId, @RequestBody @Valid SuggestReplanDtoIn dto) {
        return ResponseEntity.status(200).body(visitPlanService.suggestReplan(account.getId(), visitId, dto));
    }

}