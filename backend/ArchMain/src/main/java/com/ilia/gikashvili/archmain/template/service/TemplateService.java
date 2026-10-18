package com.ilia.gikashvili.archmain.template.service;
import com.ilia.gikashvili.archmain.template.entity.ArchitectureTemplate;
import com.ilia.gikashvili.archmain.template.repository.ArchitectureTemplateRepository;
import com.ilia.gikashvili.archmain.template.dto.TemplateRequest;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.*;
import com.ilia.gikashvili.archmain.common.exception.*;
import com.ilia.gikashvili.archmain.project.service.ProjectService;
import com.ilia.gikashvili.archmain.project.dto.CreateProjectRequest;
import com.ilia.gikashvili.archmain.project.entity.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.*;
@Service
@Transactional
public class TemplateService {
 private final ArchitectureTemplateRepository repo; private final ArchitectureMapper mapper; private final ArchitectureService architecture; private final ProjectService projects;
 public TemplateService(ArchitectureTemplateRepository repo, ArchitectureMapper mapper, ArchitectureService architecture, ProjectService projects) {this.repo=repo;this.mapper=mapper;this.architecture=architecture;this.projects=projects;}
 public List<Map<String,Object>> list(UUID user) {return repo.findByBuiltInTrueOrOwnerIdOrderByCreatedAtAsc(user).stream().map(this::response).toList();}
 public ArchitectureTemplate get(UUID id, UUID user) {var t=repo.findById(id).orElseThrow(()->new NotFoundException("Template",id)); if(!t.getBuiltIn()&&!Objects.equals(t.getOwnerId(),user)) throw new ForbiddenException(); return t;}
 public ArchitectureDocument document(UUID id, UUID user) {var e=new com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity();e.setDocumentJson(get(id,user).getDocumentJson());return mapper.toModel(e);}
 public Map<String,Object> response(ArchitectureTemplate t) {Map<String,Object> r=new LinkedHashMap<>();r.put("id",t.getId());r.put("name",t.getName());r.put("description",t.getDescription());r.put("ownerId",t.getOwnerId());r.put("builtIn",t.getBuiltIn());var e=new com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity();e.setDocumentJson(t.getDocumentJson());r.put("document",mapper.toModel(e));r.put("createdAt",t.getCreatedAt());r.put("updatedAt",t.getUpdatedAt());return r;}
 public ArchitectureTemplate create(UUID user, TemplateRequest r) {if(r.document()==null)throw new ValidationException("Document is required");var t=new ArchitectureTemplate();t.setOwnerId(user);apply(t,r);return repo.saveAndFlush(t);}
 private void apply(ArchitectureTemplate t,TemplateRequest r) {if(r.name()!=null){if(r.name().isBlank())throw new ValidationException("Name is required");t.setName(r.name());}if(t.getName()==null)throw new ValidationException("Name is required");if(r.description()!=null)t.setDescription(r.description());if(r.document()!=null){var v=architecture.validate(r.document());if(!v.isValid())throw new ValidationException("Invalid template document: "+v.getErrors().get(0).message());t.setDocumentJson(mapper.toJson(r.document()));}}
 private ArchitectureTemplate editable(UUID id,UUID user){var t=get(id,user);if(t.getBuiltIn())throw new ForbiddenException("Built-in templates are read-only");return t;}
 public ArchitectureTemplate patch(UUID id,UUID user,TemplateRequest r){var t=editable(id,user);apply(t,r);return repo.saveAndFlush(t);}
 public void delete(UUID id,UUID user){repo.delete(editable(id,user));}
 public Project from(UUID id,UUID user,CreateProjectRequest r){var t=get(id,user);String name=r==null||r.name()==null?t.getName():r.name();if(name.isBlank()||name.length()>200)throw new ValidationException("Invalid project name");var p=projects.create(user,new CreateProjectRequest(name,r==null||r.description()==null?t.getDescription():r.description()));architecture.save(p.getId(),user,document(id,user),0);return p;}
 @EventListener(ApplicationReadyEvent.class)
 public void seed(){if(repo.findByBuiltInTrueOrOwnerIdOrderByCreatedAtAsc(null).stream().anyMatch(ArchitectureTemplate::getBuiltIn))return;
 seedOne("Three-tier web app",List.of(node("web","web",0),node("api","api",250),node("db","database",500)),List.of(edge("a","web","api","sync"),edge("b","api","db","sync")));
 seedOne("Event-driven microservices",List.of(node("producer","service",0),node("events","queue",250),node("consumer","service",500)),List.of(edge("a","producer","events","event"),edge("b","events","consumer","async")));}
 private NodeModel node(String id,String type,double x){return new NodeModel(id,type,id,null,x,0,Map.of());}
 private EdgeModel edge(String id,String source,String target,String type){return new EdgeModel(id,type,source,target,null,Map.of());}
 private void seedOne(String name,List<NodeModel> nodes,List<EdgeModel> edges){var t=new ArchitectureTemplate();t.setBuiltIn(true);apply(t,new TemplateRequest(name,name,new ArchitectureDocument("1.0",nodes,edges,new Viewport(0,0,1),Map.of())));repo.save(t);}
}
