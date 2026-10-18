package com.ilia.gikashvili.archmain.access.dto;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;

import java.util.Set;
import java.util.UUID;

public record EffectivePermissionsResponse(
        UUID projectId,
        UUID userId,
        ProjectRole role,
        Set<Permission> permissions
) {
}
