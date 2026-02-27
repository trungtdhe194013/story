package org.com.story.repository;

import org.com.story.entity.ChapterVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterVersionRepository extends JpaRepository<ChapterVersion, Long> {
    List<ChapterVersion> findByChapterIdOrderByVersionDesc(Long chapterId);
    long countByChapterId(Long chapterId);
}

