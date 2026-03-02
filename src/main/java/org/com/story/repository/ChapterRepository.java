package org.com.story.repository;

import org.com.story.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByStoryIdOrderByChapterOrderAsc(Long storyId);

    List<Chapter> findByStatus(String status);

    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId AND c.status = 'PUBLISHED' ORDER BY c.chapterOrder ASC")
    List<Chapter> findPublishedByStoryId(Long storyId);

    long countByStoryId(Long storyId);
}
