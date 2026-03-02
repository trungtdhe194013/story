package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CommentRequest;
import org.com.story.dto.response.CommentResponse;
import org.com.story.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Controller", description = "Quản lý bình luận")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/chapter/{chapterId}")
    @Operation(summary = "Get comments by chapter", description = "Lấy tất cả bình luận của 1 chapter (dạng cây: comment + replies)")
    public List<CommentResponse> getCommentsByChapter(@PathVariable Long chapterId) {
        return commentService.getCommentsByChapter(chapterId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create comment", description = """
            Tạo bình luận cho chapter.
            - `chapterId`: ID của chapter muốn bình luận (bắt buộc)
            - `parentId`: null → comment gốc | có giá trị → reply vào comment khác
            """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public CommentResponse createComment(@Valid @RequestBody CommentRequest request) {
        return commentService.createComment(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete comment", description = "Xóa bình luận (chỉ chủ comment hoặc admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }
}

