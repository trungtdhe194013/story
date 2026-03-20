package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Đại diện cho 1 yêu cầu chỉnh sửa chương mà Author đặt ra có kèm thưởng coin.
 * Status flow: OPEN → IN_PROGRESS → SUBMITTED → APPROVED / (reject → IN_PROGRESS lại)
 *              OPEN → CANCELLED (author huỷ khi chưa có editor nhận)
 */
@Entity
@Table(name = "edit_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Chapter cần được chỉnh sửa */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    /** Tác giả đăng yêu cầu */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** Editor nhận việc (null khi chưa có ai nhận) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id")
    private User editor;

    /** Số coin thưởng nếu author approve — bị LOCK ngay khi tạo request */
    @Column(nullable = false)
    private Long coinReward;

    /** Mô tả author muốn editor làm gì */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Nội dung editor đã chỉnh sửa (chưa cập nhật vào chapter — chỉ cập nhật khi approve) */
    @Column(columnDefinition = "TEXT")
    private String editedContent;

    /** Ghi chú của editor khi nộp bản */
    @Column(columnDefinition = "TEXT")
    private String editorNote;

    /** Lý do từ chối của author (lần gần nhất) */
    @Column(columnDefinition = "TEXT")
    private String authorNote;

    /**
     * OPEN        — chờ editor nhận
     * IN_PROGRESS — editor đang làm
     * SUBMITTED   — editor đã nộp bản, chờ author duyệt
     * APPROVED    — author chấp thuận, coin chuyển cho editor
     * CANCELLED   — author huỷ (chỉ khi OPEN)
     */
    private String status;

    /** Số lần editor bị từ chối — không giới hạn, chỉ để hiển thị */
    @Builder.Default
    private Integer attemptCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

