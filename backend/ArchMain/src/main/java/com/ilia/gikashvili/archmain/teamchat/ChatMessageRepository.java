package com.ilia.gikashvili.archmain.teamchat;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ChatMessageRepository extends JpaRepository<ChatMessage,UUID> { List<ChatMessage> findTop100ByProjectIdOrderByCreatedAtDesc(UUID projectId); List<ChatMessage> findTop30ByProjectIdOrderByCreatedAtDesc(UUID projectId); }
