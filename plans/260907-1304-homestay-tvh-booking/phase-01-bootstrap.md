---
title: "Phase 1: Khởi tạo monorepo & hạ tầng dev"
status: todo
phase: 1
priority: P1
effort: "5h"
dependencies: []
---

# Phase 1: Khởi tạo monorepo & hạ tầng dev

## Overview

Dựng bộ khung monorepo chạy được: Spring Boot khởi động và kết nối Postgres trong Docker, Angular dev server chạy với Tailwind, hai bên nói chuyện được qua một endpoint health. Chưa có nghiệp vụ nào.

Phase này cũng là nơi chốt ba quyết định hạ tầng mà các phase sau đều dựa vào: **múi giờ**, **mô hình profile**, và **cách cấp secret**. Sai ở đây thì lỗi chỉ lộ ra ở Phase 9, tức là giờ thứ 60 trên 70.

## Requirements

- Functional: `GET /api/health` trả `{"status":"UP"}`; trang Angular gốc gọi được endpoint đó qua proxy.
- Non-functional: Postgres chạy trong Docker với `btree_gist` sẵn sàng; JVM và DB cùng chạy múi giờ `Asia/Ho_Chi_Minh`; app **không khởi động** khi thiếu secret bắt buộc.

## Architecture

```
Project-Final-TVU/
├── backend/          # Maven, Java 21, Spring Boot 3.5
├── frontend/         # Angular CLI workspace + Tailwind
├── docs/             # tài liệu tiếng Việt
├── plans/            # kế hoạch (đã có)
├── docker-compose.yml
├── docker-compose.dev.yml   # chỉ Postgres + Mailpit cho dev
├── .env.example
├── .gitignore
└── README.md
```

Dev chạy `docker compose -f docker-compose.dev.yml up -d` (chỉ hạ tầng), rồi `mvn spring-boot:run` và `ng serve` ở máy host để có hot reload. Compose đầy đủ để ở Phase 9.

### Mô hình profile: chỉ hai profile

| Profile | Dùng khi | Gồm gì |
|---|---|---|
| `demo` | Dev trên máy và bản `docker compose up` đem đi bảo vệ | Dữ liệu mẫu, Swagger UI, endpoint mô phỏng thanh toán (vẫn cần ADMIN + cờ bật) |
| `prod` | Triển khai thật | Không dữ liệu mẫu, không Swagger, không endpoint mô phỏng |

Bản kế hoạch đầu có bốn profile (`dev`, `demo`, `test`, `prod`) và gate ba thứ bằng ba profile khác nhau. Kết quả là bản `docker compose` có dữ liệu mẫu nhưng **không** có đường mô phỏng chuyển khoản — đúng thứ được dựng ra để cứu buổi bảo vệ lại vắng mặt trong bản đem đi bảo vệ. Hai profile, mỗi thứ gate bằng cờ riêng, là mô hình gọn và không tự mâu thuẫn.

`test` không phải profile ứng dụng — nó là cấu hình riêng của Testcontainers trong `application-test.yml`, chỉ tồn tại lúc chạy test.

### Múi giờ phải đặt ở tầng JVM, không phải Jackson

`spring.jackson.time-zone` chỉ chi phối cách Jackson serialize JSON. Nó **không** đổi `TimeZone.getDefault()`, không ảnh hưởng `LocalDate.now()`, không ảnh hưởng session timezone của JDBC. Image `eclipse-temurin` mặc định UTC, nên nếu chỉ đặt thuộc tính Jackson thì:

- `check_in >= hôm nay` tính sai trong khung 00:00–07:00 giờ Việt Nam,
- `date_trunc('month', created_at)` xếp booking tạo lúc 01:00 ngày mùng 1 vào tháng trước,
- mà đồng hồ đếm ngược trên màn hình QR vẫn **đúng** (vì `TIMESTAMPTZ` là mốc tuyệt đối) — chính sự đúng một nửa này khiến lỗi khó bị nghi ngờ.

Nên phase này đặt cả ba: `TZ` cho container, `-Duser.timezone` cho JVM, và quy ước code dùng `ZoneId` tường minh.

### Secret: không có giá trị mặc định ở bất kỳ đâu

`JWT_SECRET` và `SEPAY_WEBHOOK_API_KEY` **không** có giá trị mặc định trong `application.yml`, `.env.example` hay `docker-compose.yml`. App fail fast khi thiếu, ở mọi profile.

Lý do: repo công khai. Một giá trị mặc định "an toàn cho demo" nằm trong repo nghĩa là bất kỳ ai cũng tự ký được JWT với `role=ADMIN` cho mọi bản triển khai dùng repo này, và giả mạo được webhook xác nhận thanh toán. Phase 9 sinh secret ngẫu nhiên lúc dựng lần đầu và ghi vào `.env` cục bộ (không commit).

