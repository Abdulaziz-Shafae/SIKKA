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
}