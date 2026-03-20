package org.com.story.repository;

import org.com.story.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndStoryId(Long userId, Long storyId);
    List<Rating> findByStoryIdOrderByCreatedAtDesc(Long storyId);
    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.story.id = :storyId")
    Double getAverageScoreByStoryId(@Param("storyId") Long storyId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.story.id = :storyId")
    Integer getCountByStoryId(@Param("storyId") Long storyId);
}

