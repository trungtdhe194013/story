package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CommentRequest {

    /** ID chapter — bắt buộc cho chapter comment, null cho story comment */
    private Long chapterId;

    /** ID story — dùng cho story-level comment (review), null cho chapter comment */
    private Long storyId;

    @NotBlank(message = "Content is required")
    private String content;

    private Long parentId; // null → comment gốc, có giá trị → reply
}
