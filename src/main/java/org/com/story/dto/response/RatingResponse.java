package org.com.story.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long storyId;
    private String storyTitle;
    private Integer score;
    private String review;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

