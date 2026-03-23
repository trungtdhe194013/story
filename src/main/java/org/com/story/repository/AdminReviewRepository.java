package org.com.story.repository;

import org.com.story.entity.AdminReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminReviewRepository extends JpaRepository<AdminReview, Long> {

    /** Lịch sử duyệt của 1 story/chapter cụ thể (ai duyệt, khi nào, kết quả gì) */
    List<AdminReview> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    /** Toàn bộ lịch sử duyệt của một reviewer cụ thể, mới nhất lên đầu */
    List<AdminReview> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    /** Lịch sử duyệt của reviewer theo loại (STORY hoặc CHAPTER) */
    List<AdminReview> findByAdminIdAndTargetTypeOrderByCreatedAtDesc(Long adminId, String targetType);

    /** Tất cả lịch sử duyệt (admin xem toàn bộ), mới nhất lên đầu */
    List<AdminReview> findAllByOrderByCreatedAtDesc();
}
