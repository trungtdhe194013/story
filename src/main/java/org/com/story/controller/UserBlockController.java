package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.BlockUserRequest;
import org.com.story.dto.response.UserBlockResponse;
import org.com.story.service.UserBlockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/blocks")
@RequiredArgsConstructor
@Tag(name = "Block User", description = "Tác giả chặn/bỏ chặn người dùng khỏi tương tác trên tác phẩm của mình")
public class UserBlockController {

    private final UserBlockService userBlockService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Block a user",
            description = "Chặn một người dùng: họ sẽ không thể bình luận hoặc tặng quà cho bất kỳ tác phẩm nào của bạn.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public UserBlockResponse blockUser(@Valid @RequestBody BlockUserRequest request) {
        return userBlockService.blockUser(request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Unblock a user",
            description = "Bỏ chặn một người dùng.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public void unblockUser(@PathVariable Long userId) {
        userBlockService.unblockUser(userId);
    }

    @GetMapping
    @Operation(
            summary = "Get my blocked users",
            description = "Xem danh sách những người dùng mà bạn đang chặn (dùng trong phần Settings).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public List<UserBlockResponse> getMyBlockedUsers() {
        return userBlockService.getMyBlockedUsers();
    }
}

