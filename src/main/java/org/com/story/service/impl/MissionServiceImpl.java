package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.MissionRequest;
import org.com.story.dto.response.MissionResponse;
import org.com.story.entity.Mission;
import org.com.story.entity.User;
import org.com.story.entity.UserMission;
import org.com.story.entity.Wallet;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.MissionRepository;
import org.com.story.repository.UserMissionRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.service.MissionService;
import org.com.story.service.NotificationService;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;
    private final UserMissionRepository userMissionRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final WalletService walletService;

    private NotificationService notificationService;

    @Autowired
    public void setNotificationService(@Lazy NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public MissionResponse createMission(MissionRequest request) {
        Mission mission = new Mission();
        mission.setName(request.getName());
        mission.setDescription(request.getDescription());
        mission.setRewardCoin(request.getRewardCoin());
        mission.setType(request.getType());
        mission.setAction(request.getAction());
        mission.setTargetCount(request.getTargetCount() != null ? request.getTargetCount() : 1);
        mission.setIcon(request.getIcon());
        mission.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        mission.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return mapToResponse(missionRepository.save(mission), false);
    }

    @Override
    public MissionResponse updateMission(Long id, MissionRequest request) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mission not found"));
        mission.setName(request.getName());
        mission.setDescription(request.getDescription());
        mission.setRewardCoin(request.getRewardCoin());
        mission.setType(request.getType());
        mission.setAction(request.getAction());
        if (request.getTargetCount() != null) mission.setTargetCount(request.getTargetCount());
        if (request.getIcon() != null) mission.setIcon(request.getIcon());
        if (request.getDisplayOrder() != null) mission.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) mission.setIsActive(request.getIsActive());
        return mapToResponse(missionRepository.save(mission), false);
    }

    @Override
    public void deleteMission(Long id) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mission not found"));
        List<UserMission> userMissions = userMissionRepository.findByMissionId(id);
        userMissionRepository.deleteAll(userMissions);
        missionRepository.delete(mission);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getAllMissions() {
        User currentUser = null;
        try { currentUser = userService.getCurrentUser(); } catch (Exception e) {}
        User finalCurrentUser = currentUser;
        return missionRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(mission -> {
                    boolean completed = false;
                    int progress = 0;
                    if (finalCurrentUser != null) {
                        var um = userMissionRepository
                                .findByUserIdAndMissionId(finalCurrentUser.getId(), mission.getId())
                                .orElse(null);
                        completed = um != null && Boolean.TRUE.equals(um.getCompleted());
                        progress = um != null && um.getProgress() != null ? um.getProgress() : 0;
                    }
                    MissionResponse r = mapToResponse(mission, completed);
                    r.setProgress(progress);
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getMyMissions() {
        User currentUser = userService.getCurrentUser();
        List<Mission> missions = missionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<UserMission> userMissions = userMissionRepository.findByUserId(currentUser.getId());
        return missions.stream()
                .map(mission -> {
                    UserMission um = userMissions.stream()
                            .filter(x -> x.getMission().getId().equals(mission.getId()))
                            .findFirst().orElse(null);
                    return mapToResponseWithProgress(mission, um);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANUAL COMPLETE (admin/test)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public MissionResponse completeMission(Long missionId) {
        User currentUser = userService.getCurrentUser();
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new NotFoundException("Mission not found"));
        if (userMissionRepository.existsByUserIdAndMissionIdAndCompletedTrue(currentUser.getId(), missionId)) {
            throw new BadRequestException("Mission already completed");
        }
        UserMission userMission = userMissionRepository
                .findByUserIdAndMissionId(currentUser.getId(), missionId)
                .orElseGet(() -> {
                    UserMission um = new UserMission();
                    um.setUser(currentUser);
                    um.setMission(mission);
                    return um;
                });
        userMission.setCompleted(true);
        userMission.setProgress(mission.getTargetCount() != null ? mission.getTargetCount() : 1);
        userMission.setCompletedAt(LocalDateTime.now());
        userMissionRepository.save(userMission);
        rewardCoins(currentUser, mission);
        return mapToResponse(mission, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTO TRACKING  (called by other services on user actions)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void trackMissionAction(String action) {
        User currentUser;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            return; // anonymous user — không track
        }

        List<Mission> matchingMissions = missionRepository.findByActionAndIsActiveTrue(action);
        if (matchingMissions.isEmpty()) return;

        for (Mission mission : matchingMissions) {
            try {
                processMissionProgress(currentUser, mission);
            } catch (Exception ignored) {
                // jangan sampai gagalnya tracking merusak flow utama
            }
        }
    }

    private void processMissionProgress(User user, Mission mission) {
        int targetCount = mission.getTargetCount() != null ? mission.getTargetCount() : 1;

        UserMission um = userMissionRepository
                .findByUserIdAndMissionId(user.getId(), mission.getId())
                .orElseGet(() -> {
                    UserMission newUm = new UserMission();
                    newUm.setUser(user);
                    newUm.setMission(mission);
                    newUm.setProgress(0);
                    newUm.setCompleted(false);
                    return newUm;
                });

        // Nếu đã hoàn thành rồi thì không tăng thêm
        if (Boolean.TRUE.equals(um.getCompleted())) return;

        int newProgress = (um.getProgress() != null ? um.getProgress() : 0) + 1;
        um.setProgress(newProgress);

        if (newProgress >= targetCount) {
            // ✅ Hoàn thành!
            um.setCompleted(true);
            um.setCompletedAt(LocalDateTime.now());
            userMissionRepository.save(um);

            rewardCoins(user, mission);

            try {
                notificationService.sendNotification(
                        user,
                        "MISSION_COMPLETED",
                        "🎯 Hoàn thành nhiệm vụ!",
                        "Bạn đã hoàn thành nhiệm vụ '" + mission.getName()
                                + "' và nhận được " + mission.getRewardCoin() + " coin. 🪙",
                        mission.getId(), "MISSION"
                );
            } catch (Exception ignored) {}
        } else {
            userMissionRepository.save(um);
        }
    }

    private void rewardCoins(User user, Mission mission) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(user);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
        wallet.setBalance(wallet.getBalance() + mission.getRewardCoin());
        walletRepository.save(wallet);
        try {
            walletService.createTransaction(user.getId(), mission.getRewardCoin(), "REWARD", mission.getId());
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DAILY RESET  (called by cron job at midnight)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void resetDailyMissions() {
        List<Mission> dailyMissions = missionRepository.findByType("DAILY");
        if (dailyMissions.isEmpty()) return;

        List<Long> dailyMissionIds = dailyMissions.stream()
                .map(Mission::getId).collect(Collectors.toList());

        // Lấy tất cả UserMission cho các daily mission
        List<UserMission> toReset = userMissionRepository.findAll().stream()
                .filter(um -> dailyMissionIds.contains(um.getMission().getId()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        for (UserMission um : toReset) {
            um.setCompleted(false);
            um.setProgress(0);
            um.setCompletedAt(null);
            um.setLastResetAt(now);
        }
        userMissionRepository.saveAll(toReset);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPER
    // ─────────────────────────────────────────────────────────────────────────

    private MissionResponse mapToResponse(Mission mission, boolean completed) {
        return MissionResponse.builder()
                .id(mission.getId())
                .name(mission.getName())
                .description(mission.getDescription())
                .rewardCoin(mission.getRewardCoin())
                .type(mission.getType())
                .action(mission.getAction())
                .targetCount(mission.getTargetCount() != null ? mission.getTargetCount() : 1)
                .icon(mission.getIcon())
                .displayOrder(mission.getDisplayOrder())
                .isActive(mission.getIsActive())
                .progress(0)
                .completed(completed)
                .completedAt(null)
                .build();
    }

    private MissionResponse mapToResponseWithProgress(Mission mission, UserMission userMission) {
        boolean completed = userMission != null && Boolean.TRUE.equals(userMission.getCompleted());
        int progress = userMission != null && userMission.getProgress() != null ? userMission.getProgress() : 0;
        return MissionResponse.builder()
                .id(mission.getId())
                .name(mission.getName())
                .description(mission.getDescription())
                .rewardCoin(mission.getRewardCoin())
                .type(mission.getType())
                .action(mission.getAction())
                .targetCount(mission.getTargetCount() != null ? mission.getTargetCount() : 1)
                .icon(mission.getIcon())
                .displayOrder(mission.getDisplayOrder())
                .isActive(mission.getIsActive())
                .progress(progress)
                .completed(completed)
                .completedAt(userMission != null ? userMission.getCompletedAt() : null)
                .build();
    }
}
