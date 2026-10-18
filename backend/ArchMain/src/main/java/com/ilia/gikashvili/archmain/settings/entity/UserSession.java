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