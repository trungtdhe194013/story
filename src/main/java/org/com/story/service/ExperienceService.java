package org.com.story.service;

import org.com.story.entity.User;

public interface ExperienceService {
    /** Award EXP to user for an action */
    void awardExperience(User user, long amount);
    
    /** Exp needed for a specific level */
    long getRequiredExperience(int level);

    // Predefined reward amounts
    long EXP_PER_CHAPTER_READ = 10;
    long EXP_PER_COMMENT = 5;
    long EXP_PER_RATING = 15;
}
