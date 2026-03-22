package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.NotificationResponse;
import org.com.story.entity.Notification;
import org.com.story.entity.User;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.NotificationRepository;
import org.com.story.repository.UserRepository;
import org.com.story.service.NotificationService;
import org.com.story.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Async
    @Transactional
    public void sendNotification(User user, String type, String title, String message, Long refId, String refType) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .refId(refId)
                .refType(refType)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Async
    @Transactional
    public void sendToFollowers(Long storyId, String type, String title, String message, Long refId, String refType) {
        List<User> followers = userRepository.findFollowersByStoryId(storyId);
        if (followers.isEmpty()) return;

        List<Notification> notifications = followers.stream()
                .map(follower -> Notification.builder()
                        .user(follower)
                        .type(type)
                        .title(title)
                        .message(message)
                        .refId(refId)
                        .refType(refType)
                        .isRead(false)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        User currentUser = userService.getCurrentUser();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = userService.getCurrentUser();
        return notificationRepository.countUnreadByUserId(currentUser.getId());
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User currentUser = userService.getCurrentUser();
        notificationRepository.markAllAsRead(currentUser.getId());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        User currentUser = userService.getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo với id: " + id));
        notification.setIsRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void deleteNotification(Long id) {
        User currentUser = userService.getCurrentUser();
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo với id: " + id));
        notificationRepository.delete(notification);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .refId(n.getRefId())
                .refType(n.getRefType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
