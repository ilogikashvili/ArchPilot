package com.ilia.gikashvili.archmain.architecture.service;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.repository.ArchitectureDocumentRepository;
import com.ilia.gikashvili.archmain.architecture.validation.ArchitectureValidator;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import com.ilia.gikashvili.archmain.common.exception.ConflictException;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ArchitectureService {

    private final ArchitectureDocumentRepository repository;
    private final ArchitectureMapper mapper;
    private final ArchitectureValidator validator;
    private final ProjectAccessService accessService;
    private final org.springframework.context.ApplicationEventPublisher events;

    public ArchitectureService(ArchitectureDocumentRepository repository,
                               ArchitectureMapper mapper,
                               ArchitectureValidator validator,
                               ProjectAccessService accessService,
                               org.springframework.context.ApplicationEventPublisher events) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.accessService = accessService;
        this.events = events;
    }

    /**
     * Load the architecture for a project.
     * Creates an empty document if none exists yet.
     */
    @Transactional(readOnly = true)
    public ArchitectureDocumentEntity getByProjectId(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        return repository.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Architecture document for project", projectId));
    }

    /**
     * Load the parsed architecture document model for a project.
     */
    @Transactional(readOnly = true)
    public ArchitectureDocument getDocument(UUID projectId, UUID userId) {
        return mapper.toModel(getByProjectId(projectId, userId));
    }

    /** Internal read without a user permission check (callers must have checked access already). */
    @Transactional(readOnly = true)
    public ArchitectureDocument getDocumentForSystem(UUID projectId) {
        return mapper.toModel(repository.findByProjectId(projectId)
                .orElseThrow(() -> new NotFoundException("Architecture document for project", projectId)));
    }

    /**
     * Save the architecture document with optimistic concurrency check.
     */
    @Transactional
    public ArchitectureDocumentEntity save(UUID projectId, UUID userId,
                                           ArchitectureDocument document, long expectedRevision) {
        accessService.require(projectId, userId, Permission.EDIT);

        // Validate first
        ValidationResult validationResult = validator.validate(document);
        if (!validationResult.isValid()) {
            throw new ValidationException(
                    "Architecture document is invalid: " + validationResult.getErrors().get(0).message());
        }

        ArchitectureDocumentEntity entity = repository.findByProjectId(projectId)
                .orElseGet(() -> {
                    ArchitectureDocumentEntity newEntity = new ArchitectureDocumentEntity();
                    newEntity.setProjectId(projectId);
                    return newEntity;
                });

        // Optimistic concurrency
        if (entity.getId() != null && entity.getRevision() != expectedRevision) {
            throw new ConflictException(
                    "Architecture has been modified by another user. " +
                    "Expected revision " + expectedRevision + " but current is " + entity.getRevision());
        }

        entity.setDocumentJson(mapper.toJson(document));
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(java.time.Instant.now());
        var saved = repository.saveAndFlush(entity);
        events.publishEvent(new com.ilia.gikashvili.archmain.realtime.ArchitectureUpdated(projectId, null, userId));
        return saved;
    }

    /**
     * Validate a document without saving.
     */
    public ValidationResult validate(ArchitectureDocument document) {
        return validator.validate(document);
    }

    /**
     * Initialize an empty architecture for a new project.
     */
    @Transactional
    public ArchitectureDocumentEntity initializeForProject(UUID projectId) {
        if (repository.existsByProjectId(projectId)) {
            return repository.findByProjectId(projectId).orElseThrow();
        }
        ArchitectureDocumentEntity entity = new ArchitectureDocumentEntity();
        entity.setProjectId(projectId);
        entity.setDocumentJson(mapper.toJson(ArchitectureDocument.empty()));
        return repository.save(entity);
    }
}
