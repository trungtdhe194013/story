# 📚 Digital Publishing — Frontend API Guide

> **Base URL:** `http://localhost:8080`  
> **Auth:** Bearer Token — gắn vào header `Authorization: Bearer <token>`  
> **All responses** được bọc bởi `GlobalResponseWrapper`:
> ```json
> { "success": true, "status": 200, "data": { ... } }
> ```
> *(Lấy phần `data` để dùng)*

---

## ✅ BUG FIX ĐÃ SỬA (quan trọng)

### Problem — 500 khi gọi public endpoints không có token
**Root cause:** `UserServiceImpl.getCurrentUser()` bị đánh dấu `@Transactional` ở class level. Khi user chưa đăng nhập (anonymous), method này ném `RuntimeException`, khiến Spring **đánh dấu outer transaction là rollback-only**. Dù caller đã `try-catch`, transaction đã bị poison → commit fail → **500**.

**Fix đã apply:** 
1. `getCurrentUser()` giờ check `AnonymousAuthenticationToken` TRƯỚC khi query DB.
2. Override transaction với `@Transactional(noRollbackFor = Exception.class)` — exception từ method này **không làm ảnh hưởng outer transaction**.

**Kết quả:** Tất cả public endpoints bên dưới giờ **trả về 200** dù không có `Authorization` header.

---

## 🔐 Auth Endpoints (`/api/auth`)

> **Không cần token cho tất cả endpoints trong nhóm này.**

### `POST /api/auth/sign-up` — Đăng ký Step 1: Gửi OTP
**Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123",
  "fullName": "Nguyen Van A"
}
```
**Response:**
```json
{
  "message": "Mã OTP đã được gửi đến email ...",
  "email": "user@example.com",
  "devOtp": "123456"
}
```

### `POST /api/auth/sign-up/verify-otp` — Đăng ký Step 2: Xác nhận OTP
**Body:**
```json
{ "email": "user@example.com", "otp": "123456" }
```
**Response:** `UserResponse` (xem cấu trúc bên dưới)

### `POST /api/auth/sign-up/resend-otp?email=...` — Gửi lại OTP

### `POST /api/auth/login` — Đăng nhập
**Body:**
```json
{ "email": "user@example.com", "password": "Password123" }
```
**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "uuid-refresh-token"
}
```
> ⚠️ **Nếu tài khoản bị ban:** trả về **403** với message mô tả rõ thời gian ban (hoặc "vĩnh viễn").

### `POST /api/auth/refresh?refreshToken=...` — Làm mới token
**Response:** `{ "accessToken": "...", "refreshToken": "..." }`

### `POST /api/auth/logout?refreshToken=...` — Đăng xuất

### `POST /api/auth/forgot-password` — Quên mật khẩu Step 1: Gửi OTP
**Body:** `{ "email": "user@example.com" }`

### `POST /api/auth/reset-password` — Quên mật khẩu Step 2: Đặt mật khẩu mới
**Body:** `{ "email": "...", "otp": "123456", "newPassword": "NewPass123", "confirmNewPassword": "NewPass123" }`

---

## 👤 User Endpoints (`/api/users`) 🔒

### `GET /api/users/me` — Lấy profile
**Response: `UserResponse`**
```json
{
  "id": 1,
  "email": "user@example.com",
  "fullName": "Nguyen Van A",
  "roles": ["READER", "AUTHOR"],
  "provider": "LOCAL",
  "enabled": true,
  "avatarUrl": "http://localhost:8080/uploads/avatars/avatar_uuid.jpg",
  "bio": "Mô tả bản thân",
  "phone": "0909123456",
  "dateOfBirth": "1995-05-15",
  "gender": "MALE",
  "location": "Ho Chi Minh City",
  "walletBalance": 500,
  "banUntil": null,
  "totalFollowedStories": 5,
  "totalPurchasedChapters": 12,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-06-01T08:30:00"
}
```

