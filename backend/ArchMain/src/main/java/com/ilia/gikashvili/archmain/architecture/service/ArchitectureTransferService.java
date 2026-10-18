package com.ilia.gikashvili.archmain.architecture.service;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.transfer.ArchitectureExporter;
import com.ilia.gikashvili.archmain.architecture.transfer.ArchitectureImporter;
import com.ilia.gikashvili.archmain.architecture.transfer.ExporterRegistry;
import com.ilia.gikashvili.archmain.architecture.validation.ArchitectureValidator;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ArchitectureTransferService {

    private final ArchitectureService architectureService;
    private final ArchitectureValidator architectureValidator;
    private final ProjectAccessService accessService;
    private final ExporterRegistry exporterRegistry;

    public ArchitectureTransferService(ArchitectureService architectureService,
                                       ArchitectureValidator architectureValidator,
                                       ProjectAccessService accessService,
                                       ExporterRegistry exporterRegistry) {
        this.architectureService = architectureService;
        this.architectureValidator = architectureValidator;
        this.accessService = accessService;
        this.exporterRegistry = exporterRegistry;
    }

    /**
     * Export project architecture in the specified format.
     */
    @Transactional(readOnly = true)
    public ExportResult export(UUID projectId, String format, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        ArchitectureDocument document = architectureService.getDocument(projectId, userId);
        ArchitectureExporter exporter = exporterRegistry.getExporter(format);
        byte[] data = exporter.export(document);
        return new ExportResult(data, exporter.getContentType());
    }

    /**
     * Import architecture document and save it as the latest revision.
     */
    @Transactional
    public ArchitectureDocumentEntity importDocument(UUID projectId, String format, byte[] payload, UUID userId) {
        accessService.require(projectId, userId, Permission.EDIT);

        ArchitectureImporter importer = exporterRegistry.getImporter(format);
        ArchitectureDocument document = importer.parse(payload);

        ValidationResult validationResult = architectureValidator.validate(document);
        if (!validationResult.isValid()) {
            throw new ValidationException("Imported document is invalid: " + validationResult.getErrors().get(0).message());
        }

        var currentEntity = architectureService.getByProjectId(projectId, userId);
        return architectureService.save(projectId, userId, document, currentEntity.getRevision());
    }

    /**
     * Validate an imported payload without persisting.
     */
    public ValidationResult validateImport(String format, byte[] payload) {
        ArchitectureImporter importer = exporterRegistry.getImporter(format);
        ArchitectureDocument document = importer.parse(payload);
        return architectureValidator.validate(document);
    }

    public record ExportResult(byte[] data, String contentType) {
    }
}
