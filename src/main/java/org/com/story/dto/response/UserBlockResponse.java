package org.com.story.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserBlockResponse {
    private Long id;
    private Long blockedUserId;
    private String blockedUserName;
    private String blockedUserAvatar;
    private String reason;
    private LocalDateTime createdAt;
}