### `PUT /api/users/me` — Cập nhật profile
**Body:**
```json
{
  "fullName": "Nguyen Van B",
  "avatarUrl": "http://localhost:8080/uploads/avatars/avatar_uuid.jpg",
  "bio": "...",
  "phone": "0909000000",
  "dateOfBirth": "1995-05-15",
  "gender": "MALE",
  "location": "Ha Noi"
}
```
**Response:** `UserResponse` đầy đủ (giống GET /me)

### `POST /api/users/me/avatar` — Upload ảnh đại diện
**Content-Type:** `multipart/form-data`  
**Field:** `file` — file ảnh (JPG/PNG/GIF/WebP, max 5MB)  
**Response:**
```json
{ "avatarUrl": "http://localhost:8080/uploads/avatars/avatar_uuid.jpg" }
```
> Sau đó dùng `avatarUrl` này trong PUT `/api/users/me`.

### `PUT /api/users/me/change-password` — Đổi mật khẩu
**Body:** `{ "currentPassword": "...", "newPassword": "...", "confirmNewPassword": "..." }`  
**Response:** `204 No Content`

---

## 📖 Story Endpoints (`/api/stories`)

> 🌍 **Public** (không cần token): GET all, GET search, GET by ID, GET detail, GET stats  
> 🔒 **Cần token**: POST, PUT, DELETE, POST submit

### `GET /api/stories` — Danh sách truyện trang chủ *(public)*
Trả về truyện **APPROVED**, chưa bị xóa mềm, có ít nhất 1 chương PUBLISHED.  
**Response: `List<StoryResponse>`**
```json
[
  {
    "id": 1,
    "title": "Vạn Cổ Thần Vương",
    "summary": "...",
    "coverUrl": "...",
    "status": "APPROVED",
    "authorId": 2,
    "authorName": "Nguyen Van A",
    "categories": [
      { "id": 1, "name": "Tiên Hiệp" }
    ],
    "viewCount": 15000,
    "avgRating": 4.5,
    "ratingCount": 120,
    "isCompleted": false,
    "createdAt": "...",
    "updatedAt": "...",
    "publishedChapterCount": 25,
    "totalChapterCount": 30,
    "isDeleted": false,
    "followCount": 350,
    "isFollowing": true
  }
]
```
> `isFollowing`: `true/false` nếu đã đăng nhập, `null` nếu chưa đăng nhập.

### `GET /api/stories/search?keyword=...` — Tìm kiếm *(public)*
**Response:** `List<StoryResponse>`

### `GET /api/stories/my` — Truyện của tôi 🔒
Trả về tất cả truyện (mọi status kể cả DRAFT, bị xóa mềm).

### `GET /api/stories/{id}` — Chi tiết cơ bản *(public)*
**Response:** `StoryResponse`

### `GET /api/stories/{id}/detail` — Chi tiết đầy đủ + danh sách chương *(public)*
**Response: `StoryDetailResponse`**
```json
{
  "id": 1,
  "title": "Vạn Cổ Thần Vương",
  "summary": "...",
  "coverUrl": "...",
  "status": "APPROVED",
  "authorId": 2,
  "authorName": "...",
  "viewCount": 15000,
  "avgRating": 4.5,
  "ratingCount": 120,
  "isCompleted": false,
  "categories": [ { "id": 1, "name": "Tiên Hiệp" } ],
  "createdAt": "...",
  "updatedAt": "...",
  "myRating": 5,
  "myReview": "Truyện rất hay!",
  "chapters": [
    {
      "id": 10,
      "title": "Chương 1: Khởi Đầu",
      "chapterOrder": 1,
      "coinPrice": 0,
      "status": "PUBLISHED",
      "publishAt": "...",
      "isPurchased": false
    }
  ],
  "totalChapters": 25,
  "allChaptersCount": 30,
  "followCount": 350,
  "isFollowing": false
}
```
> `myRating`, `myReview`: null nếu chưa đăng nhập hoặc chưa đánh giá.  
> `isFollowing`: null nếu chưa đăng nhập.  
> `chapters`: chỉ các chương **PUBLISHED**, không có `content` (tiết kiệm băng thông).

### `GET /api/stories/{id}/chapter-stats` — Thống kê chương *(public)*
```json
{
  "storyId": 1,
  "storyTitle": "Vạn Cổ Thần Vương",
  "publishedCount": 25,
  "totalCount": 30
}
```

