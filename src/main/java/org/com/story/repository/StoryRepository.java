package org.com.story.repository;

import org.com.story.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByAuthorId(Long authorId);

    List<Story> findByStatus(String status);

    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' ORDER BY s.createdAt DESC")
    List<Story> findAllPublished();

    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' " +
           "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(s.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Story> searchPublished(@Param("keyword") String keyword);
}
