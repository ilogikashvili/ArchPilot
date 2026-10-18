package com.ilia.gikashvili.archmain.project.controller;

import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.project.dto.CreateProjectRequest;
import com.ilia.gikashvili.archmain.project.dto.ProjectResponse;
import com.ilia.gikashvili.archmain.project.dto.UpdateProjectRequest;
import com.ilia.gikashvili.archmain.project.mapper.ProjectMapper;
import com.ilia.gikashvili.archmain.project.service.ProjectService;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }

    /**
     * POST /projects — Create a project.
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @CurrentUser AuthenticatedUser currentUser,
            @Valid @RequestBody CreateProjectRequest request) {
        var project = projectService.create(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.toResponse(project));
    }

    /**
     * GET /projects — List projects accessible to the current user.
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> list(@CurrentUser AuthenticatedUser currentUser) {
        var projects = projectService.listAccessible(currentUser.id()).stream()
                .map(projectMapper::toResponse)
                .toList();
        return ResponseEntity.ok(projects);
    }

    /**
     * GET /projects/{projectId} — Get project details.
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> get(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var project = projectService.getById(projectId, currentUser.id());
        return ResponseEntity.ok(projectMapper.toResponse(project));
    }

    /**
     * PATCH /projects/{projectId} — Rename or update project description/settings.
     */
    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> update(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        var project = projectService.update(projectId, currentUser.id(), request);
        return ResponseEntity.ok(projectMapper.toResponse(project));
    }

    /**
     * DELETE /projects/{projectId} — Delete a project.
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        projectService.delete(projectId, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /projects/{projectId}/summary — Get project summary and metrics.
     */
    @GetMapping("/{projectId}/summary")
    public ResponseEntity<com.ilia.gikashvili.archmain.project.dto.ProjectSummaryResponse> getSummary(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getSummary(projectId, currentUser.id()));
    }

    /**
     * POST /projects/{projectId}/duplicate — Clone a project with its architecture.
     */
    @PostMapping("/{projectId}/duplicate")
    public ResponseEntity<ProjectResponse> duplicate(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @RequestBody(required = false) CreateProjectRequest request) {
        var newProject = projectService.duplicate(projectId, currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(projectMapper.toResponse(newProject));
    }

    /**
     * POST /projects/{projectId}/archive — Archive a project.
     */
    @PostMapping("/{projectId}/archive")
    public ResponseEntity<ProjectResponse> archive(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var project = projectService.archive(projectId, currentUser.id());
        return ResponseEntity.ok(projectMapper.toResponse(project));
    }

    /**
     * POST /projects/{projectId}/restore — Restore an archived project.
     */
    @PostMapping("/{projectId}/restore")
    public ResponseEntity<ProjectResponse> restore(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var project = projectService.restore(projectId, currentUser.id());
        return ResponseEntity.ok(projectMapper.toResponse(project));
    }
}
