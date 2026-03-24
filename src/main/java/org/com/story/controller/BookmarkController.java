package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.BookmarkResponse;
import org.com.story.service.BookmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "Lưu dấu trang vị trí đọc theo chương")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /**
     * PUT /api/bookmarks/story/{storyId}/chapter/{chapterId}
     * Đặt hoặc cập nhật bookmark cho một truyện tại chương chỉ định.
     * Nếu đã có bookmark cho truyện này thì cập nhật chương mới (upsert).
     */
    @PutMapping("/story/{storyId}/chapter/{chapterId}")
    @Operation(
            summary = "Save / update bookmark",
            description = "Lưu dấu trang vị trí đọc (chapterID) cho một truyện. Upsert: tạo mới nếu chưa có, cập nhật nếu đã có.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public BookmarkResponse saveBookmark(
            @PathVariable Long storyId,
            @PathVariable Long chapterId) {
        return bookmarkService.saveBookmark(storyId, chapterId);
    }

    /**
     * DELETE /api/bookmarks/story/{storyId}
     * Xoá bookmark của truyện.
     */
    @DeleteMapping("/story/{storyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove bookmark",
            description = "Xoá bookmark của truyện khỏi Library cá nhân.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public void removeBookmark(@PathVariable Long storyId) {
        bookmarkService.removeBookmark(storyId);
    }

    /**
     * GET /api/bookmarks
     * Danh sách tất cả bookmark (Library cá nhân), sắp xếp mới nhất trước.
     * Frontend click để nhảy thẳng đến chương đã đánh dấu.
     */
    @GetMapping
    @Operation(
            summary = "Get my bookmarks (Library)",
            description = "Danh sách bookmark trong Library cá nhân, sắp xếp theo lần cập nhật mới nhất. Mỗi item có chapterId để nhảy thẳng đến chương.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<BookmarkResponse> getMyBookmarks() {
        return bookmarkService.getMyBookmarks();
    }

    /**
     * GET /api/bookmarks/story/{storyId}
     * Lấy bookmark hiện tại của user cho một truyện cụ thể (null nếu chưa đánh dấu).
     */
    @GetMapping("/story/{storyId}")
    @Operation(
            summary = "Get bookmark for a story",
            description = "Lấy bookmark hiện tại cho truyện. Trả về null (204) nếu chưa có bookmark.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public BookmarkResponse getBookmark(@PathVariable Long storyId) {
        return bookmarkService.getBookmark(storyId);
    }
}


