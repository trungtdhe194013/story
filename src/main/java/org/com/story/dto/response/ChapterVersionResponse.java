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
public class ChapterVersionResponse {
    private Long id;
    private Long chapterId;
    private String content;
    private Integer version;
    private LocalDateTime createdAt;
}

