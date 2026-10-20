package com.example.sikka.DtoIn;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateVisitRecordDtoIn {

    @NotNull(message = "Ride ID is required")
    @Positive(message = "Ride ID must be positive")
    private Integer rideId;

    @NotEmpty(message = "Select at least one visit member")
    private List<@NotNull @Positive Integer> visitMemberIds;

    @NotBlank(message = "Record status is required")
    @Pattern(regexp = "^(COMPLETED|SKIPPED)$", message = "Status must be COMPLETED or SKIPPED")
    private String status;
}