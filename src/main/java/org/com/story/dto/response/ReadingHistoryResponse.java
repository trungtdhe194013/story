package org.com.story.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReadingHistoryResponse {
    private Long storyId;
    private String storyTitle;
    private String storyCoverUrl;
    private String authorName;
    private Long lastChapterId;
    private String lastChapterTitle;
    private Integer lastChapterOrder;
    private LocalDateTime lastReadAt;
}

