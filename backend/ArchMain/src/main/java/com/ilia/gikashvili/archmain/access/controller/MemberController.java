package com.ilia.gikashvili.archmain.access.controller;

import com.ilia.gikashvili.archmain.access.dto.ProjectMemberResponse;
import com.ilia.gikashvili.archmain.access.dto.UpdateMemberRoleRequest;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/members")
public class MemberController {

    private final ProjectAccessService accessService;

    public MemberController(ProjectAccessService accessService) {
        this.accessService = accessService;
    }

    /**
     * GET /projects/{projectId}/members — List project members.
     */
    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var members = accessService.listMembers(projectId, currentUser.id()).stream()
                .map(m -> new ProjectMemberResponse(
                        m.getId(),
                        m.getProjectId(),
                        m.getUserId(),
                        m.getRole(),
                        m.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(members);
    }

    /**
     * PATCH /projects/{projectId}/members/{userId} — Change a member's role.
     */
    @PatchMapping("/{targetUserId}")
    public ResponseEntity<ProjectMemberResponse> updateMemberRole(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        var updated = accessService.updateMemberRole(projectId, targetUserId, request.role(), currentUser.id());
        return ResponseEntity.ok(new ProjectMemberResponse(
                updated.getId(),
                updated.getProjectId(),
                updated.getUserId(),
                updated.getRole(),
                updated.getCreatedAt()
        ));
    }

    /**
     * DELETE /projects/{projectId}/members/{userId} — Remove a member from the project.
     */
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID targetUserId) {
        accessService.removeMember(projectId, targetUserId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
