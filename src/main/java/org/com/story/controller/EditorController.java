package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.EditorChapterEditRequest;
import org.com.story.dto.response.ChapterResponse;
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

    @GetMapping("/chapters/pending")
    @Operation(summary = "Get pending chapters", description = "Xem danh sách chapter chưa có editor chỉnh sửa",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ChapterResponse> getPendingChapters() {
        return editorService.getPendingChaptersForEdit();
    }

    @PostMapping("/chapters/{chapterId}/assign")
    @Operation(summary = "Assign chapter", description = "Nhận chapter để chỉnh sửa (editor tự gán cho mình)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse assignChapter(@PathVariable Long chapterId) {
        return editorService.assignChapterToEditor(chapterId);
    }

    @PutMapping("/chapters/{chapterId}/edit")
    @Operation(summary = "Submit chapter edit", description = "Gửi bản chỉnh sửa chapter (tự động lưu version cũ)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse submitEdit(
            @PathVariable Long chapterId,
            @Valid @RequestBody EditorChapterEditRequest request) {
        return editorService.submitChapterEdit(chapterId, request);
    }

    @GetMapping("/chapters/{chapterId}/versions")
    @Operation(summary = "Get chapter versions", description = "Xem lịch sử các phiên bản của chapter",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ChapterVersionResponse> getChapterVersions(@PathVariable Long chapterId) {
        return editorService.getChapterVersions(chapterId);
    }
}

