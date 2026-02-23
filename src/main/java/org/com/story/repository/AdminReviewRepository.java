package org.com.story.repository;

import org.com.story.entity.AdminReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReviewRepository extends JpaRepository<AdminReview, Long> {
    List<AdminReview> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
