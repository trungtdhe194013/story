package org.com.story.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResolveReportRequest {

    /**
     * Hành động xử lý:
     * - WARN_ONLY       : chỉ đánh dấu đã xử lý, không làm gì thêm
     * - HIDE_CONTENT    : ẩn nội dung bị báo cáo (comment → hidden, chapter → HIDDEN, story → REJECTED)
     * - DELETE_CONTENT  : xóa nội dung bị báo cáo
     * - BAN_USER        : ban tài khoản tác giả nội dung (cần banDays > 0)
     * - HIDE_AND_BAN    : ẩn nội dung + ban tác giả
     * - DELETE_AND_BAN  : xóa nội dung + ban tác giả
     */
    @NotBlank(message = "Action is required")
    private String action;

    /**
     * Số ngày ban tài khoản (chỉ áp dụng khi action có BAN).
     * 0 = không ban, -1 = ban vĩnh viễn
     */
    @Min(value = -1, message = "banDays must be -1 (permanent) or >= 0")
    private int banDays = 0;

    /** Ghi chú của admin (không bắt buộc) */
    private String adminNote;
}

