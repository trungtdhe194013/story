package org.com.story.config;

import lombok.RequiredArgsConstructor;
import org.com.story.entity.Chapter;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataInitializerHelper {

    private final UserRepository userRepository;

    @Transactional
    public void addPurchasedChaptersToUser(Long userId, List<Chapter> paidChapters, int limit) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getPurchasedChapters() == null) {
            user.setPurchasedChapters(new HashSet<>());
        }
        if (user.getPurchasedChapters().isEmpty()) {
            for (int i = 0; i < Math.min(limit, paidChapters.size()); i++) {
                user.getPurchasedChapters().add(paidChapters.get(i));
            }
            userRepository.save(user);
        }
    }

    @Transactional
    public void addFollowedStoriesToUser(Long userId, List<Story> stories, int fromIndex, int toIndex) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getFollowedStories() == null) {
            user.setFollowedStories(new HashSet<>());
        }
        if (user.getFollowedStories().isEmpty()) {
            for (int i = fromIndex; i < Math.min(toIndex, stories.size()); i++) {
                user.getFollowedStories().add(stories.get(i));
            }
            userRepository.save(user);
        }
    }
}

