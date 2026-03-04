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

    // Danh sách role hợp lệ (không cho phép đổi thành ADMIN)
    private static final Set<String> ALLOWED_ROLES = Set.of("READER", "AUTHOR", "EDITOR", "REVIEWER");

    @Override
    public RoleChangeRequestResponse submitRequest(RoleChangeRequestDto request) {
        User currentUser = userService.getCurrentUser();

        // Validate role hợp lệ
        String requestedRole = request.getRequestedRole().toUpperCase();
        if (!ALLOWED_ROLES.contains(requestedRole)) {
            throw new BadRequestException("Invalid role. Allowed roles: " + ALLOWED_ROLES);
        }

        // Lấy role hiện tại của user (chỉ 1 role) - roles là EAGER nên OK
        String currentRole = currentUser.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("READER");

        // Không cho đổi sang role giống role hiện tại
        if (currentRole.equalsIgnoreCase(requestedRole)) {
            throw new BadRequestException("You already have the role: " + requestedRole);
        }

        // Kiểm tra đã có request PENDING chưa
        roleChangeRequestRepository.findByUserIdAndStatus(currentUser.getId(), "PENDING")
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "You already have a pending role change request (ID: " + existing.getId() + "). " +
                            "Please wait for admin to review it.");
                });

        // Tạo request mới
        RoleChangeRequest roleChangeRequest = RoleChangeRequest.builder()
                .user(currentUser)
                .requestedRole(requestedRole)
                .currentRole(currentRole)
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
                .orElseThrow(() -> new NotFoundException("Role change request not found"));

        if (!"PENDING".equals(roleChangeRequest.getStatus())) {
            throw new BadRequestException("This request has already been reviewed. Status: " + roleChangeRequest.getStatus());
        }

        String action = request.getAction().toUpperCase();
        if (!action.equals("APPROVE") && !action.equals("REJECT")) {
            throw new BadRequestException("Action must be APPROVE or REJECT");
        }

        roleChangeRequest.setReviewedBy(admin);
        roleChangeRequest.setAdminNote(request.getAdminNote());

        if ("APPROVE".equals(action)) {
            // Xóa role cũ, gán role mới
            User targetUser = roleChangeRequest.getUser();
            String newRoleName = roleChangeRequest.getRequestedRole();

            Role newRole = roleRepository.findByName(newRoleName)
                    .orElseThrow(() -> new NotFoundException("Role not found: " + newRoleName));

            // Xóa tất cả role cũ, chỉ giữ 1 role mới
            Set<Role> newRoles = new HashSet<>();
            newRoles.add(newRole);
            targetUser.setRoles(newRoles);

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



