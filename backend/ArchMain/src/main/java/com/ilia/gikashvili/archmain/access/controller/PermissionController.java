package com.ilia.gikashvili.archmain.access.controller;

import com.ilia.gikashvili.archmain.access.dto.EffectivePermissionsResponse;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}")
public class PermissionController {

    private final ProjectAccessService accessService;

    public PermissionController(ProjectAccessService accessService) {
        this.accessService = accessService;
    }

    /**
     * GET /projects/{projectId}/permissions — Get current user's effective permissions.
     */
    @GetMapping("/permissions")
    public ResponseEntity<EffectivePermissionsResponse> getPermissions(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var response = accessService.getEffectivePermissionsInfo(projectId, currentUser.id());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /projects/{projectId}/leave — Current user leaves a shared project.
     */
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveProject(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        accessService.leaveProject(projectId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
