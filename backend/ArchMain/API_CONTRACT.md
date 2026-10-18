# API contract (base /api/v1; Bearer JWT)

Existing exact controller signatures and DTO/entity shapes below. Arrays are returned directly unless ResponseEntity or page wrapper is shown.

## invitation

### InvitationController.java
```java
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
 @PostMapping("/projects/{pid}/invitations") public Object create(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u,@Valid @RequestBody Create r){return s.create(pid,u,r.email(),r.role());}
 @GetMapping("/projects/{pid}/invitations") public Object list(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u){return s.list(pid,u.id());}
 @PostMapping("/projects/{pid}/invitations/{iid}/accept") public Object accept(@PathVariable UUID iid,@CurrentUser AuthenticatedUser u){return s.change(iid,u,Status.ACCEPTED);}
 @PostMapping("/projects/{pid}/invitations/{iid}/decline") public Object decline(@PathVariable UUID iid,@CurrentUser AuthenticatedUser u){return s.change(iid,u,Status.DECLINED);}
 @DeleteMapping("/projects/{pid}/invitations/{iid}") public Object cancel(@PathVariable UUID iid,@CurrentUser AuthenticatedUser u){return s.change(iid,u,Status.CANCELLED);}
}

```

### Invitation.java
```java
package com.ilia.gikashvili.archmain.invitation.entity;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="project_invitations")
public class Invitation extends BaseEntity {
 public enum Status { PENDING, ACCEPTED, DECLINED, CANCELLED }
 @Column(nullable=false) public UUID projectId;
 @Column(nullable=false) public UUID inviterId;
 @Column(nullable=false) public String email;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public ProjectRole role;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status=Status.PENDING;
 @Version public long version;
}

```

## access

### PermissionController.java
```java
package com.ilia.gikashvili.archmain.access.controller;

import com.ilia.gikashvili.archmain.access.dto.EffectivePermissionsResponse;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}")
public class PermissionController {

    private final ProjectAccessService accessService;

    public PermissionController(ProjectAccessService accessService) {
        this.accessService = accessService;
    }

    /**
     * GET /projects/{projectId}/permissions — Get current user's effective permissions.
     */
    @GetMapping("/permissions")
    public ResponseEntity<EffectivePermissionsResponse> getPermissions(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var response = accessService.getEffectivePermissionsInfo(projectId, currentUser.id());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /projects/{projectId}/leave — Current user leaves a shared project.
     */
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveProject(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        accessService.leaveProject(projectId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}

```

### MemberController.java
```java
package com.ilia.gikashvili.archmain.access.controller;

import com.ilia.gikashvili.archmain.access.dto.ProjectMemberResponse;
import com.ilia.gikashvili.archmain.access.dto.UpdateMemberRoleRequest;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/members")
public class MemberController {

    private final ProjectAccessService accessService;

    public MemberController(ProjectAccessService accessService) {
        this.accessService = accessService;
    }

    /**
     * GET /projects/{projectId}/members — List project members.
     */
    @GetMapping
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var members = accessService.listMembers(projectId, currentUser.id()).stream()
                .map(m -> new ProjectMemberResponse(
                        m.getId(),
                        m.getProjectId(),
                        m.getUserId(),
                        m.getRole(),
                        m.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(members);
    }

    /**
     * PATCH /projects/{projectId}/members/{userId} — Change a member's role.
     */
    @PatchMapping("/{targetUserId}")
    public ResponseEntity<ProjectMemberResponse> updateMemberRole(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        var updated = accessService.updateMemberRole(projectId, targetUserId, request.role(), currentUser.id());
        return ResponseEntity.ok(new ProjectMemberResponse(
                updated.getId(),
                updated.getProjectId(),
                updated.getUserId(),
                updated.getRole(),
                updated.getCreatedAt()
        ));
    }

    /**
     * DELETE /projects/{projectId}/members/{userId} — Remove a member from the project.
     */
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID targetUserId) {
        accessService.removeMember(projectId, targetUserId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}

```

### EffectivePermissionsResponse.java
```java
package com.ilia.gikashvili.archmain.access.dto;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.policy.ProjectRole;

import java.util.Set;
import java.util.UUID;

public record EffectivePermissionsResponse(
        UUID projectId,
        UUID userId,
        ProjectRole role,
        Set<Permission> permissions
) {
}

```

