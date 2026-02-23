# HƯỚNG DẪN TEST ĐĂNG NHẬP GOOGLE OAUTH2

## 📋 YÊU CẦU

Để test tính năng đăng nhập Google, bạn cần:

1. **Google Cloud Console Account** - Tạo OAuth2 Client ID
2. **Ngrok** - Để expose Spring Boot ra internet
3. **Spring Boot đang chạy** - Port 8080

---

## 🔧 BƯỚC 1: TẠO GOOGLE OAUTH2 CLIENT

### 1.1. Truy cập Google Cloud Console
- Vào: https://console.cloud.google.com/
- Đăng nhập bằng tài khoản Google

### 1.2. Tạo hoặc chọn Project
- Chọn project hiện tại hoặc tạo mới: `Story API`

### 1.3. Bật Google+ API
- Menu > **APIs & Services** > **Library**
- Tìm kiếm: **Google+ API**
- Nhấn **Enable**

### 1.4. Tạo OAuth2 Credentials
- Menu > **APIs & Services** > **Credentials**
- Nhấn **+ CREATE CREDENTIALS** > **OAuth client ID**
- Chọn **Application type**: **Web application**
- Đặt tên: `Story Web App`

### 1.5. Cấu hình Authorized redirect URIs
Thêm các URL sau:

```
http://localhost:8080/login/oauth2/code/google
https://YOUR-NGROK-URL/login/oauth2/code/google
```

**Lưu ý:** Thay `YOUR-NGROK-URL` bằng URL ngrok thực tế (ví dụ: `https://danille-laziest-yetta.ngrok-free.dev`)

### 1.6. Lưu Client ID và Client Secret
- Sau khi tạo xong, copy:
  - **Client ID**: `123456789-abcdefgh.apps.googleusercontent.com`
  - **Client Secret**: `GOCSPX-xxxxxxxxxx`

---

## 🔧 BƯỚC 2: CẤU HÌNH SPRING BOOT

### 2.1. Thiết lập biến môi trường (PowerShell)

```powershell
$env:GOOGLE_CLIENT_ID="YOUR_CLIENT_ID_HERE"
$env:GOOGLE_CLIENT_SECRET="YOUR_CLIENT_SECRET_HERE"
```

**Hoặc** tạo file `.env` trong thư mục project:

```env
GOOGLE_CLIENT_ID=123456789-abcdefgh.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxx
```

### 2.2. Kiểm tra cấu hình trong `application.properties`

File này đã có sẵn:

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
spring.security.oauth2.client.registration.google.scope=email,profile
```

---

## 🚀 BƯỚC 3: KHỞI ĐỘNG SPRING BOOT

### 3.1. Chạy Spring Boot với biến môi trường

```powershell
cd D:\FU_Learning\Semester5\SWP301\story
$env:GOOGLE_CLIENT_ID="YOUR_CLIENT_ID"
$env:GOOGLE_CLIENT_SECRET="YOUR_CLIENT_SECRET"
mvn spring-boot:run
```

### 3.2. Kiểm tra log
Đợi cho đến khi thấy:
```
Started StoryApplication in X seconds
```

### 3.3. Test localhost
Mở browser: `http://localhost:8080`
- Nếu thấy JSON response → Spring Boot đã chạy ✅

---

## 🌐 BƯỚC 4: KHỞI ĐỘNG NGROK

### 4.1. Chạy ngrok (Terminal mới)

```powershell
cd C:\Users\laptop368\Downloads\ngrok-v3-stable-windows-amd64
.\ngrok.exe http 8080
```

### 4.2. Lấy URL ngrok
Bạn sẽ thấy:
```
Forwarding    https://danille-laziest-yetta.ngrok-free.dev -> http://localhost:8080
```

Copy URL này: `https://danille-laziest-yetta.ngrok-free.dev`

---

## 🔧 BƯỚC 5: CẬP NHẬT GOOGLE OAUTH2 REDIRECT URI

Quay lại **Google Cloud Console** > **Credentials** > **OAuth 2.0 Client IDs**

Thêm URL ngrok vào **Authorized redirect URIs**:

```
https://danille-laziest-yetta.ngrok-free.dev/login/oauth2/code/google
```

⚠️ **Lưu ý:** Mỗi lần chạy ngrok, URL sẽ thay đổi, bạn cần cập nhật lại!

---

## 🧪 BƯỚC 6: TEST ĐĂNG NHẬP GOOGLE

