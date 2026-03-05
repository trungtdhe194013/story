package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryDetailResponse {
    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private String status;
    private Long authorId;
    private String authorName;
    private Long viewCount;
    private Set<CategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Danh sách chương (chỉ PUBLISHED, không có content - để tiết kiệm băng thông)
    private List<ChapterSummaryResponse> chapters;

    private Integer totalChapters;
}

