package com.ilia.gikashvili.archmain.audit.entity;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
@Entity public class AuditEvent extends com.ilia.gikashvili.archmain.common.entity.BaseEntity {
 private UUID userId;
public UUID getUserId(){return userId;}
public void setUserId(UUID v){this.userId=v;}
 private String action;
public String getAction(){return action;}
public void setAction(String v){this.action=v;}
@Column(length=2000) private String detail;
public String getDetail(){return detail;}
public void setDetail(String v){this.detail=v;}
}