# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml trước để tận dụng layer cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source và build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/story-0.0.1-SNAPSHOT.jar app.jar

# Tạo thư mục uploads bên trong image
RUN mkdir -p /app/uploads/avatars /app/uploads/covers /app/uploads/images

# Khai báo volume để dữ liệu upload không mất khi restart container
VOLUME /app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
