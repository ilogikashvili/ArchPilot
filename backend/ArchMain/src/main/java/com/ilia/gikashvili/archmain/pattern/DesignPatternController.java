package com.ilia.gikashvili.archmain.pattern;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureOperationsService;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Design pattern catalog and transformations.
 * - GET  /design-patterns                                   catalog
 * - POST /projects/{id}/architecture/patterns/{p}/preview   body: optional document (e.g. the open branch); returns operations
 * - POST /projects/{id}/architecture/patterns/{p}/apply     applies to the saved main document (used by agents/API)
 */
@RestController
@RequestMapping(ApiVersion.V1)
public class DesignPatternController {
    private final DesignPatternEngine engine;
    private final ProjectAccessService access;
    private final ArchitectureService architecture;
    private final ArchitectureOperationsService ops;
    private final ArchitectureMapper mapper;

    public DesignPatternController(DesignPatternEngine engine, ProjectAccessService access, ArchitectureService architecture,
                                   ArchitectureOperationsService ops, ArchitectureMapper mapper) {
        this.engine = engine; this.access = access; this.architecture = architecture; this.ops = ops; this.mapper = mapper;
    }

    public record PreviewRequest(ArchitectureDocument document) {}

    @GetMapping("/design-patterns")
    public List<DesignPatternEngine.PatternInfo> catalog() { return engine.catalog(); }

    @PostMapping("/projects/{projectId}/architecture/patterns/{patternId}/preview")
    public DesignPatternEngine.Transformation preview(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId,
                                                      @PathVariable String patternId, @RequestBody(required = false) PreviewRequest body) {
        access.require(projectId, u.id(), Permission.READ);
        ArchitectureDocument doc = body != null && body.document() != null ? body.document() : architecture.getDocument(projectId, u.id());
        return engine.transform(patternId, doc);
    }

    @PostMapping("/projects/{projectId}/architecture/patterns/{patternId}/apply")
    public Map<String, Object> apply(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId, @PathVariable String patternId) {
        access.require(projectId, u.id(), Permission.EDIT);
        synchronized (ops.lockFor(projectId)) {
            var t = engine.transform(patternId, architecture.getDocument(projectId, u.id()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("transformation", t);
            if (t.applicable()) result.put("architecture", mapper.toResponse(ops.apply(projectId, u.id(), "pattern:" + patternId, t.operations())));
            return result;
        }
    }
}
