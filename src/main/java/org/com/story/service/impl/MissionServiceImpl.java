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
                .rewardCoin(mission.getRewardCoin())
                .type(mission.getType())
                .completed(completed)
                .build();
    }
}

