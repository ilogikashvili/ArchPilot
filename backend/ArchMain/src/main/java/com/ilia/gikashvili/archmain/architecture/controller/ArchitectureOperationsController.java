package com.ilia.gikashvili.archmain.architecture.controller;

import com.ilia.gikashvili.archmain.architecture.dto.ArchitectureResponse;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureOperationsService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/architecture")
public class ArchitectureOperationsController {
    private final ArchitectureOperationsService service;
    private final ArchitectureMapper mapper;

    public ArchitectureOperationsController(ArchitectureOperationsService service, ArchitectureMapper mapper) {
        this.service = service; this.mapper = mapper;
    }

    public record OperationsRequest(String clientId, List<Map<String, Object>> operations) {}

    /**
     * POST /projects/{projectId}/architecture/operations — apply conflict-free edits
     * (node.add/update/remove, edge.add/update/remove). Used by the live editor and by AI agents.
     */
    @PostMapping("/operations")
    public ArchitectureResponse apply(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId,
                                      @RequestBody OperationsRequest request) {
        synchronized (service.lockFor(projectId)) {
            return mapper.toResponse(service.apply(projectId, u.id(), request.clientId(), request.operations()));
        }
    }
}
