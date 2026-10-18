package com.ilia.gikashvili.archmain.template.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ilia.gikashvili.archmain.template.entity.ArchitectureTemplate;
public interface ArchitectureTemplateRepository extends JpaRepository<ArchitectureTemplate,UUID> { java.util.List<ArchitectureTemplate> findByBuiltInTrueOrOwnerIdOrderByCreatedAtAsc(UUID ownerId); }
