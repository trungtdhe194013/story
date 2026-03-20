package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponse {
    private Long id;
    private Long storyId;
    private String storyTitle;
    private String title;
    private String content;
    private Integer chapterOrder;
    private Integer coinPrice;
    private String status;
    private LocalDateTime publishAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isPurchased;

    /** Lý do reviewer từ chối (chỉ hiển thị khi status = DRAFT sau khi bị reject) */
    private String reviewNote;

    // Comment của chương này
    private List<CommentResponse> comments;
    private Integer totalComments;
}
