package org.com.story.repository;

import org.com.story.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Total revenue VND from paid orders */
    @Query("SELECT COALESCE(SUM(p.amountVnd), 0) FROM PaymentOrder p WHERE p.status = 'PAID'")
    Long sumRevenueVnd();

    /** Count orders by status */
    long countByStatus(String status);

    /** Count orders by status created after a given time — for payment error rate alerts */
    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.status = :status AND p.createdAt >= :since")
    long countByStatusSince(@Param("status") String status, @Param("since") LocalDateTime since);

    /** Count all orders created after a given time — denominator for error rate */
    @Query("SELECT COUNT(p) FROM PaymentOrder p WHERE p.createdAt >= :since")
    long countAllSince(@Param("since") LocalDateTime since);

    /** Revenue VND from PAID orders in last N days */
    @Query("SELECT COALESCE(SUM(p.amountVnd), 0) FROM PaymentOrder p WHERE p.status = 'PAID' AND p.createdAt >= :since")
    Long sumRevenueVndSince(@Param("since") LocalDateTime since);

    /** Count distinct users who made a PAID order since a date — for activity metrics */
    @Query("SELECT COUNT(DISTINCT p.user.id) FROM PaymentOrder p WHERE p.status = 'PAID' AND p.createdAt >= :since")
    long countDistinctPayingUsersSince(@Param("since") LocalDateTime since);
}
