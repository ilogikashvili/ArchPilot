package com.ilia.gikashvili.archmain.finding.service;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.finding.dto.DecisionRequest;
import com.ilia.gikashvili.archmain.finding.entity.FindingDecision;
import com.ilia.gikashvili.archmain.finding.repository.FindingDecisionRepository;
import com.ilia.gikashvili.archmain.review.service.ReviewService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FindingDecisionService {

    private final FindingDecisionRepository decisionRepository;
    private final ProjectAccessService accessService;
    private final ReviewService reviewService;
    private final RecommendationApplier recommendationApplier;

    public FindingDecisionService(FindingDecisionRepository decisionRepository,
                                  ProjectAccessService accessService,
                                  ReviewService reviewService,
                                  RecommendationApplier recommendationApplier) {
        this.decisionRepository = decisionRepository;
        this.accessService = accessService;
        this.reviewService = reviewService;
        this.recommendationApplier = recommendationApplier;
    }

    /**
     * Record or update an acceptance/dismissal decision on a finding.
     */
    @Transactional
    public FindingDecision recordDecision(UUID projectId, UUID findingId, UUID userId, DecisionRequest request) {
        accessService.require(projectId, userId, Permission.REVIEW);

        // Verify the finding exists and belongs to this project
        reviewService.getFindingById(projectId, findingId, userId);

        FindingDecision decision = decisionRepository.findByProjectIdAndFindingId(projectId, findingId)
                .orElseGet(() -> {
                    FindingDecision d = new FindingDecision();
                    d.setProjectId(projectId);
                    d.setFindingId(findingId);
                    return d;
                });

        decision.setDecidedBy(userId);
        decision.setDecision(request.decision());
        decision.setReason(request.reason());
        decision.setDecidedAt(Instant.now());

        return decisionRepository.save(decision);
    }

    /**
     * List all finding decisions recorded for a project.
     */
    @Transactional(readOnly = true)
    public List<FindingDecision> getDecisions(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        return decisionRepository.findAllByProjectId(projectId);
    }

    /**
     * Remove a decision on a finding.
     */
    @Transactional
    public void deleteDecision(UUID projectId, UUID findingId, UUID userId) {
        accessService.require(projectId, userId, Permission.REVIEW);
        decisionRepository.deleteByProjectIdAndFindingId(projectId, findingId);
    }

    /**
     * Generate an architectural recommendation preview patch.
     */
    @Transactional(readOnly = true)
    public com.ilia.gikashvili.archmain.finding.dto.RecommendationPreviewResponse applyRecommendation(
            UUID projectId, UUID findingId, UUID userId) {
        accessService.require(projectId, userId, Permission.REVIEW);
        var finding = reviewService.getFindingById(projectId, findingId, userId);
        return recommendationApplier.generatePreviewPatch(finding);
    }

    /**
     * Get decision history for a finding.
     */
    @Transactional(readOnly = true)
    public List<com.ilia.gikashvili.archmain.finding.dto.DecisionHistoryResponse> getDecisionHistory(
            UUID projectId, UUID findingId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        reviewService.getFindingById(projectId, findingId, userId);

        return decisionRepository.findByProjectIdAndFindingId(projectId, findingId)
                .map(d -> List.of(new com.ilia.gikashvili.archmain.finding.dto.DecisionHistoryResponse(
                        d.getId(),
                        d.getFindingId(),
                        d.getDecidedBy(),
                        d.getDecision(),
                        d.getReason(),
                        d.getDecidedAt()
                )))
                .orElse(List.of());
    }
}
