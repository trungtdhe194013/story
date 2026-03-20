# 📚 Story Platform — API Documentation

> **Base URL:** `http://localhost:8080/api`  
> **Auth:** Bearer JWT Token — thêm vào header: `Authorization: Bearer <token>`  
> **Response format:** Tất cả response đều được bọc tự động:
> ```json
> {
>   "success": true,
>   "status": 200,
>   "data": { ... }
> }
> ```
> Khi lỗi:
> ```json
> {
>   "success": false,
>   "status": 400,
>   "message": "Mô tả lỗi"
> }
> ```

---

## 📑 Mục lục

1. [Auth](#1-auth)
2. [User Profile](#2-user-profile)
3. [Story](#3-story)
4. [Chapter](#4-chapter)
5. [Comment](#5-comment)
6. [Category](#6-category)
7. [Follow](#7-follow)
8. [Rating](#8-rating)
9. [Wallet](#9-wallet)
10. [Gift](#10-gift)
11. [Edit Request (Marketplace biên tập)](#11-edit-request)
12. [Mission](#12-mission)
13. [Withdraw Request](#13-withdraw-request)
14. [Report](#14-report)
15. [Role Change Request](#15-role-change-request)
16. [Reviewer](#16-reviewer)
17. [Editor](#17-editor)
18. [Admin](#18-admin)

---

## 1. Auth

### POST `/auth/sign-up`
**Mô tả:** Đăng ký tài khoản — Bước 1: gửi OTP về email.  
**Auth:** Không cần  
**Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "fullName": "Nguyễn Văn A"
}
```
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "email": "user@example.com",
    "message": "OTP đã được gửi về email. Hiệu lực 10 phút.",
    "devOtp": "123456"
  }
}
```

---

### POST `/auth/sign-up/verify-otp`
**Mô tả:** Xác nhận OTP để kích hoạt tài khoản.  
**Auth:** Không cần  
**Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```
**Response:** `UserResponse` (xem phần User)

---

### POST `/auth/sign-up/resend-otp`
**Mô tả:** Gửi lại OTP đăng ký.  
**Auth:** Không cần  
**Query param:** `email=user@example.com`  
**Response:** `OtpResponse` (giống sign-up)

---

### POST `/auth/login`
**Mô tả:** Đăng nhập bằng email/password. Nếu bị ban sẽ trả về lỗi 403.  
**Auth:** Không cần  
**Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123"
}
```
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "uuid-refresh-token"
  }
}
```
**Lỗi ban tài khoản:**
```json
{
  "success": false,
  "status": 403,
  "message": "Tài khoản của bạn đang bị khóa đến 27/03/2026 10:00 do vi phạm quy định cộng đồng."
}
```

---

### POST `/auth/refresh`
**Mô tả:** Lấy access token mới từ refresh token.  
**Auth:** Không cần  
**Query param:** `refreshToken=<token>`  
**Response:** Giống login response

---

### POST `/auth/logout`
**Mô tả:** Đăng xuất, vô hiệu hoá refresh token.  
**Auth:** Không cần  
**Query param:** `refreshToken=<token>`  
**Response:** 200 OK (không có data)

---

### POST `/auth/forgot-password`
**Mô tả:** Quên mật khẩu — Bước 1: gửi OTP về email.  
**Auth:** Không cần  
**Body:**
```json
{
  "email": "user@example.com"
}
```
**Response:** `OtpResponse`

---

### POST `/auth/reset-password`
**Mô tả:** Đặt lại mật khẩu bằng OTP.  
**Auth:** Không cần  
**Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NewPass123",
  "confirmNewPassword": "NewPass123"
}
```
**Response:** 204 No Content

---

### POST `/auth/oauth2/verify-otp`
**Mô tả:** Xác nhận OTP sau đăng nhập Google — nhận JWT token.  
**Auth:** Không cần  
**Body:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```
**Response:** `LoginResponse` (accessToken + refreshToken)

---

## 2. User Profile

### GET `/users/me` 🔒
**Mô tả:** Lấy thông tin profile đầy đủ của user đang đăng nhập.  
**Auth:** Bắt buộc  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "email": "author@story.com",
    "fullName": "Nguyễn Văn A",
    "roles": ["READER", "AUTHOR"],
    "provider": "LOCAL",
    "enabled": true,
    "avatarUrl": "https://...",
    "bio": "Tác giả yêu thích viết truyện fantasy",
    "phone": "0901234567",
    "dateOfBirth": "1995-05-15",
    "gender": "MALE",
    "location": "Hà Nội",
    "walletBalance": 5000,
    "banUntil": null,
    "totalFollowedStories": 12,
    "totalPurchasedChapters": 8,
    "createdAt": "2024-01-15T10:00:00",
    "updatedAt": "2024-03-20T15:30:00"
  }
}
```

---

### PUT `/users/me` 🔒
**Mô tả:** Cập nhật thông tin cá nhân. Trả về full UserResponse giống GET /users/me.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "fullName": "Nguyễn Văn B",
  "avatarUrl": "https://...",
  "bio": "Bio mới",
  "phone": "0909000000",
  "dateOfBirth": "1995-05-15",
  "gender": "MALE",
  "location": "TP.HCM"
}
```
**Response:** `UserResponse` (giống GET /users/me)

---

### POST `/users/me/avatar` 🔒
**Mô tả:** Upload ảnh đại diện từ máy. Chỉ chấp nhận JPG, PNG, GIF, WebP. Tối đa 5MB.  
**Auth:** Bắt buộc  
**Content-Type:** `multipart/form-data`  
**Form field:** `file` = file ảnh  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "avatarUrl": "http://localhost:8080/uploads/avatars/avatar_uuid.jpg"
  }
}
```
> 💡 **Flow:** Upload avatar → nhận URL → dùng URL đó trong PUT /users/me

---

### PUT `/users/me/change-password` 🔒
**Mô tả:** Đổi mật khẩu (cho tài khoản LOCAL, không phải Google).  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewPass456",
  "confirmNewPassword": "NewPass456"
}
```
**Response:** 204 No Content

---

## 3. Story

> **Trạng thái truyện:** `DRAFT` → `PENDING` → `APPROVED` / `REJECTED`

### GET `/stories`
**Mô tả:** Danh sách truyện đã duyệt (APPROVED), chưa bị xóa mềm, có ít nhất 1 chương PUBLISHED.  
**Auth:** Không cần  
**Response:** `List<StoryResponse>`
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 1,
      "title": "Đấu Phá Thương Khung",
      "summary": "Tóm tắt truyện...",
      "coverUrl": "https://...",
      "status": "APPROVED",
      "authorId": 2,
      "authorName": "Tiêu Đình",
      "categories": [{"id": 1, "name": "Tu Tiên"}],
      "viewCount": 15000,
      "avgRating": 4.5,
      "ratingCount": 230,
      "isCompleted": false,
      "publishedChapterCount": 45,
      "totalChapterCount": 50,
      "isDeleted": false,
      "createdAt": "2024-01-10T09:00:00",
      "updatedAt": "2024-03-15T12:00:00"
    }
  ]
}
```

---

### GET `/stories/search`
**Mô tả:** Tìm kiếm truyện theo tên.  
**Auth:** Không cần  
**Query param:** `keyword=đấu phá`  
**Response:** `List<StoryResponse>`

---

### GET `/stories/my` 🔒
**Mô tả:** Xem tất cả truyện của Author đang đăng nhập (kể cả đã xóa mềm, mọi trạng thái).  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `List<StoryResponse>`

---

### GET `/stories/{id}`
**Mô tả:** Thông tin cơ bản của 1 truyện.  
**Auth:** Không cần  
**Response:** `StoryResponse` (xem bên trên)

---

### GET `/stories/{id}/detail`
**Mô tả:** Chi tiết đầy đủ: thông tin truyện + danh sách chương PUBLISHED (không có content).  
**Auth:** Không cần  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 1,
    "title": "Đấu Phá Thương Khung",
    "summary": "...",
    "coverUrl": "https://...",
    "status": "APPROVED",
    "authorId": 2,
    "authorName": "Tiêu Đình",
    "viewCount": 15000,
    "avgRating": 4.5,
    "ratingCount": 230,
    "isCompleted": false,
    "myRating": 5,
    "myReview": "Truyện cực hay, strong recommend!",
    "categories": [{"id": 1, "name": "Tu Tiên"}],
    "chapters": [
      {
        "id": 10,
        "title": "Chương 1: Thiên Tài",
        "chapterOrder": 1,
        "coinPrice": 0,
        "status": "PUBLISHED",
        "publishAt": "2024-01-15T10:00:00",
        "isPurchased": false
      }
    ],
    "totalChapters": 45,
    "allChaptersCount": 50,
    "createdAt": "2024-01-10T09:00:00",
    "updatedAt": "2024-03-15T12:00:00"
  }
}
```

---

### GET `/stories/{id}/chapter-stats`
**Mô tả:** Thống kê số chương.  
**Auth:** Không cần  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "storyId": 1,
    "storyTitle": "Đấu Phá Thương Khung",
    "publishedCount": 45,
    "totalCount": 50
  }
}
```

---

### POST `/stories` 🔒
**Mô tả:** Tạo truyện mới (AUTHOR). Truyện tạo ra ở trạng thái DRAFT.  
**Auth:** Bắt buộc (AUTHOR)  
**Body:**
```json
{
  "title": "Tên truyện mới",
  "summary": "Tóm tắt nội dung",
  "coverUrl": "https://...",
  "categoryIds": [1, 2, 3]
}
```
**Response:** `StoryResponse`

---

### PUT `/stories/{id}` 🔒
**Mô tả:** Cập nhật thông tin truyện (chỉ Author của truyện đó).  
**Auth:** Bắt buộc (AUTHOR)  
**Body:** Giống POST  
**Response:** `StoryResponse`

---

### DELETE `/stories/{id}` 🔒
**Mô tả:** Xóa mềm truyện — ẩn khỏi trang chủ nhưng dữ liệu vẫn giữ nguyên. Author vẫn thấy trong /my.  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** 204 No Content

---

### POST `/stories/{id}/submit` 🔒
**Mô tả:** Author nộp truyện lên Reviewer duyệt. Trạng thái: DRAFT → PENDING.  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `StoryResponse` (status = "PENDING")

---

## 4. Chapter

> **Trạng thái chương:** `DRAFT` → `PENDING` → `APPROVED` → `PUBLISHED` (hoặc `REJECTED`)

### GET `/chapters/{id}`
**Mô tả:** Lấy nội dung đầy đủ 1 chương. Chương trả phí: cần đã mua hoặc là Author/Reviewer của truyện đó.  
**Auth:** Không cần (với chương free) / Bắt buộc (chương trả phí)  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 10,
    "storyId": 1,
    "storyTitle": "Đấu Phá Thương Khung",
    "title": "Chương 1: Thiên Tài",
    "content": "Nội dung chương...",
    "chapterOrder": 1,
    "coinPrice": 0,
    "status": "PUBLISHED",
    "publishAt": "2024-01-15T10:00:00",
    "createdAt": "2024-01-14T08:00:00",
    "updatedAt": "2024-01-14T09:00:00",
    "isPurchased": false,
    "reviewNote": null,
    "comments": [],
    "totalComments": 0
  }
}
```

---

### GET `/chapters/story/{storyId}`
**Mô tả:** Danh sách chương của truyện.  
- Với Reader/khách: chỉ thấy chương PUBLISHED  
- Với Author của truyện: thấy tất cả chương (mọi trạng thái)  
**Auth:** Không cần (kết quả khác nhau khi có/không có token)  
**Response:** `List<ChapterResponse>` (không có field `content` để tiết kiệm dữ liệu)

---

### POST `/chapters/story/{storyId}` 🔒
**Mô tả:** Tạo chương mới cho truyện (AUTHOR của truyện đó).  
**Auth:** Bắt buộc (AUTHOR)  
**Body:**
```json
{
  "title": "Chương 2: Học Viện",
  "content": "Nội dung chương...",
  "chapterOrder": 2,
  "coinPrice": 5,
  "publishAt": null
}
```
**Response:** `ChapterResponse`

---

### PUT `/chapters/{id}` 🔒
**Mô tả:** Cập nhật nội dung chương (chỉ khi status = DRAFT hoặc REJECTED).  
**Auth:** Bắt buộc (AUTHOR)  
**Body:** Giống POST  
**Response:** `ChapterResponse`

---

### DELETE `/chapters/{id}` 🔒
**Mô tả:** Xóa chương (chỉ khi status = DRAFT).  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** 204 No Content

---

### POST `/chapters/{id}/submit` 🔒
**Mô tả:** Nộp chương lên Reviewer duyệt. Status: DRAFT/EDITED → PENDING.  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `ChapterResponse` (status = "PENDING")

---

### POST `/chapters/{id}/publish` 🔒
**Mô tả:** Tác giả tự publish chương sau khi Reviewer APPROVE. Status: APPROVED → PUBLISHED.  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `ChapterResponse` (status = "PUBLISHED")

---

### POST `/chapters/{id}/purchase` 🔒
**Mô tả:** Mua chương trả phí bằng coin.  
**Auth:** Bắt buộc  
**Response:** `ChapterResponse` (isPurchased = true, có nội dung đầy đủ)  
**Lỗi thiếu coin:**
```json
{
  "success": false,
  "status": 400,
  "message": "Không đủ coin. Số dư hiện tại: 3, cần: 5"
}
```

---

## 5. Comment

### GET `/comments/chapter/{chapterId}`
**Mô tả:** Lấy tất cả bình luận của 1 chương (dạng cây: comment gốc + replies đệ quy vô hạn cấp).  
**Auth:** Không cần  
**Response:** `List<CommentResponse>`
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 5,
      "chapterId": 10,
      "chapterTitle": "Chương 1: Thiên Tài",
      "chapterOrder": 1,
      "storyId": 1,
      "storyTitle": "Đấu Phá Thương Khung",
      "userId": 3,
      "userName": "reader01",
      "userAvatarUrl": "https://...",
      "content": "Chương hay quá!",
      "parentId": null,
      "hidden": false,
      "likeCount": 12,
      "createdAt": "2024-02-10T14:30:00",
      "replies": [
        {
          "id": 6,
          "parentId": 5,
          "content": "Đồng ý!",
          "userId": 4,
          "userName": "reader02",
          "replies": [],
          ...
        }
      ]
    }
  ]
}
```

---

### GET `/comments/{id}`
**Mô tả:** Lấy chi tiết 1 comment theo ID (kèm toàn bộ replies đệ quy).  
**Auth:** Không cần  
**Response:** `CommentResponse` (xem bên trên)

---

### POST `/comments` 🔒
**Mô tả:** Tạo bình luận cho chapter HOẶC story (review tổng thể).  
**Auth:** Bắt buộc  
**Body (bình luận chapter):**
```json
{
  "chapterId": 10,
  "content": "Chương hay!",
  "parentId": null
}
```
**Body (reply vào comment khác):**
```json
{
  "chapterId": 10,
  "content": "Tôi cũng thấy vậy!",
  "parentId": 5
}
```
**Body (review truyện — story-level comment):**
```json
{
  "storyId": 1,
  "content": "Truyện rất hay, cốt truyện hấp dẫn!",
  "parentId": null
}
```
**Response:** `CommentResponse`

---

### DELETE `/comments/{id}` 🔒
**Mô tả:** Xóa comment (chỉ chủ comment hoặc ADMIN).  
**Auth:** Bắt buộc  
**Response:** 204 No Content

---

## 6. Category

### GET `/categories`
**Mô tả:** Lấy danh sách tất cả thể loại.  
**Auth:** Không cần  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {"id": 1, "name": "Tu Tiên"},
    {"id": 2, "name": "Huyền Huyễn"},
    {"id": 3, "name": "Kiếm Hiệp"}
  ]
}
```

---

### POST `/categories` 🔒
**Mô tả:** Tạo thể loại mới (chỉ ADMIN).  
**Auth:** Bắt buộc (ADMIN)  
**Body:**
```json
{
  "name": "Ngôn Tình"
}
```
**Response:** `CategoryResponse`

---

### PUT `/categories/{id}` 🔒
**Mô tả:** Cập nhật tên thể loại (chỉ ADMIN).  
**Auth:** Bắt buộc (ADMIN)  
**Body:** `{"name": "Tên mới"}`  
**Response:** `CategoryResponse`

---

### DELETE `/categories/{id}` 🔒
**Mô tả:** Xóa thể loại (chỉ ADMIN).  
**Auth:** Bắt buộc (ADMIN)  
**Response:** 204 No Content

---

## 7. Follow

### POST `/follows/{storyId}` 🔒
**Mô tả:** Toggle follow/unfollow truyện.  
**Auth:** Bắt buộc  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "storyId": 1,
    "storyTitle": "Đấu Phá Thương Khung",
    "status": "FOLLOWED",
    "message": "Đã theo dõi truyện thành công"
  }
}
```
> `status` = `"FOLLOWED"` hoặc `"UNFOLLOWED"`

---

### GET `/follows` 🔒
**Mô tả:** Danh sách truyện đang theo dõi.  
**Auth:** Bắt buộc  
**Response:** `List<StoryResponse>`

---

### GET `/follows/{storyId}/status` 🔒
**Mô tả:** Kiểm tra đã follow truyện chưa.  
**Auth:** Bắt buộc  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": true
}
```

---

## 8. Rating

### POST `/ratings` 🔒
**Mô tả:** Đánh giá truyện 1-5 sao. Nếu đã đánh giá → cập nhật. Tự động cập nhật avgRating của truyện.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "storyId": 1,
  "score": 5,
  "review": "Truyện cực hay, strong recommend!"
}
```
**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 15,
    "userId": 3,
    "userName": "reader01",
    "storyId": 1,
    "storyTitle": "Đấu Phá Thương Khung",
    "score": 5,
    "review": "Truyện cực hay!",
    "createdAt": "2024-03-20T10:00:00",
    "updatedAt": "2024-03-20T10:00:00"
  }
}
```

---

### GET `/ratings/story/{storyId}`
**Mô tả:** Lấy tất cả đánh giá của 1 truyện (mới nhất trước).  
**Auth:** Không cần  
**Response:** `List<RatingResponse>`

---

### GET `/ratings/my/{storyId}` 🔒
**Mô tả:** Lấy đánh giá của user hiện tại cho 1 truyện.  
**Auth:** Bắt buộc  
**Response:** `RatingResponse` hoặc lỗi 404 nếu chưa đánh giá

---

## 9. Wallet

### GET `/wallet` 🔒
**Mô tả:** Xem thông tin ví của user hiện tại.  
**Auth:** Bắt buộc  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "userId": 3,
    "userName": "reader01",
    "balance": 5000,
    "lockedBalance": 500,
    "updatedAt": "2024-03-20T10:00:00"
  }
}
```
> `lockedBalance` = coin đang bị khoá bởi EditRequest đang mở (chỉ Author)

---

### POST `/wallet/topup` 🔒
**Mô tả:** Nạp coin vào ví.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "amount": 100000
}
```
**Response:** `WalletResponse` (balance đã được cộng)

---

### GET `/wallet/transactions` 🔒
**Mô tả:** Lịch sử giao dịch ví của user.  
**Auth:** Bắt buộc  
**Response:** `List<WalletTransactionResponse>`
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 20,
      "userId": 3,
      "amount": 100000,
      "type": "TOPUP",
      "refId": null,
      "createdAt": "2024-03-20T10:00:00"
    },
    {
      "id": 21,
      "userId": 3,
      "amount": -5,
      "type": "BUY",
      "refId": 10,
      "createdAt": "2024-03-20T11:00:00"
    }
  ]
}
```
> **type:** `TOPUP` / `BUY` / `GIFT` / `REWARD` / `EDIT_REWARD` / `LOCK` / `UNLOCK`

---

## 10. Gift

### POST `/gifts` 🔒
**Mô tả:** Tặng coin cho tác giả của truyện. Trừ coin từ ví người gửi, cộng vào ví tác giả.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "storyId": 1,
  "amount": 500,
  "message": "Cảm ơn tác giả vì truyện hay!"
}
```
**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 5,
    "fromUserId": 3,
    "fromUserName": "reader01",
    "toUserId": 2,
    "toUserName": "Tiêu Đình",
    "storyId": 1,
    "storyTitle": "Đấu Phá Thương Khung",
    "amount": 500,
    "createdAt": "2024-03-20T10:00:00"
  }
}
```

---

### GET `/gifts/sent` 🔒
**Mô tả:** Danh sách quà đã tặng.  
**Auth:** Bắt buộc  
**Response:** `List<GiftResponse>`

---

### GET `/gifts/received` 🔒
**Mô tả:** Danh sách quà đã nhận (Author).  
**Auth:** Bắt buộc  
**Response:** `List<GiftResponse>`

---

### GET `/gifts/story/{storyId}`
**Mô tả:** Danh sách quà của 1 truyện (top fan).  
**Auth:** Không cần  
**Response:** `List<GiftResponse>`

---

## 11. Edit Request

> **Marketplace biên tập**: Author đặt việc + coin thưởng, Editor nhận việc, Author duyệt.

### Trạng thái: `OPEN` → `IN_PROGRESS` → `SUBMITTED` → `APPROVED` / (hoặc → `IN_PROGRESS` nếu bị reject)

---

### POST `/edit-requests` 🔒
**Mô tả:** [AUTHOR] Tạo yêu cầu chỉnh sửa chapter. Coin bị LOCK ngay lập tức.  
**Auth:** Bắt buộc (AUTHOR)  
**Body:**
```json
{
  "chapterId": 10,
  "coinReward": 200,
  "description": "Cần sửa lại đoạn đầu chương cho mượt hơn..."
}
```
**Response:** `EditRequestResponse`
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 3,
    "chapterId": 10,
    "chapterTitle": "Chương 2: Học Viện",
    "storyTitle": "Đấu Phá Thương Khung",
    "authorId": 2,
    "authorName": "Tiêu Đình",
    "editorId": null,
    "editorName": null,
    "coinReward": 200,
    "description": "Cần sửa lại đoạn đầu...",
    "editedContent": null,
    "editorNote": null,
    "authorNote": null,
    "status": "OPEN",
    "attemptCount": 0,
    "createdAt": "2024-03-20T10:00:00",
    "updatedAt": "2024-03-20T10:00:00"
  }
}
```

---

### GET `/edit-requests/my` 🔒
**Mô tả:** [AUTHOR] Xem tất cả edit requests của tôi (bên Author).  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `List<EditRequestResponse>`

---

### POST `/edit-requests/{id}/approve` 🔒
**Mô tả:** [AUTHOR] Chấp thuận bản edit. Coin escrow → ví Editor. Chapter content được cập nhật.  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `EditRequestResponse` (status = "APPROVED")

---

### POST `/edit-requests/{id}/reject` 🔒
**Mô tả:** [AUTHOR] Từ chối bản edit. Editor viết lại (vô hạn lần). Coin vẫn bị lock.  
**Auth:** Bắt buộc (AUTHOR)  
**Body (tùy chọn):**
```json
{
  "note": "Cần sửa thêm đoạn cuối chương"
}
```
**Response:** `EditRequestResponse` (status = "IN_PROGRESS", attemptCount tăng thêm 1)

---

### DELETE `/edit-requests/{id}/cancel` 🔒
**Mô tả:** [AUTHOR] Huỷ request (chỉ khi OPEN, chưa có editor nhận). Coin được hoàn trả.  
**Auth:** Bắt buộc (AUTHOR)  
**Response:** `EditRequestResponse` (status = "CANCELLED")

---

### GET `/edit-requests/open` 🔒
**Mô tả:** [EDITOR] Xem tất cả yêu cầu đang OPEN để nhận việc.  
**Auth:** Bắt buộc (EDITOR)  
**Response:** `List<EditRequestResponse>`

---

### POST `/edit-requests/{id}/assign` 🔒
**Mô tả:** [EDITOR] Nhận việc. Status: OPEN → IN_PROGRESS.  
**Auth:** Bắt buộc (EDITOR)  
**Response:** `EditRequestResponse` (status = "IN_PROGRESS", editorId đã được gán)

---

### PUT `/edit-requests/{id}/submit` 🔒
**Mô tả:** [EDITOR] Nộp bản chỉnh sửa. Status: IN_PROGRESS → SUBMITTED.  
**Auth:** Bắt buộc (EDITOR)  
**Body:**
```json
{
  "editedContent": "Nội dung chương sau khi đã chỉnh sửa...",
  "editorNote": "Đã viết lại đoạn mở đầu, cải thiện flow câu văn"
}
```
**Response:** `EditRequestResponse` (status = "SUBMITTED", editedContent có giá trị)

---

### POST `/edit-requests/{id}/withdraw` 🔒
**Mô tả:** [EDITOR] Rút lui khỏi request (chỉ khi chưa bị reject lần nào, attemptCount = 0).  
**Auth:** Bắt buộc (EDITOR)  
**Response:** `EditRequestResponse` (status = "OPEN", editorId = null)

---

### GET `/edit-requests/assigned` 🔒
**Mô tả:** [EDITOR] Xem lịch sử các việc tôi đã nhận.  
**Auth:** Bắt buộc (EDITOR)  
**Response:** `List<EditRequestResponse>`

---

## 12. Mission

### GET `/missions`
**Mô tả:** Xem tất cả nhiệm vụ kèm trạng thái hoàn thành của user hiện tại (nếu đã đăng nhập).  
**Auth:** Không cần (kết quả khác nhau khi có token)  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 1,
      "name": "Đọc 3 chương mỗi ngày",
      "rewardCoin": 10,
      "type": "DAILY_READ",
      "completed": false
    },
    {
      "id": 2,
      "name": "Bình luận 1 chương",
      "rewardCoin": 5,
      "type": "DAILY_COMMENT",
      "completed": true
    }
  ]
}
```

