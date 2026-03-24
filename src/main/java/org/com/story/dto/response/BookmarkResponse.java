package org.com.story.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookmarkResponse {
    private Long id;
    private Long storyId;
    private String storyTitle;
    private String storyCoverUrl;
    private Long chapterId;
    private String chapterTitle;
    private Integer chapterOrder;
    private LocalDateTime updatedAt;
}

