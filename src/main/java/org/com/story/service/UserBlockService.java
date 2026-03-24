package org.com.story.service;

import org.com.story.dto.request.BlockUserRequest;
import org.com.story.dto.response.UserBlockResponse;

import java.util.List;

public interface UserBlockService {

    /** Tác giả chặn một user */
    UserBlockResponse blockUser(BlockUserRequest request);

    /** Tác giả bỏ chặn một user */
    void unblockUser(Long userId);

    /** Danh sách user bị chặn bởi tác giả hiện tại */
    List<UserBlockResponse> getMyBlockedUsers();

    /** Kiểm tra xem authorId có đang chặn userId hay không */
    boolean isBlocked(Long authorId, Long userId);
}

