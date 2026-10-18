package com.ilia.gikashvili.archmain.branch.controller;
import com.ilia.gikashvili.archmain.branch.service.BranchService;
import com.ilia.gikashvili.archmain.branch.dto.BranchRequests.*;
import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.UUID;
@RestController @RequestMapping(ApiVersion.V1)
public class BranchController {
 private final BranchService s; public BranchController(BranchService s){this.s=s;}
 @GetMapping("/projects/{pid}/branches") public Object list(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u){return s.list(pid,u.id());}
 @PostMapping("/projects/{pid}/branches") public Object create(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u,@Valid @RequestBody Create r){return s.create(pid,u.id(),r);}
 @GetMapping("/projects/{pid}/branches/{bid}") public Object get(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u){return s.info(s.get(bid,u.id(),Permission.READ));}
 @PatchMapping("/projects/{pid}/branches/{bid}") public Object patch(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u,@Valid @RequestBody Patch r){return s.patch(bid,u.id(),r);}
 @DeleteMapping("/projects/{pid}/branches/{bid}") public void delete(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u){s.delete(bid,u.id());}
 @GetMapping("/projects/{pid}/branches/{bid}/architecture") public Object architecture(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u){return s.architecture(s.get(bid,u.id(),Permission.READ));}
 @PutMapping("/projects/{pid}/branches/{bid}/architecture") public Object save(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u,@Valid @RequestBody Save r){return s.save(bid,u.id(),r);}
 @PostMapping("/projects/{pid}/branches/{bid}/merge") public Object merge(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u,@RequestBody Target r){return s.merge(bid,u.id(),r);}
 @PostMapping("/projects/{pid}/branches/{bid}/compare") public Object compare(@PathVariable UUID bid,@CurrentUser AuthenticatedUser u,@RequestBody Target r){return s.compare(bid,u.id(),r);}
}
