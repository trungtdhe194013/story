package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleChangeRequestDto {

    @NotBlank(message = "Requested role is required")
    private String requestedRole; // AUTHOR, EDITOR, REVIEWER, READER

    private String reason; // lý do muốn đổi (tùy chọn)
}