## Related Code Files

- Create: `backend/pom.xml` — Spring Boot 3.5.x; dependency **đầy đủ ngay từ đầu**: `web`, `data-jpa`, `security`, `validation`, `mail`, `thymeleaf`, `postgresql`, `flyway-core`, `springdoc-openapi-starter-webmvc-ui`, `bucket4j-core`, `cloudinary-http44`, thư viện sanitize HTML, `lombok`, `mapstruct`, `testcontainers` + `junit-jupiter` (test scope)
- Create: `backend/src/main/java/com/tvh/homestay/HomestayApplication.java`
- Create: `backend/src/main/java/com/tvh/homestay/common/HealthController.java`
- Create: `backend/src/main/java/com/tvh/homestay/config/RequiredSecretsValidator.java` — fail fast lúc khởi động
- Create: `backend/src/main/resources/application.yml`, `application-demo.yml`, `application-prod.yml`
- Create: `frontend/` qua Angular CLI
- Create: `frontend/tailwind.config.js` (hoặc cấu hình CSS-first tuỳ bản Tailwind), `frontend/src/styles.css`, `frontend/proxy.conf.json`
- Create: `docker-compose.dev.yml`, `.env.example`, `.gitignore`, `README.md`
- Create: `docs/.gitkeep`

## Implementation Steps

1. Ở branch `claude/homestay-tvh-booking-site-bew1pw`, commit `.gitignore` (thư mục build Maven, `node_modules/`, `.env`, `uploads/`) + `README.md` khung.
2. Sinh backend: Maven project `com.tvh:homestay`, Java 21. Thêm **toàn bộ** dependency ở trên ngay bây giờ — Phase 6 cần Thymeleaf, Phase 4 cần Bucket4j, Phase 7 cần Cloudinary, Phase 8 cần sanitizer. Thêm dần sẽ khiến mỗi phase phải dừng lại sửa `pom.xml`.
3. `application.yml`: datasource đọc `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`; `spring.jpa.hibernate.ddl-auto: validate` (Flyway là chủ schema, **không bao giờ** `update`); `server.forward-headers-strategy: framework`; `spring.jackson.time-zone: Asia/Ho_Chi_Minh`.
4. `RequiredSecretsValidator`: `ApplicationRunner` chạy sớm, kiểm `JWT_SECRET` (≥ 32 byte) và `SEPAY_WEBHOOK_API_KEY` có mặt; thiếu → ném exception dừng khởi động, in rõ tên biến thiếu (**không** in giá trị).
5. Viết `HealthController` trả `{"status":"UP","time":...}` tại `GET /api/health`; `SecurityConfig` tạm cho phép `/api/health`, phần còn lại `denyAll()` — Phase 4 thay bằng ma trận đầy đủ. Bắt đầu bằng đóng, không bằng mở.
6. `docker-compose.dev.yml`: service `db` (`postgres:16-alpine`, volume `pgdata`, `POSTGRES_DB=homestay`, `TZ=Asia/Ho_Chi_Minh`) và `mailpit` (cổng 8025).
7. Đặt múi giờ cho tiến trình Java: `TZ=Asia/Ho_Chi_Minh` và `JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Ho_Chi_Minh`. Ghi vào `README.md` như quy ước bắt buộc cho mọi cách chạy.
8. Sinh frontend bằng Angular CLI; cài Tailwind **theo đúng hướng dẫn của phiên bản vừa cài** (các bản gần đây bỏ lệnh `tailwindcss init` và chuyển sang cấu hình CSS-first; standalone cũng đã là mặc định nên không truyền cờ đã bị gỡ). Không sao chép lệnh từ trí nhớ — đọc `README` của gói vừa cài.
9. Khai báo design token (màu thương hiệu, font, spacing) ở một chỗ duy nhất để Phase 7/8 dùng chung, tránh rải màu cứng khắp nơi.
10. `proxy.conf.json` map `/api` → `http://localhost:8080`; `ng serve` dùng proxy. Frontend và API cùng origin ở dev — cần thiết để cookie `HttpOnly` của refresh token (Phase 4) hoạt động.
11. Trang `app.component` gọi `/api/health` và hiển thị trạng thái — bằng chứng hai đầu đã nối.
12. `.env.example` liệt kê đủ biến với giá trị **rỗng** và ghi chú cách sinh: `DB_*`, `JWT_SECRET`, `SEPAY_*`, `CLOUDINARY_URL`, `MAIL_*`, `TZ`.
13. `README.md`: yêu cầu môi trường (JDK 21, Node 20+, Docker), lệnh chạy dev, và cách sinh secret.
14. **Xác nhận môi trường buổi bảo vệ** — máy hội đồng có Docker không, có mạng không, ai cấp máy. Đây là điều kiện có thể phá vỡ toàn bộ kịch bản demo, và nó rẻ để hỏi bây giờ, đắt để phát hiện ở Phase 9.

