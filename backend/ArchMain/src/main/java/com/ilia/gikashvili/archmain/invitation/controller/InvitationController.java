package com.ilia.gikashvili.archmain.invitation.controller;
import com.ilia.gikashvili.archmain.invitation.service.InvitationService;
import com.ilia.gikashvili.archmain.invitation.entity.Invitation.Status;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
@RestController @RequestMapping(ApiVersion.V1)
public class InvitationController {
 public record Create(@NotBlank @Email String email,@NotNull ProjectRole role) {}
 private final InvitationService s;public InvitationController(InvitationService s){this.s=s;}
 @GetMapping({"/users/me/invitations","/invitations"}) public Object mine(@CurrentUser AuthenticatedUser u){return s.mine(u);}
 @PostMapping("/projects/{pid}/invitations") public Object create(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u,@Valid @RequestBody Create r){return s.create(pid,u,r.email(),r.role());}
 @GetMapping("/projects/{pid}/invitations") public Object list(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u){return s.list(pid,u.id());}
 @PostMapping("/projects/{pid}/invitations/{iid}/accept") public Object accept(@PathVariable UUID iid,@CurrentUser AuthenticatedUser u){return s.change(iid,u,Status.ACCEPTED);}
 @PostMapping("/projects/{pid}/invitations/{iid}/decline") public Object decline(@PathVariable UUID iid,@CurrentUser AuthenticatedUser u){return s.change(iid,u,Status.DECLINED);}
 @DeleteMapping("/projects/{pid}/invitations/{iid}") public Object cancel(@PathVariable UUID iid,@CurrentUser AuthenticatedUser u){return s.change(iid,u,Status.CANCELLED);}
}
