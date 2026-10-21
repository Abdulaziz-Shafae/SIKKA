package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.VisitGroupDtoIn;
import com.example.sikka.Service.VisitGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visit-group")
@RequiredArgsConstructor
public class VisitGroupController {

    private final VisitGroupService visitGroupService;

    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.status(200).body(visitGroupService.get());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid VisitGroupDtoIn dto) {
        String code = visitGroupService.add(dto);
        return ResponseEntity.status(200).body(new ApiResponse("added successfully, invite code: " + code));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid VisitGroupDtoIn dto) {
        visitGroupService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        visitGroupService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("deleted successfully"));
    }

    @PostMapping("/add-group/{accountId}/{visitId}")
    public ResponseEntity<?> addGroupToVisit(@PathVariable Integer accountId, @PathVariable Integer visitId,
                                             @RequestParam String groupName) {
        String code = visitGroupService.addGroupToVisit(accountId, visitId, groupName);
        return ResponseEntity.status(200).body(new ApiResponse("group added, invite code: " + code));
    }

    @PutMapping("/regenerate-code/{accountId}/{visitId}")
    public ResponseEntity<?> regenerateCode(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        String code = visitGroupService.regenerateCode(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse("new invite code: " + code));
    }

    @PutMapping("/toggle-invitations/{accountId}/{visitId}")
    public ResponseEntity<?> toggleInvitations(@PathVariable Integer accountId, @PathVariable Integer visitId) {
        boolean enabled = visitGroupService.toggleInvitations(accountId, visitId);
        return ResponseEntity.status(200).body(new ApiResponse(enabled ? "invitations enabled" : "invitations disabled"));
    }
}