---

### POST `/missions/{missionId}/complete` 🔒
**Mô tả:** Hoàn thành nhiệm vụ và nhận thưởng coin.  
**Auth:** Bắt buộc  
**Response:** `MissionResponse` (completed = true)

---

## 13. Withdraw Request

### POST `/withdraw-requests` 🔒
**Mô tả:** Tạo yêu cầu rút coin về tài khoản ngân hàng.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "amount": 1000,
  "bankName": "Vietcombank",
  "bankAccount": "1234567890",
  "bankOwner": "NGUYEN VAN A",
  "note": "Rút tiền tháng 3"
}
```
**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 4,
    "userId": 2,
    "userName": "author01",
    "amount": 1000,
    "status": "PENDING",
    "createdAt": "2024-03-20T10:00:00"
  }
}
```

---

### GET `/withdraw-requests/my` 🔒
**Mô tả:** Xem lịch sử yêu cầu rút tiền của tôi.  
**Auth:** Bắt buộc  
**Response:** `List<WithdrawRequestResponse>`

---

## 14. Report

### POST `/reports` 🔒
**Mô tả:** Báo cáo vi phạm một truyện, chương hoặc comment.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "targetType": "STORY",
  "targetId": 1,
  "reason": "Nội dung không phù hợp, chứa yếu tố bạo lực"
}
```
> `targetType`: `STORY` / `CHAPTER` / `COMMENT`  
**Response:**
```json
{
  "success": true,
  "status": 201,
  "data": {
    "id": 10,
    "reporterId": 3,
    "reporterName": "reader01",
    "targetType": "STORY",
    "targetId": 1,
    "reason": "Nội dung không phù hợp...",
    "status": "PENDING",
    "resolvedAction": null,
    "adminNote": null,
    "createdAt": "2024-03-20T10:00:00"
  }
}
```

---

### GET `/reports/my` 🔒
**Mô tả:** Xem danh sách báo cáo tôi đã gửi.  
**Auth:** Bắt buộc  
**Response:** `List<ReportResponse>`

---

## 15. Role Change Request

### POST `/role-change-requests` 🔒
**Mô tả:** Gửi yêu cầu đổi role. Mỗi user chỉ có 1 yêu cầu PENDING tại 1 thời điểm.  
**Auth:** Bắt buộc  
**Body:**
```json
{
  "requestedRole": "AUTHOR",
  "reason": "Tôi muốn trở thành tác giả để đăng truyện của mình"
}
```
> `requestedRole`: `READER` / `AUTHOR` / `EDITOR` / `REVIEWER`  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": 7,
    "userId": 3,
    "userEmail": "reader01@example.com",
    "userFullName": "Nguyễn Văn A",
    "currentRole": "READER",
    "requestedRole": "AUTHOR",
    "status": "PENDING",
    "reason": "Tôi muốn trở thành tác giả...",
    "adminNote": null,
    "reviewedBy": null,
    "createdAt": "2024-03-20T10:00:00",
    "updatedAt": "2024-03-20T10:00:00"
  }
}
```

