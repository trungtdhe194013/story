package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.story.entity.User;
import org.com.story.repository.UserRepository;
import org.com.story.service.ExperienceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceServiceImpl implements ExperienceService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void awardExperience(User user, long amount) {
        if (amount <= 0) return;

        long currentExp = user.getExperience() != null ? user.getExperience() : 0L;
        int currentLevel = user.getLevel() != null ? user.getLevel() : 1;

        long nextExp = currentExp + amount;
        user.setExperience(nextExp);

        // Simple level-up logic: Level * 100 exp needed for next level
        // e.g. Level 1 needs 100 exp (total) for Level 2
        // Level 2 needs 200 more (total 300) for Level 3?
        // Let's use a simpler formula: RequiredTotalExp = level * (level + 1) * 50
        // L1: 1 * 2 * 50 = 100
        // L2: 2 * 3 * 50 = 300
        // L3: 3 * 4 * 50 = 600

        while (nextExp >= getRequiredExperience(currentLevel)) {
            currentLevel++;
            user.setLevel(currentLevel);
            
            log.info("User {} leveled up to level {}!", user.getUsername(), currentLevel);
        }

        userRepository.save(user);
    }

    @Override
    public long getRequiredExperience(int level) {
        // level 1 returns exp needed for level 2
        return (long) level * (level + 1) * 50;
    }
}
