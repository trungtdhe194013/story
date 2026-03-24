package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "missions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long rewardCoin;

    /** DAILY: reset mỗi ngày | READ: reset tuần hoặc không reset */
    private String type;

    /**
     * Hành động kích hoạt nhiệm vụ:
     * LOGIN, READ_CHAPTER, COMMENT, FOLLOW_STORY, BUY_CHAPTER, SEND_GIFT
     */
    @Column(length = 50)
    private String action;

    /** Số lần phải thực hiện để hoàn thành */
    private Integer targetCount = 1;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isActive = true;

    private String icon;

    private Integer displayOrder = 0;
}
