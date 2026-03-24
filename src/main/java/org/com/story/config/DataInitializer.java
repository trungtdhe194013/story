package org.com.story.config;

import lombok.RequiredArgsConstructor;
import org.com.story.common.AuthProvider;
import org.com.story.common.ImageAssets;
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
            // ✅ Cập nhật ảnh cũ (via.placeholder.com) sang ảnh thật
            migrateImages();
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
                    user.setAvatarUrl(ImageAssets.randomUserAvatar());
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
                Category category = new Category();
                category.setName(name);
                category.setSlug(name.toLowerCase().replaceAll("\\s+", "-"));
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

        // ═══════════════════════════════════════════════════════════════════════
        // AUTHOR 1 — Nguyễn Văn A  (7 truyện)
        // ═══════════════════════════════════════════════════════════════════════
        stories.add(createStory(author1, "Lục Địa Rồng Thiêng",
            "Tại lục địa Vạn Linh, nơi loài rồng và con người cùng tồn tại, chàng trai mồ côi Minh Khôi vô tình đánh thức "
            + "một con rồng cổ đại đang ngủ quên dưới lòng đất ngàn năm. Từ đó, cậu bị cuốn vào cuộc chiến giữa năm "
            + "vương quốc tranh giành quyền kiểm soát sức mạnh rồng thiêng. Với sự giúp đỡ của nữ pháp sư Linh Lan "
            + "và kiếm sĩ mù Trần Vũ, Minh Khôi phải tìm cách hàn gắn mối quan hệ giữa con người và rồng trước khi "
            + "thế giới rơi vào hỗn loạn.",
            "APPROVED", Set.of(catMap.get("Fantasy"), catMap.get("Adventure")), 18500L));

        stories.add(createStory(author1, "Mật Mã Sài Gòn",
            "Năm 1967, giữa lòng Sài Gòn hoa lệ, điệp viên Phạm Quốc Bảo nhận nhiệm vụ giải mã một bức thư được giấu "
            + "trong bức tranh sơn dầu của họa sĩ nổi tiếng Trần Văn Thanh. Bức thư chứa tọa độ kho vàng bí mật mà "
            + "cả hai phe đều muốn chiếm đoạt. Khi danh tính bị lộ, Bảo phải chạy đua với thời gian, lách qua mạng "
            + "lưới gián điệp dày đặc, đồng thời đối mặt với sự phản bội từ chính người mà anh tin tưởng nhất.",
            "APPROVED", Set.of(catMap.get("Thriller"), catMap.get("Historical")), 12300L));

        stories.add(createStory(author1, "Tiệm Cà Phê Cuối Phố",
            "Hà Linh, một cô gái 25 tuổi, quyết định bỏ công việc văn phòng ngột ngạt để mở một tiệm cà phê nhỏ "
            + "ở cuối con phố cũ tại Hà Nội. Tại đây, cô gặp Đức — chàng nhạc sĩ đường phố bí ẩn, luôn đến "
            + "đúng giờ đóng cửa để gọi một ly cà phê đen. Qua từng mùa, qua từng giai điệu guitar rỉ rả trong đêm, "
            + "hai tâm hồn cô đơn dần tìm thấy nhau. Nhưng Đức đang che giấu một bí mật từ quá khứ.",
            "APPROVED", Set.of(catMap.get("Romance"), catMap.get("Contemporary")), 34200L));

        stories.add(createStory(author1, "Bóng Ma Căn Biệt Thự",
            "Gia đình nhà Trần chuyển đến một căn biệt thự Pháp cổ ở Đà Lạt với giá thuê rẻ bất ngờ. Ban đầu, "
            + "mọi thứ tưởng như hoàn hảo — sương mù bao phủ, vườn hoa dã quỳ, và sự tĩnh lặng tuyệt đối. "
            + "Nhưng đến đêm thứ ba, cô con gái út bắt đầu nói chuyện với 'bạn tưởng tượng' không ai nhìn thấy. "
            + "Từng vết nứt trên tường bắt đầu rỉ ra chất lỏng đỏ thẫm. Và tiếng khóc trẻ con vang lên từ tầng hầm "
            + "đã bị bít kín hàng chục năm.",
            "APPROVED", Set.of(catMap.get("Horror"), catMap.get("Mystery")), 9800L));

        stories.add(createStory(author1, "Kiếm Thần Truyền Kỳ",
            "Trong thế giới Tu Tiên, nơi các môn phái tranh đấu để trở thành bá chủ, Lâm Phong — một đệ tử "
            + "tầm thường của Thanh Vân Phái — vô tình nhặt được mảnh kiếm gãy chứa linh hồn của Kiếm Thần "
            + "thời thượng cổ. Từ một kẻ bị khinh thường, Lâm Phong bắt đầu tu luyện kiếm thuật thất truyền, "
            + "vượt qua các kỳ thi môn phái, và dần hé lộ âm mưu diệt thế giới của Ma Giáo.",
            "APPROVED", Set.of(catMap.get("Fantasy"), catMap.get("Action")), 45600L));

        stories.add(createStory(author1, "Hành Tinh Xanh Cuối Cùng",
            "Năm 2487, Trái Đất đã không còn khả năng sinh sống. Con tàu Hy Vọng mang theo 10.000 người "
            + "cuối cùng của loài người bay qua vũ trụ tìm kiếm ngôi nhà mới. Kỹ sư trẻ Nguyễn Thiên An phát hiện "
            + "tín hiệu lạ từ một hành tinh chưa được khám phá. Khi con tàu đổ bộ, họ nhận ra mình không phải là "
            + "sinh vật đầu tiên đặt chân đến đây — và những gì chờ đợi họ còn đáng sợ hơn cả sự tuyệt chủng.",
            "APPROVED", Set.of(catMap.get("Science Fiction"), catMap.get("Adventure")), 28900L));

        stories.add(createStory(author1, "Chiến Binh Ánh Sáng",
            "Một chiến binh trẻ tuổi từ ngôi làng nhỏ bé phát hiện ra mình sở hữu sức mạnh ánh sáng cổ đại. "
            + "Khi bóng tối lan rộng khắp vương quốc, cậu phải tập hợp những chiến binh từ năm bộ tộc "
            + "để đối đầu với Chúa Tể Hắc Ám đã ngủ quên ngàn năm, nay đang tỉnh giấc.",
            "PENDING", Set.of(catMap.get("Action"), catMap.get("Fantasy")), 0L));

        // ═══════════════════════════════════════════════════════════════════════
        // AUTHOR 2 — Trần Thị B  (7 truyện)
        // ═══════════════════════════════════════════════════════════════════════
        stories.add(createStory(author2, "Ngọn Gió Mùa Hè",
            "Câu chuyện tình yêu tuổi học trò giữa Khánh — chàng trai lớp trưởng lạnh lùng — và Hạ Vy, "
            + "cô nàng chuyển trường từ Sài Gòn ra Hà Nội. Mùa hè năm lớp 11, một chuyến đi biển Quy Nhơn "
            + "cùng lớp đã thay đổi tất cả. Qua những bức thư tay, những lần trốn học lên sân thượng ngắm hoàng hôn, "
            + "họ nhận ra tình yêu đầu đời chưa bao giờ là đơn giản. Kết thúc mùa hè, liệu họ có dũng cảm "
            + "nói lời yêu thương?",
            "APPROVED", Set.of(catMap.get("Romance"), catMap.get("Young Adult")), 52100L));

        stories.add(createStory(author2, "Vụ Án Hồ Gươm",
            "Thám tử tư Hoàng Nam nhận được cuộc gọi lúc 2 giờ sáng: một xác chết nổi trên Hồ Gươm đúng "
            + "đêm Trung Thu. Nạn nhân là giáo sư sử học nổi tiếng — người vừa công bố phát hiện chấn động "
            + "về lăng mộ bí mật dưới lòng hồ. Khi điều tra, Nam phát hiện ra một mạng lưới buôn bán cổ vật "
            + "xuyên quốc gia, và bản thân mình cũng trở thành mục tiêu tiếp theo.",
            "APPROVED", Set.of(catMap.get("Mystery"), catMap.get("Thriller")), 15600L));

        stories.add(createStory(author2, "Mẹ Đơn Thân",
            "Ngọc Ánh, 30 tuổi, ly hôn và một mình nuôi con gái 5 tuổi tại Sài Gòn. Cô làm việc ở hai nơi — "
            + "buổi sáng là nhân viên kế toán, buổi tối bán hàng online. Cuộc sống đầy khó khăn nhưng nụ cười "
            + "của con gái là động lực để cô bước tiếp. Khi gặp lại Tuấn — người yêu cũ thời đại học — tại buổi "
            + "họp phụ huynh trường con, quá khứ và hiện tại va chạm. Liệu cô có cho phép mình yêu thêm lần nữa?",
            "APPROVED", Set.of(catMap.get("Drama"), catMap.get("Contemporary")), 38700L));

        stories.add(createStory(author2, "Đảo Hoang Sinh Tồn",
            "Chuyến bay VN-372 từ Hà Nội đi Singapore gặp nạn, rơi xuống một hòn đảo không có trên bản đồ. "
            + "12 hành khách sống sót phải đối mặt với thiên nhiên khắc nghiệt, thú dữ, và quan trọng nhất — "
            + "sự nghi ngờ lẫn nhau. Khi đồ ăn cạn dần và hy vọng được cứu ngày càng mờ nhạt, bản chất thật "
            + "của mỗi con người bắt đầu lộ ra. Ai là người đáng tin? Ai đang giấu bí mật chết người?",
            "APPROVED", Set.of(catMap.get("Thriller"), catMap.get("Adventure")), 21300L));

        stories.add(createStory(author2, "Hậu Trường Showbiz",
            "Trâm Anh, 22 tuổi, bước chân vào làng giải trí với giấc mơ trở thành ca sĩ. Nhưng đằng sau "
            + "ánh đèn sân khấu lấp lánh là một thế giới tàn khốc — hợp đồng nô lệ, scandal bị dàn dựng, "
            + "và luật chơi ngầm mà không ai dám nói. Khi bị ép phải lựa chọn giữa danh vọng và nhân phẩm, "
            + "Trâm Anh quyết định phanh phui tất cả, dù biết rằng cô sẽ đối mặt với sự trả thù.",
            "APPROVED", Set.of(catMap.get("Drama"), catMap.get("Contemporary")), 67800L));

        stories.add(createStory(author2, "Cuộc Đời Tôi",
            "Câu chuyện tự truyện về cuộc đời đầy thăng trầm của một thanh niên lớn lên từ vùng quê nghèo "
            + "miền Trung, vượt qua nghịch cảnh để trở thành doanh nhân thành đạt tại Sài Gòn.",
            "DRAFT", Set.of(catMap.get("Drama"), catMap.get("Contemporary")), 0L));

        stories.add(createStory(author2, "Truyện Vi Phạm",
            "Truyện có nội dung vi phạm quy định - dùng để test reject.",
            "REJECTED", Set.of(catMap.get("Drama")), 0L));

        // ═══════════════════════════════════════════════════════════════════════
        // AUTHOR 3 — Lê Văn C  (6 truyện)
        // ═══════════════════════════════════════════════════════════════════════
        stories.add(createStory(author3, "Thám Tử Tâm Linh",
            "Đoàn Minh Trí, thám tử 35 tuổi, có khả năng đặc biệt: anh có thể nhìn thấy dấu vết tâm linh "
            + "tại hiện trường vụ án. Khi cảnh sát bế tắc trước loạt vụ mất tích bí ẩn ở Huế, họ phải nhờ đến "
            + "Trí. Mỗi nạn nhân đều nhận được một bức thư cổ trước khi biến mất, và tất cả đều liên quan đến "
            + "một ngôi đền bị phong ấn. Trí phải đối mặt với thứ gì đó vượt xa khả năng lý giải.",
            "APPROVED", Set.of(catMap.get("Mystery"), catMap.get("Paranormal")), 22400L));

        stories.add(createStory(author3, "Cổng Thời Gian 2045",
            "Tiến sĩ Trần Quang Huy phát minh ra cỗ máy du hành thời gian tại Viện Khoa học Việt Nam. "
            + "Chuyến du hành đầu tiên đưa anh về Sài Gòn năm 1975. Tại đây, anh vô tình gặp ông nội mình — "
            + "một người lính trẻ. Khi cố gắng quay về, Huy phát hiện cỗ máy bị hỏng. Anh buộc phải sống "
            + "trong quá khứ, cẩn thận không thay đổi lịch sử, trong khi tìm cách sửa chữa cỗ máy từ "
            + "công nghệ của 70 năm trước.",
            "APPROVED", Set.of(catMap.get("Science Fiction"), catMap.get("Historical")), 41200L));

        stories.add(createStory(author3, "Học Viện Dị Năng",
            "Tại Việt Nam, 1% dân số sinh ra với khả năng siêu nhiên — di chuyển đồ vật bằng ý nghĩ, "
            + "đọc suy nghĩ người khác, điều khiển lửa. Chính phủ thành lập Học Viện Thiên Phú để đào tạo "
            + "và kiểm soát những người này. Tuấn, 16 tuổi, vừa phát hiện mình có năng lực hiếm gặp — "
            + "anh có thể sao chép bất kỳ dị năng nào mà anh chứng kiến. Điều này khiến anh trở thành "
            + "tài sản quý giá nhất và cũng là mục tiêu nguy hiểm nhất.",
            "APPROVED", Set.of(catMap.get("Paranormal"), catMap.get("Young Adult")), 55800L));

        stories.add(createStory(author3, "Bếp Của Ngoại",
            "Nguyễn Bảo Ngọc, đầu bếp sao Michelin tại Paris, nhận tin bà ngoại ở Hội An bệnh nặng. "
            + "Cô trở về Việt Nam và phát hiện cuốn sổ tay ghi chép công thức nấu ăn bí mật của bà — "
            + "mỗi món ăn gắn liền với một câu chuyện, một ký ức, một bài học cuộc sống. Qua từng trang sổ, "
            + "Ngọc không chỉ tìm lại hương vị tuổi thơ mà còn hiểu ra ý nghĩa thật sự của 'nhà' "
            + "và quyết định liệu mình nên ở lại hay quay về.",
            "APPROVED", Set.of(catMap.get("Drama"), catMap.get("Contemporary")), 31500L));

        stories.add(createStory(author3, "Ngôi Nhà Ma Ám",
            "Một nhóm 5 sinh viên thuê căn nhà cổ ở ngoại ô Huế để nghiên cứu cho luận văn về kiến trúc "
            + "thời Nguyễn. Đêm đầu tiên, camera an ninh ghi lại bóng dáng ai đó đi lại trong phòng trống. "
            + "Đêm thứ hai, mọi cánh cửa tự mở ra lúc 3 giờ sáng. Đêm thứ ba, một sinh viên biến mất.",
            "PENDING", Set.of(catMap.get("Horror"), catMap.get("Paranormal")), 0L));

        stories.add(createStory(author3, "Vua Đầu Bếp Đường Phố",
            "Từ một gánh bún bò Huế ven đường, anh chàng Đức Phúc nuôi giấc mơ biến món ăn đường phố "
            + "Việt Nam thành ẩm thực được thế giới công nhận. Hành trình từ quán vỉa hè đến cuộc thi "
            + "MasterChef quốc tế, với các thử thách nấu ăn, tình bạn, cạnh tranh, và bài học rằng "
            + "món ăn ngon nhất luôn được nấu từ trái tim.",
            "APPROVED", Set.of(catMap.get("Drama"), catMap.get("Comedy")), 19700L));

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
            story.setCoverUrl(ImageAssets.randomStoryCover());
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
            // Cập nhật lại targetCount & displayOrder cho các mission cũ nếu sai lệch
            List<Mission> existing = missionRepository.findAll();
            for (Mission m : existing) {
                if ("Đọc 5 chương truyện".equals(m.getName()))   { m.setTargetCount(5); m.setDisplayOrder(1); }
                if ("Đăng nhập hàng ngày".equals(m.getName()))   { m.setDisplayOrder(6); }
                if ("Bình luận 1 chương".equals(m.getName()))    { m.setDisplayOrder(2); }
                if ("Theo dõi 1 truyện mới".equals(m.getName())) { m.setDisplayOrder(3); }
                if ("Mua 1 chương truyện".equals(m.getName()))   { m.setDisplayOrder(4); }
                if ("Tặng quà cho tác giả".equals(m.getName()))  { m.setDisplayOrder(5); }
                if (m.getTargetCount() == null) m.setTargetCount(1);
                if (m.getIsActive() == null)    m.setIsActive(true);
            }
            missionRepository.saveAll(existing);
            return existing;
        }

        List<Mission> missions = new ArrayList<>();
        // displayOrder = thứ tự hiển thị trên UI (giống screenshot)
        missions.add(missionRepository.save(buildMission("Đọc 5 chương truyện",
                "Đọc đủ 5 chương truyện bất kỳ để nhận thưởng",
                20L, "READ", "READ_CHAPTER", 5, "📚", 1)));
        missions.add(missionRepository.save(buildMission("Bình luận 1 chương",
                "Để lại 1 bình luận trong bất kỳ chương truyện nào",
                5L, "DAILY", "COMMENT", 1, "💬", 2)));
        missions.add(missionRepository.save(buildMission("Theo dõi 1 truyện mới",
                "Theo dõi 1 bộ truyện mới trong ngày",
                10L, "DAILY", "FOLLOW_STORY", 1, "❤️", 3)));
        missions.add(missionRepository.save(buildMission("Mua 1 chương truyện",
                "Mua ít nhất 1 chương trả phí",
                15L, "READ", "BUY_CHAPTER", 1, "🛒", 4)));
        missions.add(missionRepository.save(buildMission("Tặng quà cho tác giả",
                "Tặng quà (donate) cho tác giả trong ngày",
                20L, "DAILY", "SEND_GIFT", 1, "🎁", 5)));
        missions.add(missionRepository.save(buildMission("Đăng nhập hàng ngày",
                "Đăng nhập vào hệ thống mỗi ngày để nhận coin",
                10L, "DAILY", "LOGIN", 1, "🔑", 6)));

        System.out.println("🎯 Đã tạo " + missions.size() + " missions");
        return missions;
    }

    private Mission buildMission(String name, String description, Long rewardCoin,
                                  String type, String action, int targetCount,
                                  String icon, int displayOrder) {
        Mission m = new Mission();
        m.setName(name);
        m.setDescription(description);
        m.setRewardCoin(rewardCoin);
        m.setType(type);
        m.setAction(action);
        m.setTargetCount(targetCount);
        m.setIcon(icon);
        m.setDisplayOrder(displayOrder);
        m.setIsActive(true);
        return m;
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
        String[] paragraphs = {
            "Ánh nắng chiều xuyên qua tán lá, tạo nên những vệt sáng nhảy nhót trên mặt đất ẩm ướt. Nhân vật chính đứng lặng trước khung cảnh, lòng trĩu nặng bởi hàng nghìn câu hỏi không lời đáp.",
            "Tiếng gió rít qua khe cửa như tiếng thì thầm của những linh hồn cổ xưa. Không ai biết rằng bí mật ẩn giấu trong bóng tối đang dần được hé lộ, từng chút một, như cách mà bình minh xua tan màn đêm.",
            "\"Nếu ngươi muốn biết sự thật, hãy sẵn sàng đối mặt với nỗi đau\" — giọng nói vang lên từ phía sau, trầm ấm nhưng đầy uy lực. Đó là người mà ai cũng nghĩ đã chết từ lâu.",
            "Con đường phía trước mờ mịt trong sương dày đặc. Mỗi bước chân đều là một quyết định, mỗi hơi thở đều mang theo hy vọng mỏng manh. Nhưng họ không có lựa chọn nào khác — phải tiến về phía trước.",
            "Trận chiến diễn ra nhanh hơn dự kiến. Tiếng kiếm va nhau vang lên chói tai, tia lửa bắn ra từ mỗi nhát chém. Máu thấm vào đất, nhưng không ai sẵn sàng lùi bước.",
            "Trong căn phòng tối om, chỉ có ánh nến leo lắt soi sáng những dòng chữ cổ trên tấm bản đồ đã ngả vàng. Nếu giải mã được, họ sẽ tìm ra câu trả lời cho tất cả. Nếu thất bại, mọi thứ sẽ kết thúc.",
            "Nụ cười ấy — dịu dàng nhưng buồn bã — khắc sâu vào ký ức như một vết thương không bao giờ lành. Đôi mắt nhìn nhau lần cuối trước khi con đường rẽ hai ngả.",
            "\"Chúng ta còn thời gian không?\" — câu hỏi treo lơ lửng trong không khí nặng nề. Không ai trả lời, vì tất cả đều biết câu trả lời nhưng không ai muốn nói ra.",
            "Thành phố về đêm lung linh ánh đèn, nhưng phía sau những tòa nhà chọc trời là những con hẻm tối tăm. Ở đó, những cuộc đời bị lãng quên đang âm thầm giấu đi những câu chuyện chưa kể.",
            "Giọt mưa đầu tiên rơi xuống, rồi hàng triệu giọt nối tiếp nhau, cứ thế tuôn trào. Cơn bão không chỉ đến từ bầu trời — nó đến từ sâu thẳm bên trong tâm hồn mỗi người."
        };

        int idx = (chapterNum - 1) % paragraphs.length;
        int idx2 = (chapterNum) % paragraphs.length;
        int idx3 = (chapterNum + 1) % paragraphs.length;

        return String.format("""
                %s

                %s

                %s

                Cuộc hành trình trong '%s' tiếp tục với những bước ngoặt không ai ngờ tới. \
                Chương %d mở ra một trang mới, nơi mà ranh giới giữa đúng và sai trở nên mờ nhạt hơn bao giờ hết.

                Những nhân vật quen thuộc đang thay đổi — có người trở nên mạnh mẽ hơn, \
                có người gục ngã trước áp lực. Và ẩn sau tất cả, một thế lực bí ẩn đang lặng lẽ giật dây.

                Hãy tiếp tục theo dõi để biết điều gì sẽ xảy ra tiếp theo...""",
            paragraphs[idx], paragraphs[idx2], paragraphs[idx3],
            storyTitle, chapterNum
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



    /**
     * Cập nhật cover_url của stories và avatar_url của users
     * nếu đang dùng ảnh placeholder cũ hoặc chưa có ảnh.
     */
    private void migrateImages() {
        // Update story covers
        List<org.com.story.entity.Story> allStories = storyRepository.findAll();
        int storiesUpdated = 0;
        for (org.com.story.entity.Story story : allStories) {
            String cover = story.getCoverUrl();
            if (cover == null || cover.contains("via.placeholder.com") || cover.isBlank()) {
                story.setCoverUrl(ImageAssets.randomStoryCover());
                storyRepository.save(story);
                storiesUpdated++;
            }
        }
        if (storiesUpdated > 0) {
            System.out.println("🖼️  Đã cập nhật cover cho " + storiesUpdated + " stories");
        }

        // Update user avatars
        List<org.com.story.entity.User> allUsers = userRepository.findAll();
        int usersUpdated = 0;
        for (org.com.story.entity.User user : allUsers) {
            String avatar = user.getAvatarUrl();
            if (avatar == null || avatar.isBlank()) {
                user.setAvatarUrl(ImageAssets.randomUserAvatar());
                userRepository.save(user);
                usersUpdated++;
            }
        }
        if (usersUpdated > 0) {
            System.out.println("👤 Đã cập nhật avatar cho " + usersUpdated + " users");
        }
    }
}
