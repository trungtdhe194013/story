package org.com.story.repository;

import org.com.story.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByChapterId(Long chapterId);
    List<Comment> findByChapterIdAndParentIsNull(Long chapterId);

    /** Story-level comments */
    List<Comment> findByStoryId(Long storyId);
    List<Comment> findByStoryIdAndParentIsNull(Long storyId);
}
