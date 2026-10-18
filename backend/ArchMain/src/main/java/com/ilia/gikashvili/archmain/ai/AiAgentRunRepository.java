package com.ilia.gikashvili.archmain.ai;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AiAgentRunRepository extends JpaRepository<AiAgentRun,UUID> { List<AiAgentRun> findTop50ByProjectIdOrderByCreatedAtDesc(UUID projectId); }
