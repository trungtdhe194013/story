package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastNotificationRequest {

    @NotBlank(message = "title không được để trống")
    private String title;

    @NotBlank(message = "message không được để trống")
    private String message;

    /**
     * Đối tượng nhận:
     * ALL         — tất cả user
     * READER      — chỉ Reader
     * AUTHOR      — chỉ Author
     * EDITOR      — chỉ Editor
     * REVIEWER    — chỉ Reviewer
     */
    private String targetRole; // default = ALL nếu null/blank
}

