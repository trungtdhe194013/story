package org.com.story.repository;

import org.com.story.entity.UserMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    List<UserMission> findByUserId(Long userId);
    Optional<UserMission> findByUserIdAndMissionId(Long userId, Long missionId);
    boolean existsByUserIdAndMissionIdAndCompletedTrue(Long userId, Long missionId);
}