---

### GET `/role-change-requests/my` 🔒
**Mô tả:** Xem lịch sử yêu cầu đổi role của tôi.  
**Auth:** Bắt buộc  
**Response:** `List<RoleChangeRequestResponse>`

---

## 16. Reviewer

> Các endpoint dành riêng cho role **REVIEWER** để duyệt nội dung.

### GET `/reviewer/stories/pending` 🔒
**Mô tả:** Danh sách story đang chờ duyệt (status = PENDING).  
**Auth:** Bắt buộc (REVIEWER)  
**Response:** `List<StoryResponse>`

---

### GET `/reviewer/stories/{id}/detail` 🔒
**Mô tả:** Đọc chi tiết story kèm toàn bộ chapter (kể cả PENDING) để xem xét trước khi duyệt.  
**Auth:** Bắt buộc (REVIEWER)  
**Response:** `StoryDetailResponse` (chapters gồm cả PENDING/DRAFT/APPROVED)

---

### POST `/reviewer/stories/{id}/review` 🔒
**Mô tả:** Duyệt / từ chối story.  
**Auth:** Bắt buộc (REVIEWER)  
**Body:**
```json
{
  "action": "APPROVE",
  "note": ""
}
```
Hoặc từ chối:
```json
{
  "action": "REJECT",
  "note": "Story chứa nội dung không phù hợp với chính sách"
}
```
**Response:** `StoryResponse` (status = "APPROVED" hoặc "REJECTED")