## Verify

```bash
docker compose -f docker-compose.dev.yml up -d
docker exec -it homestay-db psql -U postgres -d homestay \
  -c "CREATE EXTENSION IF NOT EXISTS btree_gist; SELECT extname FROM pg_extension;"

# Fail fast: thiếu secret thì PHẢI không khởi động được
cd backend && (unset JWT_SECRET; ./mvnw spring-boot:run 2>&1 | grep -q 'JWT_SECRET' \
  && echo "OK: fail fast dung" || echo "LOI: van khoi dong khi thieu secret")

export JWT_SECRET=$(openssl rand -base64 48)
./mvnw -q clean verify && ./mvnw spring-boot:run &
curl -s localhost:8080/api/health          # {"status":"UP",...}

# Múi giờ JVM phải là Asia/Ho_Chi_Minh, không phải UTC
curl -s localhost:8080/api/health | jq -r .time    # phải có offset +07:00

cd ../frontend && npm ci && npm run build
npm start                                   # localhost:4200 hiển thị "API: UP"

git status --porcelain | grep -E '\.env$' && echo "LOI: .env bi track" || echo "OK"
```

## Todo

- [ ] `.gitignore` + README khung
- [ ] Backend Maven skeleton + **toàn bộ** dependency của cả 8 phase
- [ ] `application.yml` đọc config từ env + `forward-headers-strategy` + `ddl-auto: validate`
- [ ] `RequiredSecretsValidator` fail fast, không in giá trị secret
- [ ] `HealthController` + `SecurityConfig` tạm mặc định `denyAll()`
- [ ] `docker-compose.dev.yml` (Postgres 16 + Mailpit) kèm `TZ`
- [ ] `TZ` + `-Duser.timezone` cho tiến trình Java, ghi vào README
- [ ] Angular workspace + Tailwind theo hướng dẫn bản vừa cài + design token
- [ ] `proxy.conf.json` cùng origin + trang health
- [ ] `.env.example` đủ biến, **giá trị rỗng**
- [ ] Xác nhận môi trường buổi bảo vệ (Docker / mạng / máy)

## Success Criteria

- [ ] `./mvnw verify` xanh, app khởi động và kết nối được Postgres trong Docker
- [ ] Thiếu `JWT_SECRET` → app **không** khởi động, log nêu đúng tên biến, không in giá trị
- [ ] `/api/health` trả thời gian có offset `+07:00`, không phải `Z`
- [ ] `npm run build` không lỗi, không cảnh báo budget
- [ ] `localhost:4200` hiển thị trạng thái lấy từ API qua proxy cùng origin
- [ ] `psql` xác nhận `btree_gist` cài được
- [ ] `git status` không thấy `.env`, thư mục build, `node_modules`
- [ ] Đã biết máy dùng lúc bảo vệ có Docker và mạng hay không

## Risk Assessment

| Rủi ro | Dấu hiệu | Phản ứng đã định |
|---|---|---|
| Máy dùng lúc bảo vệ không có Docker hoặc không có mạng | Phát hiện lúc mang máy tới | Hỏi và xác nhận **ngay phase này**. `docs/cai-dat.md` (Phase 9) có đường chạy thủ công; chuẩn bị sẵn video demo dự phòng. Đây là rủi ro thật, khác với `btree_gist` |
| Image Postgres không cài được `btree_gist` | `CREATE EXTENSION` báo lỗi | Kiểm tra ở đây vì nó rẻ. **Không có phương án lùi tương đương**: `SELECT ... FOR UPDATE` khoá dòng đã tồn tại, không ngăn hai transaction cùng `INSERT` vào `booking_rooms`, tức là quay về đảm bảo ở tầng ứng dụng — đúng thứ Phase 5 loại bỏ. Nếu thật sự xảy ra thì phải replan Phase 5 và viết lại `docs/erd.md`, không phải đổi một dòng cấu hình |
| Dependency thiếu, phát hiện giữa Phase 6 | Build gãy, phải sửa `pom.xml` giữa chừng | Khai báo đủ ngay ở bước 2; danh sách đã đối chiếu với nhu cầu của cả 8 phase |
| `ddl-auto` vô tình để `update` phá schema Flyway | Bảng có cột lạ không nằm trong migration | Chốt `validate` ngay từ Phase 1; test khởi động context phát hiện sớm |
| Lệnh khởi tạo Angular/Tailwind đã đổi ở bản mới | `ng new` hoặc `tailwindcss init` báo cờ không tồn tại | Đọc hướng dẫn của phiên bản vừa cài, không copy lệnh từ trí nhớ |

**Rollback:** phase này chỉ tạo file mới, `git reset --hard` về commit trước là sạch.
