package org.com.story.repository;

import org.com.story.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Lọc theo trạng thái (Dùng Pageable để phân trang nếu danh sách quá dài)
    List<Report> findByStatus(String status);

    long countByStatus(String status);

    // Lấy lịch sử báo cáo của một người dùng
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    // Lấy tất cả báo cáo mới nhất lên đầu
    List<Report> findAllByOrderByCreatedAtDesc();

    // Kiểm tra user đã báo cáo nội dung này chưa (chống spam)
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, String targetType, Long targetId);

    // Lọc báo cáo theo loại nội dung
    List<Report> findByTargetTypeOrderByCreatedAtDesc(String targetType);

    // Lấy tất cả báo cáo của 1 nội dung cụ thể
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    /**
     * Đếm tổng số báo cáo của một nội dung cụ thể dựa trên trạng thái
     */
    long countByTargetTypeAndTargetIdAndStatus(String targetType, Long targetId, String status);

    /**
     * Đếm số người dùng DUY NHẤT đã báo cáo 1 nội dung.
     * Dùng để tránh việc 1 người dùng clone acc hoặc spam report 1 nội dung nhiều lần.
     */
    @Query("SELECT COUNT(DISTINCT r.reporter.id) FROM Report r " +
            "WHERE r.targetType = :targetType AND r.targetId = :targetId")
    long countDistinctReportersByTarget(@Param("targetType") String targetType,
                                        @Param("targetId") Long targetId);

    /**
     * Kiểm tra mức độ tái phạm của User.
     * Đếm các báo cáo ĐÃ XỬ LÝ (RESOLVED) nhắm vào các nội dung của User này.
     * Lưu ý: Query này giả định bảng Report có cột targetOwnerId (chủ sở hữu nội dung bị report)
     */
    @Query("SELECT COUNT(r) FROM Report r " +
            "WHERE r.targetOwnerId = :userId " +
            "AND r.status = 'RESOLVED' " +
            "AND r.resolvedAction IN ('HIDE_CONTENT', 'DELETE_CONTENT', 'HIDE_AND_BAN', 'DELETE_AND_BAN')")
    long countResolvedViolationsByUser(@Param("userId") Long userId);

    /**
     * Thống kê Top 10 nội dung bị báo cáo nhiều nhất (chưa xử lý)
     * Giúp Admin ưu tiên xử lý "điểm nóng".
     */
    @Query("SELECT r.targetType, r.targetId, COUNT(r) as reportCount " +
            "FROM Report r WHERE r.status = 'PENDING' " +
            "GROUP BY r.targetType, r.targetId " +
            "ORDER BY reportCount DESC")
    List<Object[]> findTopReportedContent(Pageable pageable);
}