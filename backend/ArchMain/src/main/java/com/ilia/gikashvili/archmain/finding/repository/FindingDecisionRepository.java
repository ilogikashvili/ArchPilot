package com.ilia.gikashvili.archmain.finding.repository;

import com.ilia.gikashvili.archmain.finding.entity.FindingDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindingDecisionRepository extends JpaRepository<FindingDecision, UUID> {

    Optional<FindingDecision> findByProjectIdAndFindingId(UUID projectId, UUID findingId);

    List<FindingDecision> findAllByProjectId(UUID projectId);

    void deleteByProjectIdAndFindingId(UUID projectId, UUID findingId);
}