### ProjectMemberResponse.java
```java
package com.ilia.gikashvili.archmain.access.dto;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        ProjectRole role,
        Instant createdAt
) {
}

```

### UpdateMemberRoleRequest.java
```java
package com.ilia.gikashvili.archmain.access.dto;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role is required")
        ProjectRole role
) {
}

```

### ProjectMember.java
```java
package com.ilia.gikashvili.archmain.access.entity;

import com.ilia.gikashvili.archmain.access.policy.ProjectRole;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Represents a user's membership in a project with a specific role.
 */
@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"projectId", "userId"}))
public class ProjectMember extends BaseEntity {

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role;

    // --- Getters & Setters ---

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public ProjectRole getRole() {
        return role;
    }

    public void setRole(ProjectRole role) {
        this.role = role;
    }
}

```

## version

### VersionController.java
```java
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
 @GetMapping("/projects/{pid}/versions") public Object list(@PathVariable UUID pid,@CurrentUser AuthenticatedUser u){return service.list(pid,u.id()).stream().map(service::response).toList();}
 @GetMapping("/projects/{pid}/versions/{vid}") public Object get(@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){return service.response(service.get(vid,u.id(),Permission.READ));}
 @GetMapping("/projects/{pid}/versions/{vid}/architecture") public Object architecture(@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){return service.document(service.get(vid,u.id(),Permission.READ));}
 @PostMapping("/projects/{pid}/versions") public Object createForProject(@PathVariable UUID pid,@Valid @RequestBody VersionRequest r,@CurrentUser AuthenticatedUser u){return ResponseEntity.status(201).body(service.response(service.create(pid,u.id(),r)));}
 @PostMapping("/projects/{pid}/versions/{vid}/restore") public Object restore(@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){return service.restore(vid,u.id());}
 public record CompareRequest(UUID fromVersionId,UUID toVersionId){}
 @PostMapping("/projects/{pid}/versions/compare") public Object compare(@RequestBody CompareRequest r,@CurrentUser AuthenticatedUser u){return service.compare(r.fromVersionId(),r.toVersionId(),u.id());}
 @PatchMapping("/projects/{pid}/versions/{vid}") public Object patch(@PathVariable UUID vid,@Valid @RequestBody VersionRequest r,@CurrentUser AuthenticatedUser u){return service.response(service.patch(vid,u.id(),r));}
 @DeleteMapping("/projects/{pid}/versions/{vid}") public Object delete(@PathVariable UUID vid,@CurrentUser AuthenticatedUser u){service.delete(vid,u.id());return ResponseEntity.noContent().build();}
}

```

### VersionRequest.java
```java
package com.ilia.gikashvili.archmain.version.dto;
import jakarta.validation.constraints.Size;
import java.util.UUID;
public record VersionRequest(@Size(max=200) String name, @Size(max=2000) String description, UUID projectId) {}

```

### ArchitectureVersion.java
```java
package com.ilia.gikashvili.archmain.version.entity;
import jakarta.persistence.*;
import java.util.UUID;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
@Entity
@Table(name="version_documents")
public class ArchitectureVersion extends BaseEntity {
private UUID projectId;
public UUID getProjectId() { return projectId; }
public void setProjectId(UUID value) { this.projectId=value; }
private String name;
public String getName() { return name; }
public void setName(String value) { this.name=value; }
@Column(columnDefinition="TEXT")
private String description;
public String getDescription() { return description; }
public void setDescription(String value) { this.description=value; }
@Column(columnDefinition="TEXT")
private String documentJson;
public String getDocumentJson() { return documentJson; }
public void setDocumentJson(String value) { this.documentJson=value; }
private UUID createdBy;
public UUID getCreatedBy() { return createdBy; }
public void setCreatedBy(UUID value) { this.createdBy=value; }
private long sourceRevision;
public long getSourceRevision() { return sourceRevision; }
public void setSourceRevision(long value) { this.sourceRevision=value; }
}

```

## branch

### BranchController.java
```java
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

```

### BranchRequests.java
```java
package com.ilia.gikashvili.archmain.branch.dto;
import java.util.UUID;
import jakarta.validation.constraints.*;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
public final class BranchRequests {
 public record Create(@NotBlank @Size(max=255) String name,@Size(max=2000) String description,UUID fromBranchId) {}
 public record Patch(@Size(min=1,max=255) String name,@Size(max=2000) String description) {}
 public record Save(@NotNull ArchitectureDocument document,@PositiveOrZero Long expectedRevision) {}
 public record Target(UUID targetBranchId,String strategy) {}
}

```

