package org.com.story.repository;

import org.com.story.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(s) FROM User u JOIN u.followedStories s WHERE u.id = :userId")
    int countFollowedStories(@Param("userId") Long userId);

    /** Dem so follower cua mot story */
    @Query("SELECT COUNT(u) FROM User u JOIN u.followedStories s WHERE s.id = :storyId")
    long countFollowersByStoryId(@Param("storyId") Long storyId);

    /** Lay danh sach follower cua mot story de gui notification */
    @Query("SELECT u FROM User u JOIN u.followedStories s WHERE s.id = :storyId")
    List<User> findFollowersByStoryId(@Param("storyId") Long storyId);

    /** Lay tat ca user co role cu the — dung cho broadcast notification */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findAllByRoleName(@Param("roleName") String roleName);

    /** New users registered after a given time — for newUsers7d metric */
    long countByCreatedAtAfter(LocalDateTime since);
}


