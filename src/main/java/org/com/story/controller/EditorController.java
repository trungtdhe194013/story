package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.ChapterVersionResponse;
import org.com.story.service.EditorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/editor")
@RequiredArgsConstructor
@Tag(name = "Editor Controller", description = "Chức năng biên tập viên")
public class EditorController {

    private final EditorService editorService;

    @GetMapping("/chapters/{chapterId}/versions")
    @Operation(summary = "Xem lịch sử phiên bản chapter",
            description = "Editor hoặc Author xem lịch sử các lần chỉnh sửa nội dung chapter.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ChapterVersionResponse> getChapterVersions(@PathVariable Long chapterId) {
        return editorService.getChapterVersions(chapterId);
    }
}