---

### GET `/reviewer/chapters/pending` 🔒
**Mô tả:** Danh sách chapter đang chờ duyệt (status = PENDING).  
**Auth:** Bắt buộc (REVIEWER)  
**Response:** `List<ChapterResponse>`

---

### GET `/reviewer/chapters/{id}` 🔒
**Mô tả:** Đọc toàn bộ nội dung chapter để duyệt.  
**Auth:** Bắt buộc (REVIEWER)  
**Response:** `ChapterResponse` (có field `content` đầy đủ)

---

### POST `/reviewer/chapters/{id}/review` 🔒
**Mô tả:** Duyệt / từ chối chapter.  
**Auth:** Bắt buộc (REVIEWER)  
**Body:**
```json
{
  "action": "APPROVE",
  "note": ""
}
```
Hoặc từ chối (chapter quay về DRAFT):
```json
{
  "action": "REJECT",
  "note": "Nội dung cần chỉnh sửa lại đoạn cuối"
}
```
**Response:** `ChapterResponse` (status = "APPROVED" hoặc "DRAFT", reviewNote có giá trị nếu REJECT)

---

## 17. Editor

### GET `/editor/chapters/{chapterId}/versions` 🔒
**Mô tả:** Xem lịch sử các phiên bản nội dung của chapter (Editor hoặc Author xem được).  
**Auth:** Bắt buộc (EDITOR hoặc AUTHOR)  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": 1,
      "chapterId": 10,
      "content": "Nội dung phiên bản cũ...",
      "version": 1,
      "createdAt": "2024-03-01T10:00:00"
    },
    {
      "id": 2,
      "chapterId": 10,
      "content": "Nội dung phiên bản mới hơn...",
      "version": 2,
      "createdAt": "2024-03-15T10:00:00"
    }
  ]
}
```

---

## 18. Admin

> Tất cả endpoint Admin yêu cầu role **ADMIN**.

### GET `/admin/dashboard` 🔒
**Mô tả:** Thống kê tổng quan hệ thống.  
**Auth:** Bắt buộc (ADMIN)  
**Response:**
```json
{
  "success": true,
  "status": 200,
  "data": {
    "totalUsers": 1250,
    "totalStories": 320,
    "totalChapters": 8500,
    "pendingStories": 12,
    "pendingReports": 5,
    "pendingWithdrawRequests": 3,
    "totalCategories": 15,
    "totalComments": 45000
  }
}
```

---

### GET `/admin/stories/pending` 🔒
**Mô tả:** Danh sách truyện chờ duyệt.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<StoryResponse>`

