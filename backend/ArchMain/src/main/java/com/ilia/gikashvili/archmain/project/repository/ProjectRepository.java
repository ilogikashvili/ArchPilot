package com.ilia.gikashvili.archmain.project.repository;

import com.ilia.gikashvili.archmain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Find all projects where the user is a member.
     */
    @Query("SELECT p FROM Project p WHERE p.id IN " +
           "(SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :userId) " +
           "AND p.archived = false")
    List<Project> findAccessibleByUserId(@Param("userId") UUID userId);

    List<Project> findAllByOwnerId(UUID ownerId);
}