### `GET /api/stories/rankings` — Top xem nhiều *(public)*
### `GET /api/stories/top-rated` — Top đánh giá cao *(public)*
### `GET /api/stories/completed` — Truyện đã hoàn thành *(public)*
### `GET /api/stories/category/{categoryId}` — Truyện theo thể loại *(public)*
**Response:** `List<StoryResponse>`

### `POST /api/stories` 🔒 — Tạo truyện mới (AUTHOR)
**Body:**
```json
{
  "title": "Tên truyện",
  "summary": "Tóm tắt...",
  "coverUrl": "https://...",
  "categoryIds": [1, 2]
}
```
**Response:** `StoryResponse` (status = `DRAFT`)

### `PUT /api/stories/{id}` 🔒 — Cập nhật truyện (AUTHOR)
**Body:** giống POST, tất cả field.

### `DELETE /api/stories/{id}` 🔒 — Xóa mềm truyện (AUTHOR)
**Response:** `204 No Content`

### `POST /api/stories/{id}/submit` 🔒 — Nộp truyện cho Reviewer (AUTHOR)
Chuyển trạng thái `DRAFT/REJECTED` → `PENDING`.  
**Response:** `StoryResponse`

---

## 📄 Chapter Endpoints (`/api/chapters`)

> 🌍 `GET /api/chapters/**` — public  
> 🔒 POST/PUT/DELETE — cần token

### `GET /api/chapters/{id}` — Đọc chapter *(public)*
**Response: `ChapterResponse`**
```json
{
  "id": 10,
  "storyId": 1,
  "storyTitle": "Vạn Cổ Thần Vương",
  "title": "Chương 1: Khởi Đầu",
  "content": "Nội dung đầy đủ...",
  "chapterOrder": 1,
  "coinPrice": 0,
  "status": "PUBLISHED",
  "publishAt": "...",
  "createdAt": "...",
  "updatedAt": "...",
  "isPurchased": false,
  "reviewNote": null,
  "comments": [ { ...CommentResponse... } ],
  "totalComments": 5
}
```
> Nếu chapter có `coinPrice > 0` và user chưa mua: `content` = `"[Locked] Purchase this chapter to read"`.  
> `reviewNote`: lý do reviewer từ chối (chỉ hiện khi status = DRAFT sau khi bị reject).

### `GET /api/chapters/story/{storyId}` — Tất cả chương của truyện *(public)*
- Nếu là **Author**: trả về tất cả chương (mọi status).
- Nếu là **Reader**: chỉ trả về chương **PUBLISHED**.

**Response:** `List<ChapterResponse>` (không có content đầy đủ, không có comments)

### `POST /api/chapters/story/{storyId}` 🔒 — Tạo chương mới (AUTHOR)
**Body:**
```json
{
  "title": "Chương 2: ...",
  "content": "Nội dung...",
  "chapterOrder": 2,
  "coinPrice": 10,
  "publishAt": null
}
```
**Response:** `ChapterResponse` (status = `DRAFT`)

### `PUT /api/chapters/{id}` 🔒 — Cập nhật chương (AUTHOR)
### `DELETE /api/chapters/{id}` 🔒 — Xóa chương (AUTHOR) → `204`

### `POST /api/chapters/{id}/submit` 🔒 — Nộp chương cho Reviewer (AUTHOR)
Chuyển `DRAFT/EDITED` → `PENDING_REVIEW`.  
**Response:** `ChapterResponse`

### `POST /api/chapters/{id}/publish` 🔒 — Publish chương (AUTHOR)
Chỉ dùng sau khi Reviewer đã **APPROVE**. Chuyển `APPROVED` → `PUBLISHED`.  
**Response:** `ChapterResponse`

### `POST /api/chapters/{id}/purchase` 🔒 — Mua chương (READER)
Trừ coin từ ví, ghi nhận mua chương.  
**Response:** `ChapterResponse` (có content đầy đủ sau khi mua)

---

## 💬 Comment Endpoints (`/api/comments`)

