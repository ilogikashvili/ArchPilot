package com.ilia.gikashvili.archmain.ai;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AiProviderKeyRepository extends JpaRepository<AiProviderKey,UUID> { List<AiProviderKey> findByUserIdOrderByCreatedAtDesc(UUID userId); Optional<AiProviderKey> findByIdAndUserId(UUID id,UUID userId); }
