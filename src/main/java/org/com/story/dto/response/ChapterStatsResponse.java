package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterStatsResponse {

    private Long storyId;
    private String storyTitle;

    /** Số chương đã PUBLISHED */
    private Integer publishedCount;

    /** Tổng số chương (mọi trạng thái) */
    private Integer totalCount;
}

