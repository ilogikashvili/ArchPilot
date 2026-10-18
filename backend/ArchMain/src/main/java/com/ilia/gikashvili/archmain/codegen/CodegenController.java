package com.ilia.gikashvili.archmain.codegen;

import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import com.ilia.gikashvili.archmain.project.service.ProjectService;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/codegen")
public class CodegenController {
    private final ArchitectureService architecture;
    private final ProjectService projects;
    private final Map<String, CodeGenerator> generators = new HashMap<>();

    public CodegenController(ArchitectureService architecture, ProjectService projects, List<CodeGenerator> generators) {
        this.architecture = architecture;
        this.projects = projects;
        generators.forEach(g -> this.generators.put(g.language(), g));
    }

    private Map<String, String> build(UUID projectId, UUID userId, String language, String layout, String groupId) {
        CodeGenerator g = generators.get(language == null ? "java" : language.toLowerCase(Locale.ROOT));
        if (g == null) throw new ValidationException("Unsupported language. Available: " + generators.keySet());
        if (!"multi-module".equals(layout) && !"per-service".equals(layout))
            throw new ValidationException("layout must be 'multi-module' or 'per-service'");
        if (groupId == null || !groupId.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*"))
            throw new ValidationException("groupId must look like com.example");
        var project = projects.getById(projectId, userId);
        var doc = architecture.getDocument(projectId, userId);
        return g.generate(doc, new CodegenOptions(layout, project.getName(), groupId));
    }

    /** GET /projects/{projectId}/codegen/preview — the file tree with contents, as JSON. */
    @GetMapping("/preview")
    public Map<String, Object> preview(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId,
                                       @RequestParam(defaultValue = "java") String language,
                                       @RequestParam(defaultValue = "multi-module") String layout,
                                       @RequestParam(defaultValue = "com.example") String groupId) {
        var files = build(projectId, u.id(), language, layout, groupId);
        return Map.of("language", language, "layout", layout, "files",
                files.entrySet().stream().map(e -> Map.of("path", e.getKey(), "content", e.getValue())).toList());
    }

    /** GET /projects/{projectId}/codegen?language=java&layout=multi-module|per-service&groupId=com.example — zip download. */
    @GetMapping
    public ResponseEntity<byte[]> download(@CurrentUser AuthenticatedUser u, @PathVariable UUID projectId,
                                           @RequestParam(defaultValue = "java") String language,
                                           @RequestParam(defaultValue = "multi-module") String layout,
                                           @RequestParam(defaultValue = "com.example") String groupId) throws Exception {
        var files = build(projectId, u.id(), language, layout, groupId);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (var e : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        String root = files.keySet().stream().findFirst().map(p -> p.substring(0, p.indexOf('/'))).orElse("project");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + root + "-" + layout + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(bytes.toByteArray());
    }
}
