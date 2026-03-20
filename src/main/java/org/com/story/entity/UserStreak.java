package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_streaks")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    /** Số ngày streak liên tiếp hiện tại */
    @Column(nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    /** Streak cao nhất từng đạt */
    @Column(nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    /** Ngày check-in gần nhất */
    private LocalDate lastCheckInDate;

    /** Đã nhận thưởng hôm nay chưa */
    @Column(nullable = false)
    @Builder.Default
    private Boolean hasClaimedToday = false;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

