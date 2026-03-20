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

@Entity
@Table(name = "chapters", indexes = {
        @Index(name = "idx_chapter_story", columnList = "story_id"),
        @Index(name = "idx_chapter_status", columnList = "status"),
        @Index(name = "idx_chapter_order", columnList = "chapter_order")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id")
    private Story story;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "chapter_order")
    private Integer chapterOrder;
    private Integer coinPrice;

    /** DRAFT, PENDING, APPROVED, PUBLISHED, REJECTED, HIDDEN */
    private String status;

    /** Lý do từ chối (reviewer ghi khi REJECT) */
    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    /** Ghi chú từ Reviewer khi từ chối chapter */
    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id")
    private User editor;

    private Long viewCount = 0L;

    private Integer wordCount = 0;

    private LocalDateTime publishAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
