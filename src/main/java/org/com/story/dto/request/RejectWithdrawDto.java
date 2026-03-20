package org.com.story.dto.request;

import lombok.Data;

@Data
public class RejectWithdrawDto {
    /** Lý do từ chối (tùy chọn, admin có thể bỏ trống) */
    private String rejectedReason;
}

