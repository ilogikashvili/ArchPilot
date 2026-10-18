package com.ilia.gikashvili.archmain.settings.repository;
import java.util.*; import org.springframework.data.jpa.repository.*; import com.ilia.gikashvili.archmain.settings.entity.UserSession;
public interface UserSessionRepository extends JpaRepository<UserSession,UUID> { List<UserSession> findAllByUserId(UUID userId); Optional<UserSession> findByIdAndUserId(UUID id, UUID userId); }