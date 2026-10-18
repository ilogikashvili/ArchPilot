package com.ilia.gikashvili.archmain.audit.service;
import java.util.UUID; import org.springframework.stereotype.Service; import com.ilia.gikashvili.archmain.audit.entity.AuditEvent; import com.ilia.gikashvili.archmain.audit.repository.AuditEventRepository;
@Service public class AuditService { private final AuditEventRepository repo; public AuditService(AuditEventRepository repo){this.repo=repo;} public void record(UUID userId,String action,String detail){var e=new AuditEvent();e.setUserId(userId);e.setAction(action);e.setDetail(detail);repo.save(e);} }
