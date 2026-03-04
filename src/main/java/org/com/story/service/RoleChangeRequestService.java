package org.com.story.service;

import org.com.story.dto.request.ReviewRoleChangeRequest;
import org.com.story.dto.request.RoleChangeRequestDto;
import org.com.story.dto.response.RoleChangeRequestResponse;

import java.util.List;

public interface RoleChangeRequestService {

    // User gửi yêu cầu đổi role
    RoleChangeRequestResponse submitRequest(RoleChangeRequestDto request);

    // User xem lịch sử yêu cầu của mình
    List<RoleChangeRequestResponse> getMyRequests();

    // Admin xem tất cả yêu cầu
    List<RoleChangeRequestResponse> getAllRequests();

    // Admin xem yêu cầu theo trạng thái
    List<RoleChangeRequestResponse> getRequestsByStatus(String status);

    // Admin duyệt / từ chối
    RoleChangeRequestResponse reviewRequest(ReviewRoleChangeRequest request);
}