---

### POST `/admin/stories/{id}/review` 🔒
**Mô tả:** Admin duyệt/từ chối truyện.  
**Auth:** Bắt buộc (ADMIN)  
**Body:** `{"action": "APPROVE", "note": ""}` hoặc `{"action": "REJECT", "note": "Lý do"}`  
**Response:** `StoryResponse`

---

### GET `/admin/users` 🔒
**Mô tả:** Danh sách tất cả người dùng.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<UserResponse>`

---

### PUT `/admin/users/roles` 🔒
**Mô tả:** Cập nhật role trực tiếp cho user.  
**Auth:** Bắt buộc (ADMIN)  
**Body:**
```json
{
  "userId": 3,
  "roles": ["READER", "AUTHOR"]
}
```
**Response:** `UserResponse`

---

### POST `/admin/users/{userId}/ban` 🔒
**Mô tả:** Khóa tài khoản.  
**Auth:** Bắt buộc (ADMIN)  
**Query param:** `banDays=7` (số ngày) hoặc `banDays=-1` (vĩnh viễn)  
**Response:** `UserResponse` (banUntil có giá trị)

---

### POST `/admin/users/{userId}/unban` 🔒
**Mô tả:** Gỡ khóa tài khoản.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `UserResponse` (banUntil = null)

---

### GET `/admin/reports` 🔒
**Mô tả:** Tất cả báo cáo vi phạm.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<ReportResponse>`

