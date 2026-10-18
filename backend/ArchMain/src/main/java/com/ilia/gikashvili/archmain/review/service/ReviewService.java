package com.ilia.gikashvili.archmain.review.service;

import com.ilia.gikashvili.archmain.access.policy.Permission;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.architecture.service.ArchitectureService;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.review.dto.StartReviewRequest;
import com.ilia.gikashvili.archmain.review.engine.RuleBasedReviewEngine;
import com.ilia.gikashvili.archmain.review.entity.AiReview;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import com.ilia.gikashvili.archmain.review.repository.AiReviewRepository;
import com.ilia.gikashvili.archmain.review.repository.ReviewFindingRepository;
import com.ilia.gikashvili.archmain.review.state.ReviewStateMachine;
import com.ilia.gikashvili.archmain.review.state.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final AiReviewRepository reviewRepository;
    private final ReviewFindingRepository findingRepository;
    private final ProjectAccessService accessService;
    private final ArchitectureService architectureService;
    private final RuleBasedReviewEngine reviewEngine;
    private final ReviewStateMachine stateMachine;
    private final com.ilia.gikashvili.archmain.review.mapper.ReviewMapper reviewMapper;
    @org.springframework.beans.factory.annotation.Autowired
    private SeniorAiReviewer seniorAiReviewer;

    public ReviewService(AiReviewRepository reviewRepository,
                         ReviewFindingRepository findingRepository,
                         ProjectAccessService accessService,
                         ArchitectureService architectureService,
                         RuleBasedReviewEngine reviewEngine,
                         ReviewStateMachine stateMachine,
                         com.ilia.gikashvili.archmain.review.mapper.ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.findingRepository = findingRepository;
        this.accessService = accessService;
        this.architectureService = architectureService;
        this.reviewEngine = reviewEngine;
        this.stateMachine = stateMachine;
        this.reviewMapper = reviewMapper;
    }

    /**
     * Start a new automated architecture review.
     */
    @Transactional
    public AiReview startReview(UUID projectId, UUID userId, StartReviewRequest request) {
        accessService.require(projectId, userId, Permission.REVIEW);

        ArchitectureDocument document = architectureService.getDocument(projectId, userId);

        AiReview review = new AiReview();
        review.setProjectId(projectId);
        review.setRequestedBy(userId);
        review.setStatus(ReviewStatus.QUEUED);
        review = reviewRepository.save(review);

        // Transition to RUNNING
        stateMachine.requireTransition(review.getStatus(), ReviewStatus.RUNNING);
        review.setStatus(ReviewStatus.RUNNING);
        review.setStartedAt(Instant.now());

        // Run review engine
        List<ReviewFinding> findings = reviewEngine.review(review.getId(), projectId, document);
        findings = new java.util.ArrayList<>(findings);
        if (request != null && request.aiKeyId() != null) {
            findings.addAll(seniorAiReviewer.review(review.getId(), projectId, userId, request.aiKeyId(), document, findings));
        }
        findingRepository.saveAll(findings);

        // Transition to COMPLETED
        stateMachine.requireTransition(review.getStatus(), ReviewStatus.COMPLETED);
        review.setStatus(ReviewStatus.COMPLETED);
        review.setCompletedAt(Instant.now());
        review.setFindingCount(findings.size());
        review.setSummary(verdict(findings));

        return reviewRepository.save(review);
    }

    /**
     * List all reviews for a project.
     */
    @Transactional(readOnly = true)
    public List<AiReview> listReviews(UUID projectId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        return reviewRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
    }

    /**
     * Get review details by ID.
     */
    @Transactional(readOnly = true)
    public AiReview getReview(UUID projectId, UUID reviewId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        AiReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));
        if (!review.getProjectId().equals(projectId)) {
            throw new NotFoundException("Review", reviewId);
        }
        return review;
    }

    /**
     * List findings produced by a specific review.
     */
    @Transactional(readOnly = true)
    public List<ReviewFinding> listFindings(UUID projectId, UUID reviewId, UUID userId) {
        getReview(projectId, reviewId, userId);
        return findingRepository.findAllByReviewId(reviewId);
    }

    /**
     * Get an individual finding by ID.
     */
    @Transactional(readOnly = true)
    public ReviewFinding getFinding(UUID projectId, UUID reviewId, UUID findingId, UUID userId) {
        getReview(projectId, reviewId, userId);
        ReviewFinding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new NotFoundException("Finding", findingId));
        if (!finding.getReviewId().equals(reviewId) || !finding.getProjectId().equals(projectId)) {
            throw new NotFoundException("Finding", findingId);
        }
        return finding;
    }

    /**
     * Cancel an ongoing review.
     */
    @Transactional
    public AiReview cancelReview(UUID projectId, UUID reviewId, UUID userId) {
        accessService.require(projectId, userId, Permission.REVIEW);
        AiReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review", reviewId));
        if (!review.getProjectId().equals(projectId)) {
            throw new NotFoundException("Review", reviewId);
        }

        stateMachine.requireTransition(review.getStatus(), ReviewStatus.CANCELLED);
        review.setStatus(ReviewStatus.CANCELLED);
        return reviewRepository.save(review);
    }

    /**
     * Get a finding by project and finding ID.
     */
    @Transactional(readOnly = true)
    public ReviewFinding getFindingById(UUID projectId, UUID findingId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);
        return findingRepository.findByIdAndProjectId(findingId, projectId)
                .orElseThrow(() -> new NotFoundException("Finding", findingId));
    }

    /**
     * Compare findings between two review runs of the same project.
     */
    @Transactional(readOnly = true)
    public com.ilia.gikashvili.archmain.review.dto.ReviewComparisonResponse compareReviews(
            UUID projectId, UUID baseReviewId, UUID targetReviewId, UUID userId) {
        accessService.require(projectId, userId, Permission.READ);

        List<ReviewFinding> baseFindings = listFindings(projectId, baseReviewId, userId);
        List<ReviewFinding> targetFindings = listFindings(projectId, targetReviewId, userId);

        java.util.Set<String> baseSignatures = baseFindings.stream()
                .map(this::findingSignature)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> targetSignatures = targetFindings.stream()
                .map(this::findingSignature)
                .collect(java.util.stream.Collectors.toSet());

        List<com.ilia.gikashvili.archmain.review.dto.FindingResponse> newFindings = targetFindings.stream()
                .filter(f -> !baseSignatures.contains(findingSignature(f)))
                .map(reviewMapper::toFindingResponse)
                .toList();

        List<com.ilia.gikashvili.archmain.review.dto.FindingResponse> resolvedFindings = baseFindings.stream()
                .filter(f -> !targetSignatures.contains(findingSignature(f)))
                .map(reviewMapper::toFindingResponse)
                .toList();

        List<com.ilia.gikashvili.archmain.review.dto.FindingResponse> unchangedFindings = targetFindings.stream()
                .filter(f -> baseSignatures.contains(findingSignature(f)))
                .map(reviewMapper::toFindingResponse)
                .toList();

        return new com.ilia.gikashvili.archmain.review.dto.ReviewComparisonResponse(
                baseReviewId,
                targetReviewId,
                newFindings.size(),
                resolvedFindings.size(),
                unchangedFindings.size(),
                newFindings,
                resolvedFindings,
                unchangedFindings
        );
    }

    /**
     * Generate structured analytics report for a review.
     */
    @Transactional(readOnly = true)
    public com.ilia.gikashvili.archmain.review.dto.ReviewReportResponse generateReport(
            UUID projectId, UUID reviewId, UUID userId) {
        AiReview review = getReview(projectId, reviewId, userId);
        List<ReviewFinding> findings = listFindings(projectId, reviewId, userId);

        java.util.Map<String, Long> bySeverity = findings.stream()
                .collect(java.util.stream.Collectors.groupingBy(ReviewFinding::getSeverity, java.util.stream.Collectors.counting()));

        java.util.Map<String, Long> byCategory = findings.stream()
                .collect(java.util.stream.Collectors.groupingBy(ReviewFinding::getCategory, java.util.stream.Collectors.counting()));

        List<com.ilia.gikashvili.archmain.review.dto.FindingResponse> critical = findings.stream()
                .filter(f -> "CRITICAL".equalsIgnoreCase(f.getSeverity()) || "WARNING".equalsIgnoreCase(f.getSeverity()))
                .map(reviewMapper::toFindingResponse)
                .toList();

        List<String> keyRecs = findings.stream()
                .map(ReviewFinding::getRecommendation)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        return new com.ilia.gikashvili.archmain.review.dto.ReviewReportResponse(
                review.getId(),
                projectId,
                java.time.Instant.now(),
                review.getStatus().name(),
                findings.size(),
                bySeverity,
                byCategory,
                critical,
                keyRecs
        );
    }

    private String findingSignature(ReviewFinding f) {
        return (f.getCategory() != null ? f.getCategory() : "") + "|" +
               (f.getTitle() != null ? f.getTitle() : "") + "|" +
               (f.getAffectedElementId() != null ? f.getAffectedElementId() : "");
    }

    /** Senior-reviewer style verdict, like a pull request review. */
    static String verdict(List<ReviewFinding> findings) {
        long critical = findings.stream().filter(f -> "CRITICAL".equalsIgnoreCase(f.getSeverity())).count();
        long warning = findings.stream().filter(f -> "WARNING".equalsIgnoreCase(f.getSeverity())).count();
        long info = findings.size() - critical - warning;
        String counts = critical + " critical, " + warning + " warnings, " + info + " suggestions.";
        if (critical > 0) return "Changes requested: this design would not pass a senior review. " + counts;
        if (warning > 0) return "Approve with changes: workable, but fix the warnings before production. " + counts;
        return "Approved: no blocking issues found. " + counts;
    }
}
