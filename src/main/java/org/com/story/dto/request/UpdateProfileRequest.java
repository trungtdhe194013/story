package org.com.story.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String password; // optional, chỉ cập nhật nếu có giá trị
}

