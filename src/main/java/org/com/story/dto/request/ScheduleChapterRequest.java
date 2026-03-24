package org.com.story.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleChapterRequest {

    @NotNull(message = "publishAt is required")
    @Future(message = "publishAt must be a future date-time")
    private LocalDateTime publishAt;
}

