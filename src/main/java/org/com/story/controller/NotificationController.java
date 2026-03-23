package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.NotificationResponse;
import org.com.story.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Controller", description = "Quản lý thông báo của người dùng")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách thông báo",
            description = """
                    Trả về toàn bộ thông báo của user hiện tại, sắp xếp mới nhất lên đầu.
                    
                    **Các loại type:**
                    - `NEW_CHAPTER`       — Truyện đang follow có chương mới được publish
                    - `STORY_APPROVED`    — Story của bạn vừa được Reviewer duyệt
                    - `STORY_REJECTED`    — Story của bạn bị từ chối, kèm lý do
                    - `CHAPTER_APPROVED`  — Chương truyện được Reviewer duyệt (Author tự publish)
                    - `CHAPTER_REJECTED`  — Chương bị từ chối, kèm lý do để sửa lại
                    - `GIFT_RECEIVED`     — Có người tặng quà cho truyện của bạn
                    - `NEW_FOLLOWER`      — Có người mới follow truyện của bạn
                    - `SYSTEM`            — Thông báo hệ thống từ Admin
                    
                    **refType:** `STORY`, `CHAPTER`, `GIFT`, `SYSTEM`
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "Số thông báo chưa đọc",
            description = "Trả về { \"count\": N } — dùng để hiển thị badge đỏ trên icon thông báo.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    @PutMapping("/mark-all-read")
    @Operation(
            summary = "Đánh dấu tất cả thông báo là đã đọc",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("message", "Đã đánh dấu tất cả thông báo là đã đọc"));
    }

    @PutMapping("/{id}/read")
    @Operation(
            summary = "Đánh dấu một thông báo là đã đọc",
            description = "Đánh dấu thông báo với id cụ thể là đã đọc.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xoá một thông báo",
            description = "Xoá vĩnh viễn một thông báo khỏi danh sách.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Đã xoá thông báo"));
    }
}

