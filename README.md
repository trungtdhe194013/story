# Digital Publishing – Backend

Spring Boot 3 · PostgreSQL · Docker

## Yêu cầu
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (không cần cài Java hay Maven)

## Chạy nhanh (Docker Compose)

```bash
# 1. Clone repo
git clone <repo-url>
cd story

# 2. Tạo file .env từ template
cp .env.example .env
# Sau đó mở .env và điền thông tin thực (Google OAuth, Gmail...)

# 3. Build & chạy
docker-compose up --build
```

Ứng dụng sẽ chạy tại: **http://localhost:8080**  
Swagger UI: **http://localhost:8080/swagger-ui.html**

## Biến môi trường (.env)

| Biến | Mô tả | Bắt buộc |
|------|-------|----------|
| `GOOGLE_CLIENT_ID` | Google OAuth2 Client ID | Chỉ cần nếu dùng Google Login |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret | Chỉ cần nếu dùng Google Login |
| `MAIL_USERNAME` | Gmail dùng để gửi mail | Chỉ cần nếu dùng tính năng mail |
| `MAIL_PASSWORD` | Gmail App Password | Chỉ cần nếu dùng tính năng mail |
| `DB_PASSWORD` | Mật khẩu PostgreSQL | Mặc định: `admin` |
| `APP_BASE_URL` | URL ngoài của app (ngrok, domain...) | Mặc định: `http://localhost:8080` |

> **Lưu ý:** Chức năng đăng ký / đăng nhập thường hoạt động mà **không cần** Google OAuth hay Mail.

## Dừng ứng dụng

```bash
docker-compose down        # dừng, giữ data DB
docker-compose down -v     # dừng + xóa sạch data DB
```

## Chạy local (không Docker)

Yêu cầu: Java 17+, Maven, PostgreSQL 16

```bash
# Tạo file cấu hình local
cp .env.example src/main/resources/application-local.properties
# Chỉnh sửa application-local.properties cho phù hợp

.\mvnw spring-boot:run -Dspring-boot.run.profiles=local   # Windows
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # Linux/Mac
```

