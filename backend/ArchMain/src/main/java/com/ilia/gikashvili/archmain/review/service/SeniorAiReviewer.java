package com.ilia.gikashvili.archmain.review.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilia.gikashvili.archmain.ai.AiProviderClient;
import com.ilia.gikashvili.archmain.ai.AiProviderKeyRepository;
import com.ilia.gikashvili.archmain.architecture.model.ArchitectureDocument;
import com.ilia.gikashvili.archmain.common.exception.NotFoundException;
import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Asks the user's own AI provider to review the design as a demanding senior architect
 * reviewing a junior engineer's work. Provider failures never fail the review; they become an INFO finding.
 */
@Component
public class SeniorAiReviewer {

    static final String SYSTEM = """
            You are a principal/senior software architect with 15+ years of experience running design reviews.
            A junior engineer submitted the system design below. Review it critically and skeptically, the way you
            would block a pull request: assume it is going to production with real traffic, real money and real attackers.
            Hunt for misuse and design errors: wrong component for the job, layering violations, clients touching data stores,
            shared databases, distributed monoliths, synchronous chains and cascading failures, missing timeouts/retries/idempotency,
            consistency and transaction problems across services, single points of failure, scaling bottlenecks, missing auth,
            security holes, missing observability, chatty or cyclic dependencies, over-engineering for the scale, and unclear ownership.
            Be specific: name the exact components, explain the concrete failure scenario ("when X happens, Y breaks"),
            and give an actionable fix (name the pattern when relevant). Do not praise. Do not repeat issues already listed as known.
            Reply with JSON only, no markdown:
            {"findings":[{"severity":"CRITICAL|WARNING|INFO","category":"SECURITY|RESILIENCE|DATA_OWNERSHIP|COUPLING|SCALABILITY|PERFORMANCE|CONSISTENCY|OPERABILITY|DESIGN",
            "title":"short","description":"what is wrong and the concrete failure scenario","recommendation":"how to fix","affectedElementId":"node or edge id or null"}]}
            Return at most 10 findings, most severe first.""";

    private final AiProviderKeyRepository keys;
    private final AiProviderClient provider;
    private final ObjectMapper json;

    public SeniorAiReviewer(AiProviderKeyRepository keys, AiProviderClient provider, ObjectMapper json) {
        this.keys = keys; this.provider = provider; this.json = json;
    }

    public List<ReviewFinding> review(UUID reviewId, UUID projectId, UUID userId, UUID keyId,
                                      ArchitectureDocument doc, List<ReviewFinding> known) {
        var key = keys.findByIdAndUserId(keyId, userId).orElseThrow(() -> new NotFoundException("AI key", keyId));
        List<ReviewFinding> out = new ArrayList<>();
        try {
            String knownList = known.stream().map(ReviewFinding::getTitle).reduce("", (a, b) -> a + "\n- " + b);
            String prompt = "Design (JSON):\n" + json.writeValueAsString(Map.of("nodes", doc.nodes(), "edges", doc.edges()))
                    + "\n\nKnown issues already reported (do not repeat):" + (knownList.isBlank() ? " none" : knownList);
            String text = provider.call(key, SYSTEM, prompt, false).trim();
            int a = text.indexOf('{'), b = text.lastIndexOf('}');
            if (a < 0 || b <= a) throw new IllegalStateException("AI reply was not JSON");
            JsonNode findings = json.readTree(text.substring(a, b + 1)).path("findings");
            Set<String> ids = new HashSet<>();
            if (doc.nodes() != null) doc.nodes().forEach(n -> ids.add(n.id()));
            if (doc.edges() != null) doc.edges().forEach(e -> ids.add(e.id()));
            for (JsonNode n : findings) {
                if (out.size() >= 10) break;
                String sev = n.path("severity").asText("WARNING").toUpperCase();
                if (!Set.of("CRITICAL", "WARNING", "INFO").contains(sev)) sev = "WARNING";
                String el = n.path("affectedElementId").asText(null);
                ReviewFinding f = new ReviewFinding();
                f.setReviewId(reviewId); f.setProjectId(projectId); f.setSeverity(sev);
                f.setCategory(cut("AI · " + n.path("category").asText("DESIGN"), 200));
                f.setTitle(cut(n.path("title").asText("Design issue"), 500));
                f.setDescription(cut(n.path("description").asText(""), 4000));
                f.setRecommendation(cut(n.path("recommendation").asText(""), 4000));
                f.setAffectedElementId(el != null && ids.contains(el) ? el : null);
                out.add(f);
            }
        } catch (Exception e) {
            ReviewFinding f = new ReviewFinding();
            f.setReviewId(reviewId); f.setProjectId(projectId); f.setSeverity("INFO"); f.setCategory("AI · UNAVAILABLE");
            f.setTitle("AI senior reviewer could not run");
            f.setDescription(cut("The " + key.provider + " key returned an error: " + e.getMessage(), 4000));
            f.setRecommendation("Test the key in Settings → AI providers. Built-in senior checks above still ran.");
            out.add(f);
        }
        return out;
    }

    private static String cut(String s, int max) { return s == null ? null : s.length() <= max ? s : s.substring(0, max - 1) + "…"; }
}
