package com.ilia.gikashvili.archmain.architecture.controller;

import com.ilia.gikashvili.archmain.architecture.dto.ArchitectureResponse;
import com.ilia.gikashvili.archmain.architecture.dto.ValidationResponse;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureTransferService;
import com.ilia.gikashvili.archmain.architecture.validation.ValidationResult;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/architecture")
public class ArchitectureTransferController {

    private final ArchitectureTransferService transferService;
    private final ArchitectureMapper architectureMapper;

    public ArchitectureTransferController(ArchitectureTransferService transferService,
                                          ArchitectureMapper architectureMapper) {
        this.transferService = transferService;
        this.architectureMapper = architectureMapper;
    }

    /**
     * GET /projects/{projectId}/architecture/export?format=json — Export architecture document.
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "json") String format) {
        var result = transferService.export(projectId, format, currentUser.id());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"architecture-" + projectId + "." + format + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.data());
    }

    /**
     * POST /projects/{projectId}/architecture/import?format=json — Import and overwrite architecture.
     */
    @PostMapping(value = "/import", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<ArchitectureResponse> importDocument(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "json") String format,
            @RequestBody byte[] payload) {
        var entity = transferService.importDocument(projectId, format, payload, currentUser.id());
        return ResponseEntity.ok(architectureMapper.toResponse(entity));
    }

    /**
     * POST /projects/{projectId}/architecture/import/validate?format=json — Validate import without saving.
     */
    @PostMapping(value = "/import/validate", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<ValidationResponse> validateImport(
            @RequestParam(defaultValue = "json") String format,
            @RequestBody byte[] payload) {
        ValidationResult result = transferService.validateImport(format, payload);
        return ResponseEntity.ok(new ValidationResponse(result.isValid(), result.getErrors()));
    }
}
