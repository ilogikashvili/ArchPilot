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
