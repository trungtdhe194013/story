package org.com.story.repository;

import org.com.story.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(s) FROM User u JOIN u.followedStories s WHERE u.id = :userId")
    int countFollowedStories(@Param("userId") Long userId);

    /** Đếm số follower của một story */
    @Query("SELECT COUNT(u) FROM User u JOIN u.followedStories s WHERE s.id = :storyId")
    long countFollowersByStoryId(@Param("storyId") Long storyId);

    /** Lấy danh sách follower của một story — để gửi notification */
    @Query("SELECT u FROM User u JOIN u.followedStories s WHERE s.id = :storyId")
    List<User> findFollowersByStoryId(@Param("storyId") Long storyId);

    /** Lấy tất cả user có role cụ thể — dùng cho broadcast notification */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findAllByRoleName(@Param("roleName") String roleName);

    /** Đếm user mới đăng ký sau một thời điểm */
    long countByCreatedAtAfter(java.time.LocalDateTime since);
}


