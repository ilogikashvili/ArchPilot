package com.ilia.gikashvili.archmain.ai;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*; import java.time.Instant;
@Entity public class AiProviderKey {
 public enum Provider { OPENAI, ANTHROPIC, GEMINI, OPENROUTER, OLLAMA }
 @Id public UUID id=UUID.randomUUID();
 @JsonIgnore @Column(nullable=false) public UUID userId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Provider provider;
 public String label; public String last4; public String model;
 /** Optional custom endpoint, e.g. http://localhost:11434 for Ollama. */
 public String baseUrl;
 @JsonIgnore @Column(nullable=false,columnDefinition="TEXT") public String encryptedKey;
 public Instant createdAt=Instant.now();
}
