package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.StreakResponse;
import org.com.story.entity.*;
import org.com.story.exception.BadRequestException;
import org.com.story.repository.*;
import org.com.story.service.StreakService;
import org.com.story.service.NotificationService;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class StreakServiceImpl implements StreakService {

    private final UserStreakRepository userStreakRepository;
    private final RewardConfigRepository rewardConfigRepository;
    private final UserRewardHistoryRepository userRewardHistoryRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final WalletService walletService;
    @Lazy
    private final NotificationService notificationService;

    /** Các mốc streak đặc biệt */
    private static final Set<Integer> MILESTONE_DAYS = Set.of(3, 7, 14, 30, 100);

    @Override
    public StreakResponse checkIn() {
        User currentUser = userService.getCurrentUser();
        LocalDate today = LocalDate.now();

        UserStreak streak = userStreakRepository.findByUserId(currentUser.getId())
                .orElse(UserStreak.builder()
                        .user(currentUser)
                        .currentStreak(0)
                        .longestStreak(0)
                        .hasClaimedToday(false)
                        .build());

        if (Boolean.TRUE.equals(streak.getHasClaimedToday()) && today.equals(streak.getLastCheckInDate())) {
            throw new BadRequestException("Bạn đã check-in hôm nay rồi!");
        }

        // Tính streak
        if (streak.getLastCheckInDate() != null && streak.getLastCheckInDate().plusDays(1).equals(today)) {
            // Tiếp nối streak
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else if (streak.getLastCheckInDate() == null || !streak.getLastCheckInDate().equals(today)) {
            // Bắt đầu streak mới (vỡ streak hoặc lần đầu)
            streak.setCurrentStreak(1);
        }

        if (streak.getCurrentStreak() > streak.getLongestStreak()) {
            streak.setLongestStreak(streak.getCurrentStreak());
        }

        streak.setLastCheckInDate(today);
        streak.setHasClaimedToday(true);
        userStreakRepository.save(streak);

        // Tính coin thưởng dựa trên RewardConfig
        long coinEarned = calculateReward(currentUser, streak.getCurrentStreak());

        if (coinEarned > 0) {
            Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                    .orElseGet(() -> {
                        Wallet w = new Wallet();
                        w.setUser(currentUser);
                        w.setBalance(0L);
                        return walletRepository.save(w);
                    });
            wallet.setBalance(wallet.getBalance() + coinEarned);
            walletRepository.save(wallet);
            walletService.createTransaction(currentUser.getId(), coinEarned, "REWARD", null);
        }

        // Gửi thông báo check-in
        int currentStreakDay = streak.getCurrentStreak();
        try {
            if (MILESTONE_DAYS.contains(currentStreakDay)) {
                // Mốc đặc biệt → thông báo ăn mừng
                notificationService.sendNotification(
                        currentUser,
                        "STREAK_MILESTONE",
                        "🎉 Mốc streak " + currentStreakDay + " ngày!",
                        "Tuyệt vời! Bạn đã duy trì streak " + currentStreakDay
                                + " ngày liên tiếp và nhận được " + coinEarned + " coin thưởng đặc biệt!",
                        null,
                        "SYSTEM"
                );
            } else {
                // Check-in bình thường
                notificationService.sendNotification(
                        currentUser,
                        "STREAK_CHECKIN",
                        "✅ Check-in thành công! (Ngày " + currentStreakDay + ")",
                        "Bạn đã check-in hôm nay và nhận được " + coinEarned
                                + " coin. Tiếp tục duy trì streak nhé!",
                        null,
                        "SYSTEM"
                );
            }
        } catch (Exception ignored) {}

        return StreakResponse.builder()
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .lastCheckInDate(streak.getLastCheckInDate())
                .hasClaimedToday(true)
                .coinEarned(coinEarned)
                .message("Check-in thành công! Streak: " + streak.getCurrentStreak() + " ngày" +
                        (coinEarned > 0 ? ". Nhận " + coinEarned + " coin!" : ""))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StreakResponse getMyStreak() {
        User currentUser = userService.getCurrentUser();
        UserStreak streak = userStreakRepository.findByUserId(currentUser.getId())
                .orElse(UserStreak.builder()
                        .currentStreak(0)
                        .longestStreak(0)
                        .hasClaimedToday(false)
                        .build());

        return StreakResponse.builder()
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .lastCheckInDate(streak.getLastCheckInDate())
                .hasClaimedToday(Boolean.TRUE.equals(streak.getHasClaimedToday())
                        && LocalDate.now().equals(streak.getLastCheckInDate()))
                .coinEarned(0L)
                .message(null)
                .build();
    }

    /**
     * Tính coin thưởng: lấy RewardConfig có streakDay <= currentStreak lớn nhất mà user chưa nhận.
     */
    private long calculateReward(User user, int currentStreak) {
        List<RewardConfig> configs = rewardConfigRepository
                .findByStreakDayLessThanEqualOrderByStreakDayDesc(currentStreak);

        long totalCoin = 0;
        for (RewardConfig config : configs) {
            if (config.getStreakDay() == currentStreak &&
                    !userRewardHistoryRepository.existsByUserIdAndRewardConfigId(user.getId(), config.getId())) {
                totalCoin += config.getRewardCoin();
                userRewardHistoryRepository.save(UserRewardHistory.builder()
                        .user(user)
                        .rewardConfig(config)
                        .coinReceived(config.getRewardCoin())
                        .streakDayAtClaim(currentStreak)
                        .build());
            }
        }

        // Thưởng cơ bản mỗi ngày check-in: 5 coin
        totalCoin += 5;
        return totalCoin;
    }
}

