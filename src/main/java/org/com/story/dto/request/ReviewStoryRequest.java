package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewStoryRequest {

    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    private String action; // APPROVE, REJECT

    private String note;
}
