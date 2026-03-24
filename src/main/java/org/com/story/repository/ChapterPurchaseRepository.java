package org.com.story.repository;

import org.com.story.entity.ChapterPurchase;
import org.com.story.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterPurchaseRepository extends JpaRepository<ChapterPurchase, Long> {
    boolean existsByUserIdAndChapterId(Long userId, Long chapterId);
    Optional<ChapterPurchase> findByUserIdAndChapterId(Long userId, Long chapterId);
    List<ChapterPurchase> findByUserId(Long userId);
    long countByUserId(Long userId);
    long countByChapterId(Long chapterId);

    /** Tổng coin đã tiêu qua mua chương */
    @Query("SELECT COALESCE(SUM(cp.pricePaid), 0) FROM ChapterPurchase cp")
    Long sumTotalCoinSpend();

    /** Tổng coin hoa hồng hệ thống đã thu */
    @Query("SELECT COALESCE(SUM(cp.commissionCoin), 0) FROM ChapterPurchase cp WHERE cp.commissionCoin IS NOT NULL")
    Long sumTotalSystemCommission();

    /**
     * Lấy danh sách người dùng KHÁC NHAU đã mua bất kỳ chương nào của một story.
     * Dùng để gửi thông báo khi story bị ẩn/gỡ bỏ.
     */
    @Query("SELECT DISTINCT cp.user FROM ChapterPurchase cp " +
           "JOIN cp.chapter c WHERE c.story.id = :storyId")
    List<User> findDistinctBuyersByStoryId(@Param("storyId") Long storyId);
}

