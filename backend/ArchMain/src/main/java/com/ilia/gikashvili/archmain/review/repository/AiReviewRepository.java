package com.ilia.gikashvili.archmain.review.repository;

import com.ilia.gikashvili.archmain.review.entity.AiReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiReviewRepository extends JpaRepository<AiReview, UUID> {

    List<AiReview> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
