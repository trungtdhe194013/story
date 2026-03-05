package org.com.story.common;

import java.util.List;
import java.util.Random;

/**
 * Tập trung quản lý danh sách ảnh mẫu cho stories (cover) và users (avatar).
 * Thêm URL ảnh vào các list bên dưới, hệ thống sẽ random khi tạo dữ liệu demo.
 *
 * Cách thêm ảnh:
 *   - Tải ảnh về, đặt vào thư mục static/images/covers/ hoặc static/images/avatars/
 *   - Hoặc dùng URL online trực tiếp
 */
public class ImageAssets {

    private static final Random RANDOM = new Random();

    // =========================================================
    //  STORY COVER IMAGES
    //  Kích thước đề xuất: 300x400 (portrait)
    // =========================================================
    public static final List<String> STORY_COVERS = List.of(
            // Fantasy / Adventure
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1507842217343-583bb7270b66?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1462275646964-a0e3386b89fa?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1535666669445-e8c15cd2e7d9?w=300&h=400&fit=crop",

            // Romance / Drama
            "https://images.unsplash.com/photo-1474552226712-ac0f0961a954?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1516589091380-5d8e87df6999?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1551488831-00ddcb6c6bd3?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1502767882256-d3e6d4f8ef4c?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=300&h=400&fit=crop",

            // Mystery / Thriller
            "https://images.unsplash.com/photo-1509557965875-b88c97052f0e?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1519074069444-1ba4fff66d16?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1572887767859-0b8f5f2d3e37?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1490730141103-6cac27aaab94?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&h=400&fit=crop",

            // Science Fiction
            "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1614730321146-b6fa6a46bcb4?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1484600899469-230e8d1d59c0?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1608178398319-48f814d0750c?w=300&h=400&fit=crop",

            // Horror / Dark
            "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1570639565969-5b5b7cc12d7c?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1518562180175-34a163b1a9a6?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1601993611049-c4a5e5cf7394?w=300&h=400&fit=crop",
            "https://images.unsplash.com/photo-1598182198871-d3f4ab4fd181?w=300&h=400&fit=crop"
    );

    // =========================================================
    //  USER AVATAR IMAGES
    //  Kích thước đề xuất: 150x150 (square)
    // =========================================================
    public static final List<String> USER_AVATARS = List.of(
            // Nam
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1599566150163-29194dcaad36?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face",

            // Nữ
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=150&h=150&fit=crop&crop=face",

            // Trung tính / Illustrated
            "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&h=150&fit=crop&crop=face",
            "https://images.unsplash.com/photo-1546961342-ea5f60b193b6?w=150&h=150&fit=crop&crop=face"
    );

    /**
     * Lấy ngẫu nhiên một URL ảnh cover story.
     */
    public static String randomStoryCover() {
        return STORY_COVERS.get(RANDOM.nextInt(STORY_COVERS.size()));
    }

    /**
     * Lấy ngẫu nhiên một URL avatar user.
     */
    public static String randomUserAvatar() {
        return USER_AVATARS.get(RANDOM.nextInt(USER_AVATARS.size()));
    }
}

