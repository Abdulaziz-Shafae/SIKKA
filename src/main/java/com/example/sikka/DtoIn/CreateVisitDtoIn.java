package com.example.sikka.DtoIn;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateVisitDtoIn {

    @Size(max = 150, message = "group name must not exceed 150 characters")
    private String groupName;

    @Size(max = 150, message = "name must not exceed 150 characters")
    private String name;

    @NotNull(message = "visit date is required")
    @FutureOrPresent(message = "visit date cannot be in the past")
    private LocalDate visitDate;

    private LocalTime plannedStartTime;

    private LocalTime plannedEndTime;

    @NotNull(message = "max participants is required")
    @Positive(message = "max participants must be positive")
    private Integer maxParticipants;
}