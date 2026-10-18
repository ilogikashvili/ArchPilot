package com.ilia.gikashvili.archmain.architecture.dto;

import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;

import java.util.List;

public record ValidationResponse(
        boolean valid,
        List<ValidationResult.ValidationError> errors
) {
}
