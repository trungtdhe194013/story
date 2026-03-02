package org.com.story.config;

import lombok.RequiredArgsConstructor;
import org.com.story.common.AuthProvider;
import org.com.story.entity.*;
import org.com.story.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final CommentRepository commentRepository;
    private final WalletRepository walletRepository;
    private final MissionRepository missionRepository;
    private final AdminReviewRepository adminReviewRepository;
    private final ReportRepository reportRepository;
    private final GiftRepository giftRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final UserMissionRepository userMissionRepository;
    private final ChapterVersionRepository chapterVersionRepository;
    private final DataInitializerHelper dataInitializerHelper;

    @Bean
    CommandLineRunner initData() {
        return args -> {
            // Tạo roles
            Role adminRole = createRoleIfNotExists("ADMIN");
            Role authorRole = createRoleIfNotExists("AUTHOR");
            Role readerRole = createRoleIfNotExists("READER");
            Role reviewerRole = createRoleIfNotExists("REVIEWER");
            Role editorRole = createRoleIfNotExists("EDITOR");

            // Tạo users
            User admin = createUserIfNotExists("admin@story.com", "admin123", "Admin User", Set.of(adminRole));
            User author1 = createUserIfNotExists("author@story.com", "author123", "Nguyễn Văn A", Set.of(authorRole));
            User author2 = createUserIfNotExists("author2@story.com", "author123", "Trần Thị B", Set.of(authorRole));
            User author3 = createUserIfNotExists("author3@story.com", "author123", "Lê Văn C", Set.of(authorRole));
            User reader1 = createUserIfNotExists("reader@story.com", "reader123", "Phạm Thị D", Set.of(readerRole));
            User reader2 = createUserIfNotExists("reader2@story.com", "reader123", "Hoàng Văn E", Set.of(readerRole));
            User reader3 = createUserIfNotExists("reader3@story.com", "reader123", "Đỗ Thị F", Set.of(readerRole));
            User reviewer = createUserIfNotExists("reviewer@story.com", "reviewer123", "Reviewer User", Set.of(reviewerRole));
            User editor = createUserIfNotExists("editor@story.com", "editor123", "Editor User", Set.of(editorRole));

            // Tạo categories
            List<Category> categories = createCategories();

            // Tạo stories
            List<Story> stories = createStories(author1, author2, author3, categories);

            // Tạo chapters (bao gồm PUBLISHED, SCHEDULED, DRAFT)
            List<Chapter> chapters = createChapters(stories, editor);

            // Tạo ChapterVersions (lịch sử chỉnh sửa chương)
            createChapterVersions(chapters);

            // Tạo comments (bao gồm reply)
            createComments(chapters, reader1, reader2, reader3);

            // Tạo wallets cho users
            createWallets(List.of(admin, author1, author2, author3, reader1, reader2, reader3, reviewer, editor));

            // Tạo WalletTransactions (nạp tiền, mua chương, nhận gift)
            createWalletTransactions(List.of(reader1, reader2, reader3, author1, author2));

            // Tạo purchasedChapters cho readers
            createPurchasedChapters(chapters, reader1, reader2, reader3);

            // Tạo follows (readers theo dõi stories)
            createFollows(stories, reader1, reader2, reader3);

            // Tạo gifts (readers tặng xu cho tác giả)
            createGifts(stories, reader1, reader2, reader3, author1, author2, author3);

            // Tạo reports (báo cáo vi phạm)
            createReports(stories, chapters, reader1, reader2, reader3);

            // Tạo AdminReviews (admin duyệt story/chapter)
            createAdminReviews(stories, admin, reviewer);

            // Tạo WithdrawRequests (tác giả rút tiền)
            createWithdrawRequests(author1, author2, author3);

            // Tạo missions
            List<Mission> missions = createMissions();

            // Tạo UserMissions (missions đã hoàn thành & chưa hoàn thành)
            createUserMissions(missions, List.of(reader1, reader2, reader3, author1));

            System.out.println("==============================================");
            System.out.println("✅ DEMO DATA CREATED SUCCESSFULLY!");
            System.out.println("==============================================");
            System.out.println("DEMO ACCOUNTS:");
            System.out.println("1. Admin    - email: admin@story.com    | password: admin123");
            System.out.println("2. Author1  - email: author@story.com   | password: author123");
            System.out.println("3. Author2  - email: author2@story.com  | password: author123");
            System.out.println("4. Author3  - email: author3@story.com  | password: author123");
            System.out.println("5. Reader1  - email: reader@story.com   | password: reader123");
            System.out.println("6. Reader2  - email: reader2@story.com  | password: reader123");
            System.out.println("7. Reader3  - email: reader3@story.com  | password: reader123");
            System.out.println("8. Reviewer - email: reviewer@story.com | password: reviewer123");
            System.out.println("9. Editor   - email: editor@story.com   | password: editor123");
            System.out.println("==============================================");
            System.out.println("DATABASE STATS:");
            System.out.println("- Categories: " + categories.size());
            System.out.println("- Stories: " + stories.size());
            System.out.println("- Chapters: " + chapters.size());
            System.out.println("- Users: 9");
            System.out.println("==============================================");
        };
    }

    private Role createRoleIfNotExists(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }

    private User createUserIfNotExists(String email, String password, String fullName, Set<Role> roles) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setFullName(fullName);
                    user.setRoles(roles);
                    user.setEnabled(true);
                    user.setProvider(AuthProvider.LOCAL);
                    return userRepository.save(user);
                });
    }

    private List<Category> createCategories() {
        List<Category> categories = new ArrayList<>();
        String[] categoryNames = {
                "Fantasy", "Romance", "Mystery", "Thriller", "Science Fiction",
                "Horror", "Adventure", "Drama", "Comedy", "Action",
                "Historical", "Young Adult", "Contemporary", "Paranormal", "Urban Fantasy"
        };

        for (String name : categoryNames) {
            if (categoryRepository.findByName(name).isEmpty()) {
                Category category = new Category(null, name);
                categories.add(categoryRepository.save(category));
            } else {
                categoryRepository.findByName(name).ifPresent(categories::add);
            }
        }
        return categories;
    }

    private List<Story> createStories(User author1, User author2, User author3, List<Category> categories) {
        List<Story> stories = new ArrayList<>();

        // Helper để lấy category theo tên
        java.util.Map<String, Category> catMap = new java.util.HashMap<>();
        for (Category c : categories) catMap.put(c.getName(), c);

        // Stories của Author 1 - APPROVED
        stories.add(createStory(author1, "Hành Trình Phép Thuật",
            "Câu chuyện về một cậu bé phát hiện ra khả năng phép thuật đặc biệt và hành trình trở thành pháp sư vĩ đại nhất.",
            "APPROVED", Set.of(catMap.get("Fantasy"), catMap.get("Adventure")), 1250L));
        stories.add(createStory(author1, "Vương Quốc Thất Lạc",
            "Cuộc phiêu lưu tìm kiếm vương quốc huyền thoại đã mất tích hàng nghìn năm.",
            "APPROVED", Set.of(catMap.get("Fantasy"), catMap.get("Historical")), 890L));
        stories.add(createStory(author1, "Chiến Binh Ánh Sáng",
            "Một chiến binh trẻ tuổi chiến đấu chống lại thế lực hắc ám để bảo vệ thế giới.",
            "PENDING", Set.of(catMap.get("Action"), catMap.get("Fantasy")), 0L));

        // Stories của Author 2 - APPROVED
        stories.add(createStory(author2, "Tình Yêu Qua Mùa",
            "Một câu chuyện tình lãng mạn qua bốn mùa của năm.",
            "APPROVED", Set.of(catMap.get("Romance"), catMap.get("Drama")), 2100L));
        stories.add(createStory(author2, "Bí Ẩn Thành Phố",
            "Những bí mật đen tối ẩn giấu trong một thành phố hiện đại.",
            "APPROVED", Set.of(catMap.get("Mystery"), catMap.get("Thriller")), 750L));
        stories.add(createStory(author2, "Cuộc Đời Tôi",
            "Câu chuyện tự truyện về cuộc đời đầy thăng trầm.",
            "DRAFT", Set.of(catMap.get("Drama"), catMap.get("Contemporary")), 0L));
        // Story bị REJECTED để test nghiệp vụ admin reject
        stories.add(createStory(author2, "Truyện Vi Phạm",
            "Truyện có nội dung vi phạm quy định - dùng để test reject.",
            "REJECTED", Set.of(catMap.get("Drama")), 0L));

        // Stories của Author 3
        stories.add(createStory(author3, "Thám Tử Số 1",
            "Thám tử tài ba giải quyết những vụ án phức tạp nhất.",
            "APPROVED", Set.of(catMap.get("Mystery"), catMap.get("Thriller")), 1800L));
        stories.add(createStory(author3, "Du Hành Thời Gian",
            "Khám phá quá khứ và tương lai qua cỗ máy thời gian.",
            "APPROVED", Set.of(catMap.get("Science Fiction"), catMap.get("Adventure")), 3200L));
        stories.add(createStory(author3, "Ngôi Nhà Ma Ám",
            "Những sự kiện kinh hoàng xảy ra trong ngôi nhà bị ma ám.",
            "PENDING", Set.of(catMap.get("Horror"), catMap.get("Paranormal")), 0L));
        stories.add(createStory(author3, "Học Viện Siêu Nhiên",
            "Trường học dành cho những học sinh có năng lực đặc biệt.",
            "APPROVED", Set.of(catMap.get("Paranormal"), catMap.get("Young Adult")), 4500L));

        stories.removeIf(Objects::isNull);
        System.out.println("📚 Đã tạo " + stories.size() + " stories");
        return stories;
    }

    private Story createStory(User author, String title, String summary, String status,
                              Set<Category> cats, Long viewCount) {
        if (storyRepository.findAll().stream().noneMatch(s -> s.getTitle().equals(title))) {
            Story story = new Story();
            story.setAuthor(author);
            story.setTitle(title);
            story.setSummary(summary);
            story.setStatus(status);
            story.setCategories(cats);
            story.setViewCount(viewCount);
            story.setCoverUrl("https://via.placeholder.com/300x400?text=" + title.replaceAll(" ", "+"));
            return storyRepository.save(story);
        }
        return null;
    }

    private List<Chapter> createChapters(List<Story> stories, User editor) {
        List<Chapter> chapters = new ArrayList<>();

        for (Story story : stories) {
            if (story == null) continue;

            if ("APPROVED".equals(story.getStatus())) {
                int numChapters = 10 + (int)(Math.random() * 6);

                for (int i = 1; i <= numChapters; i++) {
                    Chapter chapter = new Chapter();
                    chapter.setStory(story);
                    chapter.setTitle("Chương " + i + ": " + generateChapterTitle(i));
                    chapter.setContent(generateChapterContent(story.getTitle(), i));
                    chapter.setChapterOrder(i);
                    chapter.setCoinPrice(i <= 3 ? 0 : 10 + (i * 5));
                    chapter.setPublishAt(LocalDateTime.now().minusDays(numChapters - i));

                    // Chương cuối để SCHEDULED (lên lịch, đã có editor), chương áp cuối để DRAFT (chưa có editor - chờ editor nhận)
                    if (i == numChapters) {
                        chapter.setStatus("SCHEDULED");
                        chapter.setPublishAt(LocalDateTime.now().plusDays(3)); // lên lịch 3 ngày sau
                        chapter.setEditor(editor);
                    } else if (i == numChapters - 1) {
                        chapter.setStatus("DRAFT");
                        // Không gán editor → để editor tự nhận qua API assign
                    } else {
                        chapter.setStatus("PUBLISHED");
                    }

                    chapters.add(chapterRepository.save(chapter));
                }
            }
        }

        System.out.println("📄 Đã tạo " + chapters.size() + " chapters");
        return chapters;
    }

    // ===================== CHAPTER VERSIONS =====================
    private void createChapterVersions(List<Chapter> chapters) {
        if (chapterVersionRepository.count() > 0) return;

        int count = 0;
        // Tạo 2-3 versions cho 10 chương đầu tiên (mô phỏng editor chỉnh sửa)
        for (int i = 0; i < Math.min(10, chapters.size()); i++) {
            Chapter chapter = chapters.get(i);
            int numVersions = 2 + (i % 2); // 2 hoặc 3 versions
            for (int v = 1; v <= numVersions; v++) {
                ChapterVersion cv = new ChapterVersion();
                cv.setChapter(chapter);
                cv.setVersion(v);
                cv.setContent("[Version " + v + "] " + chapter.getContent()
                        + "\n\n--- Chỉnh sửa lần " + v + " bởi Editor ---");
                chapterVersionRepository.save(cv);
                count++;
            }
        }
        System.out.println("📝 Đã tạo " + count + " chapter versions");
    }

    // ===================== FOLLOWS =====================
    private void createFollows(List<Story> stories, User reader1, User reader2, User reader3) {
        List<Story> approvedStories = stories.stream()
                .filter(s -> s != null && "APPROVED".equals(s.getStatus()))
                .toList();

        if (approvedStories.isEmpty()) return;

        int size = approvedStories.size();
        // Reader1 follow 5 truyện đầu
        dataInitializerHelper.addFollowedStoriesToUser(reader1.getId(), approvedStories, 0, Math.min(5, size));
        // Reader2 follow 3 truyện
        dataInitializerHelper.addFollowedStoriesToUser(reader2.getId(), approvedStories, 0, Math.min(3, size));
        // Reader3 follow 2 truyện cuối
        dataInitializerHelper.addFollowedStoriesToUser(reader3.getId(), approvedStories, Math.max(0, size - 2), size);

        System.out.println("❤️ Đã tạo follow data cho 3 readers");
    }

    // ===================== PURCHASED CHAPTERS =====================
    private void createPurchasedChapters(List<Chapter> chapters, User reader1, User reader2, User reader3) {
        List<Chapter> paidChapters = chapters.stream()
                .filter(c -> c != null && c.getCoinPrice() != null && c.getCoinPrice() > 0
                        && "PUBLISHED".equals(c.getStatus()))
                .toList();

        if (paidChapters.isEmpty()) return;

        dataInitializerHelper.addPurchasedChaptersToUser(reader1.getId(), paidChapters, 8);
        dataInitializerHelper.addPurchasedChaptersToUser(reader2.getId(), paidChapters, 5);
        dataInitializerHelper.addPurchasedChaptersToUser(reader3.getId(), paidChapters, 3);

        System.out.println("🛒 Đã tạo purchased chapters cho 3 readers");
    }

    // ===================== WALLET TRANSACTIONS =====================
    private void createWalletTransactions(List<User> users) {
        if (walletTransactionRepository.count() > 0) return;

        for (User user : users) {
            // TOPUP - nạp tiền
            saveTransaction(user, 500L, "TOPUP", null);
            saveTransaction(user, 1000L, "TOPUP", null);

            // BUY - mua chương
            saveTransaction(user, -15L, "BUY", 1L);
            saveTransaction(user, -20L, "BUY", 2L);
            saveTransaction(user, -25L, "BUY", 3L);

            // REWARD - nhận thưởng từ mission
            saveTransaction(user, 10L, "REWARD", null);
            saveTransaction(user, 5L, "REWARD", null);

            // GIFT - tặng/nhận xu
            saveTransaction(user, 100L, "GIFT", null);
        }

        System.out.println("💳 Đã tạo wallet transactions");
    }

    private void saveTransaction(User user, Long amount, String type, Long refId) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setRefId(refId);
        walletTransactionRepository.save(tx);
    }

    // ===================== GIFTS =====================
    private void createGifts(List<Story> stories, User reader1, User reader2, User reader3,
                             User author1, User author2, User author3) {
        if (giftRepository.count() > 0) return;

        List<Story> approvedStories = stories.stream()
                .filter(s -> s != null && "APPROVED".equals(s.getStatus()))
                .toList();

        if (approvedStories.isEmpty()) return;

        // Reader1 tặng author1 qua story 1
        saveGift(reader1, author1, approvedStories.get(0), 200L);
        saveGift(reader1, author1, approvedStories.get(0), 500L);

        // Reader2 tặng author2 qua story khác
        if (approvedStories.size() > 2) {
            saveGift(reader2, author2, approvedStories.get(2), 100L);
        }

        // Reader3 tặng author3
        if (approvedStories.size() > 4) {
            saveGift(reader3, author3, approvedStories.get(4), 300L);
            saveGift(reader1, author3, approvedStories.get(4), 150L);
        }

        // Reader2 tặng author1 truyện Fantasy
        saveGift(reader2, author1, approvedStories.get(0), 250L);

        System.out.println("🎁 Đã tạo gifts");
    }

    private void saveGift(User from, User to, Story story, Long amount) {
        Gift gift = new Gift();
        gift.setFromUser(from);
        gift.setToUser(to);
        gift.setStory(story);
        gift.setAmount(amount);
        giftRepository.save(gift);
    }

    // ===================== REPORTS =====================
    private void createReports(List<Story> stories, List<Chapter> chapters,
                               User reader1, User reader2, User reader3) {
        if (reportRepository.count() > 0) return;

        List<Story> approvedStories = stories.stream()
                .filter(s -> s != null && "APPROVED".equals(s.getStatus()))
                .toList();

        // Report STORY - PENDING (chưa xử lý)
        if (approvedStories.size() > 3) {
            saveReport(reader1, "STORY", approvedStories.get(3).getId(),
                    "Truyện có nội dung không phù hợp, vi phạm quy định cộng đồng.", "PENDING");
        }

        // Report STORY - RESOLVED (đã xử lý)
        if (approvedStories.size() > 1) {
            saveReport(reader2, "STORY", approvedStories.get(1).getId(),
                    "Nội dung sao chép từ tác giả khác mà không xin phép.", "RESOLVED");
        }

        // Report CHAPTER - PENDING
        List<Chapter> publishedChapters = chapters.stream()
                .filter(c -> c != null && "PUBLISHED".equals(c.getStatus()))
                .toList();
        if (!publishedChapters.isEmpty()) {
            saveReport(reader3, "CHAPTER", publishedChapters.get(0).getId(),
                    "Chương này có nội dung bạo lực quá mức.", "PENDING");
        }
        if (publishedChapters.size() > 2) {
            saveReport(reader1, "CHAPTER", publishedChapters.get(2).getId(),
                    "Nội dung spam, lặp lại nhiều lần.", "PENDING");
        }

        // Report COMMENT - PENDING
        saveReport(reader2, "COMMENT", 1L,
                "Bình luận này chứa ngôn từ xúc phạm.", "PENDING");
        saveReport(reader3, "COMMENT", 2L,
                "Spam bình luận quảng cáo.", "RESOLVED");

        System.out.println("🚨 Đã tạo reports (PENDING & RESOLVED)");
    }

    private void saveReport(User reporter, String targetType, Long targetId, String reason, String status) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(reason);
        report.setStatus(status);
        reportRepository.save(report);
    }

    // ===================== ADMIN REVIEWS =====================
    private void createAdminReviews(List<Story> stories, User admin, User reviewer) {
        if (adminReviewRepository.count() > 0) return;

        List<Story> pendingStories = stories.stream()
                .filter(s -> s != null && "PENDING".equals(s.getStatus()))
                .toList();

        List<Story> approvedStories = stories.stream()
                .filter(s -> s != null && "APPROVED".equals(s.getStatus()))
                .toList();

        List<Story> rejectedStories = stories.stream()
                .filter(s -> s != null && "REJECTED".equals(s.getStatus()))
                .toList();

        // Reviewer đã duyệt (APPROVE) các story APPROVED
        for (int i = 0; i < Math.min(3, approvedStories.size()); i++) {
            saveAdminReview(reviewer, "STORY", approvedStories.get(i).getId(),
                    "APPROVE", "Nội dung phù hợp, chất lượng tốt. Đã duyệt.");
        }

        // Admin đã REJECT story vi phạm
        for (Story s : rejectedStories) {
            saveAdminReview(admin, "STORY", s.getId(),
                    "REJECT", "Nội dung vi phạm quy định cộng đồng. Từ chối xuất bản.");
        }

        // Reviewer chưa xử lý story PENDING (tạo review lưu ý cần review)
        for (Story s : pendingStories) {
            saveAdminReview(reviewer, "STORY", s.getId(),
                    "PENDING_REVIEW", "Đang trong quá trình xem xét.");
        }

        // Admin đã duyệt CHAPTER
        saveAdminReview(admin, "CHAPTER", 1L,
                "APPROVE", "Nội dung chương phù hợp.");
        saveAdminReview(reviewer, "CHAPTER", 2L,
                "APPROVE", "Chương hay, đã duyệt.");
        saveAdminReview(admin, "CHAPTER", 3L,
                "REJECT", "Chương có nội dung vi phạm, yêu cầu chỉnh sửa.");

        System.out.println("🔍 Đã tạo admin reviews");
    }

    private void saveAdminReview(User admin, String targetType, Long targetId, String action, String note) {
        AdminReview review = new AdminReview();
        review.setAdmin(admin);
        review.setTargetType(targetType);
        review.setTargetId(targetId);
        review.setAction(action);
        review.setNote(note);
        adminReviewRepository.save(review);
    }

    // ===================== WITHDRAW REQUESTS =====================
    private void createWithdrawRequests(User author1, User author2, User author3) {
        if (withdrawRequestRepository.count() > 0) return;

        // Author1: 1 APPROVED, 1 PENDING
        saveWithdrawRequest(author1, 500L, "APPROVED");
        saveWithdrawRequest(author1, 300L, "PENDING");

        // Author2: 1 PENDING
        saveWithdrawRequest(author2, 800L, "PENDING");

        // Author3: 1 APPROVED, 1 REJECTED
        saveWithdrawRequest(author3, 1000L, "APPROVED");
        saveWithdrawRequest(author3, 200L, "REJECTED");

        System.out.println("💸 Đã tạo withdraw requests (PENDING, APPROVED, REJECTED)");
    }

    private void saveWithdrawRequest(User user, Long amount, String status) {
        WithdrawRequest req = new WithdrawRequest();
        req.setUser(user);
        req.setAmount(amount);
        req.setStatus(status);
        withdrawRequestRepository.save(req);
    }

    // ===================== MISSIONS & USER MISSIONS =====================
    private List<Mission> createMissions() {
        if (missionRepository.count() > 0) {
            return missionRepository.findAll();
        }

        List<Mission> missions = new ArrayList<>();
        missions.add(missionRepository.save(createMission("Đăng nhập hàng ngày", 10L, "DAILY")));
        missions.add(missionRepository.save(createMission("Đọc 1 chương truyện", 5L, "READ")));
        missions.add(missionRepository.save(createMission("Đọc 5 chương truyện", 20L, "READ")));
        missions.add(missionRepository.save(createMission("Bình luận 1 chương", 5L, "DAILY")));
        missions.add(missionRepository.save(createMission("Theo dõi 1 truyện mới", 10L, "DAILY")));
        missions.add(missionRepository.save(createMission("Mua 1 chương truyện", 15L, "READ")));
        missions.add(missionRepository.save(createMission("Tặng quà cho tác giả", 20L, "DAILY")));
        System.out.println("🎯 Đã tạo " + missions.size() + " missions");
        return missions;
    }

    private void createUserMissions(List<Mission> missions, List<User> users) {
        if (userMissionRepository.count() > 0) return;

        for (User user : users) {
            for (int i = 0; i < missions.size(); i++) {
                Mission mission = missions.get(i);
                UserMission um = new UserMission();
                um.setUser(user);
                um.setMission(mission);
                // User đầu hoàn thành tất cả, user còn lại hoàn thành xen kẽ
                boolean completed = (users.indexOf(user) == 0) || (i % 2 == 0);
                um.setCompleted(completed);
                userMissionRepository.save(um);
            }
        }
        System.out.println("🏆 Đã tạo user missions (completed & pending)");
    }

    private String generateChapterTitle(int chapterNum) {
        String[] titles = {
            "Khởi Đầu", "Cuộc Gặp Gỡ", "Bí Mật Được Tiết Lộ", "Thử Thách Đầu Tiên",
            "Sức Mạnh Mới", "Người Bạn Đồng Hành", "Nguy Hiểm Rình Rập", "Trận Chiến",
            "Chuyển Biến Bất Ngờ", "Hy Vọng Mới", "Quyết Định Quan Trọng", "Hành Trình Tiếp Tục",
            "Bí Ẩn Sâu Thẳm", "Đối Đầu", "Chiến Thắng"
        };
        return titles[chapterNum % titles.length];
    }

    private String generateChapterContent(String storyTitle, int chapterNum) {
        return String.format(
                """
                        Đây là nội dung của chương %d trong câu chuyện '%s'.
                        
                        Câu chuyện đang dần dần hấp dẫn hơn. Nhân vật chính đang phải đối mặt với những thử thách mới. \
                        Những bí mật từ quá khứ đang dần được hé lộ.
                        
                        Trong chương này, độc giả sẽ được khám phá thêm về thế giới và các nhân vật. \
                        Những diễn biến bất ngờ sẽ làm cho câu chuyện trở nên kịch tính hơn bao giờ hết.
                        
                        Hãy tiếp tục theo dõi để biết những gì sẽ xảy ra tiếp theo!
                        
                        [Nội dung đầy đủ của chương %d...]""",
            chapterNum, storyTitle, chapterNum
        );
    }

    private void createComments(List<Chapter> chapters, User reader1, User reader2, User reader3) {
        List<User> readers = List.of(reader1, reader2, reader3);
        String[] commentTexts = {
            "Chương này hay quá! Rất mong chờ chương tiếp theo.",
            "Tác giả viết rất hay, cảm ơn tác giả!",
            "Tuyệt vời! Không thể bỏ qua series này được.",
            "Đoạn này hồi hộp quá!",
            "Nhân vật chính thật tuyệt vời!",
            "Cốt truyện rất cuốn hút!",
            "Chờ chương mới lâu quá ạ!",
            "Đỉnh của chóp!",
            "Hay nhưng hơi ngắn!",
            "Mong tác giả ra chương nhanh hơn.",
            "Chương này cảm động quá!",
            "Không ngờ lại có twist này!",
            "Xuất sắc! 5 sao!",
            "Đọc mãi không chán!",
            "Recommend cho mọi người đọc!"
        };

        List<Chapter> publishedChapters = chapters.stream()
                .filter(c -> c != null && "PUBLISHED".equals(c.getStatus()))
                .toList();

        List<Comment> savedComments = new ArrayList<>();

        // Tạo 3-5 comments cho mỗi chapter (tối đa 20 chapters)
        for (int i = 0; i < Math.min(publishedChapters.size(), 20); i++) {
            Chapter chapter = publishedChapters.get(i);
            int numComments = 3 + (int)(Math.random() * 3);

            for (int j = 0; j < numComments; j++) {
                Comment comment = new Comment();
                comment.setUser(readers.get(j % readers.size()));
                comment.setChapter(chapter);
                comment.setContent(commentTexts[(i * numComments + j) % commentTexts.length]);
                comment.setParent(null);
                savedComments.add(commentRepository.save(comment));
            }
        }

        // Tạo REPLY comments (comment lồng nhau)
        if (savedComments.size() >= 3) {
            String[] replyTexts = {
                "Mình cũng nghĩ vậy!", "Đồng ý với bạn!", "Hay thật đấy bạn ơi!",
                "Mình cũng thích đoạn này!", "Bạn tinh tế quá!", "Cảm ơn bạn đã chia sẻ!"
            };
            for (int i = 0; i < Math.min(6, savedComments.size()); i++) {
                Comment parent = savedComments.get(i);
                Comment reply = new Comment();
                reply.setUser(readers.get((i + 1) % readers.size()));
                reply.setChapter(parent.getChapter());
                reply.setContent(replyTexts[i % replyTexts.length]);
                reply.setParent(parent);
                commentRepository.save(reply);
            }
        }

        System.out.println("💬 Đã tạo comments và replies");
    }

    private void createWallets(List<User> users) {
        for (User user : users) {
            if (walletRepository.findByUserId(user.getId()).isEmpty()) {
                User managedUser = userRepository.findById(user.getId())
                        .orElseThrow(() -> new RuntimeException("User not found: " + user.getId()));
                Wallet wallet = new Wallet();
                wallet.setUser(managedUser);
                boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
                boolean isAuthor = user.getRoles().stream().anyMatch(r -> r.getName().equals("AUTHOR"));
                wallet.setBalance(isAdmin ? 9999L : isAuthor ? 1000L : 500L);
                walletRepository.save(wallet);
            }
        }
        System.out.println("💰 Đã tạo wallets cho " + users.size() + " users");
    }


    private Mission createMission(String name, Long reward, String type) {
        Mission mission = new Mission();
        mission.setName(name);
        mission.setRewardCoin(reward);
        mission.setType(type);
        return mission;
    }
}
