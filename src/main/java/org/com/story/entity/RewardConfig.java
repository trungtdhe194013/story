package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reward_configs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RewardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mốc streak ngày (1, 3, 7, 14, 30, 100) */
    @Column(nullable = false, unique = true)
    private Integer streakDay;

    /** Số coin thưởng khi đạt mốc */
    @Column(nullable = false)
    private Long rewardCoin;

    private String description;
}

