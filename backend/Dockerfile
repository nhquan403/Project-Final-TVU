# ── Tầng dựng ────────────────────────────────────────────────────────────────
# Chép pom.xml TRƯỚC rồi mới chép mã nguồn: sửa một dòng Java không được phép
# làm Maven tải lại toàn bộ dependency. Hai lệnh COPY tách rời chính là thứ giữ
# cho tầng cache dependency còn dùng lại được giữa các lần dựng ảnh.
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /src

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Bỏ test ở tầng này: test tích hợp cần Docker (Testcontainers), mà bên trong
# một lần `docker build` thì không có Docker. Test chạy bằng ./mvnw verify ở máy
# phát triển và ở CI — xem docs/kiem-thu.md.
RUN mvn -B -q package -DskipTests

# ── Tầng chạy ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Chạy bằng người dùng thường. Container chạy root nghĩa là một lỗ hổng thực thi
# mã trong ứng dụng trở thành root trong container — và với vài cấu hình Docker
# thì đó là một bước ngắn nữa tới root trên máy chủ.
RUN addgroup -S homestay && adduser -S -G homestay homestay

WORKDIR /app

# Thư mục ảnh tải lên phải tồn tại VÀ thuộc về người dùng chạy ứng dụng trước
# khi volume được gắn vào; nếu không Docker tạo nó với chủ sở hữu root và
# LocalImageStorage không ghi được.
RUN mkdir -p /app/uploads && chown -R homestay:homestay /app

COPY --from=builder --chown=homestay:homestay /src/target/homestay-*.jar /app/app.jar

USER homestay
EXPOSE 8080

# Múi giờ đặt ở HAI chỗ và cả hai đều cần: biến TZ chi phối đồng hồ hệ điều hành
# trong container, còn -Duser.timezone chi phối TimeZone.getDefault() của JVM.
# Thiếu cái thứ hai thì JVM đọc TZ đúng trên phần lớn bản Linux nhưng không phải
# mọi bản — và sai múi giờ ở đây nghĩa là ngày nhận phòng lệch một ngày.
ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Ho_Chi_Minh

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
