package com.ilia.gikashvili.archmain.review.controller;

import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.review.dto.FindingResponse;
import com.ilia.gikashvili.archmain.review.dto.ReviewResponse;
import com.ilia.gikashvili.archmain.review.dto.ReviewSummary;
import com.ilia.gikashvili.archmain.review.dto.StartReviewRequest;
import com.ilia.gikashvili.archmain.review.mapper.ReviewMapper;
import com.ilia.gikashvili.archmain.review.service.ReviewService;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;

    public ReviewController(ReviewService reviewService, ReviewMapper reviewMapper) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
    }

    /**
     * POST /projects/{projectId}/reviews — Start a new architecture review.
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> startReview(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @RequestBody(required = false) StartReviewRequest request) {
        var review = reviewService.startReview(
                projectId,
                currentUser.id(),
                request != null ? request : new StartReviewRequest("RULE_BASED", null)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewMapper.toResponse(review));
    }

    /**
     * GET /projects/{projectId}/reviews — List previous reviews for a project.
     */
    @GetMapping
    public ResponseEntity<List<ReviewSummary>> listReviews(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var reviews = reviewService.listReviews(projectId, currentUser.id());
        var response = reviews.stream()
                .map(reviewMapper::toSummary)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /projects/{projectId}/reviews/{reviewId} — Get review status and summary.
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID reviewId) {
        var review = reviewService.getReview(projectId, reviewId, currentUser.id());
        return ResponseEntity.ok(reviewMapper.toResponse(review));
    }

    /**
     * GET /projects/{projectId}/reviews/{reviewId}/findings — List findings from a review.
     */
    @GetMapping("/{reviewId}/findings")
    public ResponseEntity<List<FindingResponse>> listFindings(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID reviewId) {
        var findings = reviewService.listFindings(projectId, reviewId, currentUser.id());
        var response = findings.stream()
                .map(reviewMapper::toFindingResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /projects/{projectId}/reviews/{reviewId}/findings/{findingId} — Get one finding.
     */
    @GetMapping("/{reviewId}/findings/{findingId}")
    public ResponseEntity<FindingResponse> getFinding(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID reviewId,
            @PathVariable UUID findingId) {
        var finding = reviewService.getFinding(projectId, reviewId, findingId, currentUser.id());
        return ResponseEntity.ok(reviewMapper.toFindingResponse(finding));
    }

    /**
     * POST /projects/{projectId}/reviews/{reviewId}/cancel — Cancel a running review.
     */
    @PostMapping("/{reviewId}/cancel")
    public ResponseEntity<ReviewResponse> cancelReview(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID reviewId) {
        var review = reviewService.cancelReview(projectId, reviewId, currentUser.id());
        return ResponseEntity.ok(reviewMapper.toResponse(review));
    }

    /**
     * POST /projects/{projectId}/reviews/compare — Compare reviews of different revisions.
     */
    @PostMapping("/compare")
    public ResponseEntity<com.ilia.gikashvili.archmain.review.dto.ReviewComparisonResponse> compareReviews(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @jakarta.validation.Valid @RequestBody com.ilia.gikashvili.archmain.review.dto.CompareReviewsRequest request) {
        var comparison = reviewService.compareReviews(projectId, request.baseReviewId(), request.targetReviewId(), currentUser.id());
        return ResponseEntity.ok(comparison);
    }

    /**
     * GET /projects/{projectId}/reviews/{reviewId}/report — Generate or retrieve a review report.
     */
    @GetMapping("/{reviewId}/report")
    public ResponseEntity<com.ilia.gikashvili.archmain.review.dto.ReviewReportResponse> getReport(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID reviewId) {
        var report = reviewService.generateReport(projectId, reviewId, currentUser.id());
        return ResponseEntity.ok(report);
    }
}
