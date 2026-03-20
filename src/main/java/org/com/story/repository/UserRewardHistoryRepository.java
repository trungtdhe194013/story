package org.com.story.repository;

import org.com.story.entity.UserRewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRewardHistoryRepository extends JpaRepository<UserRewardHistory, Long> {
    List<UserRewardHistory> findByUserIdOrderByClaimedAtDesc(Long userId);
    boolean existsByUserIdAndRewardConfigId(Long userId, Long rewardConfigId);
}