> `GET` comments — public. `POST/DELETE` — cần token.

### `GET /api/comments/chapter/{chapterId}` — Comments của chapter *(public)*
**Response: `List<CommentResponse>`** (dạng cây, cha-con đệ quy)
```json
[
  {
    "id": 1,
    "chapterId": 10,
    "chapterTitle": "Chương 1",
    "chapterOrder": 1,
    "storyId": 1,
    "storyTitle": "Vạn Cổ Thần Vương",
    "userId": 3,
    "userName": "Reader A",
    "userAvatarUrl": "...",
    "content": "Truyện hay quá!",
    "parentId": null,
    "replies": [
      {
        "id": 2,
        "parentId": 1,
        "content": "Đồng ý!",
        "replies": [],
        ...
      }
    ],
    "hidden": false,
    "likeCount": 10,
    "createdAt": "..."
  }
]
```

### `GET /api/comments/{id}` — Chi tiết 1 comment *(public)*
**Response:** `CommentResponse` (kèm toàn bộ replies đệ quy)

### `POST /api/comments` 🔒 — Tạo comment / reply
**Body:**
```json
{
  "chapterId": 10,
  "content": "Nội dung comment...",
  "parentId": null
}
```
> `parentId`: null = comment gốc, số = reply vào comment đó (đệ quy vô hạn cấp).

### `DELETE /api/comments/{id}` 🔒 — Xóa comment → `204`

---

## ⭐ Rating Endpoints (`/api/ratings`)

### `POST /api/ratings` 🔒 — Đánh giá truyện (1-5 sao)
```json
{ "storyId": 1, "score": 5, "review": "Truyện tuyệt vời!" }
```
> Đánh giá lại sẽ **cập nhật** điểm cũ (không tạo mới).

**Response: `RatingResponse`**
```json
{
  "id": 1, "userId": 3, "userName": "Reader A",
  "storyId": 1, "storyTitle": "...",
  "score": 5, "review": "...",
  "createdAt": "...", "updatedAt": "..."
}
```

### `GET /api/ratings/story/{storyId}` — Tất cả đánh giá của truyện *(public)*
### `GET /api/ratings/my/{storyId}` 🔒 — Đánh giá của tôi cho truyện

---

## 💖 Follow Endpoints (`/api/follows`) 🔒

### `POST /api/follows/{storyId}` — Toggle Follow/Unfollow
**Response: `FollowResponse`**
```json
{
  "storyId": 1,
  "storyTitle": "Vạn Cổ Thần Vương",
  "status": "FOLLOWED",
  "message": "Đã theo dõi truyện Vạn Cổ Thần Vương",
  "followCount": 351
}
```
> `status`: `FOLLOWED` hoặc `UNFOLLOWED`

### `GET /api/follows` 🔒 — Danh sách truyện đang follow
**Response:** `List<StoryResponse>`

### `GET /api/follows/{storyId}/status` 🔒 — Kiểm tra đang follow chưa
**Response:** `true` / `false`

### `GET /api/follows/story/{storyId}/count` *(public)* — Số người follow
**Response:** `350` (số nguyên)

---

## 🔔 Notification Endpoints (`/api/notifications`) 🔒

### `GET /api/notifications` — Lấy tất cả thông báo
**Response: `List<NotificationResponse>`** (mới nhất lên đầu)
```json
[
  {
    "id": 1,
    "type": "NEW_CHAPTER",
    "title": "Chương mới từ Vạn Cổ Thần Vương",
    "message": "Chương 26: Thiên Địa Biến Sắc vừa được xuất bản!",
    "refId": 26,
    "refType": "CHAPTER",
    "isRead": false,
    "createdAt": "2024-06-15T10:30:00"
  }
]
```

