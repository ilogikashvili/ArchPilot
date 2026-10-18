package com.ilia.gikashvili.archmain.architecture.controller;

import com.ilia.gikashvili.archmain.architecture.dto.ArchitectureResponse;
import com.ilia.gikashvili.archmain.architecture.dto.SaveArchitectureRequest;
import com.ilia.gikashvili.archmain.architecture.dto.ValidationResponse;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/architecture")
public class ArchitectureController {

    private final ArchitectureService architectureService;
    private final ArchitectureMapper architectureMapper;

    public ArchitectureController(ArchitectureService architectureService,
                                  ArchitectureMapper architectureMapper) {
        this.architectureService = architectureService;
        this.architectureMapper = architectureMapper;
    }

    /**
     * GET /projects/{projectId}/architecture — Load the project's architecture.
     */
    @GetMapping
    public ResponseEntity<ArchitectureResponse> get(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var entity = architectureService.getByProjectId(projectId, currentUser.id());
        return ResponseEntity.ok(architectureMapper.toResponse(entity));
    }

    /**
     * PUT /projects/{projectId}/architecture — Save the complete architecture document.
     */
    @PutMapping
    public ResponseEntity<ArchitectureResponse> save(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @Valid @RequestBody SaveArchitectureRequest request) {
        var entity = architectureService.save(
                projectId, currentUser.id(), request.document(), request.expectedRevision());
        return ResponseEntity.ok(architectureMapper.toResponse(entity));
    }

    /**
     * POST /projects/{projectId}/architecture/validate — Validate a proposed document.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(
            @RequestBody ArchitectureDocument document) {
        if (document == null || document.nodes() == null || document.edges() == null) {
            return ResponseEntity.badRequest().body(new ValidationResponse(false,
                    java.util.List.of(new ValidationResult.ValidationError("Schema", "Document must contain nodes and edges arrays", null))));
        }
        ValidationResult result = architectureService.validate(document);
        return ResponseEntity.ok(new ValidationResponse(result.isValid(), result.getErrors()));
    }
}
