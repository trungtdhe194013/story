package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitEditDto {

    @NotBlank(message = "editedContent là bắt buộc")
    private String editedContent;

    private String editorNote;
}