**Các loại `type`:**
| type | Mô tả | refType | refId |
|------|-------|---------|-------|
| `NEW_CHAPTER` | Truyện follow có chương mới | `CHAPTER` | chapterId |
| `STORY_APPROVED` | Story được Reviewer duyệt | `STORY` | storyId |
| `STORY_REJECTED` | Story bị từ chối | `STORY` | storyId |
| `CHAPTER_APPROVED` | Chapter được duyệt (Author tự publish) | `CHAPTER` | chapterId |
| `CHAPTER_REJECTED` | Chapter bị từ chối, kèm lý do | `CHAPTER` | chapterId |
| `GIFT_RECEIVED` | Có người tặng quà | `GIFT` | giftId |
| `SYSTEM` | Thông báo từ Admin | `SYSTEM` | null |

### `GET /api/notifications/unread-count` — Số thông báo chưa đọc
```json
{ "count": 5 }
```

### `PUT /api/notifications/mark-all-read` — Đánh dấu tất cả đã đọc
```json
{ "message": "Đã đánh dấu tất cả thông báo là đã đọc" }
```

### `PUT /api/notifications/{id}/read` — Đánh dấu 1 thông báo đã đọc
**Response:** `NotificationResponse`

### `DELETE /api/notifications/{id}` — Xóa thông báo
```json
{ "message": "Đã xoá thông báo" }
```

---

## 💰 Payment & Wallet Endpoints

### `GET /api/payment/packages` — Danh sách gói coin *(public)*
**Response: `List<CoinPackageResponse>`**
```json
[
  { "id": "BASIC",    "displayName": "Gói Cơ Bản",      "amountVnd": 10000,  "coinAmount": 10000,  "bonusPercent": 0  },
  { "id": "SAVING",   "displayName": "Gói Tiết Kiệm",   "amountVnd": 20000,  "coinAmount": 22000,  "bonusPercent": 10 },
  { "id": "POPULAR",  "displayName": "Gói Phổ Biến ⭐", "amountVnd": 50000,  "coinAmount": 57500,  "bonusPercent": 15 },
  { "id": "PREMIUM",  "displayName": "Gói Cao Cấp",     "amountVnd": 100000, "coinAmount": 120000, "bonusPercent": 20 },
  { "id": "ULTIMATE", "displayName": "Gói Siêu Cấp",   "amountVnd": 200000, "coinAmount": 250000, "bonusPercent": 25 }
]
```

### `POST /api/payment/create-link` 🔒 — Tạo link thanh toán PayOS
**Body:** `{ "packageId": "POPULAR" }`  
**Response: `PaymentLinkResponse`**
```json
{
  "orderCode": 123456789,
  "packageId": "POPULAR",
  "packageName": "Gói Phổ Biến ⭐",
  "amountVnd": 50000,
  "coinAmount": 57500,
  "bonusPercent": 15,
  "checkoutUrl": "https://pay.payos.vn/web/..."
}
```
> **Frontend flow:** Nhận `checkoutUrl` → redirect user sang trang PayOS → PayOS redirect về `returnUrl` → frontend gọi `GET /api/payment/verify/{orderCode}`.

### `GET /api/payment/verify/{orderCode}` 🔒 — Xác minh kết quả thanh toán
> Gọi ngay sau khi PayOS redirect về. Backend tự query PayOS → cộng coin nếu PAID.

**Response: `PaymentOrderResponse`**
```json
{
  "id": 1,
  "orderCode": 123456789,
  "packageId": "POPULAR",
  "packageName": "Gói Phổ Biến ⭐",
  "amountVnd": 50000,
  "coinAmount": 57500,
  "status": "PAID",
  "checkoutUrl": "...",
  "createdAt": "...",
  "paidAt": "2024-06-15T10:35:00"
}
```
> `status`: `PENDING` | `PAID` | `CANCELLED`

### `POST /api/payment/recover` 🔒 — Khôi phục coin cho đơn PENDING
Dùng khi đã chuyển tiền nhưng coin chưa vào.  
**Response:** `List<PaymentOrderResponse>`

### `GET /api/payment/history` 🔒 — Lịch sử nạp coin
**Response:** `List<PaymentOrderResponse>`

---

### `GET /api/wallet` 🔒 — Xem ví
```json
{
  "userId": 1,
  "userName": "...",
  "balance": 5000,
  "lockedBalance": 200,
  "updatedAt": "..."
}
```
> `lockedBalance`: coin đang bị khoá do EditRequest (tác giả đặt thưởng cho editor).

