package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Loại thông báo:
     * NEW_CHAPTER, STORY_APPROVED, STORY_REJECTED, CHAPTER_APPROVED, CHAPTER_REJECTED,
     * GIFT_RECEIVED, SYSTEM
     */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** ID tham chiếu (story, chapter, gift...) */
    private Long refId;

    private String refType; // STORY, CHAPTER, GIFT

    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

