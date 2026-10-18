package com.ilia.gikashvili.archmain.realtime;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.security.*;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping(ApiVersion.V1)
public class PresenceController {
 private final ProjectSocket socket;private final ProjectAccessService access;
 public PresenceController(ProjectSocket s,ProjectAccessService a){socket=s;access=a;}
 @GetMapping("/projects/{pid}/presence") public Object presence(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u){access.require(pid,u.id(),Permission.READ);return socket.presence(pid);}
}
