package com.ilia.gikashvili.archmain.agent.repository;

import com.ilia.gikashvili.archmain.agent.entity.AgentToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentTokenRepository extends JpaRepository<AgentToken, UUID> {
    Optional<AgentToken> findByTokenHash(String tokenHash);
    List<AgentToken> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<AgentToken> findByAgentUserId(UUID agentUserId);
}
