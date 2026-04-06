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

    /** Đếm số user KHÁC NHAU đã đọc ít nhất 1 chương kể từ mốc thời gian — dùng cho DAU/MAU */
    @Query("SELECT COUNT(DISTINCT rh.user.id) FROM ReadingHistory rh WHERE rh.lastReadAt >= :since")
    long countDistinctUsersReadingSince(@Param("since") LocalDateTime since);
}