---

### GET `/admin/reports/pending` 🔒
**Mô tả:** Báo cáo chưa xử lý.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<ReportResponse>`

---

### POST `/admin/reports/{id}/resolve` 🔒
**Mô tả:** Xử lý báo cáo vi phạm.  
**Auth:** Bắt buộc (ADMIN)  
**Body:**
```json
{
  "action": "HIDE_AND_BAN",
  "adminNote": "Vi phạm nghiêm trọng",
  "banDays": 30
}
```
> **action options:**
> - `WARN_ONLY` — đánh dấu đã xử lý, không làm gì thêm
> - `HIDE_CONTENT` — ẩn nội dung (comment → hidden, chapter → HIDDEN, story → REJECTED)
> - `DELETE_CONTENT` — xóa nội dung
> - `BAN_USER` — ban tài khoản tác giả (cần banDays)
> - `HIDE_AND_BAN` — ẩn nội dung + ban tác giả
> - `DELETE_AND_BAN` — xóa nội dung + ban tác giả

**Response:** `ReportResponse` (status = "RESOLVED", resolvedAction có giá trị)

---

### GET `/admin/withdraw-requests` 🔒
**Mô tả:** Tất cả yêu cầu rút tiền.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<WithdrawRequestResponse>`

---

### GET `/admin/withdraw-requests/pending` 🔒
**Mô tả:** Yêu cầu rút tiền chờ duyệt.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<WithdrawRequestResponse>`

---

### POST `/admin/withdraw-requests/{id}/approve` 🔒
**Mô tả:** Duyệt yêu cầu rút tiền (trừ coin từ ví user).  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `WithdrawRequestResponse` (status = "APPROVED")

---

### POST `/admin/withdraw-requests/{id}/reject` 🔒
**Mô tả:** Từ chối yêu cầu rút tiền.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `WithdrawRequestResponse` (status = "REJECTED")

---

### POST `/admin/missions` 🔒
**Mô tả:** Tạo nhiệm vụ mới.  
**Auth:** Bắt buộc (ADMIN)  
**Body:**
```json
{
  "name": "Đọc 5 chương mỗi ngày",
  "rewardCoin": 15,
  "type": "DAILY_READ"
}
```
**Response:** `MissionResponse`

---

### PUT `/admin/missions/{id}` 🔒
**Mô tả:** Cập nhật nhiệm vụ.  
**Auth:** Bắt buộc (ADMIN)  
**Body:** Giống POST  
**Response:** `MissionResponse`

---

### DELETE `/admin/missions/{id}` 🔒
**Mô tả:** Xóa nhiệm vụ.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** 200 OK

---

### GET `/admin/role-change-requests` 🔒
**Mô tả:** Tất cả yêu cầu đổi role.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<RoleChangeRequestResponse>`

