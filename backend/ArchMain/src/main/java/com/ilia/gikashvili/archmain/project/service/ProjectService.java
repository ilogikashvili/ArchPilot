package com.ilia.gikashvili.archmain.project.service;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.project.dto.CreateProjectRequest;
import com.ilia.gikashvili.archmain.project.dto.UpdateProjectRequest;
import com.ilia.gikashvili.archmain.project.entity.Project;
import com.ilia.gikashvili.archmain.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final com.ilia.gikashvili.archmain.audit.service.AuditService audit;
    private final ProjectAccessService accessService;
    private final com.ilia.gikashvili.archmain.architecture.service.ArchitectureService architectureService;

    public ProjectService(ProjectRepository projectRepository, com.ilia.gikashvili.archmain.audit.service.AuditService audit,
                          ProjectAccessService accessService,
                          com.ilia.gikashvili.archmain.architecture.service.ArchitectureService architectureService) {
        this.projectRepository = projectRepository;this.audit=audit;
        this.accessService = accessService;
        this.architectureService = architectureService;
    }

    /**
     * Create a new project. The creator becomes the OWNER.
     */
    @Transactional
    public Project create(UUID userId, CreateProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setOwnerId(userId);
        project = projectRepository.save(project);

        // Add creator as OWNER
        accessService.addMember(project.getId(), userId, ProjectRole.OWNER);

        // Initialize empty architecture document
        architectureService.initializeForProject(project.getId());

        return project;
    }

    /**
     * List all projects accessible to the user.
     */
    @Transactional(readOnly = true)
    public List<Project> listAccessible(UUID userId) {
        return projectRepository.findAccessibleByUserId(userId);
    }

    /**
     * Get a project by ID (with access check).
     */
    @Transactional(readOnly = true)
    public Project getById(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));
    }

    /**
     * Update project name/description.
     */
    @Transactional
    public Project update(UUID projectId, UUID userId, UpdateProjectRequest request) {
        accessService.require(projectId, userId, Permission.EDIT);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));

        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        return projectRepository.save(project);
    }

    /**
     * Delete a project.
     */
    @Transactional
    public void delete(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.DELETE);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));
        projectRepository.delete(project);audit.record(userId,"PROJECT_DELETE",projectId.toString());
    }

    /**
     * Archive a project.
     */
    @Transactional
    public Project archive(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.EDIT);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));
        project.setArchived(true);
        return projectRepository.save(project);
    }

    /**
     * Restore an archived project.
     */
    @Transactional
    public Project restore(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.EDIT);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project", projectId));
        project.setArchived(false);
        return projectRepository.save(project);
    }

    /**
     * Clone/duplicate a project including its architecture document.
     */
    @Transactional
    public Project duplicate(UUID sourceProjectId, UUID userId, CreateProjectRequest request) {
        accessService.require(sourceProjectId, userId, Permission.READ);

        Project source = projectRepository.findById(sourceProjectId)
                .orElseThrow(() -> new NotFoundException("Project", sourceProjectId));

        String newName = request != null && request.name() != null ? request.name() : source.getName() + " (Copy)";
        String newDesc = request != null && request.description() != null ? request.description() : source.getDescription();

        Project newProject = new Project();
        newProject.setName(newName);
        newProject.setDescription(newDesc);
        newProject.setOwnerId(userId);
        newProject = projectRepository.save(newProject);

        accessService.addMember(newProject.getId(), userId, ProjectRole.OWNER);

        var doc = architectureService.getDocument(sourceProjectId, userId);
        architectureService.initializeForProject(newProject.getId());
        architectureService.save(newProject.getId(), userId, doc, 0);

        return newProject;
    }

    /**
     * Get summary metrics for a project (nodes, edges, members).
     */
    @Transactional(readOnly = true)
    public com.ilia.gikashvili.archmain.project.dto.ProjectSummaryResponse getSummary(UUID projectId, UUID userId) {
        Project project = getById(projectId, userId);
        var doc = architectureService.getDocument(projectId, userId);

        int nodeCount = doc.nodes() != null ? doc.nodes().size() : 0;
        int edgeCount = doc.edges() != null ? doc.edges().size() : 0;
        int memberCount = accessService.getMemberCount(projectId);

        return new com.ilia.gikashvili.archmain.project.dto.ProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwnerId(),
                project.isArchived(),
                nodeCount,
                edgeCount,
                memberCount,
                project.getUpdatedAt()
        );
    }
}
