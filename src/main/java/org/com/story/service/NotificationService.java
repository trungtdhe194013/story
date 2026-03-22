package org.com.story.service;

import org.com.story.dto.response.NotificationResponse;
import org.com.story.entity.User;

import java.util.List;

public interface NotificationService {
    void sendNotification(User user, String type, String title, String message, Long refId, String refType);
    void sendToFollowers(Long storyId, String type, String title, String message, Long refId, String refType);
    List<NotificationResponse> getMyNotifications();
    long getUnreadCount();
    void markAllAsRead();
    NotificationResponse markAsRead(Long id);
    void deleteNotification(Long id);
}

