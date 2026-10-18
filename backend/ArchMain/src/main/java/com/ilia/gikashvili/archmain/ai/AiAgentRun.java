package com.ilia.gikashvili.archmain.ai;
import jakarta.persistence.*; import java.util.*; import java.time.Instant; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity public class AiAgentRun {
 @Id public UUID id=UUID.randomUUID(); public UUID projectId; public UUID userId; public UUID keyId;
 @Column(columnDefinition="TEXT") public String prompt;
 public String status; @Column(columnDefinition="TEXT") public String summary;
 @JsonIgnore @Column(columnDefinition="TEXT") public String operationsJson="[]";
 @Column(columnDefinition="TEXT") public String error; public Instant createdAt=Instant.now();
}
