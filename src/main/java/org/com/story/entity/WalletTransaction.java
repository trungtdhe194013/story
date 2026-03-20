package org.com.story.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import lombok.*;
import org.com.story.common.AuthProvider;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Long amount;

    private String type; // TOPUP, BUY, GIFT, REWARD, LOCKED, EDIT_REWARD_PAID, EDIT_REWARD_RECEIVED, EDIT_REFUND, WITHDRAW

    /** Loại tham chiếu: CHAPTER, GIFT, MISSION, EDIT_REQUEST, WITHDRAW ... */
    private String refType;

    private Long refId;

    /** Số dư ví sau giao dịch */
    private Long balanceAfter;

    /** Mô tả chi tiết giao dịch */
    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
