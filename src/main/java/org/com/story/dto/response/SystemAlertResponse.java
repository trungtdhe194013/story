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
public class SystemAlertResponse {
    private String id;
    private LocalDateTime timestamp;
    /** CRITICAL, HIGH, MEDIUM, LOW */
    private String level;
    /** CRITICAL, HIGH, MEDIUM, LOW — alias kept for compat */
    private String severity;
    private String message;
    private String source;
    private Boolean isAcknowledged;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
}
