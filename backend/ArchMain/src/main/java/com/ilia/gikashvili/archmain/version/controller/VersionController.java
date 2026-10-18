package com.ilia.gikashvili.archmain.version.controller;
import com.ilia.gikashvili.archmain.version.service.VersionService;
import com.ilia.gikashvili.archmain.version.dto.VersionRequest;
import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.util.UUID;
@RestController
@RequestMapping(ApiVersion.V1)
public class VersionController {
 private final VersionService service;
 public VersionController(VersionService service){this.service=service;}
 @GetMapping("/projects/{pid}/versions") public Object list(@PathVariable UUID pid,@RequestParam(required=false) String branchId,@CurrentUser AuthenticatedUser u){return service.list(pid,u.id()).stream().filter(v->branchId==null || (branchId.equals("main")?v.branchId==null:branchId.equals(String.valueOf(v.branchId)))).map(service::response).toList();}
 @GetMapping("/projects/{pid}/versions/{vid}") public Object get(@PathVariable UUID pid,@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){return service.response(service.scoped(pid,vid,u.id(),Permission.READ));}
 @GetMapping("/projects/{pid}/versions/{vid}/architecture") public Object architecture(@PathVariable UUID pid,@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){return service.document(service.scoped(pid,vid,u.id(),Permission.READ));}
 @PostMapping("/projects/{pid}/versions") public Object createForProject(@PathVariable UUID pid,@Valid @RequestBody VersionRequest r,@CurrentUser AuthenticatedUser u){return ResponseEntity.status(201).body(service.response(service.create(pid,u.id(),r)));}
 @PostMapping("/projects/{pid}/versions/{vid}/restore") public Object restore(@PathVariable UUID pid,@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){service.scoped(pid,vid,u.id(),Permission.EDIT);return service.restore(vid,u.id());}
 @GetMapping("/projects/{pid}/versions/{vid}/diff") public Object diff(@PathVariable UUID pid,@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){return service.parentDiff(service.scoped(pid,vid,u.id(),Permission.READ));}
 public record CompareRequest(UUID fromVersionId,UUID toVersionId){}
 @PostMapping("/projects/{pid}/versions/compare") public Object compare(@RequestBody CompareRequest r,@CurrentUser AuthenticatedUser u){return service.compare(r.fromVersionId(),r.toVersionId(),u.id());}
 @PatchMapping("/projects/{pid}/versions/{vid}") public Object patch(@PathVariable UUID pid,@PathVariable UUID vid,@Valid @RequestBody VersionRequest r,@CurrentUser AuthenticatedUser u){service.scoped(pid,vid,u.id(),Permission.EDIT);return service.response(service.patch(vid,u.id(),r));}
 @DeleteMapping("/projects/{pid}/versions/{vid}") public Object delete(@PathVariable UUID pid,@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){service.scoped(pid,vid,u.id(),Permission.EDIT);service.delete(vid,u.id());return ResponseEntity.noContent().build();}
}
