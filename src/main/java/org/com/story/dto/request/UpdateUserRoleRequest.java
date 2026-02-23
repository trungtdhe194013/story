package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Roles are required")
    private Set<String> roles; // ["READER", "AUTHOR", "EDITOR", "REVIEWER", "ADMIN"]
}