### 6.1. Truy cập URL đăng nhập Google

Mở browser và truy cập:

```
https://YOUR-NGROK-URL/oauth2/authorization/google
```

**Ví dụ:**
```
https://danille-laziest-yetta.ngrok-free.dev/oauth2/authorization/google
```

### 6.2. Luồng OAuth2 diễn ra như sau:

1. **Browser redirect** → Google Login Page
2. **Bạn đăng nhập** bằng tài khoản Google
3. **Google hỏi permission** → Nhấn "Allow"
4. **Google redirect về** `https://YOUR-NGROK-URL/login/oauth2/code/google`
5. **Spring Boot nhận code** → Gọi Google API lấy user info
6. **CustomOAuth2UserService** tạo/tìm user trong DB
7. **OAuth2AuthenticationSuccessHandler** tạo JWT
8. **Redirect về frontend** với token: `http://localhost:3000/oauth2/success?token=JWT_TOKEN`

### 6.3. Kết quả mong đợi

**Nếu thành công:**
- Browser redirect về: `http://localhost:3000/oauth2/success?token=eyJhbGciOiJIUzI1NiJ9...`
- User được tạo trong database với:
  - `provider = GOOGLE`
  - `providerId = sub từ Google`
  - `email = email từ Google`

**Nếu thất bại:**
- Kiểm tra log Spring Boot để xem lỗi
- Kiểm tra redirect URI trong Google Console

---

## 🔍 BƯỚC 7: KIỂM TRA KÊT QUẢ

### 7.1. Xem log Spring Boot

```
CustomOAuth2UserService: Authenticating Google user: email@gmail.com
OAuth2AuthenticationSuccessHandler: Generated token for user: email@gmail.com
```

### 7.2. Kiểm tra database

```sql
SELECT * FROM users WHERE provider = 'GOOGLE';
```

Bạn sẽ thấy user mới được tạo với:
- `email`: từ Google
- `full_name`: từ Google
- `provider`: GOOGLE
- `provider_id`: Google sub
- `password`: NULL
- `enabled`: true

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. Ngrok URL thay đổi
Mỗi lần chạy ngrok (free plan), URL sẽ thay đổi → Phải cập nhật lại Google Redirect URI

### 2. Frontend chưa có
Hiện tại `OAuth2AuthenticationSuccessHandler` redirect về `http://localhost:3000/oauth2/success?token=...`

Nếu frontend chưa có, bạn sẽ thấy lỗi "This site can't be reached"

**Giải pháp tạm:**
- Copy JWT token từ URL
- Dùng token này trong Swagger để test API

### 3. Test bằng Postman không được
OAuth2 flow yêu cầu browser redirect, không thể test trực tiếp bằng Postman

### 4. CORS với ngrok
File `SecurityConfig.java` đã cấu hình:
```java
config.addAllowedOrigin("https://*.ngrok-free.dev");
config.addAllowedOrigin("https://*.ngrok.io");
```

---

## 🎯 TÓM TẮT CÁCH TEST

1. **Tạo Google OAuth2 Client** → Lấy Client ID & Secret
2. **Set biến môi trường** → `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
3. **Chạy Spring Boot** → `mvn spring-boot:run`
4. **Chạy ngrok** → `ngrok http 8080`
5. **Cập nhật Redirect URI** trong Google Console
6. **Mở browser** → `https://YOUR-NGROK-URL/oauth2/authorization/google`
7. **Đăng nhập Google** → Allow permissions
8. **Copy JWT token** từ URL redirect
9. **Dùng token trong Swagger** để test API

---

## 🔗 ENDPOINTS LIÊN QUAN

| Endpoint | Mô tả |
|----------|-------|
| `/oauth2/authorization/google` | Bắt đầu OAuth2 flow |
| `/login/oauth2/code/google` | Google redirect về đây (callback) |
| `/api/auth/login` | Login bằng email/password (LOCAL) |
| `/api/auth/sign-up` | Đăng ký tài khoản LOCAL |

---

## 📞 SUPPORT

Nếu gặp lỗi:

1. Kiểm tra log Spring Boot
2. Kiểm tra biến môi trường: `echo $env:GOOGLE_CLIENT_ID`
3. Kiểm tra redirect URI trong Google Console
4. Kiểm tra ngrok đang chạy: `http://localhost:4040` (ngrok dashboard)

---

**Ngày tạo:** 2026-02-03  
**Version:** 1.0
