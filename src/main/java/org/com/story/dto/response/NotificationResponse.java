package org.com.story.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private Long refId;
    private String refType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

