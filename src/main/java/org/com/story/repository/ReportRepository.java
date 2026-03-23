package org.com.story.repository;

import org.com.story.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(String status);
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<Report> findAllByOrderByCreatedAtDesc();

    /** Filter theo loại nội dung bị báo cáo */
    List<Report> findByTargetTypeOrderByCreatedAtDesc(String targetType);

    /** Filter theo trạng thái + loại nội dung */
    List<Report> findByStatusAndTargetTypeOrderByCreatedAtDesc(String status, String targetType);

    /** Lịch sử báo cáo của 1 nội dung cụ thể (ví dụ: story này đã bị báo bao nhiêu lần) */
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    /** Kiểm tra user đã báo cáo nội dung này chưa (chống spam báo cáo) */
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, String targetType, Long targetId);

    /** Đếm số report PENDING của 1 nội dung (dùng để tự động flag) */
    long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

    /**
     * Đếm số người dùng KHÁC NHAU đã báo cáo 1 nội dung
     * (dùng để auto-hide comment khi nhận đủ ngưỡng báo cáo)
     */
    @Query("SELECT COUNT(DISTINCT r.reporter.id) FROM Report r " +
           "WHERE r.targetType = :targetType AND r.targetId = :targetId")
    long countDistinctReportersByTarget(@Param("targetType") String targetType,
                                        @Param("targetId") Long targetId);

    /**
     * Đếm số lần comment của user đã bị xử lý (để phát hiện tái phạm)
     */
    @Query("SELECT COUNT(r) FROM Report r " +
           "WHERE r.targetType = 'COMMENT' AND r.targetId IN " +
           "  (SELECT c.id FROM Comment c WHERE c.user.id = :userId) " +
           "AND r.status = 'RESOLVED' " +
           "AND r.resolvedAction IN ('HIDE_CONTENT','DELETE_CONTENT','HIDE_AND_BAN','DELETE_AND_BAN')")
    long countResolvedCommentViolationsByUser(@Param("userId") Long userId);
}
