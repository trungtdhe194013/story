package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditRequestResponse {

    private Long id;

    // Chapter info
    private Long chapterId;
    private String chapterTitle;
    private String storyTitle;

    // Author info
    private Long authorId;
    private String authorName;

    // Editor info (null nếu chưa có ai nhận)
    private Long editorId;
    private String editorName;

    private Long coinReward;
    private String description;

    /** Nội dung editor đã submit — chỉ trả về khi status = SUBMITTED hoặc APPROVED */
    private String editedContent;

    private String editorNote;

    /** Lý do từ chối gần nhất của author */
    private String authorNote;

    /**
     * OPEN / IN_PROGRESS / SUBMITTED / APPROVED / CANCELLED
     */
    private String status;

    /** Số lần đã bị từ chối */
    private Integer attemptCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