### `GET /api/wallet/transactions` 🔒 — Lịch sử giao dịch
```json
[
  { "id": 1, "userId": 1, "amount": 57500, "type": "TOPUP", "refId": null, "createdAt": "..." },
  { "id": 2, "userId": 1, "amount": -10,   "type": "BUY",   "refId": 10,  "createdAt": "..." }
]
```
> `type`: `TOPUP` | `BUY` | `GIFT` | `REWARD`

---

## 💸 Withdraw Request (`/api/withdraw-requests`) 🔒

### `POST /api/withdraw-requests` — Yêu cầu rút tiền
**Body:**
```json
{
  "amount": 50000,
  "bankName": "Vietcombank",
  "bankAccount": "1234567890",
  "bankOwner": "NGUYEN VAN A",
  "note": "Rút tiền tháng 6"
}
```
> `amount` phải nhỏ hơn hoặc bằng `balance` trong ví.

**Response: `WithdrawRequestResponse`**
```json
{
  "id": 1, "userId": 1, "userName": "...",
  "amount": 50000,
  "status": "PENDING",
  "bankName": "Vietcombank",
  "bankAccount": "1234567890",
  "bankOwner": "NGUYEN VAN A",
  "note": "...",
  "processedById": null, "processedByName": null,
  "processedAt": null, "rejectedReason": null,
  "createdAt": "..."
}
```
> `status`: `PENDING` | `APPROVED` | `REJECTED`  
> Admin sẽ tự chuyển khoản và duyệt thủ công.

### `GET /api/withdraw-requests/my` — Lịch sử yêu cầu rút tiền
**Response:** `List<WithdrawRequestResponse>`

---

## 🎖 Daily Streak (`/api/streak`) 🔒

### `POST /api/streak/check-in` — Check-in hàng ngày
**Response: `StreakResponse`**
```json
{
  "currentStreak": 7,
  "longestStreak": 14,
  "lastCheckInDate": "2024-06-15",
  "hasClaimedToday": true,
  "coinEarned": 50,
  "message": "Check-in ngày 7! Bạn nhận được 50 coin."
}
```

### `GET /api/streak/status` 🔒 — Xem trạng thái streak hiện tại

---

## 📝 Edit Request — Tác Giả ↔ Editor Marketplace (`/api/edit-requests`)

### Luồng nghiệp vụ:
```
Author tạo request (OPEN) → Editor nhận (IN_PROGRESS) → Editor nộp (SUBMITTED)
  → Author đồng ý (APPROVED, coin release) hoặc Từ chối (IN_PROGRESS, Editor viết lại)
```

### `POST /api/edit-requests` 🔒 — [AUTHOR] Tạo yêu cầu
```json
{ "chapterId": 10, "coinReward": 200, "description": "Cần sửa lỗi văn phong, thêm chi tiết cảnh mở đầu" }
```
> Coin bị **lock** ngay vào escrow từ ví Author.

### `GET /api/edit-requests/my` 🔒 — [AUTHOR] Xem requests của tôi
### `POST /api/edit-requests/{id}/approve` 🔒 — [AUTHOR] Chấp thuận bản edit
> Coin escrow chuyển sang ví Editor. Chapter content được cập nhật.

### `POST /api/edit-requests/{id}/reject` 🔒 — [AUTHOR] Từ chối, Editor viết lại
```json
{ "note": "Văn phong chưa ổn, cần chỉnh lại đoạn giữa" }
```
> Editor được viết lại vô hạn lần.

### `DELETE /api/edit-requests/{id}/cancel` 🔒 — [AUTHOR] Huỷ (chỉ khi OPEN)
> Coin được hoàn lại cho Author.

### `GET /api/edit-requests/open` 🔒 — [EDITOR] Xem danh sách việc đang tuyển
### `POST /api/edit-requests/{id}/assign` 🔒 — [EDITOR] Nhận việc
### `PUT /api/edit-requests/{id}/submit` 🔒 — [EDITOR] Nộp bản edit
```json
{ "editedContent": "Nội dung đã sửa...", "editorNote": "Đã sửa văn phong và bổ sung chi tiết" }
```
### `POST /api/edit-requests/{id}/withdraw` 🔒 — [EDITOR] Rút lui (chỉ khi chưa bị reject)
### `GET /api/edit-requests/assigned` 🔒 — [EDITOR] Lịch sử việc đã nhận

