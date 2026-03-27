# 📘 FEATURE FLOW DOCUMENT — Digital Publishing Platform

> Tài liệu mô tả chi tiết luồng nghiệp vụ, trạng thái thực thể và API cho từng chức năng.  
> Cập nhật: 2026-03-26

---

## MỤC LỤC

1. [Comment & Rate & Donate](#1-comment--rate--donate)
2. [Report Content (Báo cáo vi phạm)](#2-report-content)
3. [Write Draft via Online Editor (Tác giả viết nháp)](#3-write-draft-via-online-editor)
4. [Manage Chapter List (Quản lý danh sách chương)](#4-manage-chapter-list)
5. [Set Story Status (Đặt trạng thái truyện)](#5-set-story-status)
6. [Create Editing Request (Tạo yêu cầu chỉnh sửa)](#6-create-editing-request)
7. [Review Edited Version (Tác giả xem bản sửa của Editor)](#7-review-edited-version)
8. [Schedule Chapter Release (Hẹn lịch xuất bản)](#8-schedule-chapter-release)
9. [Reply to Reader Comment (Trả lời bình luận)](#9-reply-to-reader-comment)
10. [Block User on Own Stories (Chặn người dùng)](#10-block-user-on-own-stories)
11. [Edit Chapter Content (Editor chỉnh sửa nội dung)](#11-edit-chapter-content)
12. [Track Approval Status (Theo dõi trạng thái duyệt)](#12-track-approval-status)
13. [Review Chapter Content (Reviewer duyệt chương)](#13-review-chapter-content)
14. [Review User Report (Admin xử lý báo cáo)](#14-review-user-report)

---

## 1. Comment & Rate & Donate

### 1.1 Comment (Bình luận)

**Ai dùng:** Reader (và tất cả user đã đăng nhập)

**Luồng:**
```
Reader mở chapter
    → POST /api/comments  { chapterId, content, parentId? }
        → Kiểm tra user có bị block bởi tác giả không?
            ✗ → 403 Forbidden ("Bạn đã bị tác giả chặn")
            ✓ → Lưu Comment vào DB
              → Trả về CommentResponse (cây comment)
```

**Trạng thái thực thể Comment:**

| Field | Giá trị | Ý nghĩa |
|-------|---------|---------|
| `hidden` | `false` | Hiển thị bình thường |
| `hidden` | `true` | Bị ẩn (vi phạm), có `hideReason` |
| `parent` | `null` | Comment gốc |
| `parent` | `có ID` | Reply vào comment khác (đệ quy vô hạn cấp) |

**API:**
```
POST   /api/comments                    → Tạo comment / reply
GET    /api/comments/chapter/{id}       → Lấy tất cả comment của chapter (dạng cây)
GET    /api/comments/{id}               → Lấy chi tiết 1 comment + replies
DELETE /api/comments/{id}               → Xóa (chỉ chủ comment hoặc admin)
```

**Request body:**
```json
{
  "chapterId": 5,
  "content": "Chương hay quá!",
  "parentId": null    // null = comment gốc, có ID = reply
}
```

---

### 1.2 Rate (Đánh giá sao)

**Ai dùng:** Reader đã đăng nhập

**Luồng:**
```
Reader đọc xong truyện
    → POST /api/ratings  { storyId, score (1-5), review? }
        → Nếu chưa đánh giá → INSERT Rating mới
        → Nếu đã đánh giá   → UPDATE Rating cũ (đánh giá lại)
        → Tự động cập nhật story.avgRating và story.ratingCount
        → Trả về RatingResponse
```

**Ràng buộc:** Unique per `(user_id, story_id)` — mỗi user chỉ có 1 rating/truyện.

**API:**
```
POST /api/ratings                   → Đánh giá (tạo mới hoặc cập nhật)
GET  /api/ratings/story/{storyId}   → Xem tất cả đánh giá của truyện
GET  /api/ratings/my/{storyId}      → Xem đánh giá của tôi cho truyện này
```

---

### 1.3 Donate (Tặng quà/Coin)

**Ai dùng:** Reader có coin trong ví

**Luồng:**
```
Reader chọn tặng quà cho truyện
    → POST /api/gifts  { storyId, coinAmount, message? }
        → Kiểm tra reader có bị block không?
        → Kiểm tra reader có đủ coin không?
            ✗ → 400 Bad Request
            ✓ → Trừ coin khỏi ví Reader
              → Cộng coin vào ví tác giả của truyện
              → Cập nhật author.totalEarnedCoin
              → Ghi WalletTransaction cho cả 2 bên
              → Gửi Notification cho tác giả (type: GIFT_RECEIVED)
              → Cập nhật Mission "Tặng quà cho tác giả" → +progress
              → Trả về GiftResponse
```

**API:**
```
POST /api/gifts                     → Tặng quà
GET  /api/gifts/sent                → Danh sách quà tôi đã tặng
GET  /api/gifts/received            → Danh sách quà tôi đã nhận
GET  /api/gifts/story/{storyId}     → Danh sách quà của 1 truyện
```

---

## 2. Report Content

**Ai dùng:** Bất kỳ user đã đăng nhập

**Luồng:**
```
User phát hiện nội dung vi phạm
    → POST /api/reports  { targetType, targetId, category, reason }
        → Tạo Report với status = PENDING
        → Trả về ReportResponse

Admin xử lý báo cáo
    → GET  /api/admin/reports           → Xem danh sách báo cáo (có filter)
    → POST /api/admin/reports/{id}/resolve
        { resolvedAction, adminNote }
        → Report.status → RESOLVED
        → Thực hiện hành động:
            WARN_ONLY      → chỉ ghi chú
            HIDE_CONTENT   → ẩn nội dung (story/chapter/comment)
            DELETE_CONTENT → xóa nội dung
            BAN_USER       → ban tài khoản chủ nội dung
            HIDE_AND_BAN   → ẩn nội dung + ban user
            DELETE_AND_BAN → xóa nội dung + ban user
```

**Trạng thái thực thể Report:**

| `status` | Ý nghĩa |
|----------|---------|
| `PENDING` | Chờ admin xử lý |
| `RESOLVED` | Đã xử lý xong |

**Phân loại vi phạm (`category`):**

| Giá trị | Ý nghĩa |
|---------|---------|
| `SPAM` | Spam |
| `COPYRIGHT` | Vi phạm bản quyền |
| `INAPPROPRIATE` | Nội dung không phù hợp |
| `VIOLENCE` | Bạo lực |
| `OTHER` | Khác |

**Đối tượng bị báo cáo (`targetType`):**
- `STORY` — cả bộ truyện
- `CHAPTER` — 1 chương cụ thể
- `COMMENT` — 1 bình luận

**API:**
```
POST /api/reports           → Tạo báo cáo (mọi user)
GET  /api/reports/my        → Xem báo cáo tôi đã gửi

GET  /api/admin/reports                   → [ADMIN] Danh sách tất cả báo cáo
GET  /api/admin/reports/pending           → [ADMIN] Báo cáo chưa xử lý
POST /api/admin/reports/{id}/resolve      → [ADMIN] Xử lý báo cáo
```

---

## 3. Write Draft via Online Editor

**Ai dùng:** Author

**Luồng tạo truyện mới:**
```
Author tạo truyện
    → POST /api/stories  { title, summary, categoryIds[], coverUrl? }
        → Story.status = DRAFT
        → Trả về StoryResponse

Author upload ảnh bìa (nếu cần)
    → POST /api/stories/{id}/cover  (multipart file)
        → Lưu file vào /uploads/covers/
        → Trả về { "coverUrl": "https://..." }
    → PUT /api/stories/{id}  { ..., coverUrl: "https://..." }
        → Cập nhật URL vào Story
```

**Luồng viết chương:**
```
Author tạo chương
    → POST /api/chapters/story/{storyId}
        { title, content, chapterOrder, coinPrice? }
        → Chapter.status = DRAFT
        → Tính wordCount tự động
        → Trả về ChapterResponse

Author chỉnh sửa lại
    → PUT /api/chapters/{id}  { title, content, ... }
        → Chapter.status giữ nguyên DRAFT
        → Trả về ChapterResponse
```

**Trạng thái thực thể Story:**

| `status` | Ý nghĩa |
|----------|---------|
| `DRAFT` | Nháp, chưa nộp duyệt |
| `PENDING` | Đã nộp, chờ Reviewer |
| `APPROVED` | Reviewer duyệt xong ✅ |
| `REJECTED` | Bị từ chối, kèm `rejectReason` |
| `REQUEST_EDIT` | Reviewer yêu cầu sửa thêm |

**API:**
```
POST   /api/stories                         → Tạo truyện mới
PUT    /api/stories/{id}                    → Cập nhật thông tin truyện
POST   /api/stories/{id}/cover              → Upload ảnh bìa
DELETE /api/stories/{id}                    → Xóa mềm (soft delete)
GET    /api/stories/my                      → Danh sách truyện của tôi
```

---

## 4. Manage Chapter List

**Ai dùng:** Author

**Luồng:**
```
Author xem danh sách chương
    → GET /api/chapters/story/{storyId}
        → Trả về list ChapterResponse (tất cả trạng thái của story mình)

Author xem thống kê
    → GET /api/stories/{id}/chapter-stats
        → Trả về { publishedCount, totalCount }

Author xóa chương
    → DELETE /api/chapters/{id}
        → Chỉ được xóa khi chapter ở DRAFT
```

**Trạng thái thực thể Chapter:**

| `status` | Ý nghĩa |
|----------|---------|
| `DRAFT` | Nháp — Author đang viết |
| `PENDING` | Đã nộp cho Reviewer duyệt |
| `APPROVED` | Reviewer duyệt xong, chờ Author publish |
| `PUBLISHED` | Đã xuất bản, độc giả đọc được |
| `SCHEDULED` | Hẹn lịch, cron job sẽ tự publish đúng giờ |
| `REJECTED` | Bị từ chối, kèm `rejectReason` + `reviewNote` |
| `HIDDEN` | Bị admin ẩn (vi phạm) |

**API:**
```
GET    /api/chapters/story/{storyId}    → Danh sách tất cả chapter của truyện
GET    /api/stories/{id}/chapter-stats  → Thống kê: publishedCount / totalCount
POST   /api/chapters/story/{storyId}    → Tạo chương mới
PUT    /api/chapters/{id}               → Sửa nội dung chương
DELETE /api/chapters/{id}               → Xóa chương (chỉ DRAFT)
```

---

## 5. Set Story Status

**Ai dùng:** Author (và Admin)

### 5.1 Nộp truyện lên Reviewer

```
Author hoàn thiện truyện (ít nhất 1 chapter PUBLISHED)
    → POST /api/stories/{id}/submit
        → Kiểm tra: story.status phải là DRAFT hoặc REJECTED
        → Kiểm tra: phải có ít nhất 1 chapter PUBLISHED
            ✗ → 400 Bad Request
            ✓ → Story.status → PENDING
              → Trả về StoryResponse
```

### 5.2 Đánh dấu hoàn thành / đang ra

```
Author kết thúc bộ truyện
    → PATCH /api/stories/{id}/completion-status?completed=true
        → Story.isCompleted = true
        → Story.completedAt = now()
        → Trả về StoryResponse với badge "Hoàn thành"

Author tiếp tục ra chapter
    → PATCH /api/stories/{id}/completion-status?completed=false
        → Story.isCompleted = false
        → Story.completedAt = null
```

**Điều kiện để story hiển thị trên trang chủ:**
- `status = APPROVED`
- `isDeleted = false`
- Có ít nhất 1 chapter `status = PUBLISHED`

**API:**
```
POST  /api/stories/{id}/submit                          → Nộp duyệt
PATCH /api/stories/{id}/completion-status?completed=    → Đặt trạng thái hoàn thành
DELETE /api/stories/{id}                                → Soft delete (ẩn khỏi trang chủ)
```

---

## 6. Create Editing Request

**Ai dùng:** Author — muốn thuê Editor chỉnh sửa 1 chapter

**Luồng:**
```
Author chọn chapter cần sửa
    → POST /api/edit-requests
        { chapterId, coinReward, description }
        → Kiểm tra chapter thuộc story của Author?
        → Kiểm tra chapter ở trạng thái DRAFT hoặc REJECTED?
        → Kiểm tra không có request đang active cho chapter này?
        → Kiểm tra Author có đủ coin không?
            ✗ → 400 Bad Request
            ✓ → LOCK coin vào escrow (trừ khỏi ví Author ngay)
              → EditRequest.status = OPEN
              → Ghi WalletTransaction: ESCROW_LOCK
              → Trả về EditRequestResponse
```

**Trạng thái thực thể EditRequest:**

| `status` | Ý nghĩa |
|----------|---------|
| `OPEN` | Đang chờ Editor nhận |
| `IN_PROGRESS` | Editor đang làm việc |
| `SUBMITTED` | Editor đã nộp bản, chờ Author duyệt |
| `APPROVED` | Author chấp thuận ✅, coin chuyển cho Editor |
| `CANCELLED` | Author huỷ (chỉ được khi OPEN, coin hoàn lại) |

**API:**
```
POST   /api/edit-requests           → [AUTHOR] Tạo yêu cầu chỉnh sửa
GET    /api/edit-requests/my        → [AUTHOR] Danh sách request của tôi (Author side)
DELETE /api/edit-requests/{id}/cancel  → [AUTHOR] Huỷ request (chỉ khi OPEN)
```

---

## 7. Review Edited Version

**Ai dùng:** Author — xem và quyết định bản sửa của Editor

**Luồng khi Editor đã nộp bản (`SUBMITTED`):**
```
Author xem bản sửa
    → GET /api/edit-requests/my
        → Xem editedContent + editorNote từ Editor

Author chấp thuận bản sửa
    → POST /api/edit-requests/{id}/approve
        → Nội dung cũ của chapter được lưu vào ChapterVersion (version history)
        → chapter.content = editedContent (của Editor)
        → chapter.status → DRAFT (cần nộp lại Reviewer để publish)
        → Coin escrow CHUYỂN sang ví Editor
        → Ghi WalletTransaction: ESCROW_RELEASE
        → EditRequest.status → APPROVED
        → Gửi Notification cho Editor

Author từ chối, yêu cầu viết lại
    → POST /api/edit-requests/{id}/reject
        { authorNote: "Chưa đúng văn phong, cần sửa đoạn 2" }
        → EditRequest.status → IN_PROGRESS (Editor được phép nộp lại)
        → EditRequest.attemptCount tăng +1
        → Coin vẫn bị LOCK (chưa hoàn trả)
        → Editor có thể submit lại VÔ HẠN lần
```

**Lưu ý về ChapterVersion:**
- Mỗi lần approve → nội dung cũ được snapshot vào bảng `chapter_versions`
- Có thể xem lịch sử `version` để so sánh

**API:**
```
POST /api/edit-requests/{id}/approve    → [AUTHOR] Chấp thuận bản sửa
POST /api/edit-requests/{id}/reject     → [AUTHOR] Từ chối, Editor viết lại
```

---

## 8. Schedule Chapter Release

**Ai dùng:** Author — hẹn lịch xuất bản sau khi chapter được APPROVED

**Luồng:**
```
Chapter đã được Reviewer APPROVE
    → Author chọn: publish ngay HOẶC hẹn lịch

Publish ngay
    → POST /api/chapters/{id}/publish
        → chapter.status → PUBLISHED
        → Gửi Notification đến tất cả follower của truyện (type: NEW_CHAPTER)

Hẹn lịch
    → POST /api/chapters/{id}/schedule
        { publishAt: "2026-04-01T08:00:00" }
        → Kiểm tra publishAt phải là thời gian TRONG TƯƠNG LAI
            ✗ → 400 Bad Request
            ✓ → chapter.status    → SCHEDULED
              → chapter.publishAt = "2026-04-01T08:00:00"

Cron job (chạy mỗi 15 phút)
    → Quét tất cả chapter có status = SCHEDULED và publishAt <= now()
    → chapter.status → PUBLISHED
    → Gửi Notification đến follower
```

**Trạng thái Chapter trong luồng publish:**
```
APPROVED → (Author publish ngay)   → PUBLISHED
APPROVED → (Author hẹn lịch)      → SCHEDULED
SCHEDULED → (Cron 15 phút)        → PUBLISHED
```

**API:**
```
POST /api/chapters/{id}/publish     → [AUTHOR] Publish ngay (APPROVED → PUBLISHED)
POST /api/chapters/{id}/schedule    → [AUTHOR] Hẹn lịch (APPROVED → SCHEDULED)
```

> ⚠️ **Lưu ý Timezone:** Server chạy theo giờ Việt Nam (**UTC+7 / Asia/Ho_Chi_Minh**).
> Frontend gửi `publishAt` theo định dạng ISO 8601 giờ Việt Nam — ví dụ: `"2026-04-01T20:30:00"` (tức 20:30 tối giờ VN).
> **KHÔNG** gửi UTC hay offset (+07:00) vì server tự hiểu là VN time.

---

## 9. Reply to Reader Comment

**Ai dùng:** Author (hoặc bất kỳ user) — reply vào comment

**Luồng:**
```
User thấy comment muốn trả lời
    → POST /api/comments
        {
          "chapterId": 5,
          "content": "Cảm ơn bạn!",
          "parentId": 123   ← ID của comment muốn reply
        }
        → Kiểm tra parentId có tồn tại không?
        → Kiểm tra user có bị block bởi tác giả không?
        → Lưu Comment với parent = comment có id = 123
        → Trả về CommentResponse với parentId được set

Lấy cây comment (bao gồm replies):
    → GET /api/comments/chapter/{chapterId}
        → Trả về list gốc, mỗi comment có trường "replies": [...]
        → Đệ quy vô hạn cấp
```

**Cấu trúc response cây comment:**
```json
{
  "id": 123,
  "content": "Chương hay!",
  "parentId": null,
  "replies": [
    {
      "id": 456,
      "content": "Cảm ơn bạn!",
      "parentId": 123,
      "replies": []
    }
  ]
}
```

**API:**
```
POST /api/comments                      → Tạo reply (set parentId)
GET  /api/comments/chapter/{chapterId}  → Lấy cây comment đầy đủ
```

---

## 10. Block User on Own Stories

**Ai dùng:** Author — chặn reader khỏi tương tác trên TẤT CẢ tác phẩm của mình

**Luồng:**
```
Author muốn chặn reader vi phạm
    → POST /api/users/blocks  { blockedUserId, reason? }
        → Tạo UserBlock { blocker=Author, blocked=Reader }
        → Unique constraint: mỗi cặp (blocker, blocked) chỉ tồn tại 1 lần

Khi reader bị block cố comment hoặc donate
    → CommentService / GiftService kiểm tra UserBlock
        → Nếu tìm thấy bản ghi block → 403 Forbidden

Author muốn bỏ chặn
    → DELETE /api/users/blocks/{userId}
        → Xóa bản ghi UserBlock

Author xem danh sách đã chặn
    → GET /api/users/blocks
        → Trả về list UserBlockResponse
```

**Hành động bị chặn:**
- ❌ Bình luận (comment)
- ❌ Tặng quà (donate/gift)

**Hành động KHÔNG bị ảnh hưởng:**
- ✅ Vẫn đọc được truyện (chapter miễn phí)
- ✅ Vẫn mua được chapter trả phí
- ✅ Vẫn follow truyện

**API:**
```
POST   /api/users/blocks              → [AUTHOR] Chặn user
DELETE /api/users/blocks/{userId}     → [AUTHOR] Bỏ chặn
GET    /api/users/blocks              → [AUTHOR] Danh sách đã chặn
```

---

## 11. Edit Chapter Content

**Ai dùng:** Editor — nhận việc và chỉnh sửa chapter từ Author

**Luồng:**
```
Editor xem danh sách việc đang mở
    → GET /api/edit-requests/open
        → Thấy: tên chapter, tên truyện, coin thưởng, mô tả Author

Editor nhận việc
    → POST /api/edit-requests/{id}/assign
        → Kiểm tra: request phải OPEN và chưa có editor nào nhận
        → EditRequest.editor = CurrentUser
        → EditRequest.status → IN_PROGRESS

Editor đọc nội dung chapter gốc
    → GET /api/chapters/{chapterId}   ← ID từ EditRequestResponse.chapter.id

Editor chỉnh sửa và nộp bản
    → PUT /api/edit-requests/{id}/submit
        {
          "editedContent": "Nội dung đã chỉnh sửa...",
          "editorNote": "Đã sửa lỗi chính tả và cải thiện văn phong đoạn 2-3"
        }
        → EditRequest.editedContent = nội dung mới
        → EditRequest.status → SUBMITTED (chờ Author duyệt)

Nếu Author từ chối → Editor viết lại
    → EditRequest.status về IN_PROGRESS
    → attemptCount += 1
    → Editor có thể submit lại nhiều lần

Editor muốn rút lui (chỉ khi attemptCount = 0)
    → POST /api/edit-requests/{id}/withdraw
        → EditRequest.editor = null
        → EditRequest.status → OPEN (để editor khác nhận)
```

**Quy tắc:**
- Editor chỉ thấy các request `OPEN` — KHÔNG thấy nội dung chapter của các truyện khác
- Chỉ được rút lui khi `attemptCount = 0` (chưa nộp lần nào)
- Sau khi nộp ít nhất 1 lần → phải hoàn thành hoặc bị Author quyết định

**API:**
```
GET  /api/edit-requests/open           → [EDITOR] Danh sách việc đang mở
POST /api/edit-requests/{id}/assign    → [EDITOR] Nhận việc
PUT  /api/edit-requests/{id}/submit    → [EDITOR] Nộp bản chỉnh sửa
POST /api/edit-requests/{id}/withdraw  → [EDITOR] Rút lui (chỉ khi attemptCount=0)
GET  /api/edit-requests/assigned       → [EDITOR] Lịch sử các việc đã nhận
```

---

## 12. Track Approval Status

**Ai dùng:** Author — theo dõi tiến trình duyệt của story và chapter

**Luồng xem trạng thái:**
```
Author xem tất cả truyện của mình
    → GET /api/stories/my
        → Mỗi StoryResponse có field: status, rejectReason

Author xem danh sách chapter và trạng thái
    → GET /api/chapters/story/{storyId}
        → Mỗi ChapterResponse có: status, rejectReason, reviewNote

Author xem thống kê chapter
    → GET /api/stories/{id}/chapter-stats
        → { publishedCount, totalCount }
```

**Các trạng thái Author cần theo dõi:**

**Story:**
```
DRAFT        → Chưa nộp duyệt
PENDING      → Đã nộp, đang chờ Reviewer
APPROVED     → ✅ Được duyệt
REJECTED     → ❌ Bị từ chối (xem rejectReason)
REQUEST_EDIT → ⚠️ Reviewer yêu cầu sửa thêm
```

**Chapter:**
```
DRAFT      → Đang soạn thảo
PENDING    → Đã nộp, chờ Reviewer
APPROVED   → ✅ Được duyệt, Author có thể publish
PUBLISHED  → 📖 Đã xuất bản
SCHEDULED  → ⏰ Đã hẹn lịch, chờ đến giờ
REJECTED   → ❌ Bị từ chối (xem rejectReason + reviewNote)
HIDDEN     → 🚫 Bị ẩn bởi Admin (vi phạm)
```

**Thông báo tự động (Notification):**
- `CHAPTER_APPROVED` → Reviewer vừa approve chapter của bạn
- `CHAPTER_REJECTED` → Chapter bị từ chối kèm lý do
- `STORY_APPROVED`   → Story của bạn được duyệt
- `STORY_REJECTED`   → Story bị từ chối

**API:**
```
GET /api/stories/my                     → [AUTHOR] Tất cả truyện + status
GET /api/chapters/story/{storyId}       → [AUTHOR] Tất cả chapter + status
GET /api/stories/{id}/chapter-stats     → [AUTHOR] Thống kê chapter
GET /api/notifications                  → [ALL] Hộp thư thông báo
```

---

## 13. Review Chapter Content

**Ai dùng:** Reviewer

**Luồng xem danh sách chờ duyệt:**
```
Reviewer vào trang kiểm duyệt
    → GET /api/reviewer/chapters/pending
        → Trả về list chapter có status = PENDING
        → Thấy: tiêu đề, tên truyện, tác giả, ngày nộp

Reviewer đọc nội dung đầy đủ
    → GET /api/reviewer/chapters/{id}
        → Trả về ChapterResponse với full content

Reviewer duyệt (APPROVE)
    → POST /api/reviewer/chapters/{id}/review
        { "action": "APPROVE", "note": "" }
        → chapter.status → APPROVED
        → Ghi AdminReview record (lịch sử duyệt)
        → Gửi Notification đến Author: CHAPTER_APPROVED
        → Author có thể tự publish sau

Reviewer từ chối (REJECT)
    → POST /api/reviewer/chapters/{id}/review
        { "action": "REJECT", "note": "Nội dung chưa phù hợp, cần sửa đoạn..." }
        → chapter.status → DRAFT (trả về để Author sửa)
        → chapter.reviewNote = lý do từ chối
        → Ghi AdminReview record
        → Gửi Notification đến Author: CHAPTER_REJECTED

Xem lịch sử duyệt của mình
    → GET /api/reviewer/history           → Tất cả (story + chapter)
    → GET /api/reviewer/history/chapters  → Chỉ chapter
    → GET /api/reviewer/history/chapter/{chapterId} → Lịch sử 1 chapter cụ thể
```

**Lịch sử duyệt lưu vào bảng `admin_reviews`:**
| Field | Nội dung |
|-------|----------|
| `targetType` | `CHAPTER` |
| `targetId` | ID chapter |
| `action` | `APPROVE` hoặc `REJECT` |
| `note` | Ghi chú/lý do |
| `createdAt` | Thời điểm duyệt |

**API:**
```
GET  /api/reviewer/chapters/pending        → [REVIEWER] Danh sách chờ duyệt
GET  /api/reviewer/chapters/{id}           → [REVIEWER] Đọc full nội dung
POST /api/reviewer/chapters/{id}/review    → [REVIEWER] Duyệt / Từ chối
GET  /api/reviewer/history                 → [REVIEWER] Lịch sử duyệt của tôi
GET  /api/reviewer/history/chapters        → [REVIEWER] Lịch sử duyệt chapter
GET  /api/reviewer/history/chapter/{id}    → [REVIEWER] Lịch sử 1 chapter
```

> **Lưu ý:** Reviewer cũng duyệt Story tương tự — xem `/api/reviewer/stories/*`

---

## 14. Review User Report

**Ai dùng:** Admin

**Luồng:**
```
Admin xem báo cáo chờ xử lý
    → GET /api/admin/reports?status=PENDING
        → Xem: targetType, category, reason, reporter info

Admin xem chi tiết nội dung bị báo cáo
    → Nếu COMMENT: GET /api/comments/{targetId}
    → Nếu CHAPTER:  GET /api/reviewer/chapters/{targetId}
    → Nếu STORY:    GET /api/reviewer/stories/{targetId}/detail

Admin ra quyết định xử lý
    → POST /api/admin/reports/{id}/resolve
        {
          "resolvedAction": "HIDE_CONTENT",
          "adminNote": "Vi phạm điều khoản cộng đồng mục 3.2"
        }
        → Report.status   → RESOLVED
        → Report.resolvedBy = Admin
        → Report.resolvedAt = now()
        → Thực hiện hành động theo resolvedAction (bảng dưới)
```

**Bảng hành động xử lý:**

| `resolvedAction` | Hành động thực hiện |
|------------------|-------------------|
| `WARN_ONLY` | Chỉ ghi chú, không có action tự động |
| `HIDE_CONTENT` | Ẩn story/chapter/comment (`hidden=true` hoặc `status=HIDDEN`) |
| `DELETE_CONTENT` | Xóa nội dung khỏi DB |
| `BAN_USER` | Ban tài khoản chủ nội dung |
| `HIDE_AND_BAN` | Ẩn nội dung + Ban user |
| `DELETE_AND_BAN` | Xóa nội dung + Ban user |

**Khi user bị BAN:**
- `user.isBanned = true`
- User đăng nhập sẽ nhận thông báo tài khoản bị khóa
- Không thể thực hiện bất kỳ thao tác nào

**API:**
```
GET  /api/admin/reports                   → [ADMIN] Tất cả báo cáo
GET  /api/admin/reports/pending           → [ADMIN] Chưa xử lý
POST /api/admin/reports/{id}/resolve      → [ADMIN] Xử lý báo cáo
```

---

## PHỤ LỤC — Sơ đồ trạng thái tổng hợp

### Story Status Flow
```
                    ┌──────────────────────────────┐
                    ↓                              ↑ (Author sửa và nộp lại)
DRAFT ──[submit]──→ PENDING ──[Reviewer APPROVE]──→ APPROVED
                         │
                         ├──[Reviewer REJECT]────→ REJECTED
                         │                              │
                         └──[Reviewer REQUEST_EDIT]─→ REQUEST_EDIT
```

### Chapter Status Flow
```
DRAFT ──[submit]──→ PENDING ──[Reviewer APPROVE]──→ APPROVED ──[Author publish]──→ PUBLISHED
  ↑                     │                                    └──[Author schedule]──→ SCHEDULED ──[Cron]──→ PUBLISHED
  │                     └──[Reviewer REJECT]──→ DRAFT (vòng lặp)
  │
  └──[Editor/Author chỉnh]── DRAFT (giữ nguyên)
```

### EditRequest Status Flow
```
OPEN ──[Editor assign]──→ IN_PROGRESS ──[Editor submit]──→ SUBMITTED
 │                              ↑                               │
 │                              └──────[Author reject]──────────┘
 │
 └──[Author cancel]──→ CANCELLED (coin hoàn lại)
                                                         SUBMITTED ──[Author approve]──→ APPROVED (coin chuyển cho Editor)
```

### Report Status Flow
```
PENDING ──[Admin resolve]──→ RESOLVED
                               + action: WARN_ONLY / HIDE_CONTENT / DELETE_CONTENT
                                         BAN_USER / HIDE_AND_BAN / DELETE_AND_BAN
```

---

## PHỤ LỤC — Notification Types

| Type | Trigger | Nhận |
|------|---------|------|
| `NEW_CHAPTER` | Chapter PUBLISHED (kể cả qua cron) | Follower của truyện |
| `STORY_APPROVED` | Reviewer approve story | Author của story |
| `STORY_REJECTED` | Reviewer reject story | Author của story |
| `CHAPTER_APPROVED` | Reviewer approve chapter | Author của chapter |
| `CHAPTER_REJECTED` | Reviewer reject chapter | Author của chapter |
| `GIFT_RECEIVED` | Reader gửi gift | Author của truyện |
| `NEW_FOLLOWER` | Reader follow truyện | Author của truyện |
| `MISSION_COMPLETED` | Hoàn thành nhiệm vụ + nhận thưởng | User đó |
| `STREAK_CHECKIN` | Check-in hàng ngày | User đó |
| `STREAK_MILESTONE` | Đạt mốc 3/7/14/30/100 ngày | User đó |
| `SYSTEM` | Admin broadcast | Tất cả / theo role |

