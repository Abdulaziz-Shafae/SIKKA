package com.example.sikka.Controller;

import com.example.sikka.DtoIn.*;
import com.example.sikka.DtoOut.ActivitySessionDtoOut;
import com.example.sikka.Service.ActivitySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/activity-session")
public class ActivitySessionController {

    private final ActivitySessionService activitySessionService;

    @GetMapping("/history/{accountId}/{visitId}")
    public ResponseEntity<List<ActivitySessionDtoOut>> getMyActivityHistory(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(activitySessionService.getMyActivityHistory(accountId, visitId));
    }

    @GetMapping("/current/{accountId}/{visitId}")
    public ResponseEntity<ActivitySessionDtoOut> getMyCurrentActivity(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        return ResponseEntity.status(200).body(activitySessionService.getMyCurrentActivity(accountId, visitId));
    }

    @PostMapping("/start/manual/{accountId}/{visitMemberId}/{planItemId}")
    public ResponseEntity<ActivitySessionDtoOut> startManualActivity(@PathVariable Integer accountId, @PathVariable Integer visitMemberId, @PathVariable Integer planItemId) {
        return ResponseEntity.status(200).body(activitySessionService.startManualActivity(accountId, visitMemberId, planItemId, null).get(0));    }

    @PostMapping("/complete/{accountId}/{activitySessionId}")
    public ResponseEntity<ActivitySessionDtoOut> completeActivity(@PathVariable Integer accountId, @PathVariable Integer activitySessionId, @RequestBody @Valid CompleteActivityDtoIn completeActivityDtoIn) {
        return ResponseEntity.status(200).body(activitySessionService.completeActivity(accountId, activitySessionId, completeActivityDtoIn, null).get(0));
    }

    @PutMapping("/rate/{accountId}/{activitySessionId}")
    public ResponseEntity<ActivitySessionDtoOut> rateActivity(@PathVariable Integer accountId, @PathVariable Integer activitySessionId, @RequestBody @Valid RateActivityDtoIn rateActivityDtoIn) {
        return ResponseEntity.status(200).body(activitySessionService.rateActivity(accountId, activitySessionId, rateActivityDtoIn));
    }

    @PutMapping("/location/{accountId}/{visitId}")
    public ResponseEntity<ActivitySessionDtoOut> updateActivityLocation(@PathVariable Integer accountId, @PathVariable Integer visitId, @RequestBody @Valid UpdateActivityLocationDtoIn dto) {
        return ResponseEntity.status(200).body(activitySessionService.updateActivityLocation(accountId, visitId, dto));
    }
    @PostMapping("/start/gps/{accountId}/{visitMemberId}/{planItemId}")
    public ResponseEntity<ActivitySessionDtoOut> startGpsActivity(@PathVariable Integer accountId, @PathVariable Integer visitMemberId, @PathVariable Integer planItemId) {
        return ResponseEntity.status(200).body(activitySessionService.startGpsActivity(accountId, visitMemberId, planItemId));
    }

    @PostMapping("/start/manual/family/{accountId}/{planItemId}")
    public ResponseEntity<List<ActivitySessionDtoOut>> startFamilyActivity(@PathVariable Integer accountId, @PathVariable Integer planItemId, @RequestBody @Valid StartFamilyActivityDtoIn dto) {
        return ResponseEntity.status(200).body(activitySessionService.startManualActivity(accountId, null, planItemId, dto.getVisitMemberIds()));
    }

    @PostMapping("/complete/family/{accountId}/{activitySessionId}")
    public ResponseEntity<List<ActivitySessionDtoOut>> completeFamilyActivity(@PathVariable Integer accountId, @PathVariable Integer activitySessionId, @RequestBody @Valid CompleteFamilyActivityDtoIn dto) {
        CompleteActivityDtoIn completion = new CompleteActivityDtoIn();
        completion.setRequestKey(dto.getRequestKey());
        return ResponseEntity.status(200).body(activitySessionService.completeActivity(accountId, activitySessionId, completion, dto.getVisitMemberIds()));
    }

}