### Branch.java
```java
package com.ilia.gikashvili.archmain.branch.entity;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="architecture_branches", uniqueConstraints=@UniqueConstraint(columnNames={"projectId","name"}))
public class Branch extends BaseEntity {
 @Column(nullable=false) public UUID projectId;
 @Column(nullable=false) public String name;
 @Column(length=2000) public String description;
 @Column(nullable=false,columnDefinition="TEXT") public String documentJson;
 public long revision;
 @Version public long lockVersion;
}

```

## notification

### NotificationController.java
```java
package com.ilia.gikashvili.archmain.notification.controller;
import java.util.*; import org.springframework.web.bind.annotation.*; import org.springframework.data.domain.*; import com.ilia.gikashvili.archmain.security.*; import com.ilia.gikashvili.archmain.notification.repository.*; import com.ilia.gikashvili.archmain.notification.service.*; import com.ilia.gikashvili.archmain.notification.entity.*;
@RestController @RequestMapping(com.ilia.gikashvili.archmain.common.api.ApiVersion.V1+"/notifications") public class NotificationController {private final NotificationRepository repo; private final NotificationService service;public NotificationController(NotificationRepository r,NotificationService s){repo=r;service=s;}
@GetMapping public Page<Notification> list(@CurrentUser AuthenticatedUser u,@RequestParam(defaultValue="false") boolean unreadOnly,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){var p=PageRequest.of(page,Math.min(size,100),Sort.by("createdAt").descending());return unreadOnly?repo.findByUserIdAndReadFalse(u.id(),p):repo.findByUserId(u.id(),p);}
@GetMapping("/unread-count") public Map<String,Long> count(@CurrentUser AuthenticatedUser u){return Map.of("count",repo.countByUserIdAndReadFalse(u.id()));}
public record ReadRequest(@jakarta.validation.constraints.NotNull Boolean read){}
@PatchMapping("/{id}") public Notification mark(@CurrentUser AuthenticatedUser u,@PathVariable UUID id,@jakarta.validation.Valid @RequestBody ReadRequest b){return service.mark(u.id(),id,b.read());}
@PostMapping("/mark-all-read") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void all(@CurrentUser AuthenticatedUser u){service.all(u.id());}
@DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(@CurrentUser AuthenticatedUser u,@PathVariable UUID id){repo.delete(service.owned(u.id(),id));}
}

```

### Notification.java
```java
package com.ilia.gikashvili.archmain.notification.entity; import jakarta.persistence.*; import java.util.*; @Entity public class Notification extends com.ilia.gikashvili.archmain.common.entity.BaseEntity {@Column(nullable=false) private UUID userId; public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;} private String type; public String getType(){return type;} public void setType(String v){type=v;} private String title; public String getTitle(){return title;} public void setTitle(String v){title=v;}@Column(length=10000) private String body; public String getBody(){return body;} public void setBody(String v){body=v;} private String link; public String getLink(){return link;} public void setLink(String v){link=v;}@Column(name="is_read") private boolean read; public boolean getRead(){return read;} public void setRead(boolean v){read=v;}}
```

## search

### SearchController.java
```java
package com.ilia.gikashvili.archmain.search.controller;
import java.util.*;import org.springframework.web.bind.annotation.*;import com.ilia.gikashvili.archmain.security.*;import com.ilia.gikashvili.archmain.search.service.SearchService;
@RestController @RequestMapping(com.ilia.gikashvili.archmain.common.api.ApiVersion.V1) public class SearchController {private final SearchService service;public SearchController(SearchService s){service=s;}@GetMapping("/search") public Object global(@CurrentUser AuthenticatedUser u,@RequestParam String q){return service.global(u.id(),q);}@GetMapping("/projects/{pid}/search") public Object project(@CurrentUser AuthenticatedUser u,@PathVariable UUID pid,@RequestParam String q){return service.project(u.id(),pid,q);}}

```

## settings

