package com.ilia.gikashvili.archmain.architecture.controller;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.template.service.TemplateService;
import com.ilia.gikashvili.archmain.project.service.ProjectService;
import com.ilia.gikashvili.archmain.project.mapper.ProjectMapper;
import com.ilia.gikashvili.archmain.project.dto.CreateProjectRequest;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
@RestController
@RequestMapping(ApiVersion.V1)
public class ArchitectureExtrasController {
 private final ArchitectureService architecture;private final ArchitectureMapper mapper;private final TemplateService templates;private final ProjectService projects;private final ProjectMapper projectMapper;
 public ArchitectureExtrasController(ArchitectureService architecture,ArchitectureMapper mapper,TemplateService templates,ProjectService projects,ProjectMapper projectMapper){this.architecture=architecture;this.mapper=mapper;this.templates=templates;this.projects=projects;this.projectMapper=projectMapper;}
 public record ResetRequest(UUID templateId){}
 @PostMapping("/projects/{projectId}/architecture/reset") public Object reset(@PathVariable UUID projectId,@RequestBody(required=false) ResetRequest r,@CurrentUser AuthenticatedUser u){var current=architecture.getByProjectId(projectId,u.id());var doc=r==null||r.templateId()==null?ArchitectureDocument.empty():templates.document(r.templateId(),u.id());return mapper.toResponse(architecture.save(projectId,u.id(),doc,current.getRevision()));}
 @PostMapping("/projects/{projectId}/architecture/duplicate") public Object duplicate(@PathVariable UUID projectId,@RequestBody(required=false) CreateProjectRequest r,@CurrentUser AuthenticatedUser u){if(r!=null&&r.name()!=null&&(r.name().isBlank()||r.name().length()>200))throw new com.ilia.gikashvili.archmain.common.exception.ValidationException("Invalid project name");return ResponseEntity.status(201).body(projectMapper.toResponse(projects.duplicate(projectId,u.id(),r)));}
 @GetMapping("/projects/{projectId}/architecture/metadata") public Object metadata(@PathVariable UUID projectId,@CurrentUser AuthenticatedUser u){var e=architecture.getByProjectId(projectId,u.id());var d=mapper.toModel(e);Map<String,Object> r=new LinkedHashMap<>();r.put("revision",e.getRevision());r.put("schemaVersion",d.schemaVersion());r.put("nodeCount",d.nodes().size());r.put("edgeCount",d.edges().size());r.put("updatedAt",e.getUpdatedAt());r.put("updatedBy",e.getUpdatedBy());return r;}
 public record Type(String id,String label,String description,String category,String icon){}
 @GetMapping("/architecture/component-types") public Object components(){return List.of(new Type("service","Service","Application service","compute","server"),new Type("api","API","API endpoint or gateway","network","route"),new Type("database","Database","Persistent data store","storage","database"),new Type("queue","Queue","Message broker","messaging","list"),new Type("cache","Cache","Fast temporary storage","storage","zap"),new Type("external","External","Third-party system","integration","globe"),new Type("web","Web","Web frontend","client","monitor"));}
 @GetMapping("/architecture/edge-types") public Object edges(){return List.of(new Type("sync","Synchronous","Request-response call","communication","arrow-right"),new Type("async","Asynchronous","Non-blocking message","communication","send"),new Type("event","Event","Published event","messaging","radio"),new Type("dependency","Dependency","Logical dependency","structure","link"));}
}
