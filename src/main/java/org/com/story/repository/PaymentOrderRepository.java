package org.com.story.repository;

import org.com.story.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Tổng doanh thu VND từ các đơn thanh toán thành công */
    @Query("SELECT COALESCE(SUM(p.amountVnd), 0) FROM PaymentOrder p WHERE p.status = 'PAID'")
    Long sumRevenueVnd();

    /** Đếm số đơn thanh toán thành công */
    long countByStatus(String status);
}