---

### GET `/admin/role-change-requests/pending` 🔒
**Mô tả:** Yêu cầu đổi role đang chờ duyệt.  
**Auth:** Bắt buộc (ADMIN)  
**Response:** `List<RoleChangeRequestResponse>`

---

### POST `/admin/role-change-requests/review` 🔒
**Mô tả:** Admin duyệt/từ chối yêu cầu đổi role.  
**Auth:** Bắt buộc (ADMIN)  
**Body:**
```json
{
  "requestId": 7,
  "action": "APPROVE",
  "adminNote": ""
}
```
> `action`: `APPROVE` → gán role mới, xóa role cũ (exclusive) | `REJECT` → từ chối  
**Response:** `RoleChangeRequestResponse` (status = "APPROVED" hoặc "REJECTED")

---

## 📊 Tóm tắt các trạng thái (Status)

### Story Status
| Status | Mô tả |
|--------|-------|
| `DRAFT` | Mới tạo, chưa nộp duyệt |
| `PENDING` | Đã nộp, chờ Reviewer duyệt |
| `APPROVED` | Được duyệt (công khai trên trang chủ nếu có ≥1 chương PUBLISHED) |
| `REJECTED` | Bị từ chối (Author xem được lý do trong `rejectReason`) |

### Chapter Status
| Status | Mô tả |
|--------|-------|
| `DRAFT` | Nháp, chưa nộp duyệt |
| `PENDING` | Đã nộp, chờ Reviewer duyệt |
| `APPROVED` | Được Reviewer duyệt, chờ Author tự publish |
| `PUBLISHED` | Đã xuất bản, công khai cho độc giả |
| `REJECTED` | Bị từ chối (chapter về lại DRAFT với `reviewNote`) |

