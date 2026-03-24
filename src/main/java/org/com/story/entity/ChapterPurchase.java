package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_purchases", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "chapter_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChapterPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    /** Giá lúc mua (snapshot) */
    private Integer pricePaid;

    /** Số coin tác giả thực nhận sau khi trừ hoa hồng hệ thống */
    private Long authorShare;

    /** Số coin hệ thống thu (hoa hồng). commissionCoin + authorShare = pricePaid */
    private Long commissionCoin;

    @CreationTimestamp
    private LocalDateTime purchasedAt;
}