### SettingsController.java
```java
package com.ilia.gikashvili.archmain.settings.controller;
import java.util.*; import org.springframework.web.bind.annotation.*; import com.ilia.gikashvili.archmain.security.*; import com.ilia.gikashvili.archmain.settings.repository.*; import com.ilia.gikashvili.archmain.settings.entity.*; import com.ilia.gikashvili.archmain.settings.service.*; import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
@RestController @RequestMapping(com.ilia.gikashvili.archmain.common.api.ApiVersion.V1+"/users/me") public class SettingsController {private final PreferencesService preferences;private final UserSessionRepository sessions;private final ConnectedAccountRepository accounts;public SettingsController(PreferencesService p,UserSessionRepository s,ConnectedAccountRepository a){preferences=p;sessions=s;accounts=a;}
@GetMapping("/preferences") public Map<String,Object> get(@CurrentUser AuthenticatedUser u){return preferences.get(u.id());}
@PatchMapping("/preferences") public Map<String,Object> patch(@CurrentUser AuthenticatedUser u,@RequestBody Map<String,Object> b){return preferences.patch(u.id(),b,false);}
@PatchMapping("/notification-preferences") public Map<String,Object> notifications(@CurrentUser AuthenticatedUser u,@RequestBody Map<String,Object> b){return preferences.patch(u.id(),b,true);}
public record SessionView(UUID id,String userAgent,String ip,java.time.Instant createdAt,java.time.Instant lastSeenAt,boolean revoked){}
@GetMapping("/sessions") public List<SessionView> sessions(@CurrentUser AuthenticatedUser u){return sessions.findAllByUserId(u.id()).stream().map(s->new SessionView(s.getId(),s.getUserAgent(),s.getIp(),s.getCreatedAt(),s.getLastSeenAt(),s.getRevoked())).toList();}
@DeleteMapping("/sessions/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void revoke(@CurrentUser AuthenticatedUser u,@PathVariable UUID id){var s=sessions.findByIdAndUserId(id,u.id()).orElseThrow(()->new NotFoundException("Session",id));s.setRevoked(true);sessions.save(s);}
@GetMapping("/connected-accounts") public List<ConnectedAccount> accounts(@CurrentUser AuthenticatedUser u){return accounts.findAllByUserId(u.id());}
@DeleteMapping("/connected-accounts/{provider}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void disconnect(@CurrentUser AuthenticatedUser u,@PathVariable String provider){var a=accounts.findByUserIdAndProvider(u.id(),provider).orElseThrow(()->new NotFoundException("Connected account not found"));accounts.delete(a);}
}

```

### ConnectedAccount.java
```java
package com.ilia.gikashvili.archmain.settings.entity; import jakarta.persistence.*; import java.util.*; @Entity public class ConnectedAccount extends com.ilia.gikashvili.archmain.common.entity.BaseEntity {@Column(nullable=false) private UUID userId; public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;} private String provider; public String getProvider(){return provider;} public void setProvider(String v){provider=v;} private String externalId; public String getExternalId(){return externalId;} public void setExternalId(String v){externalId=v;}}
```

### UserPreferences.java
```java
package com.ilia.gikashvili.archmain.settings.entity; import jakarta.persistence.*; import java.util.*; @Entity public class UserPreferences extends com.ilia.gikashvili.archmain.common.entity.BaseEntity {@Column(nullable=false,unique=true) private UUID userId; public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;}@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) private Map<String,Object> preferences; public Map<String,Object> getPreferences(){return preferences;} public void setPreferences(Map<String,Object> v){preferences=v;}@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) private Map<String,Object> notificationPreferences; public Map<String,Object> getNotificationPreferences(){return notificationPreferences;} public void setNotificationPreferences(Map<String,Object> v){notificationPreferences=v;}}
```

### UserSession.java
```java
package com.ilia.gikashvili.archmain.settings.entity;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
@Entity public class UserSession extends com.ilia.gikashvili.archmain.common.entity.BaseEntity {
@Column(nullable=false) private UUID userId;
public UUID getUserId(){return userId;}
public void setUserId(UUID v){this.userId=v;}
@Column(length=1000) private String userAgent;
public String getUserAgent(){return userAgent;}
public void setUserAgent(String v){this.userAgent=v;}
 private String ip;
public String getIp(){return ip;}
public void setIp(String v){this.ip=v;}
 private Instant lastSeenAt;
public Instant getLastSeenAt(){return lastSeenAt;}
public void setLastSeenAt(Instant v){this.lastSeenAt=v;}
 private boolean revoked;
public boolean getRevoked(){return revoked;}
public void setRevoked(boolean v){this.revoked=v;}
 private String refreshId;
public String getRefreshId(){return refreshId;}
public void setRefreshId(String v){this.refreshId=v;}
}
```

## template

### TemplateController.java
```java
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

```

