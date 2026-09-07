---
title: "Phase 1: Khởi tạo monorepo & hạ tầng dev"
status: todo
phase: 1
priority: P1
effort: "4h"
dependencies: []
---

# Phase 1: Khởi tạo monorepo & hạ tầng dev

## Overview

Dựng bộ khung monorepo chạy được: Spring Boot khởi động và kết nối Postgres trong Docker, Angular dev server chạy với Tailwind, hai bên nói chuyện được qua một endpoint health. Chưa có nghiệp vụ nào.

## Requirements

- Functional: `GET /api/health` trả `{"status":"UP"}`; trang Angular gốc gọi được endpoint đó qua proxy.
- Non-functional: Postgres chạy trong Docker với `btree_gist` sẵn sàng; mọi cấu hình nhạy cảm đọc từ biến môi trường.

## Architecture

Monorepo hai thư mục độc lập, không chia sẻ build tool:

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

Dev chạy `docker compose -f docker-compose.dev.yml up -d` (chỉ hạ tầng), rồi `mvn spring-boot:run` và `ng serve` ở máy host để có hot reload. Compose đầy đủ (đóng gói cả API + web) để ở Phase 8.

## Related Code Files

- Create: `backend/pom.xml` — Spring Boot 3.5.x parent; dependency: `web`, `data-jpa`, `security`, `validation`, `mail`, `postgresql`, `flyway-core`, `springdoc-openapi-starter-webmvc-ui`, `lombok`, `mapstruct`, `testcontainers` (test scope)
- Create: `backend/src/main/java/com/tvh/homestay/HomestayApplication.java`
- Create: `backend/src/main/java/com/tvh/homestay/common/HealthController.java`
- Create: `backend/src/main/resources/application.yml`, `application-dev.yml`, `application-prod.yml`
- Create: `frontend/` qua `ng new frontend --standalone --style=css --routing --ssr=false`
- Create: `frontend/tailwind.config.js`, `frontend/src/styles.css`, `frontend/proxy.conf.json`
- Create: `docker-compose.dev.yml`, `.env.example`, `.gitignore`, `README.md`
- Create: `docs/.gitkeep`

## Implementation Steps

1. Ở branch `claude/homestay-tvh-booking-site-bew1pw`, commit `.gitignore` + `README.md` khung.
2. Sinh backend: Maven project `com.tvh:homestay`, Java 21. Thêm dependency như trên vào `pom.xml`.
3. `application.yml`: datasource đọc `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`; `spring.jpa.hibernate.ddl-auto: validate` (Flyway là chủ schema, **không bao giờ** để `update`); `spring.jackson.time-zone: Asia/Ho_Chi_Minh`.
4. Viết `HealthController` trả `{"status":"UP","time":...}` tại `GET /api/health`; tạm mở toàn bộ security bằng `SecurityConfig` permitAll (Phase 3 siết lại).
5. `docker-compose.dev.yml`: service `db` (`postgres:16-alpine`, port 5432, volume `pgdata`, `POSTGRES_DB=homestay`) và `mailpit` (port 8025 để xem mail dev).
6. Sinh frontend bằng Angular CLI; cài Tailwind (`npm i -D tailwindcss postcss autoprefixer` + `npx tailwindcss init`); cấu hình `content` trỏ `./src/**/*.{html,ts}`.
7. Khai báo design token trong `tailwind.config.js` (màu thương hiệu, font, spacing) để Phase 6/7 dùng chung, tránh hard-code màu rải rác.
8. `proxy.conf.json` map `/api` → `http://localhost:8080`; sửa `angular.json` để `ng serve` dùng proxy.
9. Trang `app.component` gọi `/api/health` và hiển thị trạng thái — bằng chứng hai đầu đã nối.
10. Viết `.env.example` liệt kê đủ biến (DB, JWT, SEPAY, CLOUDINARY, SMTP) với giá trị rỗng.
11. Viết `README.md`: yêu cầu môi trường (JDK 21, Node 20+, Docker), lệnh chạy dev.

## Verify

```bash
docker compose -f docker-compose.dev.yml up -d
docker exec -it homestay-db psql -U postgres -d homestay \
  -c "CREATE EXTENSION IF NOT EXISTS btree_gist; SELECT extname FROM pg_extension;"

cd backend && ./mvnw -q clean verify && ./mvnw spring-boot:run &
curl -s localhost:8080/api/health          # => {"status":"UP",...}

cd ../frontend && npm ci && npm run build  # build production phải sạch
npm start                                   # localhost:4200 hiển thị "API: UP"

git status --porcelain | grep -E '\.env$' && echo "LOI: .env bi track" || echo "OK"
```

## Todo

- [ ] `.gitignore` (thư mục build Maven, `node_modules/`, `.env`, `uploads/`) + README khung
- [ ] Backend Maven skeleton + dependency đầy đủ
- [ ] `application.yml` đọc toàn bộ config từ env
- [ ] `HealthController` + `SecurityConfig` tạm
- [ ] `docker-compose.dev.yml` (Postgres 16 + Mailpit)
- [ ] Angular workspace + Tailwind + design token
- [ ] `proxy.conf.json` + trang health hiển thị trạng thái API
- [ ] `.env.example` đủ biến

## Success Criteria

- [ ] `./mvnw verify` xanh, app khởi động và kết nối được Postgres trong Docker
- [ ] `npm run build` của Angular không lỗi, không cảnh báo budget
- [ ] `localhost:4200` hiển thị trạng thái lấy từ `localhost:8080/api/health`
- [ ] `psql` xác nhận extension `btree_gist` cài được (điều kiện bắt buộc cho Phase 2)
- [ ] `git status` sạch, không track `.env`, thư mục build, `node_modules`

## Risk Assessment

| Rủi ro | Dấu hiệu | Phản ứng đã định |
|---|---|---|
| Image Postgres không cho tạo `btree_gist` | Lệnh `CREATE EXTENSION` báo lỗi | Đổi sang image `postgres:16` bản đầy đủ (không alpine); nếu vẫn lỗi → replan Phase 4 sang khoá bi quan `SELECT ... FOR UPDATE` và ghi rõ đánh đổi vào docs |
| `ddl-auto` vô tình để `update` phá schema Flyway | Bảng có cột lạ không nằm trong migration | Chốt `validate` ngay từ Phase 1; thêm test khởi động context để phát hiện sớm |
| Tailwind bản mới đổi cách cấu hình | `npx tailwindcss init` không sinh file | Theo đúng hướng dẫn của bản CLI vừa cài, không copy config từ trí nhớ |

**Rollback:** phase này chỉ tạo file mới, `git reset --hard` về commit trước là sạch.
