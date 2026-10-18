package com.ilia.gikashvili.archmain.access.service;

import com.ilia.gikashvili.archmain.access.entity.ProjectMember;
import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import com.ilia.gikashvili.archmain.access.policy.RolePermissionPolicy;
import com.ilia.gikashvili.archmain.access.repository.ProjectMemberRepository;
import com.ilia.gikashvili.archmain.common.exception.ForbiddenException;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * FACADE — the single entry point for all permission checks.
 * Every project-scoped service method should call {@code require(...)} before proceeding.
 *
 * Usage:
 * <pre>
 *   accessService.require(projectId, currentUserId, Permission.EDIT);
 * </pre>
 */
@Service
public class ProjectAccessService {

    private final ProjectMemberRepository memberRepository;
    private final RolePermissionPolicy policy;

    public ProjectAccessService(ProjectMemberRepository memberRepository,
                                RolePermissionPolicy policy) {
        this.memberRepository = memberRepository;
        this.policy = policy;
    }

    /**
     * Assert that the user has the given permission on the project.
     * Throws ForbiddenException if not.
     */
    @Transactional(readOnly = true)
    public void require(UUID projectId, UUID userId, Permission permission) {
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not a member of this project"));

        if (!policy.hasPermission(member.getRole(), permission)) {
            throw new ForbiddenException(
                    "You do not have " + permission + " permission on this project");
        }
    }

    /**
     * Check if user has permission (returns boolean instead of throwing).
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID projectId, UUID userId, Permission permission) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(m -> policy.hasPermission(m.getRole(), permission))
                .orElse(false);
    }

    /**
     * Get the effective permissions for a user on a project.
     */
    @Transactional(readOnly = true)
    public Set<Permission> getEffectivePermissions(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(m -> policy.getPermissions(m.getRole()))
                .orElse(Set.of());
    }

    /**
     * Get the user's role on a project.
     */
    @Transactional(readOnly = true)
    public ProjectRole getRole(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMember::getRole)
                .orElseThrow(() -> new NotFoundException("Membership not found"));
    }

    /**
     * Add a member to a project.
     */
    @Transactional
    public ProjectMember addMember(UUID projectId, UUID userId, ProjectRole role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(role);
        return memberRepository.save(member);
    }

    /**
     * Count members of a project.
     */
    @Transactional(readOnly = true)
    public int getMemberCount(UUID projectId) {
        return (int) memberRepository.countByProjectId(projectId);
    }

    /**
     * List all members of a project.
     */
    @Transactional(readOnly = true)
    public java.util.List<ProjectMember> listMembers(UUID projectId, UUID currentUserId) {
        require(projectId, currentUserId, Permission.READ);
        return memberRepository.findAllByProjectId(projectId);
    }

    /**
     * Update a member's role.
     */
    @Transactional
    public ProjectMember updateMemberRole(UUID projectId, UUID targetUserId, ProjectRole newRole, UUID currentUserId) {
        require(projectId, currentUserId, Permission.MANAGE_MEMBERS);
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Member", targetUserId));

        member.setRole(newRole);
        return memberRepository.save(member);
    }

    /**
     * Remove a member from the project.
     */
    @Transactional
    public void removeMember(UUID projectId, UUID targetUserId, UUID currentUserId) {
        require(projectId, currentUserId, Permission.MANAGE_MEMBERS);
        memberRepository.deleteByProjectIdAndUserId(projectId, targetUserId);
    }

    /**
     * Current user leaves the project.
     */
    @Transactional
    public void leaveProject(UUID projectId, UUID currentUserId) {
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new NotFoundException("Membership not found"));

        if (member.getRole() == ProjectRole.OWNER) {
            long ownerCount = memberRepository.findAllByProjectId(projectId).stream()
                    .filter(m -> m.getRole() == ProjectRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new com.ilia.gikashvili.archmain.common.exception.ValidationException(
                        "Cannot leave project as the sole owner. Transfer ownership first or delete the project.");
            }
        }

        memberRepository.deleteByProjectIdAndUserId(projectId, currentUserId);
    }

    /**
     * Get effective permissions response for current user.
     */
    @Transactional(readOnly = true)
    public com.ilia.gikashvili.archmain.access.dto.EffectivePermissionsResponse getEffectivePermissionsInfo(UUID projectId, UUID currentUserId) {
        ProjectRole role = getRole(projectId, currentUserId);
        Set<Permission> perms = policy.getPermissions(role);
        return new com.ilia.gikashvili.archmain.access.dto.EffectivePermissionsResponse(projectId, currentUserId, role, perms);
    }
}