### EditRequest Status
| Status | Mô tả |
|--------|-------|
| `OPEN` | Chờ Editor nhận việc |
| `IN_PROGRESS` | Editor đang làm |
| `SUBMITTED` | Editor đã nộp bản, chờ Author duyệt |
| `APPROVED` | Author chấp thuận, coin chuyển cho Editor |
| `CANCELLED` | Author huỷ (trước khi có Editor) |

### Report Status
| Status | Mô tả |
|--------|-------|
| `PENDING` | Chờ Admin xử lý |
| `RESOLVED` | Đã xử lý |

### RoleChangeRequest Status
| Status | Mô tả |
|--------|-------|
| `PENDING` | Chờ Admin duyệt |
| `APPROVED` | Được duyệt |
| `REJECTED` | Bị từ chối |

### WithdrawRequest Status
| Status | Mô tả |
|--------|-------|
| `PENDING` | Chờ Admin duyệt |
| `APPROVED` | Đã duyệt (đã chuyển tiền) |
| `REJECTED` | Bị từ chối |

---

## 🔐 Hệ thống Role

| Role | Quyền |
|------|-------|
| `READER` | Đọc truyện, comment, follow, mua chương, đánh giá, gửi quà |
| `AUTHOR` | Tất cả của READER + tạo/sửa truyện, viết chương, tạo EditRequest |
| `EDITOR` | Tất cả của READER + nhận EditRequest, chỉnh sửa chapter thuê |
| `REVIEWER` | Tất cả của READER + duyệt/từ chối story và chapter |
| `ADMIN` | Toàn quyền hệ thống |

> **Lưu ý:** Mỗi user có role `READER` làm nền. Chỉ được thêm **1 role exclusive** (`AUTHOR`, `EDITOR`, hoặc `REVIEWER`). ADMIN là role riêng biệt.

---

## 🛒 Luồng mua chương trả phí

```
1. GET /stories/{id}/detail          → xem danh sách chương (có isPurchased, coinPrice)
2. GET /wallet                        → kiểm tra số dư coin
3. POST /wallet/topup                 → nạp coin nếu thiếu
4. POST /chapters/{id}/purchase       → mua chương
5. GET /chapters/{id}                 → đọc nội dung đầy đủ
```

---

## ✍️ Luồng Author tạo và publish chương

```
1. POST /stories                      → tạo truyện (DRAFT)
2. POST /chapters/story/{id}          → viết chương (DRAFT)
3. POST /stories/{id}/submit          → nộp truyện lên Reviewer
4. POST /chapters/{id}/submit         → nộp từng chương lên Reviewer
   -- Reviewer xem và duyệt --
5. [Reviewer] POST /reviewer/chapters/{id}/review  → APPROVE
6. POST /chapters/{id}/publish        → tác giả tự publish chương đã được duyệt
```

---

## 🎨 Luồng Edit Request (thuê Editor)

```
1. [AUTHOR] POST /edit-requests       → tạo request, coin bị LOCK
2. [EDITOR] GET /edit-requests/open   → xem danh sách request đang mở
3. [EDITOR] POST /edit-requests/{id}/assign   → nhận việc
4. [EDITOR] PUT /edit-requests/{id}/submit    → nộp bản chỉnh sửa
5. [AUTHOR] xem editedContent trong response
   → POST /edit-requests/{id}/approve  → duyệt, coin → Editor
   → POST /edit-requests/{id}/reject   → từ chối, Editor viết lại
```

---

*Tài liệu này được tạo tự động từ source code. Cập nhật lần cuối: 20/03/2026*

