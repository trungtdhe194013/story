package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.GiftRequest;
import org.com.story.dto.response.GiftResponse;
import org.com.story.entity.Gift;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.GiftRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.service.GiftService;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GiftServiceImpl implements GiftService {

    private final GiftRepository giftRepository;
    private final StoryRepository storyRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final WalletService walletService;

    @Override
    public GiftResponse sendGift(GiftRequest request) {
        User sender = userService.getCurrentUser();

        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new NotFoundException("Story not found"));

        User receiver = story.getAuthor();

        // Không thể tặng cho chính mình
        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("You cannot send a gift to yourself");
        }

        // Kiểm tra số dư
        Wallet senderWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (senderWallet.getBalance() < request.getAmount()) {
            throw new BadRequestException("Insufficient balance. You need " + request.getAmount() + " coins");
        }

        // Trừ tiền người gửi
        senderWallet.setBalance(senderWallet.getBalance() - request.getAmount());
        walletRepository.save(senderWallet);

        // Cộng tiền người nhận
        Wallet receiverWallet = walletRepository.findByUserId(receiver.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(receiver);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
        receiverWallet.setBalance(receiverWallet.getBalance() + request.getAmount());
        walletRepository.save(receiverWallet);

        // Lưu gift
        Gift gift = new Gift();
        gift.setFromUser(sender);
        gift.setToUser(receiver);
        gift.setStory(story);
        gift.setAmount(request.getAmount());

        Gift savedGift = giftRepository.save(gift);

        // Ghi giao dịch
        walletService.createTransaction(sender.getId(), -request.getAmount(), "GIFT", savedGift.getId());
        walletService.createTransaction(receiver.getId(), request.getAmount(), "GIFT", savedGift.getId());

        return mapToResponse(savedGift);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftResponse> getMySentGifts() {
        User currentUser = userService.getCurrentUser();
        return giftRepository.findByFromUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftResponse> getMyReceivedGifts() {
        User currentUser = userService.getCurrentUser();
        return giftRepository.findByToUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftResponse> getGiftsByStory(Long storyId) {
        return giftRepository.findByStoryIdOrderByCreatedAtDesc(storyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private GiftResponse mapToResponse(Gift gift) {
        return GiftResponse.builder()
                .id(gift.getId())
                .fromUserId(gift.getFromUser().getId())
                .fromUserName(gift.getFromUser().getFullName())
                .toUserId(gift.getToUser().getId())
                .toUserName(gift.getToUser().getFullName())
                .storyId(gift.getStory().getId())
                .storyTitle(gift.getStory().getTitle())
                .amount(gift.getAmount())
                .createdAt(gift.getCreatedAt())
                .build();
    }
}

