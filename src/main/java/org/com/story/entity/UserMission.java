package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_missions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "mission_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission;

    private Boolean completed = false;

    /** Tiến độ hiện tại (ví dụ: đọc 3/5 chương) */
    private Integer progress = 0;

    private LocalDateTime completedAt;

    private LocalDateTime lastResetAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
