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
    private Double avgRating;
    private Integer ratingCount;
    private Boolean isCompleted;
    private Set<CategoryResponse> categories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Rating của user hiện tại (null nếu chưa đánh giá hoặc chưa đăng nhập) */
    private Integer myRating;

    /** Nhận xét của user hiện tại (null nếu chưa đánh giá) */
    private String myReview;

    // Danh sách chương (chỉ PUBLISHED, không có content - để tiết kiệm băng thông)
    private List<ChapterSummaryResponse> chapters;

    /** Số chương đã PUBLISHED (bằng chapters.size()) */
    private Integer totalChapters;

    /** Tổng số chương mọi trạng thái */
    private Integer allChaptersCount;

    /** Số người đang follow truyện này */
    private Long followCount;

    /** true nếu user hiện tại đang follow (null nếu chưa đăng nhập) */
    private Boolean isFollowing;
}