**Response `EditRequestResponse`:**
```json
{
  "id": 1,
  "chapterId": 10, "chapterTitle": "Chương 1", "storyTitle": "Vạn Cổ Thần Vương",
  "authorId": 2, "authorName": "...",
  "editorId": 5, "editorName": "...",
  "coinReward": 200,
  "description": "...",
  "editedContent": "...",
  "editorNote": "...",
  "authorNote": "...",
  "status": "SUBMITTED",
  "attemptCount": 1,
  "createdAt": "...", "updatedAt": "..."
}
```

---

## 🔍 Reviewer Endpoints (`/api/reviewer`) 🔒 (REVIEWER, ADMIN)

### `GET /api/reviewer/stories/pending` — Danh sách story chờ duyệt
### `GET /api/reviewer/stories/{id}/detail` — Đọc chi tiết story để duyệt
### `POST /api/reviewer/stories/{id}/review` — Duyệt / từ chối story
```json
{ "action": "APPROVE" }
// hoặc
{ "action": "REJECT", "reason": "Nội dung vi phạm..." }
```

### `GET /api/reviewer/chapters/pending` — Danh sách chapter chờ duyệt
### `GET /api/reviewer/chapters/{id}` — Đọc full nội dung chapter để duyệt
### `POST /api/reviewer/chapters/{id}/review` — Duyệt / từ chối chapter
```json
{ "action": "APPROVE" }
// hoặc
{ "action": "REJECT", "note": "Nội dung chưa đạt yêu cầu..." }
```
> APPROVE → chapter chuyển sang **APPROVED** (Author tự publish sau)  
> REJECT → chapter trả về **DRAFT** kèm `reviewNote`

---

### Lịch sử duyệt (Review History)

> Tất cả endpoints lịch sử đều trả về `List<ReviewHistoryResponse>` sắp xếp **mới nhất lên đầu**.

**`ReviewHistoryResponse`:**
```json
{
  "id": 1,
  "reviewerId": 5,
  "reviewerName": "Reviewer A",
  "targetType": "CHAPTER",
  "targetId": 10,
  "targetTitle": "Chương 3: Đại Chiến",
  "storyTitle": "Vạn Cổ Thần Vương",
  "action": "REJECT",
  "note": "Nội dung chưa đủ 1000 từ, cần bổ sung thêm chi tiết",
  "currentStatus": "DRAFT",
  "createdAt": "2024-06-15T14:20:00"
}
```

| field | Ý nghĩa |
|-------|---------|
| `targetType` | `STORY` hoặc `CHAPTER` |
| `targetTitle` | Tên story hoặc tên chapter đã duyệt |
| `storyTitle` | Tên truyện — chỉ có khi `targetType = CHAPTER` |
| `action` | `APPROVE` hoặc `REJECT` |
| `note` | Lý do từ chối (chỉ có khi `action = REJECT`) |
| `currentStatus` | Trạng thái hiện tại của story/chapter sau khi duyệt |

### `GET /api/reviewer/history` — Toàn bộ lịch sử duyệt của tôi (STORY + CHAPTER)
### `GET /api/reviewer/history/stories` — Chỉ lịch sử duyệt story
### `GET /api/reviewer/history/chapters` — Chỉ lịch sử duyệt chapter
### `GET /api/reviewer/history/story/{storyId}` — Lịch sử duyệt của 1 story cụ thể
> Ai đã duyệt, khi nào, approve hay reject, lý do gì.

### `GET /api/reviewer/history/chapter/{chapterId}` — Lịch sử duyệt của 1 chapter cụ thể

---

## 🔐 Role Change Request (`/api/role-change-requests`) 🔒

