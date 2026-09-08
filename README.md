# Homestay TVH — Website giới thiệu & đặt phòng

Đồ án tốt nghiệp: hệ thống giới thiệu và đặt phòng homestay, gồm landing page
cho khách, trang quản trị cho chủ homestay, và REST API.

| Thành phần | Công nghệ |
|---|---|
| `backend/` | Java 21 · Spring Boot 3.5.6 · PostgreSQL 16 · Flyway · Spring Security |
| `frontend/` | Angular 21 · Tailwind CSS 4 · design system tự viết |
| `docs/` | Tài liệu kỹ thuật tiếng Việt |
| `plans/` | Kế hoạch triển khai 9 phase |

Điểm kỹ thuật cốt lõi: **chống đặt trùng phòng khoá ở tầng cơ sở dữ liệu** bằng
`EXCLUDE USING gist` trên kiểu `daterange` của PostgreSQL, không phụ thuộc kiểm
tra ở tầng service. Đây là lý do dự án chọn PostgreSQL thay vì MySQL.

## Yêu cầu môi trường

| Công cụ | Bản tối thiểu | Ghi chú |
|---|---|---|
| JDK | 21 | |
| Node.js | 20.19+ hoặc 22.12+ | Angular 21 yêu cầu |
| npm | 11+ | npm 10.9.x có lỗi khi giải phụ thuộc của Angular 21, xem mục Sự cố |
| Docker | có Compose v2 | chạy PostgreSQL và Mailpit |

## Chạy lần đầu

```bash
# 1. Tạo file .env từ mẫu rồi sinh bí mật
cp .env.example .env

# Không biến nào có giá trị mặc định trong mã nguồn — thiếu là app không khởi
# động. Sinh hai bí mật bắt buộc:
echo "JWT_SECRET=$(openssl rand -base64 48)"        >> .env
echo "SEPAY_WEBHOOK_API_KEY=$(openssl rand -hex 32)" >> .env
# rồi điền DB_URL, DB_USER, DB_PASSWORD trong .env

# 2. Dựng hạ tầng (PostgreSQL + Mailpit)
docker compose -f docker-compose.dev.yml up -d

# 3. Backend — cổng 8080
cd backend
export $(grep -v '^#' ../.env | grep -v '^$' | xargs)
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# 4. Frontend — cổng 4200
cd frontend
npm ci
npm start
```

Mở <http://localhost:4200>. Mailpit ở <http://localhost:8025>.

## Múi giờ

Toàn hệ thống chạy `Asia/Ho_Chi_Minh`, đặt ở **ba tầng độc lập**:

1. Biến `TZ` cho container (`docker-compose.dev.yml`).
2. `-Duser.timezone=Asia/Ho_Chi_Minh` cho JVM (`spring-boot-maven-plugin`).
3. Bean `Clock` trong `ClockConfig` cho mã nghiệp vụ.

Chỉ đặt `spring.jackson.time-zone` là **không đủ** — thuộc tính đó chỉ chi phối
cách Jackson serialize JSON, không đổi `TimeZone.getDefault()` và không ảnh
hưởng `LocalDate.now()`. Hệ quả nếu làm sai: booking tạo trong khung 00:00–07:00
giờ Việt Nam bị gom nhóm vào tháng trước ở dashboard, trong khi đồng hồ đếm
ngược trên màn hình thanh toán vẫn đúng — sai một nửa nên rất khó nghi ngờ.

Endpoint `/api/health` phơi bày cả `appZone` lẫn `jvmDefaultZone` để kiểm tra
được điều này bằng mắt.

## Bí mật

`JWT_SECRET` và `SEPAY_WEBHOOK_API_KEY` không có giá trị mặc định ở bất kỳ đâu:
không trong `application.yml`, không trong `.env.example`, không trong
`docker-compose`. Ứng dụng dừng khởi động khi thiếu, ở mọi profile.

Repo này công khai. Một giá trị mặc định "an toàn cho demo" nằm trong repo đồng
nghĩa với việc bất kỳ ai clone về cũng tự ký được JWT vai trò ADMIN cho mọi bản
triển khai dùng repo này.

## Profile

Chỉ có hai:

| Profile | Dùng khi | Gồm gì |
|---|---|---|
| `demo` | Phát triển và bản đem đi bảo vệ | Dữ liệu mẫu, Swagger UI, trang `/ui-kit` |
| `prod` | Triển khai thật | Không dữ liệu mẫu, không Swagger, không `/ui-kit` |

`application-test.yml` là cấu hình Testcontainers, không phải profile ứng dụng.

## Sự cố thường gặp

**`npm error Cannot read properties of null (reading 'edgesOut')`**
Lỗi của npm 10.9.x khi giải phụ thuộc peer optional của `vitest`. Dùng npm 11:

```bash
npx npm@11 install
```

**App không khởi động, log báo thiếu `JWT_SECRET`**
Đúng như thiết kế. Sinh bí mật theo mục "Chạy lần đầu" ở trên.

## Tài liệu

Kế hoạch triển khai đầy đủ: [`plans/260907-1304-homestay-tvh-booking/`](plans/260907-1304-homestay-tvh-booking/)
— mở `plan.html` để xem bản trình bày có sơ đồ và mockup.
