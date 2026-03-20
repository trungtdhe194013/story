package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CreateEditRequestDto;
import org.com.story.dto.request.RejectEditDto;
import org.com.story.dto.request.SubmitEditDto;
import org.com.story.dto.response.EditRequestResponse;
import org.com.story.service.EditRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/edit-requests")
@RequiredArgsConstructor
@Tag(name = "Edit Request", description = "Marketplace chỉnh sửa chapter — Author đặt việc, Editor nhận, coin thưởng khi hoàn thành")
public class EditRequestController {

    private final EditRequestService editRequestService;

    // ==================== AUTHOR SIDE ====================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[AUTHOR] Tạo yêu cầu chỉnh sửa chapter",
            description = """
                    Author đăng yêu cầu edit 1 chapter kèm số coin thưởng.
                    - Coin bị **LOCK** ngay lập tức vào escrow.
                    - Chapter phải ở trạng thái `DRAFT` hoặc `EDITED`.
                    - Mỗi chapter chỉ có 1 request đang active tại 1 thời điểm.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse createRequest(@Valid @RequestBody CreateEditRequestDto dto) {
        return editRequestService.createRequest(dto);
    }

    @GetMapping("/my")
    @Operation(summary = "[AUTHOR] Xem tất cả edit requests của tôi (Author side)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<EditRequestResponse> getMyRequestsAsAuthor() {
        return editRequestService.getMyRequestsAsAuthor();
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "[AUTHOR] Chấp thuận bản edit của Editor",
            description = """
                    - Chapter content được cập nhật bằng nội dung Editor đã nộp.
                    - Nội dung cũ được lưu vào ChapterVersion (có thể xem lại).
                    - Coin escrow chuyển sang ví Editor.
                    - Request status → APPROVED.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse approveEdit(@PathVariable Long id) {
        return editRequestService.approveEdit(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "[AUTHOR] Từ chối bản edit, Editor viết lại",
            description = """
                    - Editor được phép submit lại vô hạn lần.
                    - Coin vẫn bị lock — chưa hoàn trả.
                    - Request status → IN_PROGRESS (Editor tiếp tục).
                    - `attemptCount` tăng lên 1.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse rejectEdit(@PathVariable Long id,
                                          @RequestBody(required = false) RejectEditDto dto) {
        return editRequestService.rejectEdit(id, dto != null ? dto : new RejectEditDto());
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "[AUTHOR] Huỷ edit request (chỉ khi OPEN, chưa có editor nhận)",
            description = "Coin escrow được hoàn trả về ví Author. Status → CANCELLED.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse cancelRequest(@PathVariable Long id) {
        return editRequestService.cancelRequest(id);
    }

    // ==================== EDITOR SIDE ====================

    @GetMapping("/open")
    @Operation(summary = "[EDITOR] Xem danh sách yêu cầu đang OPEN",
            description = "Editor xem tất cả yêu cầu chờ nhận: tên chapter, tên story, coin thưởng, mô tả.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<EditRequestResponse> getOpenRequests() {
        return editRequestService.getOpenRequests();
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "[EDITOR] Nhận việc (OPEN → IN_PROGRESS)",
            description = "Editor tự nhận 1 request. Mỗi request chỉ có 1 editor tại 1 thời điểm.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse assignRequest(@PathVariable Long id) {
        return editRequestService.assignRequest(id);
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "[EDITOR] Nộp bản chỉnh sửa (IN_PROGRESS → SUBMITTED)",
            description = "Editor nộp `editedContent` + `editorNote`. Author sẽ đọc và approve/reject.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse submitEdit(@PathVariable Long id,
                                          @Valid @RequestBody SubmitEditDto dto) {
        return editRequestService.submitEdit(id, dto);
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "[EDITOR] Rút lui khỏi request (chỉ khi chưa bị reject lần nào)",
            description = "Editor bỏ việc → request về OPEN để editor khác nhận. Chỉ được rút khi `attemptCount = 0`.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public EditRequestResponse withdrawFromRequest(@PathVariable Long id) {
        return editRequestService.withdrawFromRequest(id);
    }

    @GetMapping("/assigned")
    @Operation(summary = "[EDITOR] Xem lịch sử các việc tôi đã nhận",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<EditRequestResponse> getMyRequestsAsEditor() {
        return editRequestService.getMyRequestsAsEditor();
    }
}

