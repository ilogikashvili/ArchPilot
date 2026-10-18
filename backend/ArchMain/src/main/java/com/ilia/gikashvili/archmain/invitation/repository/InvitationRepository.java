package com.ilia.gikashvili.archmain.invitation.repository;
import com.ilia.gikashvili.archmain.invitation.entity.Invitation;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface InvitationRepository extends JpaRepository<Invitation,UUID> {
 List<Invitation> findByEmailIgnoreCaseAndStatus(String email,Invitation.Status status);
 List<Invitation> findAllByProjectId(UUID projectId);
 boolean existsByProjectIdAndEmailAndStatus(UUID projectId,String email,Invitation.Status status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Invitation i where i.id = :id") Optional<Invitation> findLockedById(UUID id);
}
