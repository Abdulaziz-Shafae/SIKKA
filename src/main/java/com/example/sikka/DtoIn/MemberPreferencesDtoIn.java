package com.example.sikka.DtoIn;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberPreferencesDtoIn {
    @NotBlank
    @Pattern(regexp = "^(ANSWERED|SKIPPED)$")
    private String answerStatus;

    @Valid
    private List<@NotNull SelectedRideDtoIn> selectedRides;

    private List<@NotBlank String> dislikedFeatures;
}
