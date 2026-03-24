package org.com.story.repository;

import org.com.story.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByUserIdAndStoryId(Long userId, Long storyId);

    boolean existsByUserIdAndStoryId(Long userId, Long storyId);

    /** Danh sách bookmark của một user, sắp xếp mới nhất trước */
    List<Bookmark> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserIdAndStoryId(Long userId, Long storyId);
}

