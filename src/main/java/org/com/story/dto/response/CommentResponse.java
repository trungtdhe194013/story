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
public class CommentResponse {
    private Long id;

    // Thông tin chapter
    private Long chapterId;
    private String chapterTitle;
    private Integer chapterOrder;
    private Long storyId;
    private String storyTitle;

    // Thông tin người comment
    private Long userId;
    private String userName;

    private String content;
    private Long parentId;
    private List<CommentResponse> replies;
    private LocalDateTime createdAt;
}
