package org.com.story.repository;

import org.com.story.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByStoryIdOrderByChapterOrderAsc(Long storyId);

    List<Chapter> findByStatus(String status);

    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId AND c.status = 'PUBLISHED' ORDER BY c.chapterOrder ASC")
    List<Chapter> findPublishedByStoryId(@Param("storyId") Long storyId);

    long countByStoryId(Long storyId);

    long countByStoryIdAndStatus(Long storyId, String status);

    /** Next chapter navigation */
    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId AND c.chapterOrder > :currentOrder AND c.status = 'PUBLISHED' ORDER BY c.chapterOrder ASC")
    List<Chapter> findNextChapter(@Param("storyId") Long storyId, @Param("currentOrder") Integer currentOrder);

    /** Prev chapter navigation */
    @Query("SELECT c FROM Chapter c WHERE c.story.id = :storyId AND c.chapterOrder < :currentOrder AND c.status = 'PUBLISHED' ORDER BY c.chapterOrder DESC")
    List<Chapter> findPrevChapter(@Param("storyId") Long storyId, @Param("currentOrder") Integer currentOrder);

    /** Chapters chưa có editor (dùng cho editor nhận việc) */
    @Query("SELECT c FROM Chapter c WHERE c.editor IS NULL AND c.status = 'DRAFT'")
    List<Chapter> findChaptersWithoutEditor();

    /** Chapters của một editor */
    List<Chapter> findByEditorId(Long editorId);

    /** Scheduled chapters cần auto-publish */
    @Query("SELECT c FROM Chapter c WHERE c.status = 'APPROVED' AND c.publishAt IS NOT NULL AND c.publishAt <= :now")
    List<Chapter> findScheduledForPublish(@Param("now") LocalDateTime now);
}
