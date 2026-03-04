package org.com.story.repository;

import org.com.story.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(s) FROM User u JOIN u.followedStories s WHERE u.id = :userId")
    int countFollowedStories(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM User u JOIN u.purchasedChapters c WHERE u.id = :userId")
    int countPurchasedChapters(@Param("userId") Long userId);
}
