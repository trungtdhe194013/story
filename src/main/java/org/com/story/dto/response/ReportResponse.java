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
public class ReportResponse {
    private Long id;

    // Người báo cáo
    private Long reporterId;
    private String reporterName;

    // Nội dung bị báo cáo
    private String targetType;       // STORY, CHAPTER, COMMENT
    private Long targetId;
    private String targetTitle;      // Tiêu đề story/chapter, hoặc 50 ký tự đầu của comment
    private String targetOwnerName;  // Tên chủ sở hữu nội dung bị báo cáo

    // Phân loại & lý do
    private String category;         // SPAM, COPYRIGHT, INAPPROPRIATE, VIOLENCE, OTHER
    private String reason;

    // Trạng thái
    private String status;           // PENDING, RESOLVED

    // Xử lý
    private String resolvedAction;   // WARN_ONLY, HIDE_CONTENT, DELETE_CONTENT, BAN_USER, HIDE_AND_BAN, DELETE_AND_BAN
    private String adminNote;
    private Long resolvedById;
    private String resolvedByName;
    private LocalDateTime resolvedAt;

    private LocalDateTime createdAt;
}
