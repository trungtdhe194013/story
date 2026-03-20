package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponse {
    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private String status;
    private Long authorId;
    private String authorName;
    private Set<CategoryResponse> categories;
    private Long viewCount;
    private Double avgRating;
    private Integer ratingCount;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Số chương đã PUBLISHED */
    private Integer publishedChapterCount;

    /** Tổng số chương (mọi trạng thái) */
    private Integer totalChapterCount;

    /** true nếu truyện đã bị xóa mềm (ẩn khỏi trang chủ) */
    private Boolean isDeleted;

    /** Số người đang follow truyện này */
    private Long followCount;

    /** true nếu user hiện tại đang follow (null nếu chưa đăng nhập) */
    private Boolean isFollowing;
}
