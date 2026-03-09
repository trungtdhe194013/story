package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReviewRoleChangeRequest;
import org.com.story.dto.request.RoleChangeRequestDto;
import org.com.story.dto.response.RoleChangeRequestResponse;
import org.com.story.entity.Role;
import org.com.story.entity.RoleChangeRequest;
import org.com.story.entity.User;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.RoleChangeRequestRepository;
import org.com.story.repository.RoleRepository;
import org.com.story.service.RoleChangeRequestService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleChangeRequestServiceImpl implements RoleChangeRequestService {

    private final RoleChangeRequestRepository roleChangeRequestRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;

    /**
     * Role logic:
     * - Mỗi user LUÔN có READER (base role).
     * - Chỉ được thêm tối đa 1 role trong nhóm [AUTHOR, EDITOR, REVIEWER].
     * - 3 role này exclusive với nhau (không có 2 cùng lúc).
     * - Không được đổi thành ADMIN.
     */
    private static final Set<String> EXCLUSIVE_ROLES = Set.of("AUTHOR", "EDITOR", "REVIEWER");

    @Override
    public RoleChangeRequestResponse submitRequest(RoleChangeRequestDto request) {
        User currentUser = userService.getCurrentUser();

        String requestedRole = request.getRequestedRole().toUpperCase();

        // Chỉ cho phép đổi trong nhóm exclusive
        if (!EXCLUSIVE_ROLES.contains(requestedRole)) {
            throw new BadRequestException(
                    "Chỉ có thể yêu cầu một trong các role: " + EXCLUSIVE_ROLES +
                    ". READER là role mặc định và không thể thay đổi.");
        }

        // Lấy các role hiện tại của user
        Set<String> currentRoleNames = currentUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Kiểm tra đã có role đó chưa
        if (currentRoleNames.contains(requestedRole)) {
            throw new BadRequestException("Bạn đã có role " + requestedRole + " rồi.");
        }

        // Xác định currentRole để hiển thị (ưu tiên role exclusive nếu có)
        String currentExclusiveRole = currentRoleNames.stream()
                .filter(EXCLUSIVE_ROLES::contains)
                .findFirst()
                .orElse("READER");

        // Kiểm tra đã có PENDING request chưa
        roleChangeRequestRepository.findByUserIdAndStatus(currentUser.getId(), "PENDING")
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Bạn đã có yêu cầu đổi role đang chờ duyệt (ID: " + existing.getId() + "). " +
                            "Vui lòng đợi admin xem xét.");
                });

        RoleChangeRequest roleChangeRequest = RoleChangeRequest.builder()
                .user(currentUser)
                .requestedRole(requestedRole)
                .currentRole(currentExclusiveRole)
                .reason(request.getReason())
                .status("PENDING")
                .build();

        RoleChangeRequest saved = roleChangeRequestRepository.save(roleChangeRequest);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleChangeRequestResponse> getMyRequests() {
        User currentUser = userService.getCurrentUser();
        return roleChangeRequestRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleChangeRequestResponse> getAllRequests() {
        return roleChangeRequestRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleChangeRequestResponse> getRequestsByStatus(String status) {
        return roleChangeRequestRepository.findByStatus(status.toUpperCase())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoleChangeRequestResponse reviewRequest(ReviewRoleChangeRequest request) {
        User admin = userService.getCurrentUser();

        RoleChangeRequest roleChangeRequest = roleChangeRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu đổi role"));

        if (!"PENDING".equals(roleChangeRequest.getStatus())) {
            throw new BadRequestException("Yêu cầu này đã được xem xét. Trạng thái: " + roleChangeRequest.getStatus());
        }

        String action = request.getAction().toUpperCase();
        if (!action.equals("APPROVE") && !action.equals("REJECT")) {
            throw new BadRequestException("Action phải là APPROVE hoặc REJECT");
        }

        roleChangeRequest.setReviewedBy(admin);
        roleChangeRequest.setAdminNote(request.getAdminNote());

        if ("APPROVE".equals(action)) {
            User targetUser = roleChangeRequest.getUser();
            String newRoleName = roleChangeRequest.getRequestedRole();

            Role readerRole = roleRepository.findByName("READER")
                    .orElseThrow(() -> new NotFoundException("Role READER không tồn tại"));
            Role newRole = roleRepository.findByName(newRoleName)
                    .orElseThrow(() -> new NotFoundException("Role không tồn tại: " + newRoleName));

            // Giữ READER + thêm/thay thế role exclusive mới
            // Xóa tất cả role exclusive cũ (AUTHOR/EDITOR/REVIEWER), giữ READER, thêm role mới
            Set<Role> updatedRoles = new HashSet<>();
            updatedRoles.add(readerRole); // luôn có READER
            updatedRoles.add(newRole);    // thêm role mới (AUTHOR/EDITOR/REVIEWER)

            // Giữ lại các role đặc biệt khác nếu có (ví dụ ADMIN - không bao giờ bị xóa)
            for (Role existingRole : targetUser.getRoles()) {
                if (!EXCLUSIVE_ROLES.contains(existingRole.getName()) && !existingRole.getName().equals("READER")) {
                    updatedRoles.add(existingRole);
                }
            }

            targetUser.setRoles(updatedRoles);
            roleChangeRequest.setStatus("APPROVED");
        } else {
            roleChangeRequest.setStatus("REJECTED");
        }

        RoleChangeRequest updated = roleChangeRequestRepository.save(roleChangeRequest);
        return mapToResponse(updated);
    }

    private RoleChangeRequestResponse mapToResponse(RoleChangeRequest r) {
        User user = r.getUser();
        return RoleChangeRequestResponse.builder()
                .id(r.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getFullName())
                .currentRole(r.getCurrentRole())
                .requestedRole(r.getRequestedRole())
                .status(r.getStatus())
                .reason(r.getReason())
                .adminNote(r.getAdminNote())
                .reviewedBy(r.getReviewedBy() != null ? r.getReviewedBy().getEmail() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}


