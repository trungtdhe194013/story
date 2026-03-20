package org.com.story.repository;

import org.com.story.entity.ReadingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
    Optional<ReadingHistory> findByUserIdAndStoryId(Long userId, Long storyId);
    List<ReadingHistory> findByUserIdOrderByLastReadAtDesc(Long userId);
}

