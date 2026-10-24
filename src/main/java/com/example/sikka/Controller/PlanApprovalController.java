package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.PlanApprovalDtoIn;
import com.example.sikka.Service.PlanApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plan-approval")
public class PlanApprovalController {

    private final PlanApprovalService planApprovalService;

    @GetMapping("/get/{accountId}/{visitPlanId}")
    public ResponseEntity<?> getPlanApproval(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        return ResponseEntity.status(200).body(planApprovalService.getPlanApproval(accountId, visitPlanId));
    }

    @PostMapping("/create/{accountId}/{visitPlanId}")
    public ResponseEntity<?> createPlanApproval(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        planApprovalService.createPlanApproval(accountId, visitPlanId);
        return ResponseEntity.status(201).body(new ApiResponse("Plan approval created successfully"));
    }

    @PutMapping("/respond/{accountId}")
    public ResponseEntity<?> respondToPlanApproval(@PathVariable Integer accountId, @RequestBody @Valid PlanApprovalDtoIn planApprovalDtoIn) {
        planApprovalService.respondToPlanApproval(accountId, planApprovalDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("Plan approval response saved successfully"));
    }
}
