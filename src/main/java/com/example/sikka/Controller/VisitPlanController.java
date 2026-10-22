package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.Service.VisitPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-plan")
public class VisitPlanController {

    private final VisitPlanService visitPlanService;

    @GetMapping("/get/plans/{accountId}/{visitId}")
    public ResponseEntity<?> getVisitPlans(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitPlans(accountId, visitId));
    }

    @GetMapping("/get/details/{accountId}/{visitPlanId}")
    public ResponseEntity<?> getVisitPlanDetails(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitPlanDetails(accountId, visitPlanId));
    }

    @PostMapping("/create/individual/{accountId}/{visitId}")
    public ResponseEntity<?> createIndividualVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitPlanService.createIndividualVisitPlan(accountId, visitId);
        return ResponseEntity.status(201).body(new ApiResponse("Individual visit plan created successfully"));
    }

    @PostMapping("/create/group/{accountId}/{visitId}")
    public ResponseEntity<?> createGroupVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitPlanService.createGroupVisitPlan(accountId, visitId);
        return ResponseEntity.status(201).body(new ApiResponse("Group visit plan created successfully"));
    }

    @DeleteMapping("/delete/{accountId}/{visitPlanId}")
    public ResponseEntity<?> deleteVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        visitPlanService.deleteVisitPlan(accountId, visitPlanId);
        return ResponseEntity.status(200).body(new ApiResponse("Visit plan deleted successfully"));
    }

    @GetMapping("/get/my-individual-plan/{accountId}/{visitId}")
    public ResponseEntity<?> getMyIndividualVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getMyIndividualVisitPlan(accountId, visitId));
    }

    @GetMapping("/get/approved-group-plan/{accountId}/{visitId}")
    public ResponseEntity<?> getApprovedGroupVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getApprovedGroupVisitPlan(accountId, visitId));
    }

    @GetMapping("/get/pending-group-plan/{accountId}/{visitId}")
    public ResponseEntity<?> getPendingGroupVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getPendingGroupVisitPlan(accountId, visitId));
    }

    @GetMapping("/meeting-points/{accountId}/{visitId}")
    public ResponseEntity<?> getVisitMeetingPoints(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitMeetingPoints(accountId, visitId));
    }

    @GetMapping("/next-activity/{accountId}/{visitId}")
    public ResponseEntity<?> getMyNextActivity(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getMyNextActivity(accountId, visitId));
    }

    @GetMapping("/my-subgroup/{accountId}/{visitId}")
    public ResponseEntity<?> getMyCurrentSubgroup(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getMyCurrentSubgroup(accountId, visitId));
    }
}
