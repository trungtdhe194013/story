package org.com.story.repository;

import org.com.story.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByOrderCode(Long orderCode);

    List<PaymentOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}

