package org.com.story.dto.request;

import lombok.Data;

@Data
public class RejectEditDto {
    /** Lý do từ chối bản edit của editor */
    private String authorNote;
}

