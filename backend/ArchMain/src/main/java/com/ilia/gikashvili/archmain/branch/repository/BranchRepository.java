package com.ilia.gikashvili.archmain.branch.repository;
import com.ilia.gikashvili.archmain.branch.entity.Branch;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface BranchRepository extends JpaRepository<Branch,UUID> {
 List<Branch> findAllByProjectId(UUID projectId);
 boolean existsByProjectIdAndName(UUID projectId,String name);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select b from Branch b where b.id = :id") Optional<Branch> findLockedById(UUID id);
}
