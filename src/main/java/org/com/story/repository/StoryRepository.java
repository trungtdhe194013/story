package org.com.story.repository;

import org.com.story.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long>, JpaSpecificationExecutor<Story> {

    List<Story> findByAuthorId(Long authorId);

    /** Dùng cho admin/reviewer — bao gồm cả soft-deleted nếu cần */
    List<Story> findByStatus(String status);

    long countByStatus(String status);

    /** Reviewer queue: chỉ lấy PENDING và chưa bị soft-delete */
    @Query("SELECT s FROM Story s WHERE s.status = 'PENDING' AND s.isDeleted = false ORDER BY s.createdAt ASC")
    List<Story> findPendingForReview();

    /**
     * Trang chủ: story APPROVED, chưa bị soft-delete,
     * VÀ có ÍT NHẤT 1 chapter đã PUBLISHED.
     */
    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' AND s.isDeleted = false " +
            "AND EXISTS (SELECT c FROM Chapter c WHERE c.story = s AND c.status = 'PUBLISHED') " +
            "ORDER BY s.createdAt DESC")
    List<Story> findAllPublished();

    Page<Story> findAllPublishedWithFilters(
            @Param("categories") List<String> categories,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("year") Integer year,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Story s SET s.viewCount = COALESCE(s.viewCount, 0) + 1 WHERE s.id = :storyId")
    void incrementViewCount(@Param("storyId") Long storyId);

    /**
     * Tìm kiếm: tương tự trang chủ nhưng có keyword filter.
     */
    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' AND s.isDeleted = false " +
            "AND EXISTS (SELECT c FROM Chapter c WHERE c.story = s AND c.status = 'PUBLISHED') " +
            "AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY s.createdAt DESC")
    List<Story> searchPublished(@Param("keyword") String keyword);

    /** Tìm theo category */
    @Query("SELECT s FROM Story s JOIN s.categories c WHERE c.id = :categoryId " +
            "AND s.status = 'APPROVED' AND s.isDeleted = false " +
            "AND EXISTS (SELECT ch FROM Chapter ch WHERE ch.story = s AND ch.status = 'PUBLISHED') " +
            "ORDER BY s.createdAt DESC")
    List<Story> findByCategoryId(@Param("categoryId") Long categoryId);

    /** Top view */
    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' AND s.isDeleted = false " +
            "AND EXISTS (SELECT c FROM Chapter c WHERE c.story = s AND c.status = 'PUBLISHED') " +
            "ORDER BY s.viewCount DESC")
    List<Story> findTopByViewCount();

    /** Top rated */
    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' AND s.isDeleted = false " +
            "AND s.ratingCount > 0 " +
            "AND EXISTS (SELECT c FROM Chapter c WHERE c.story = s AND c.status = 'PUBLISHED') " +
            "ORDER BY s.avgRating DESC, s.ratingCount DESC")
    List<Story> findTopRated();

    /** Truyện hoàn thành */
    @Query("SELECT s FROM Story s WHERE s.status = 'APPROVED' AND s.isDeleted = false " +
            "AND s.isCompleted = true " +
            "AND EXISTS (SELECT c FROM Chapter c WHERE c.story = s AND c.status = 'PUBLISHED') " +
            "ORDER BY s.completedAt DESC")
    List<Story> findCompleted();
}
