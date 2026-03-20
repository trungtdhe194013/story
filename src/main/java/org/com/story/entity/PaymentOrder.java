package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Lưu mỗi đơn thanh toán PayOS — dùng để idempotent khi nhận IPN webhook
 */
@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_order_code", columnList = "order_code", unique = true),
        @Index(name = "idx_payment_user_id",   columnList = "user_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đơn hàng gửi lên PayOS (unique) — dùng để đối soát IPN */
    @Column(name = "order_code", unique = true, nullable = false)
    private Long orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tên enum CoinPackage (BASIC, SAVING, POPULAR, ADVANCED, VIP) */
    @Column(name = "package_id", nullable = false)
    private String packageId;

    /** Số tiền VND người dùng trả */
    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    /** Số coin sẽ được cộng sau khi thanh toán thành công */
    @Column(name = "coin_amount", nullable = false)
    private Long coinAmount;

    /** PENDING → PAID | CANCELLED */
    @Column(nullable = false)
    private String status;

    /** URL checkout trả về từ PayOS */
    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;

    /** Payment link ID từ PayOS */
    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Thời điểm thanh toán thành công (IPN confirmed) */
    private LocalDateTime paidAt;
}

