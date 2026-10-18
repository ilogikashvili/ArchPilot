package com.ilia.gikashvili.archmain.audit.repository;
import java.util.*; import org.springframework.data.jpa.repository.*; import com.ilia.gikashvili.archmain.audit.entity.AuditEvent;
public interface AuditEventRepository extends JpaRepository<AuditEvent,UUID> {  }