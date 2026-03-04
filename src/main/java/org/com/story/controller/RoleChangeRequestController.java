package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.RoleChangeRequestDto;
import org.com.story.dto.response.RoleChangeRequestResponse;
import org.com.story.service.RoleChangeRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-change-requests")
@RequiredArgsConstructor
@Tag(name = "Role Change Request", description = "Yêu cầu đổi Role - User gửi, Admin duyệt")
public class RoleChangeRequestController {

    private final RoleChangeRequestService roleChangeRequestService;

    @PostMapping
    @Operation(
        summary = "Gửi yêu cầu đổi role",
        description = """
            User gửi yêu cầu đổi role mới.
            - Mỗi user chỉ được có **1 yêu cầu PENDING** tại 1 thời điểm.
            - Role hợp lệ: `READER`, `AUTHOR`, `EDITOR`, `REVIEWER`
            - Không thể tự đổi thành `ADMIN`
            - Body: `requestedRole` (bắt buộc), `reason` (tùy chọn)
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<RoleChangeRequestResponse> submitRequest(
            @Valid @RequestBody RoleChangeRequestDto request) {
        return ResponseEntity.ok(roleChangeRequestService.submitRequest(request));
    }

    @GetMapping("/my")
    @Operation(
        summary = "Xem lịch sử yêu cầu đổi role của tôi",
        description = "Lấy toàn bộ lịch sử yêu cầu đổi role của user đang đăng nhập.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<RoleChangeRequestResponse>> getMyRequests() {
        return ResponseEntity.ok(roleChangeRequestService.getMyRequests());
    }
}

