package org.com.story.repository;

import org.com.story.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUserId(Long userId);

    @Query("SELECT us FROM UserStreak us WHERE us.hasClaimedToday = true")
    List<UserStreak> findAllClaimedToday();
}

