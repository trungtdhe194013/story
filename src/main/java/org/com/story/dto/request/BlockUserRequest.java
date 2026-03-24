package org.com.story.dto.request;

import lombok.Data;

@Data
public class BlockUserRequest {
    private Long userId;   // ID người muốn chặn
    private String reason; // lý do (tùy chọn)
}

