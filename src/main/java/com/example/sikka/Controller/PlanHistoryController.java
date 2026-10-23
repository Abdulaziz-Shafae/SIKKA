package com.example.sikka.Controller;

import com.example.sikka.DtoOut.PlanHistoryDtoOut;
import com.example.sikka.Service.PlanHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/plan-history")
public class PlanHistoryController {

    private final PlanHistoryService planHistoryService;

    @GetMapping("/my-history/{accountId}")
    public ResponseEntity<List<PlanHistoryDtoOut>> getMyPlanHistory(@PathVariable Integer accountId) {
        return ResponseEntity.status(200).body(planHistoryService.getMyPlanHistory(accountId));
    }

    @GetMapping("/details/{accountId}/{planHistoryId}")
    public ResponseEntity<PlanHistoryDtoOut> getPlanHistoryDetails(@PathVariable Integer accountId, @PathVariable Integer planHistoryId) {
        return ResponseEntity.status(200).body(planHistoryService.getPlanHistoryDetails(accountId, planHistoryId));
    }
}