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
public class SystemLogResponse {
    private String id;
    private LocalDateTime timestamp;
    private String severity; // INFO, WARN, ERROR, DEBUG
    private String component;
    private String message;
    private String traceId;
}
