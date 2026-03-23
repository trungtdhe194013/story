package org.com.story.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import lombok.*;
import org.com.story.common.AuthProvider;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "stories", indexes = {
        @Index(name = "idx_story_author", columnList = "author_id"),
        @Index(name = "idx_story_status", columnList = "status"),
        @Index(name = "idx_story_view_count", columnList = "view_count")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String coverUrl;

    /** DRAFT, PENDING, APPROVED, REJECTED, REQUEST_EDIT */
    private String status;

    /** Lý do từ chối (reviewer ghi khi REJECT) */
    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "story_categories",
            joinColumns = @JoinColumn(name = "story_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Long likeCount = 0L;

    /** Điểm trung bình đánh giá (1-5) */
    @Builder.Default
    private Double avgRating = 0.0;

    /** Số lượt đánh giá */
    @Builder.Default
    private Integer ratingCount = 0;

    /** Đánh dấu truyện đã hoàn thành */
    @Column(name = "is_completed", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT false")
    @Builder.Default
    private Boolean isCompleted = false;

    private LocalDateTime completedAt;

    /** Soft-delete flag */
    @Column(name = "is_deleted", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT false")
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
