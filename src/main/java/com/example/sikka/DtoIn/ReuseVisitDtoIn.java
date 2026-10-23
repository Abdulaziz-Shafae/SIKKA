package com.example.sikka.DtoIn;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReuseVisitDtoIn {

    @NotNull(message = "Visit date is required")
    @FutureOrPresent(message = "Visit date must be today or a future date")
    private LocalDate visitDate;

    @NotNull(message = "Visit duration is required")
    @Positive(message = "Visit duration must be greater than zero")
    private Integer durationMinutes;
}