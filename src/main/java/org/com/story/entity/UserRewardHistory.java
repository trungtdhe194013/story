package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_reward_histories")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserRewardHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_config_id", nullable = false)
    private RewardConfig rewardConfig;

    private Long coinReceived;

    private Integer streakDayAtClaim;

    @CreationTimestamp
    private LocalDateTime claimedAt;
}

