package com.ilia.gikashvili.archmain.branch.service;
import com.ilia.gikashvili.archmain.branch.entity.Branch;
import com.ilia.gikashvili.archmain.branch.repository.BranchRepository;
import com.ilia.gikashvili.archmain.branch.dto.BranchRequests.*;
import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.architecture.mapper.ArchitectureMapper;
import com.ilia.gikashvili.archmain.architecture.model.*;
import com.ilia.gikashvili.archmain.common.exception.*;
import com.ilia.gikashvili.archmain.realtime.ArchitectureUpdated;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
@Service @Transactional
public class BranchService {
 private final BranchRepository repo; private final ProjectAccessService access; private final ArchitectureService main; private final ArchitectureMapper mapper; private final ObjectMapper json; private final ApplicationEventPublisher events;
 public BranchService(BranchRepository r,ProjectAccessService a,ArchitectureService m,ArchitectureMapper x,ObjectMapper j,ApplicationEventPublisher e){repo=r;access=a;main=m;mapper=x;json=j;events=e;}
 public Branch get(UUID id,UUID user,Permission permission){Branch b=repo.findLockedById(id).orElseThrow(()->new NotFoundException("Branch",id));access.require(b.projectId,user,permission);return b;}
 public List<Map<String,Object>> list(UUID pid,UUID u){access.require(pid,u,Permission.READ);return repo.findAllByProjectId(pid).stream().map(this::info).toList();}
 public Map<String,Object> info(Branch b){Map<String,Object> v=new LinkedHashMap<>();v.put("id",b.getId());v.put("projectId",b.projectId);v.put("name",b.name);v.put("description",b.description);v.put("revision",b.revision);return v;}
 private void name(UUID pid,String name){if(name==null||name.isBlank())throw new ValidationException("Name is required");if(repo.existsByProjectIdAndName(pid,name))throw new ConflictException("Branch name already exists");}
 public Map<String,Object> create(UUID pid,UUID u,Create r){access.require(pid,u,Permission.EDIT);name(pid,r.name());Branch b=new Branch();b.projectId=pid;b.name=r.name();b.description=r.description();b.documentJson=mapper.toJson(r.fromBranchId()==null?main.getDocument(pid,u):document(same(r.fromBranchId(),pid,u)));return info(repo.saveAndFlush(b));}
 private Branch same(UUID id,UUID pid,UUID u){Branch b=get(id,u,Permission.READ);if(!b.projectId.equals(pid))throw new ValidationException("Branches must belong to the same project");return b;}
 public Map<String,Object> patch(UUID id,UUID u,Patch r){Branch b=get(id,u,Permission.EDIT);if(r.name()!=null&&!r.name().equals(b.name)){name(b.projectId,r.name());b.name=r.name();}if(r.description()!=null)b.description=r.description();return info(repo.saveAndFlush(b));}
 public void delete(UUID id,UUID u){repo.delete(get(id,u,Permission.EDIT));}
 public ArchitectureDocument document(Branch b){try{return json.readValue(b.documentJson,ArchitectureDocument.class);}catch(Exception e){throw new IllegalStateException(e);}}
 public Map<String,Object> architecture(Branch b){return Map.of("branchId",b.getId(),"projectId",b.projectId,"document",document(b),"revision",b.revision);}
 public Map<String,Object> save(UUID id,UUID u,Save r){Branch b=get(id,u,Permission.EDIT);if(r.expectedRevision()!=null&&r.expectedRevision()!=b.revision)throw new ConflictException("Branch revision mismatch");validate(r.document());b.documentJson=mapper.toJson(r.document());b.revision++;repo.saveAndFlush(b);events.publishEvent(new ArchitectureUpdated(b.projectId,b.getId(),u));return architecture(b);}
 private void validate(ArchitectureDocument d){if(d==null||d.nodes()==null||d.edges()==null)throw new ValidationException("Document nodes and edges are required");var v=main.validate(d);if(!v.isValid())throw new ValidationException(v.getErrors().get(0).message());}
 private <T> List<T> mergeList(List<T> target,List<T> source,Function<T,String> id,boolean theirs){Map<String,T> m=new LinkedHashMap<>();target.forEach(x->m.put(id.apply(x),x));source.forEach(x->{if(theirs)m.put(id.apply(x),x);else m.putIfAbsent(id.apply(x),x);});return new ArrayList<>(m.values());}
 public Object merge(UUID id,UUID u,Target r){Branch source=get(id,u,Permission.EDIT);boolean theirs=r.strategy()==null||r.strategy().equals("theirs");if(!theirs&&!"ours".equals(r.strategy()))throw new ValidationException("Strategy must be theirs or ours");if(id.equals(r.targetBranchId()))throw new ValidationException("Cannot merge a branch into itself");Branch target=r.targetBranchId()==null?null:same(r.targetBranchId(),source.projectId,u);ArchitectureDocument a=document(source),b=target==null?main.getDocument(source.projectId,u):document(target);ArchitectureDocument merged=new ArchitectureDocument(b.schemaVersion(),mergeList(b.nodes(),a.nodes(),NodeModel::id,theirs),mergeList(b.edges(),a.edges(),EdgeModel::id,theirs),b.viewport(),b.metadata());validate(merged);if(target!=null)return save(target.getId(),u,new Save(merged,target.revision));var entity=main.getByProjectId(source.projectId,u);return mapper.toResponse(main.save(source.projectId,u,merged,entity.getRevision()));}
 private <T> Map<String,Object> diff(List<T> source,List<T> target,Function<T,String> id){Map<String,T> a=new LinkedHashMap<>(),b=new LinkedHashMap<>();source.forEach(x->a.put(id.apply(x),x));target.forEach(x->b.put(id.apply(x),x));return Map.of("added",a.entrySet().stream().filter(e->!b.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),"removed",b.entrySet().stream().filter(e->!a.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),"changed",a.entrySet().stream().filter(e->b.containsKey(e.getKey())&&!Objects.equals(e.getValue(),b.get(e.getKey()))).map(e->Map.of("id",e.getKey(),"before",b.get(e.getKey()),"after",e.getValue())).toList());}
 public Object compare(UUID id,UUID u,Target r){Branch s=get(id,u,Permission.READ);ArchitectureDocument a=document(s),b=r.targetBranchId()==null?main.getDocument(s.projectId,u):document(same(r.targetBranchId(),s.projectId,u));return Map.of("nodes",diff(a.nodes(),b.nodes(),NodeModel::id),"edges",diff(a.edges(),b.edges(),EdgeModel::id));}
}
