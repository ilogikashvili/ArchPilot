package com.ilia.gikashvili.archmain.teamchat;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="team_chat_message",indexes=@Index(columnList="projectId,createdAt")) public class ChatMessage {
 public enum AuthorType { USER, API_AGENT, AI_AGENT, SYSTEM }
 @Id public UUID id=UUID.randomUUID();
 @Column(nullable=false) public UUID projectId;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public AuthorType authorType;
 public String authorId; public String authorName;
 @Column(nullable=false,columnDefinition="TEXT") public String content;
 public UUID discussionId;
 public Instant createdAt=Instant.now();
}
