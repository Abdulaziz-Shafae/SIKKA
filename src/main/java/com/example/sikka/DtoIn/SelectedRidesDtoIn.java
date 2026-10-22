package com.example.sikka.DtoIn;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelectedRidesDtoIn {

    @NotNull(message = "rides list is required")
    @Valid
    private List<SelectedRideDtoIn> rides;
}
