package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoIn.VisitDtoIn;
import com.example.sikka.Service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visit")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @GetMapping("/get")
    public ResponseEntity<?> get() {
        return ResponseEntity.status(200).body(visitService.get());
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody @Valid VisitDtoIn dto) {
        visitService.add(dto);
        return ResponseEntity.status(200).body(new ApiResponse("added successfully"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody @Valid VisitDtoIn dto) {
        visitService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("updated successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        visitService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("deleted successfully"));
    }
}