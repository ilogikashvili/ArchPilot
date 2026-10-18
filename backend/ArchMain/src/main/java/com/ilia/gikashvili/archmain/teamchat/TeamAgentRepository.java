package com.ilia.gikashvili.archmain.teamchat;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TeamAgentRepository extends JpaRepository<TeamAgent,UUID> { List<TeamAgent> findByProjectIdOrderByCreatedAtAsc(UUID projectId); Optional<TeamAgent> findByIdAndProjectId(UUID id,UUID projectId); }
