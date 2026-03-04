package org.com.story.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.MissionRequest;
import org.com.story.dto.request.ReviewRoleChangeRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.*;
import org.com.story.service.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Controller", description = "Quản trị hệ thống (chỉ ADMIN)")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;
    private final WithdrawRequestService withdrawRequestService;
    private final MissionService missionService;
    private final RoleChangeRequestService roleChangeRequestService;

    // ============== DASHBOARD ==============

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard stats", description = "Thống kê tổng quan hệ thống",
            security = @SecurityRequirement(name = "bearerAuth"))
    public DashboardStatsResponse getDashboard() {
        return adminService.getDashboardStats();
    }

    @Hidden
    @GetMapping("/test")
    public String test() {
        System.out.println("Controller auth = "
                + SecurityContextHolder.getContext().getAuthentication());
        return "ADMIN OK";
    }

    // ============== STORIES ==============

    @GetMapping("/stories/pending")
    @Operation(summary = "Get pending stories", description = "Xem danh sách truyện chờ duyệt",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<StoryResponse> getPendingStories() {
        return adminService.getPendingStories();
    }

    @PostMapping("/stories/{id}/review")
    @Operation(summary = "Review story", description = "Duyệt/từ chối truyện (action: APPROVE hoặc REJECT)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public StoryResponse reviewStory(
            @PathVariable Long id,
            @Valid @RequestBody ReviewStoryRequest request) {
        return adminService.reviewStory(id, request);
    }

    // ============== USERS ==============

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Xem danh sách tất cả người dùng",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<UserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @PutMapping("/users/roles")
    @Operation(summary = "Update user roles", description = "Cập nhật role cho người dùng",
            security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse updateUserRoles(@Valid @RequestBody UpdateUserRoleRequest request) {
        return adminService.updateUserRoles(request);
    }

    // ============== REPORTS ==============

    @GetMapping("/reports")
    @Operation(summary = "Get all reports", description = "Xem tất cả báo cáo vi phạm",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReportResponse> getAllReports() {
        return reportService.getAllReports();
    }

    @GetMapping("/reports/pending")
    @Operation(summary = "Get pending reports", description = "Xem báo cáo chờ xử lý",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReportResponse> getPendingReports() {
        return reportService.getPendingReports();
    }

    @PostMapping("/reports/{id}/resolve")
    @Operation(summary = "Resolve report", description = "Đánh dấu báo cáo đã xử lý",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ReportResponse resolveReport(@PathVariable Long id) {
        return reportService.resolveReport(id);
    }

    // ============== WITHDRAW REQUESTS ==============

    @GetMapping("/withdraw-requests")
    @Operation(summary = "Get all withdraw requests", description = "Xem tất cả yêu cầu rút tiền",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<WithdrawRequestResponse> getAllWithdrawRequests() {
        return withdrawRequestService.getAllWithdrawRequests();
    }

    @GetMapping("/withdraw-requests/pending")
    @Operation(summary = "Get pending withdraw requests", description = "Xem yêu cầu rút tiền chờ duyệt",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<WithdrawRequestResponse> getPendingWithdrawRequests() {
        return withdrawRequestService.getPendingWithdrawRequests();
    }

    @PostMapping("/withdraw-requests/{id}/approve")
    @Operation(summary = "Approve withdraw request", description = "Duyệt yêu cầu rút tiền (trừ tiền ví user)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public WithdrawRequestResponse approveWithdrawRequest(@PathVariable Long id) {
        return withdrawRequestService.approveWithdrawRequest(id);
    }

    @PostMapping("/withdraw-requests/{id}/reject")
    @Operation(summary = "Reject withdraw request", description = "Từ chối yêu cầu rút tiền",
            security = @SecurityRequirement(name = "bearerAuth"))
    public WithdrawRequestResponse rejectWithdrawRequest(@PathVariable Long id) {
        return withdrawRequestService.rejectWithdrawRequest(id);
    }

    // ============== MISSIONS ==============

    @PostMapping("/missions")
    @Operation(summary = "Create mission", description = "Tạo nhiệm vụ mới",
            security = @SecurityRequirement(name = "bearerAuth"))
    public MissionResponse createMission(@Valid @RequestBody MissionRequest request) {
        return missionService.createMission(request);
    }

    @PutMapping("/missions/{id}")
    @Operation(summary = "Update mission", description = "Cập nhật nhiệm vụ",
            security = @SecurityRequirement(name = "bearerAuth"))
    public MissionResponse updateMission(@PathVariable Long id, @Valid @RequestBody MissionRequest request) {
        return missionService.updateMission(id, request);
    }

    @DeleteMapping("/missions/{id}")
    @Operation(summary = "Delete mission", description = "Xóa nhiệm vụ",
            security = @SecurityRequirement(name = "bearerAuth"))
    public void deleteMission(@PathVariable Long id) {
        missionService.deleteMission(id);
    }

    // ============== ROLE CHANGE REQUESTS ==============

    @GetMapping("/role-change-requests")
    @Operation(summary = "Get all role change requests", description = "Xem tất cả yêu cầu đổi role",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<RoleChangeRequestResponse> getAllRoleChangeRequests() {
        return roleChangeRequestService.getAllRequests();
    }

    @GetMapping("/role-change-requests/pending")
    @Operation(summary = "Get pending role change requests", description = "Xem yêu cầu đổi role đang chờ duyệt",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<RoleChangeRequestResponse> getPendingRoleChangeRequests() {
        return roleChangeRequestService.getRequestsByStatus("PENDING");
    }

    @PostMapping("/role-change-requests/review")
    @Operation(
        summary = "Review role change request",
        description = """
            Admin duyệt hoặc từ chối yêu cầu đổi role.
            - `action`: `APPROVE` → xóa role cũ, gán role mới | `REJECT` → từ chối
            - `adminNote`: ghi chú (tùy chọn)
            """,
        security = @SecurityRequirement(name = "bearerAuth"))
    public RoleChangeRequestResponse reviewRoleChangeRequest(
            @Valid @RequestBody ReviewRoleChangeRequest request) {
        return roleChangeRequestService.reviewRequest(request);
    }
}

