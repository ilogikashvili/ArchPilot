package com.ilia.gikashvili.archmain.access.policy;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps each ProjectRole to its set of Permissions.
 */
@Component
public class RolePermissionPolicy {

    private static final Map<ProjectRole, Set<Permission>> ROLE_PERMISSIONS;

    static {
        ROLE_PERMISSIONS = new EnumMap<>(ProjectRole.class);
        ROLE_PERMISSIONS.put(ProjectRole.OWNER,
                EnumSet.allOf(Permission.class));
        ROLE_PERMISSIONS.put(ProjectRole.ADMIN,
                EnumSet.of(Permission.READ, Permission.EDIT, Permission.REVIEW, Permission.MANAGE_MEMBERS));
        ROLE_PERMISSIONS.put(ProjectRole.EDITOR,
                EnumSet.of(Permission.READ, Permission.EDIT, Permission.REVIEW));
        ROLE_PERMISSIONS.put(ProjectRole.REVIEWER,
                EnumSet.of(Permission.READ, Permission.REVIEW));
        ROLE_PERMISSIONS.put(ProjectRole.VIEWER,
                EnumSet.of(Permission.READ));
    }

    /**
     * Check if a role has a specific permission.
     */
    public boolean hasPermission(ProjectRole role, Permission permission) {
        Set<Permission> perms = ROLE_PERMISSIONS.get(role);
        return perms != null && perms.contains(permission);
    }

    /**
     * Get all permissions for a role.
     */
    public Set<Permission> getPermissions(ProjectRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }
}
