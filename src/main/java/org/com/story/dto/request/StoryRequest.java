package org.com.story.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class StoryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String summary;

    private String coverUrl;

    /** Danh sách ID thể loại muốn gắn cho truyện (tuỳ chọn) */
    private Set<Long> categoryIds;
}
