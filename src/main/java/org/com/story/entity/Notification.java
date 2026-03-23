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
     * NEW_CHAPTER       — Truyện đang follow có chương mới được publish
     * STORY_APPROVED    — Story của bạn vừa được Reviewer duyệt
     * STORY_REJECTED    — Story của bạn bị từ chối
     * CHAPTER_APPROVED  — Chapter được Reviewer APPROVE (Author tự publish sau)
     * CHAPTER_REJECTED  — Chapter bị từ chối kèm lý do
     * GIFT_RECEIVED     — Có người tặng quà cho truyện của bạn
     * NEW_FOLLOWER      — Có người mới follow truyện của bạn
     * MISSION_COMPLETED — Hoàn thành nhiệm vụ và nhận thưởng coin
     * STREAK_CHECKIN    — Check-in hàng ngày thành công (ngày bình thường)
     * STREAK_MILESTONE  — Đạt mốc streak đặc biệt (ngày 3, 7, 14, 30, 100)
     * SYSTEM            — Thông báo hệ thống từ Admin
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

