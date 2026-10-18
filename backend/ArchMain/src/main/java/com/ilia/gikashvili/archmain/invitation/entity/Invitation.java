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
