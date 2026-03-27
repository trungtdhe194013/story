package org.com.story.repository;

import org.com.story.entity.ReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Optional<ReadingHistory> findByUserIdAndStoryId(Long userId, Long storyId);
    List<ReadingHistory> findByUserIdOrderByLastReadAtDesc(Long userId);

    /** DAU: distinct users who read anything since a given time */
    @Query("SELECT COUNT(DISTINCT r.user.id) FROM ReadingHistory r WHERE r.lastReadAt >= :since")
    long countDistinctActiveUsersSince(@Param("since") LocalDateTime since);
}
