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
@Table(name = "reports")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User reporter;

    private String targetType; // STORY, CHAPTER, COMMENT
    private Long targetId;

    /** Phân loại: SPAM, COPYRIGHT, INAPPROPRIATE, VIOLENCE, OTHER */
    private String category;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private String status; // PENDING, RESOLVED

    private String resolvedAction; // WARN_ONLY, HIDE_CONTENT, DELETE_CONTENT, BAN_USER, HIDE_AND_BAN, DELETE_AND_BAN

    @Column(columnDefinition = "TEXT")
    private String adminNote; // ghi chú của admin khi xử lý

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    private LocalDateTime resolvedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
