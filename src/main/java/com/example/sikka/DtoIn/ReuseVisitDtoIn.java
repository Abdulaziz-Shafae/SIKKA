package com.example.sikka.DtoIn;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReuseVisitDtoIn {

    @NotNull(message = "visit date is required")
    @FutureOrPresent(message = "visit date cannot be in the past")
    private LocalDate visitDate;
}