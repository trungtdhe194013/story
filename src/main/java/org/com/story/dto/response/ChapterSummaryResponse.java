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
public class ChapterSummaryResponse {
    private Long id;
    private String title;
    private Integer chapterOrder;
    private Integer coinPrice;
    private String status;
    private LocalDateTime publishAt;
    private Boolean isPurchased;
}

