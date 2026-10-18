package com.ilia.gikashvili.archmain.version.repository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ilia.gikashvili.archmain.version.entity.ArchitectureVersion;
public interface ArchitectureVersionRepository extends JpaRepository<ArchitectureVersion,UUID> { java.util.List<ArchitectureVersion> findByProjectIdOrderByCreatedAtDesc(UUID projectId); }
