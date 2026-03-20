package org.com.story.repository;

import org.com.story.entity.ChapterPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterPurchaseRepository extends JpaRepository<ChapterPurchase, Long> {
    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);
    Optional<ChapterPurchase> findByUserIdAndChapterId(Long userId, Long chapterId);
    List<ChapterPurchase> findByUserId(Long userId);
    long countByUserId(Long userId);
    long countByChapterId(Long chapterId);
}

