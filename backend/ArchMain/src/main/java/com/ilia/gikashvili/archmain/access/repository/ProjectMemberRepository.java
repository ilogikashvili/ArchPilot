package com.ilia.gikashvili.archmain.access.repository;

import com.ilia.gikashvili.archmain.access.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findAllByProjectId(UUID projectId);

    List<ProjectMember> findAllByUserId(UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    long countByProjectId(UUID projectId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);
}
