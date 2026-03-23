package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lịch sử một lần duyệt (approve/reject) của Reviewer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewHistoryResponse {

    private Long id;

    /** ID của reviewer đã thực hiện hành động */
    private Long reviewerId;

    /** Tên reviewer */
    private String reviewerName;

    /** STORY hoặc CHAPTER */
    private String targetType;

    /** ID của story/chapter được duyệt */
    private Long targetId;

    /** Tiêu đề story hoặc chapter được duyệt */
    private String targetTitle;

    /**
     * Tên truyện — chỉ có giá trị khi targetType = CHAPTER
     * (để biết chapter thuộc truyện nào)
     */
    private String storyTitle;

    /** APPROVE hoặc REJECT */
    private String action;

    /** Lý do từ chối (chỉ có khi action = REJECT) */
    private String note;

    /** Trạng thái hiện tại của story/chapter sau khi duyệt */
    private String currentStatus;

    private LocalDateTime createdAt;
}

