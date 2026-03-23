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
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public MissionResponse createMission(MissionRequest request) {
        Mission mission = new Mission();
        mission.setName(request.getName());
        mission.setRewardCoin(request.getRewardCoin());
        mission.setType(request.getType());

        Mission saved = missionRepository.save(mission);
        return mapToResponse(saved, false);
    }

    @Override
    public MissionResponse updateMission(Long id, MissionRequest request) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mission not found"));

        mission.setName(request.getName());
        mission.setRewardCoin(request.getRewardCoin());
        mission.setType(request.getType());

        Mission updated = missionRepository.save(mission);
        return mapToResponse(updated, false);
    }

    @Override
    public void deleteMission(Long id) {
        Mission mission = missionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mission not found"));
        // Xóa tất cả UserMission liên quan trước để tránh lỗi foreign key constraint
        List<UserMission> userMissions = userMissionRepository.findByMissionId(id);
        userMissionRepository.deleteAll(userMissions);
        missionRepository.delete(mission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getAllMissions() {
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // Anonymous user
        }

        User finalCurrentUser = currentUser;
        return missionRepository.findAll().stream()
                .map(mission -> {
                    boolean completed = false;
                    if (finalCurrentUser != null) {
                        completed = userMissionRepository
                                .existsByUserIdAndMissionIdAndCompletedTrue(finalCurrentUser.getId(), mission.getId());
                    }
                    return mapToResponse(mission, completed);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> getMyMissions() {
        User currentUser = userService.getCurrentUser();

        // Lấy tất cả mission đang active
        List<Mission> missions = missionRepository.findByIsActive(true);

        // Lấy tiến độ của user cho từng mission
        List<UserMission> userMissions = userMissionRepository.findByUserId(currentUser.getId());

        return missions.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() != null ? a.getDisplayOrder() : 0,
                        b.getDisplayOrder() != null ? b.getDisplayOrder() : 0))
                .map(mission -> {
                    UserMission um = userMissions.stream()
                            .filter(x -> x.getMission().getId().equals(mission.getId()))
                            .findFirst()
                            .orElse(null);
                    return mapToResponseWithProgress(mission, um);
                })
                .collect(Collectors.toList());
    }

    @Override
    public MissionResponse completeMission(Long missionId) {
        User currentUser = userService.getCurrentUser();

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new NotFoundException("Mission not found"));

        // Kiểm tra đã hoàn thành chưa
        if (userMissionRepository.existsByUserIdAndMissionIdAndCompletedTrue(currentUser.getId(), missionId)) {
            throw new BadRequestException("Mission already completed");
        }

        // Đánh dấu hoàn thành
        UserMission userMission = userMissionRepository
                .findByUserIdAndMissionId(currentUser.getId(), missionId)
                .orElseGet(() -> {
                    UserMission um = new UserMission();
                    um.setUser(currentUser);
                    um.setMission(mission);
                    return um;
                });

        userMission.setCompleted(true);
        userMissionRepository.save(userMission);

        // Thưởng coin vào ví
        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(currentUser);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });

        wallet.setBalance(wallet.getBalance() + mission.getRewardCoin());
        walletRepository.save(wallet);

        // Ghi giao dịch
        walletService.createTransaction(currentUser.getId(), mission.getRewardCoin(), "REWARD", mission.getId());

        return mapToResponse(mission, true);
    }

    private MissionResponse mapToResponse(Mission mission, boolean completed) {
        return MissionResponse.builder()
                .id(mission.getId())
                .name(mission.getName())
                .description(mission.getDescription())
                .rewardCoin(mission.getRewardCoin())
                .type(mission.getType())
                .targetCount(mission.getTargetCount())
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
                .targetCount(mission.getTargetCount())
                .icon(mission.getIcon())
                .displayOrder(mission.getDisplayOrder())
                .isActive(mission.getIsActive())
                .progress(progress)
                .completed(completed)
                .completedAt(userMission != null ? userMission.getCompletedAt() : null)
                .build();
    }
}

