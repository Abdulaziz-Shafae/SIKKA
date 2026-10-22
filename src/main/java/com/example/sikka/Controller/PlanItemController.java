package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.PlanItemDtoIn;
import com.example.sikka.Service.PlanItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plan-item")
public class PlanItemController {

    private final PlanItemService planItemService;

    @GetMapping("/get/items/{accountId}/{visitPlanId}")
    public ResponseEntity<?> getPlanItems(@PathVariable Integer accountId, @PathVariable Integer visitPlanId) {
        return ResponseEntity.status(200).body(planItemService.getPlanItems(accountId, visitPlanId));
    }

    @GetMapping("/get/details/{accountId}/{planItemId}")
    public ResponseEntity<?> getPlanItemDetails(@PathVariable Integer accountId, @PathVariable Integer planItemId) {
        return ResponseEntity.status(200).body(planItemService.getPlanItemDetails(accountId, planItemId));
    }

    @PutMapping("/update/preferences/{accountId}")
    public ResponseEntity<?> updatePlanItemPreferences(@PathVariable Integer accountId, @RequestBody @Valid PlanItemDtoIn planItemDtoIn) {
        planItemService.updatePlanItemPreferences(accountId, planItemDtoIn);
        return ResponseEntity.status(200).body(new ApiResponse("Plan item preferences updated successfully"));
    }
}