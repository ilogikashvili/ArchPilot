package com.ilia.gikashvili.archmain.finding.mapper;

import com.ilia.gikashvili.archmain.finding.dto.DecisionResponse;
import com.ilia.gikashvili.archmain.finding.entity.FindingDecision;
import org.springframework.stereotype.Component;

@Component
public class FindingDecisionMapper {

    public DecisionResponse toResponse(FindingDecision entity) {
        return new DecisionResponse(
                entity.getId(),
                entity.getProjectId(),
                entity.getFindingId(),
                entity.getDecidedBy(),
                entity.getDecision(),
                entity.getReason(),
                entity.getDecidedAt(),
                entity.getCreatedAt()
        );
    }
}