### TemplateRequest.java
```java
package com.ilia.gikashvili.archmain.template.dto;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import jakarta.validation.constraints.Size;
public record TemplateRequest(@Size(max=200) String name, @Size(max=2000) String description, ArchitectureDocument document) {}

```

### ArchitectureTemplate.java
```java
package com.ilia.gikashvili.archmain.template.entity;
import jakarta.persistence.*;
import java.util.UUID;
import com.ilia.gikashvili.archmain.common.entity.BaseEntity;
@Entity
@Table(name="template_documents")
public class ArchitectureTemplate extends BaseEntity {
private String name;
public String getName() { return name; }
public void setName(String value) { this.name=value; }
@Column(columnDefinition="TEXT")
private String description;
public String getDescription() { return description; }
public void setDescription(String value) { this.description=value; }
@Column(columnDefinition="TEXT")
private String documentJson;
public String getDocumentJson() { return documentJson; }
public void setDocumentJson(String value) { this.documentJson=value; }
private UUID ownerId;
public UUID getOwnerId() { return ownerId; }
public void setOwnerId(UUID value) { this.ownerId=value; }
private boolean builtIn;
public boolean getBuiltIn() { return builtIn; }
public void setBuiltIn(boolean value) { this.builtIn=value; }
}

```

## admin

### AdminController.java
```java
package com.ilia.gikashvili.archmain.admin.controller;
import java.util.*;import java.time.Instant;import org.springframework.web.bind.annotation.*;import org.springframework.data.domain.*;import com.ilia.gikashvili.archmain.security.*;import com.ilia.gikashvili.archmain.admin.repository.*;import com.ilia.gikashvili.archmain.admin.service.*;import com.ilia.gikashvili.archmain.audit.repository.*;import com.ilia.gikashvili.archmain.audit.entity.*;import com.ilia.gikashvili.archmain.user.repository.*;import com.ilia.gikashvili.archmain.common.exception.ForbiddenException;
@RestController @RequestMapping(com.ilia.gikashvili.archmain.common.api.ApiVersion.V1+"/admin") public class AdminController {private final UserRepository users;private final BackgroundJobRepository jobs;private final BackgroundJobService service;private final AuditEventRepository audits;public AdminController(UserRepository u,BackgroundJobRepository j,BackgroundJobService s,AuditEventRepository a){users=u;jobs=j;service=s;audits=a;}
private void require(AuthenticatedUser u){if(!users.findById(u.id()).map(x->"ADMIN".equals(x.getRole())).orElse(false))throw new ForbiddenException("Admin role required");}
public record UserView(UUID id,String email,String displayName,String role,boolean deleted,Instant createdAt){}
@GetMapping("/users") public Page<UserView> users(@CurrentUser AuthenticatedUser u,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){require(u);return users.findAll(PageRequest.of(page,Math.min(size,100),Sort.by("createdAt").descending())).map(x->new UserView(x.getId(),x.getEmail(),x.getDisplayName(),x.getRole(),x.isDeleted(),x.getCreatedAt()));}
@GetMapping("/jobs") public Object jobs(@CurrentUser AuthenticatedUser u,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){require(u);return jobs.findAll(PageRequest.of(page,Math.min(size,100),Sort.by("createdAt").descending()));}
@PostMapping("/jobs/{jobId}/retry") public Object retry(@CurrentUser AuthenticatedUser u,@PathVariable UUID jobId){require(u);var j=service.retry(jobId);service.execute(jobId);return j;}
@GetMapping("/audit-events") public Object audits(@CurrentUser AuthenticatedUser u,@RequestParam(required=false) UUID userId,@RequestParam(required=false) String action,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){require(u);if(from!=null&&to!=null&&from.isAfter(to))throw new IllegalArgumentException();var list=audits.findAll(Sort.by("createdAt").descending()).stream().filter(e->userId==null||userId.equals(e.getUserId())).filter(e->action==null||action.equals(e.getAction())).filter(e->from==null||!e.getCreatedAt().isBefore(from)).filter(e->to==null||!e.getCreatedAt().isAfter(to)).toList();var p=PageRequest.of(page,Math.min(size,100));int start=(int)Math.min(p.getOffset(),list.size());return new PageImpl<>(list.subList(start,Math.min(start+p.getPageSize(),list.size())),p,list.size());}
}

```

