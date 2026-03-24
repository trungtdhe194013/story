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
@Table(name = "withdraw_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class WithdrawRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private Long amount;    // số coin muốn rút
    private String status; // PENDING, APPROVED, REJECTED

    /**
     * Số tiền VND admin cần chuyển thực tế.
     * Tính khi tạo request: vndAmount = amount * coinToVndRate
     * Null nếu là yêu cầu cũ (trước khi có field này).
     */
    private Long vndAmount;

    private String bankName;
    private String bankAccount;
    private String bankOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private LocalDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectedReason;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
