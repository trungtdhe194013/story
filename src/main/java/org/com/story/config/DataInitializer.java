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
            List<Story> stories = createStories(author1, author2, author3);

            // Tạo chapters
            List<Chapter> chapters = createChapters(stories);

            // Tạo comments
            createComments(chapters, reader1, reader2, reader3);

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
                    user.setCreatedAt(LocalDateTime.now());
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
            }
        }
        return categories;
    }

    private List<Story> createStories(User author1, User author2, User author3) {
        List<Story> stories = new ArrayList<>();

        // Stories của Author 1
        stories.add(createStory(author1, "Hành Trình Phép Thuật",
            "Câu chuyện về một cậu bé phát hiện ra khả năng phép thuật đặc biệt và hành trình trở thành pháp sư vĩ đại nhất.",
            "APPROVED"));
        stories.add(createStory(author1, "Vương Quốc Thất Lạc",
            "Cuộc phiêu lưu tìm kiếm vương quốc huyền thoại đã mất tích hàng nghìn năm.",
            "APPROVED"));
        stories.add(createStory(author1, "Chiến Binh Ánh Sáng",
            "Một chiến binh trẻ tuổi chiến đấu chống lại thế lực hắc ám để bảo vệ thế giới.",
            "PENDING"));

        // Stories của Author 2
        stories.add(createStory(author2, "Tình Yêu Qua Mùa",
            "Một câu chuyện tình lãng mạn qua bốn mùa của năm.",
            "APPROVED"));
        stories.add(createStory(author2, "Bí Ẩn Thành Phố",
            "Những bí mật đen tối ẩn giấu trong một thành phố hiện đại.",
            "APPROVED"));
        stories.add(createStory(author2, "Cuộc Đời Tôi",
            "Câu chuyện tự truyện về cuộc đời đầy thăng trầm.",
            "DRAFT"));

        // Stories của Author 3
        stories.add(createStory(author3, "Thám Tử Số 1",
            "Thám tử tài ba giải quyết những vụ án phức tạp nhất.",
            "APPROVED"));
        stories.add(createStory(author3, "Du Hành Thời Gian",
            "Khám phá quá khứ và tương lai qua cỗ máy thời gian.",
            "APPROVED"));
        stories.add(createStory(author3, "Ngôi Nhà Ma Ám",
            "Những sự kiện kinh hoàng xảy ra trong ngôi nhà bị ma ám.",
            "PENDING"));
        stories.add(createStory(author3, "Học Viện Siêu Nhiên",
            "Trường học dành cho những học sinh có năng lực đặc biệt.",
            "APPROVED"));

        // Loại bỏ các story null
        stories.removeIf(story -> story == null);

        System.out.println("📚 Đã tạo " + stories.size() + " stories");
        return stories;
    }

    private Story createStory(User author, String title, String summary, String status) {
        if (storyRepository.findAll().stream().noneMatch(s -> s.getTitle().equals(title))) {
            Story story = new Story();
            story.setAuthor(author);
            story.setTitle(title);
            story.setSummary(summary);
            story.setStatus(status);
            story.setCoverUrl("https://via.placeholder.com/300x400?text=" + title.replaceAll(" ", "+"));
            return storyRepository.save(story);
        }
        return null;
    }

    private List<Chapter> createChapters(List<Story> stories) {
        List<Chapter> chapters = new ArrayList<>();

        System.out.println("🔍 Bắt đầu tạo chapters...");
        System.out.println("📚 Số stories nhận được: " + stories.size());

        for (Story story : stories) {
            if (story != null) {
                System.out.println("  - Story: " + story.getTitle() + " | Status: " + story.getStatus());

                if ("APPROVED".equals(story.getStatus())) {
                    // Tạo 10-15 chapters cho mỗi story đã được approve
                    int numChapters = 10 + (int)(Math.random() * 6);
                    System.out.println("    ✅ Tạo " + numChapters + " chapters cho story này");

                    for (int i = 1; i <= numChapters; i++) {
                        Chapter chapter = new Chapter();
                        chapter.setStory(story);
                        chapter.setTitle("Chương " + i + ": " + generateChapterTitle(i));
                        chapter.setContent(generateChapterContent(story.getTitle(), i));
                        chapter.setChapterOrder(i);
                        chapter.setCoinPrice(i <= 3 ? 0 : 10 + (i * 5)); // 3 chương đầu free
                        chapter.setStatus("PUBLISHED");
                        chapter.setPublishAt(LocalDateTime.now().minusDays(numChapters - i));
                        Chapter savedChapter = chapterRepository.save(chapter);
                        chapters.add(savedChapter);
                        System.out.println("      📄 Đã tạo: " + savedChapter.getTitle());
                    }
                } else {
                    System.out.println("    ⏭️ Bỏ qua - không phải APPROVED");
                }
            } else {
                System.out.println("  ⚠️ Story null - bỏ qua");
            }
        }

        System.out.println("✅ Tổng số chapters đã tạo: " + chapters.size());
        return chapters;
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
            "Đây là nội dung của chương %d trong câu chuyện '%s'.\n\n" +
            "Câu chuyện đang dần dần hấp dẫn hơn. Nhân vật chính đang phải đối mặt với những thử thách mới. " +
            "Những bí mật từ quá khứ đang dần được hé lộ.\n\n" +
            "Trong chương này, độc giả sẽ được khám phá thêm về thế giới và các nhân vật. " +
            "Những diễn biến bất ngờ sẽ làm cho câu chuyện trở nên kịch tính hơn bao giờ hết.\n\n" +
            "Hãy tiếp tục theo dõi để biết những gì sẽ xảy ra tiếp theo!\n\n" +
            "[Nội dung đầy đủ của chương %d...]",
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

        // Tạo 3-5 comments cho một số chapters ngẫu nhiên
        for (int i = 0; i < Math.min(chapters.size(), 20); i++) {
            Chapter chapter = chapters.get(i);
            int numComments = 3 + (int)(Math.random() * 3);

            for (int j = 0; j < numComments; j++) {
                Comment comment = new Comment();
                comment.setUser(readers.get(j % readers.size()));
                comment.setChapter(chapter);
                comment.setContent(commentTexts[(i * numComments + j) % commentTexts.length]);
                comment.setParent(null);
                commentRepository.save(comment);
            }
        }
    }
}
