package org.com.story.repository;

import org.com.story.entity.RewardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardConfigRepository extends JpaRepository<RewardConfig, Long> {
    Optional<RewardConfig> findByStreakDay(Integer streakDay);
    List<RewardConfig> findAllByOrderByStreakDayAsc();
    List<RewardConfig> findByStreakDayLessThanEqualOrderByStreakDayDesc(Integer streakDay);
}

