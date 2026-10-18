package com.ilia.gikashvili.archmain.architecture.repository;

import com.ilia.gikashvili.archmain.architecture.entity.ArchitectureDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ArchitectureDocumentRepository extends JpaRepository<ArchitectureDocumentEntity, UUID> {

    Optional<ArchitectureDocumentEntity> findByProjectId(UUID projectId);

    boolean existsByProjectId(UUID projectId);
}
