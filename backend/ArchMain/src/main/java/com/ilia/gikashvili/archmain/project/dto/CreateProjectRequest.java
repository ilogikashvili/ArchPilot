package com.ilia.gikashvili.archmain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 1, max = 200, message = "Project name must be between 1 and 200 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
