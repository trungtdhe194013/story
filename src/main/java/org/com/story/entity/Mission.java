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
    private String type; // DAILY, READ

    private Integer targetCount = 1;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isActive = true;

    private String icon;

    private Integer displayOrder = 0;
}
