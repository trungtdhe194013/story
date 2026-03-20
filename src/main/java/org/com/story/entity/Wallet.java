package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long balance = 0L;

    /**
     * Coin đang bị lock do EditRequest đang mở — không được tiêu.
     * Null-safe: luôn trả về 0 nếu DB row cũ chưa có giá trị.
     */
    @Column(name = "locked_balance", columnDefinition = "bigint default 0")
    @Getter(AccessLevel.NONE)
    private Long lockedBalance = 0L;

    public Long getLockedBalance() {
        return lockedBalance != null ? lockedBalance : 0L;
    }

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