### BackgroundJob.java
```java
package com.ilia.gikashvili.archmain.admin.entity; import jakarta.persistence.*; import java.util.*; @Entity public class BackgroundJob extends com.ilia.gikashvili.archmain.common.entity.BaseEntity { private UUID userId; public UUID getUserId(){return userId;} public void setUserId(UUID v){userId=v;} private String type; public String getType(){return type;} public void setType(String v){type=v;} private String status; public String getStatus(){return status;} public void setStatus(String v){status=v;}@Column(length=10000) private String payload; public String getPayload(){return payload;} public void setPayload(String v){payload=v;}@Column(length=2000) private String error; public String getError(){return error;} public void setError(String v){error=v;} private int attempts; public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}}
```

## New AI API (implementation in progress)
GET /users/me/ai-keys -> [{id,provider,label,last4,model,createdAt}]
POST /users/me/ai-keys {provider:"OPENAI|ANTHROPIC|GEMINI|OPENROUTER",label,apiKey,model?} -> same metadata object (201)
DELETE /users/me/ai-keys/{id} -> 204
POST /users/me/ai-keys/{id}/test -> {ok,message}
POST /projects/{projectId}/ai/agent-runs {keyId,prompt} -> {id,status,summary,operations,error,createdAt,projectId,userId}
GET /projects/{projectId}/ai/agent-runs -> array of recent run objects. Status SUCCEEDED or FAILED.
Versions gain message,author:{id,name},parentVersionId,branch:{id,name},changeSummary:{nodes:{added,removed,changed},edges:{added,removed,changed}}. Create accepts message and branchId, lists accept ?branchId=UUID (or main); diff GET /projects/{pid}/versions/{vid}/diff -> {nodes:{added:[],removed:[],changed:[{id,before,after}]},edges:same}.

## Implementation verified
- GET /users/me/invitations (alias /invitations) returns pending invitation objects for authenticated email: [{id,createdAt,updatedAt,projectId,inviterId,email,role,status}].
- Invitation create/list/accept/decline/revoke all return HTTP 200; revoke is DELETE and status CANCELLED. Accept/decline require matching invitee email.
- Branch create returns HTTP 200. Version create and key create return 201; key delete returns 204.
- Provider URLs: app.ai.providers.openai.base-url (default https://api.openai.com/v1), anthropic.base-url (https://api.anthropic.com/v1), gemini.base-url (https://generativelanguage.googleapis.com/v1beta), openrouter.base-url (https://openrouter.ai/api/v1), all under app.ai.providers. No trailing endpoint path in base URL.
- Encryption: app.ai.key-encryption-secret; override development default in production. Key plaintext/ciphertext never returned.
- Agent run failures return HTTP 200 with status FAILED and error; successful status SUCCEEDED. GET returns newest 50. Existing realtime operations.applied event is emitted through the existing operations service, attributed to requesting user.
- Version create accepts {name?,description?,projectId?,message?,branchId?}; name defaults to message; branchId omitted means main. First commit diff compares with empty document.

## SOLID principles (design pattern dropdown, category "SOLID Principles")
Pattern ids: solid-srp, solid-ocp, solid-lsp, solid-isp, solid-dip, solid-all — use the existing
`/projects/{id}/architecture/patterns/{patternId}/preview|apply` endpoints.

## Ollama (local models)
- `POST /users/me/ai-keys` `{provider:"OLLAMA", model:"qwen3:4b", baseUrl:"http://localhost:11434", apiKey?}` (key optional)
- `GET /users/me/ai-keys/ollama/models?baseUrl=...` → `{ok, models:[{name,size,modifiedAt}], message?}`
Calls `POST {baseUrl}/api/chat` with `stream:false`; `<think>` blocks are stripped. The backend must be able to reach Ollama.

## Team chat (humans online + API agents + AI teammates)
- `GET/POST /projects/{id}/team-agents`, `PATCH/DELETE /projects/{id}/team-agents/{agentId}` `{name, role, persona, keyId, autoReply}`
- `GET /projects/{id}/chat` → `{messages, agents, online, discussionRunning}`
- `POST /projects/{id}/chat/messages` `{content}` — `@Handle` asks one teammate, `@all` asks all; teammates may @mention each other (max chain depth 3)
- `POST /projects/{id}/chat/discussions` `{topic, agentIds?, rounds(1-5)}`, `POST /projects/{id}/chat/discussions/stop`
- WebSocket events: `chat.message {message}`, `chat.typing {agentId,name,typing}`
