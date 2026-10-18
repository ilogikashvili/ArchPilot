package com.ilia.gikashvili.archmain.template.controller;
import com.ilia.gikashvili.archmain.template.service.TemplateService;
import com.ilia.gikashvili.archmain.template.dto.TemplateRequest;
import com.ilia.gikashvili.archmain.project.dto.CreateProjectRequest;
import com.ilia.gikashvili.archmain.project.mapper.ProjectMapper;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.UUID;
@RestController
@RequestMapping(ApiVersion.V1)
public class TemplateController {
 private final TemplateService service;private final ProjectMapper mapper;
 public TemplateController(TemplateService service,ProjectMapper mapper){this.service=service;this.mapper=mapper;}
 @GetMapping("/templates") public Object list(@CurrentUser AuthenticatedUser u){return service.list(u.id());}
 @GetMapping("/templates/{id}") public Object get(@PathVariable UUID id,@CurrentUser AuthenticatedUser u){return service.response(service.get(id,u.id()));}
 @PostMapping("/templates") public Object create(@Valid @RequestBody TemplateRequest r,@CurrentUser AuthenticatedUser u){return ResponseEntity.status(201).body(service.response(service.create(u.id(),r)));}
 @PatchMapping("/templates/{id}") public Object patch(@PathVariable UUID id,@Valid @RequestBody TemplateRequest r,@CurrentUser AuthenticatedUser u){return service.response(service.patch(id,u.id(),r));}
 @DeleteMapping("/templates/{id}") public Object delete(@PathVariable UUID id,@CurrentUser AuthenticatedUser u){service.delete(id,u.id());return ResponseEntity.noContent().build();}
 @PostMapping("/projects/from-template/{id}") public Object from(@PathVariable UUID id,@RequestBody(required=false) CreateProjectRequest r,@CurrentUser AuthenticatedUser u){return ResponseEntity.status(201).body(mapper.toResponse(service.from(id,u.id(),r)));}
}
