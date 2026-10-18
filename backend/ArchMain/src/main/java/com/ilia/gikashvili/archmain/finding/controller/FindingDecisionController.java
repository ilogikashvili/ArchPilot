package com.ilia.gikashvili.archmain.finding.controller;

import com.ilia.gikashvili.archmain.common.api.ApiVersion;
import com.ilia.gikashvili.archmain.finding.dto.DecisionRequest;
import com.ilia.gikashvili.archmain.finding.dto.DecisionResponse;
import com.ilia.gikashvili.archmain.finding.mapper.FindingDecisionMapper;
import com.ilia.gikashvili.archmain.finding.service.FindingDecisionService;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiVersion.V1 + "/projects/{projectId}/findings")
public class FindingDecisionController {

    private final FindingDecisionService decisionService;
    private final FindingDecisionMapper mapper;

    public FindingDecisionController(FindingDecisionService decisionService,
                                     FindingDecisionMapper mapper) {
        this.decisionService = decisionService;
        this.mapper = mapper;
    }

    /**
     * PUT /projects/{projectId}/findings/{findingId}/decision — Accept or dismiss a finding.
     */
    @PutMapping("/{findingId}/decision")
    public ResponseEntity<DecisionResponse> recordDecision(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID findingId,
            @Valid @RequestBody DecisionRequest request) {
        var decision = decisionService.recordDecision(projectId, findingId, currentUser.id(), request);
        return ResponseEntity.ok(mapper.toResponse(decision));
    }

    /**
     * GET /projects/{projectId}/findings/decisions — Get saved decisions for a project.
     */
    @GetMapping("/decisions")
    public ResponseEntity<List<DecisionResponse>> getDecisions(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId) {
        var decisions = decisionService.getDecisions(projectId, currentUser.id());
        var response = decisions.stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /projects/{projectId}/findings/{findingId}/decision — Remove a previous decision.
     */
    @DeleteMapping("/{findingId}/decision")
    public ResponseEntity<Void> deleteDecision(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID findingId) {
        decisionService.deleteDecision(projectId, findingId, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /projects/{projectId}/findings/{findingId}/apply — Request application of a recommendation.
     * Returns a preview patch rather than directly mutating the diagram.
     */
    @PostMapping("/{findingId}/apply")
    public ResponseEntity<com.ilia.gikashvili.archmain.finding.dto.RecommendationPreviewResponse> applyRecommendation(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID findingId) {
        var preview = decisionService.applyRecommendation(projectId, findingId, currentUser.id());
        return ResponseEntity.ok(preview);
    }

    /**
     * GET /projects/{projectId}/findings/{findingId}/history — View decision history.
     */
    @GetMapping("/{findingId}/history")
    public ResponseEntity<List<com.ilia.gikashvili.archmain.finding.dto.DecisionHistoryResponse>> getHistory(
            @CurrentUser AuthenticatedUser currentUser,
            @PathVariable UUID projectId,
            @PathVariable UUID findingId) {
        var history = decisionService.getDecisionHistory(projectId, findingId, currentUser.id());
        return ResponseEntity.ok(history);
    }
}
