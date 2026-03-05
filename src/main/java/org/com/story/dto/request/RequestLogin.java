package org.com.story.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request with email and password")
public class LoginRequest {

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Schema(description = "User email address", example = "author@story.com")
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "User password", example = "author123")
    private String password;
}
