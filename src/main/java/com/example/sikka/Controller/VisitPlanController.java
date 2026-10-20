package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoOut.VisitPlanDetailsDtoOut;
import com.example.sikka.DtoOut.VisitPlanDtoOut;
import com.example.sikka.Service.VisitPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-plan")
public class VisitPlanController {

    private final VisitPlanService visitPlanService;

    @GetMapping("/get/plans/{accountId}/{visitId}")
    public ResponseEntity<List<VisitPlanDtoOut>> getVisitPlans(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitPlans(accountId, visitId));
    }

    @GetMapping("/get/details/{accountId}/{visitPlanId}")
    public ResponseEntity<VisitPlanDetailsDtoOut> getVisitPlanDetails(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        return ResponseEntity.status(200).body(visitPlanService.getVisitPlanDetails(accountId, visitPlanId));
    }

    @PostMapping("/create/individual/{accountId}/{visitId}")
    public ResponseEntity<ApiResponse> createIndividualVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitPlanService.createIndividualVisitPlan(accountId, visitId);
        return ResponseEntity.status(201).body(new ApiResponse("Individual visit plan created successfully"));
    }

    @PostMapping("/create/group/{accountId}/{visitId}")
    public ResponseEntity<ApiResponse> createGroupVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        visitPlanService.createGroupVisitPlan(accountId, visitId);
        return ResponseEntity.status(201).body(new ApiResponse("Group visit plan created successfully"));
    }

    @DeleteMapping("/delete/{accountId}/{visitPlanId}")
    public ResponseEntity<ApiResponse> deleteVisitPlan(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        visitPlanService.deleteVisitPlan(accountId, visitPlanId);
        return ResponseEntity.status(200).body(new ApiResponse("Visit plan deleted successfully"));
    }
}