### `POST /api/role-change-requests` — Gửi yêu cầu đổi role
```json
{ "requestedRole": "AUTHOR", "reason": "Tôi muốn viết truyện..." }
```
> Role hệ thống: `READER` (mặc định) + có thể thêm 1 trong: `AUTHOR`, `REVIEWER`, `EDITOR`

### `GET /api/role-change-requests/my` — Xem requests của tôi

---

## 📊 Admin Endpoints (`/api/admin`) 🔒 (ADMIN only)

### Quản lý User
- `GET /api/admin/users` — Danh sách tất cả user
- `GET /api/admin/users/{id}` — Chi tiết user
- `PUT /api/admin/users/{id}/ban` — Ban tài khoản
- `PUT /api/admin/users/{id}/unban` — Gỡ ban
- `PUT /api/admin/users/{id}/role` — Cập nhật role
- `GET /api/admin/reports` — Danh sách báo cáo vi phạm
- `PUT /api/admin/reports/{id}/resolve` — Xử lý báo cáo

### Quản lý Withdraw
- `GET /api/admin/withdraw-requests` — Tất cả yêu cầu rút tiền
- `POST /api/admin/withdraw-requests/{id}/approve` — Duyệt
- `POST /api/admin/withdraw-requests/{id}/reject` — Từ chối

### Thống kê
- `GET /api/admin/dashboard` — Dashboard stats

---

## 🗂 Category Endpoints (`/api/categories`)

### `GET /api/categories` — Tất cả thể loại *(public)*
```json
[
  { "id": 1, "name": "Tiên Hiệp" },
  { "id": 2, "name": "Ngôn Tình" }
]
```
### `POST /api/categories` 🔒 (ADMIN) — Tạo thể loại
### `PUT /api/categories/{id}` 🔒 (ADMIN) — Cập nhật
### `DELETE /api/categories/{id}` 🔒 (ADMIN) — Xóa

---

## 📌 Content Workflow — Sơ đồ trạng thái

### Story Status
```
DRAFT → (author submit) → PENDING → (reviewer) → APPROVED
                                                 → REJECTED → DRAFT (fix + resubmit)
```

### Chapter Status
```
DRAFT → (author submit) → PENDING_REVIEW → (reviewer APPROVE) → APPROVED
                                                               → (reviewer REJECT) → DRAFT (fix + resubmit)
                       → (author publish) → PUBLISHED
                                          ↑
                              (sau khi reviewer APPROVE)
```

### Edit Request Status
```
OPEN → (editor assign) → IN_PROGRESS → (editor submit) → SUBMITTED
                                  ↑   (author reject)         ↓
                                  └────────────────────── (author approve) → APPROVED
OPEN → (author cancel) → CANCELLED
```

---

## ⚡ Tips cho Frontend

1. **Token lưu ở đâu:** `localStorage` hoặc `sessionStorage`. Key: `accessToken`, `refreshToken`.

2. **Auto refresh token:** Khi nhận 401, tự động gọi `POST /api/auth/refresh` và retry request.

3. **Follow toggle:** Gọi `POST /api/follows/{storyId}` — response trả về `status: FOLLOWED/UNFOLLOWED` và `followCount` mới.

4. **Sau khi thanh toán PayOS redirect về:**  
   URL sẽ có dạng `https://yourapp.com/payment/return?orderCode=123&status=PAID`  
   → Gọi ngay `GET /api/payment/verify/{orderCode}` để cộng coin (idempotent, gọi nhiều lần an toàn).

5. **Notification badge:** Poll `GET /api/notifications/unread-count` mỗi 30 giây hoặc dùng WebSocket.

6. **Kiểm tra ban:** Khi login trả về 403 với message chứa `"bị khóa"` → hiển thị thông báo ban + thời gian.

7. **Upload avatar flow:**  
   `POST /me/avatar` → lấy `avatarUrl` → `PUT /me` với `avatarUrl` đó.

8. **Public endpoints** — Không cần token:  
   GET /stories, /stories/search, /stories/{id}, /stories/{id}/detail, /stories/rankings, /stories/top-rated, /stories/completed, /stories/category/{id}, /chapters/**, /comments/chapter/**, /ratings/story/**, /categories, /payment/packages, /follows/story/{id}/count

