package com.ilia.gikashvili.archmain.access.dto;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        ProjectRole role
) {
}
