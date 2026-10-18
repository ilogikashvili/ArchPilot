package com.ilia.gikashvili.archmain.review.repository;

import com.ilia.gikashvili.archmain.review.entity.ReviewFinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewFindingRepository extends JpaRepository<ReviewFinding, UUID> {

    List<ReviewFinding> findAllByReviewId(UUID reviewId);

    List<ReviewFinding> findAllByProjectId(UUID projectId);

    Optional<ReviewFinding> findByIdAndProjectId(UUID id, UUID projectId);
}
