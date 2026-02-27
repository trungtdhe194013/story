package org.com.story.repository;

import org.com.story.entity.Gift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftRepository extends JpaRepository<Gift, Long> {
    List<Gift> findByFromUserIdOrderByCreatedAtDesc(Long userId);
    List<Gift> findByToUserIdOrderByCreatedAtDesc(Long userId);
    List<Gift> findByStoryIdOrderByCreatedAtDesc(Long storyId);
}

