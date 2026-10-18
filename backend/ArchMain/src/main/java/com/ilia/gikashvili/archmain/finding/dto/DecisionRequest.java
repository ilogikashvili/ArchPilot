package com.ilia.gikashvili.archmain.finding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DecisionRequest(
        @NotBlank(message = "Decision cannot be blank")
        @Pattern(regexp = "ACCEPTED|DISMISSED|POSTPONED", message = "Decision must be ACCEPTED, DISMISSED, or POSTPONED")
        String decision,

        String reason
) {
}
