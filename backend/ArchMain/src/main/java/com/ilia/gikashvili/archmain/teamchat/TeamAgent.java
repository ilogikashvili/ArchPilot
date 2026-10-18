package com.ilia.gikashvili.archmain.teamchat;
import jakarta.persistence.*; import com.fasterxml.jackson.annotation.JsonIgnore; import java.time.Instant; import java.util.UUID;
/** An AI teammate that lives in a project's team chat and answers when mentioned or during discussions. */
@Entity @Table(name="team_agent") public class TeamAgent {
 @Id public UUID id=UUID.randomUUID();
 @Column(nullable=false) public UUID projectId;
 @Column(nullable=false) public String name;
 /** Mention handle, e.g. "Security" for @Security. Unique per project. */
 @Column(nullable=false) public String handle;
 public String role;
 @Column(columnDefinition="TEXT") public String persona;
 /** Saved AI key (any provider, incl. Ollama) belonging to ownerUserId. */
 @Column(nullable=false) public UUID keyId;
 @JsonIgnore @Column(nullable=false) public UUID ownerUserId;
 public String ownerName; public String providerLabel; public String color;
 /** When true the agent also answers human messages that mention nobody. */
 public boolean autoReply;
 public Instant createdAt=Instant.now();
}
