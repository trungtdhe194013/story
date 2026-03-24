package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.BlockUserRequest;
import org.com.story.dto.response.UserBlockResponse;
import org.com.story.entity.User;
import org.com.story.entity.UserBlock;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.UserBlockRepository;
import org.com.story.repository.UserRepository;
import org.com.story.service.UserBlockService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public UserBlockResponse blockUser(BlockUserRequest request) {
        User currentUser = userService.getCurrentUser();

        if (request.getUserId() == null) {
            throw new BadRequestException("userId is required");
        }

        if (currentUser.getId().equals(request.getUserId())) {
            throw new BadRequestException("Bạn không thể tự chặn chính mình");
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), targetUser.getId())) {
            throw new BadRequestException("Bạn đã chặn người dùng này rồi");
        }

        UserBlock block = UserBlock.builder()
                .blocker(currentUser)
                .blocked(targetUser)
                .reason(request.getReason())
                .build();

        UserBlock saved = userBlockRepository.save(block);
        return toResponse(saved);
    }

    @Override
    public void unblockUser(Long userId) {
        User currentUser = userService.getCurrentUser();

        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(currentUser.getId(), userId)
                .orElseThrow(() -> new NotFoundException("Bạn chưa chặn người dùng này"));

        userBlockRepository.delete(block);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBlockResponse> getMyBlockedUsers() {
        User currentUser = userService.getCurrentUser();
        return userBlockRepository.findByBlockerId(currentUser.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long authorId, Long userId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(authorId, userId);
    }

    private UserBlockResponse toResponse(UserBlock block) {
        return UserBlockResponse.builder()
                .id(block.getId())
                .blockedUserId(block.getBlocked().getId())
                .blockedUserName(block.getBlocked().getFullName())
                .blockedUserAvatar(block.getBlocked().getAvatarUrl())
                .reason(block.getReason())
                .createdAt(block.getCreatedAt())
                .build();
    }
}

