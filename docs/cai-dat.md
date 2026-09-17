# Cài đặt và chạy

Hai đường: **Docker** (một lệnh, khuyến nghị) và **thủ công** (khi máy không có
Docker).

---

## Đường 1 — Docker

### Yêu cầu

| Công cụ | Bản tối thiểu |
|---|---|
| Docker Engine | 24 |
| Docker Compose | v2 |
| openssl | bản nào cũng được (thường có sẵn) |

### Ba lệnh

```bash
git clone <repo-url> homestay-tvh && cd homestay-tvh
./scripts/init-env.sh
docker compose up -d --build
```

Lần dựng đầu mất vài phút vì phải tải phụ thuộc Maven và npm. Những lần sau dùng
lại tầng cache nên nhanh hơn nhiều.

Kiểm tra:

```bash
docker compose ps          # cả 4 service phải healthy
curl localhost/api/health  # {"status":"UP",...}
```

| Địa chỉ | Là gì |
|---|---|
| http://localhost | Trang khách |
| http://localhost/admin | Khu quản trị |
| http://localhost/swagger-ui/index.html | Tài liệu API tương tác (chỉ profile `demo`) |
| http://localhost:8025 | Hộp thư giả — xem thư xác nhận gửi đi |

### Lấy mật khẩu quản trị demo

Mật khẩu sinh ngẫu nhiên lúc nạp dữ liệu mẫu và **chỉ** nằm trong log container:

```bash
docker compose logs api | grep -i -A 4 'mat khau admin demo'
```

Tài khoản: `admin@tvh.local`. Lần đăng nhập đầu, hệ thống **bắt buộc** đổi mật
khẩu trước khi vào được bất kỳ màn hình quản trị nào — mật khẩu tạm đã đi qua
log nên không còn là bí mật.

Hai tài khoản khách mẫu dùng chung mật khẩu `khachdemo123`:
`an.nguyen@example.com` và `binh.tran@example.com`. Đây là tài khoản trình diễn,
chỉ xem được đơn của chính nó.

> **Khi triển khai thật:** đổi mật khẩu quản trị, và chạy ở profile `prod`
> (`SPRING_PROFILES_ACTIVE=prod`) để không nạp dữ liệu mẫu và không mở Swagger.
> Bản compose này phục vụ HTTP trần — xem
> [bao-mat.md](./bao-mat.md#7-không-có-https-trong-bản-đóng-gói).

### Lệnh hay dùng

```bash
docker compose logs -f api            # theo dõi log backend
docker compose exec db psql -U homestay -d homestay   # vào cơ sở dữ liệu
docker compose restart api            # khởi động lại backend
docker compose down                   # dừng, GIỮ dữ liệu
docker compose down -v                # dừng và XOÁ SẠCH dữ liệu
```

`down -v` xoá cả volume `pgdata` lẫn `uploads`. Lần `up` kế tiếp dựng lại dữ
liệu mẫu từ đầu và sinh **mật khẩu quản trị mới**.

### Đổi cổng

Cổng 80 bận thì sửa `.env`:

```
WEB_PORT=8081
MAILPIT_PORT=8026
```

rồi `docker compose up -d`.

---

## Đường 2 — Chạy thủ công, không cần Docker

Dùng khi máy không cài được Docker. Cần **PostgreSQL 16 cài sẵn trên máy**.

### Yêu cầu

| Công cụ | Bản tối thiểu |
|---|---|
| JDK | 21 |
| Node.js | 20 (khuyến nghị 22) |
| PostgreSQL | 16 — bắt buộc, vì `EXCLUDE USING gist` và kiểu `daterange` |

PostgreSQL 16 là điều kiện cứng, không phải sở thích: ràng buộc chống bán trùng
phòng dựa trên `EXCLUDE USING gist` và `btree_gist`. MySQL không có.

### 1. Cơ sở dữ liệu

```bash
createdb homestay
psql -d homestay -c "CREATE EXTENSION IF NOT EXISTS btree_gist;"
```

Flyway tự dựng 21 bảng ở lần chạy đầu.

### 2. Biến môi trường

```bash
export DB_URL=jdbc:postgresql://localhost:5432/homestay
export DB_USER=postgres
export DB_PASSWORD=<mật khẩu postgres của bạn>
export JWT_SECRET=$(openssl rand -base64 48)
export SEPAY_WEBHOOK_API_KEY=$(openssl rand -hex 32)
export SEPAY_ACCOUNT_NUMBER=0123456789
export SEPAY_BANK_CODE=MBBank
export TZ=Asia/Ho_Chi_Minh
```

Thiếu `JWT_SECRET` hoặc `SEPAY_WEBHOOK_API_KEY` thì ứng dụng **dừng khởi động**
kèm thông báo rõ tên biến. Đó là chủ ý, không phải lỗi.

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Chạy ở `localhost:8080`. Profile `demo` nạp dữ liệu mẫu và in mật khẩu quản trị
ra màn hình.

### 4. Frontend

Cửa sổ dòng lệnh khác:

```bash
cd frontend
npm ci
npm start
```

Chạy ở `localhost:4200`, có hot reload, và proxy sẵn `/api` + `/uploads` sang
`localhost:8080` (xem `proxy.conf.json`).

### 5. Không có Docker thì không có Mailpit

Bỏ trống `MAIL_HOST` thì hệ thống dùng `LoggingMailSender`: thư không gửi đi
đâu nhưng vẫn được ghi vào bảng `outbound_emails` và in ra log. Đủ để trình diễn
luồng gửi thư.

### Hạ tầng dev bằng Docker, ứng dụng trên máy

Trường hợp trung gian — có Docker nhưng muốn hot reload:

```bash
docker compose -f docker-compose.dev.yml up -d   # chỉ Postgres + Mailpit
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
cd frontend && npm start
```

**Không** chạy `docker-compose.dev.yml` cùng lúc với `docker-compose.yml`: bản
dev bind 5432/1025/8025 ra máy chủ và sẽ đụng cổng.

---

## Xử lý sự cố

| Triệu chứng | Nguyên nhân thường gặp |
|---|---|
| `api` restart liên tục | Thiếu biến bí mật. `docker compose logs api` in rõ tên biến. |
| `POSTGRES_PASSWORD chua duoc dat` | Chưa chạy `./scripts/init-env.sh`. |
| Trang chủ trắng, không có nội dung | Không chạy ở profile `demo` nên `DemoDataSeeder` không chạy. Kiểm tra `SPRING_PROFILES_ACTIVE`. |
| `/swagger-ui` trả trang Angular | nginx thiếu `location ^~ /swagger-ui`. So với `frontend/nginx.conf`. |
| Ảnh tải lên biến mất sau `up` | Volume `uploads` gắn sai chỗ. Phải khớp `STORAGE_LOCAL_DIR`. |
| Đăng nhập luôn báo sai mật khẩu | Với tài khoản demo: mật khẩu đã bị đổi ở lần đăng nhập đầu. `docker compose down -v` để dựng lại. |
| Cổng 80 đã bị chiếm | Đổi `WEB_PORT` trong `.env`. |

## Kiểm thử

Xem [kiem-thu.md](./kiem-thu.md). Tóm tắt:

```bash
cd backend  && ./mvnw verify     # 129 test, CẦN Docker (Testcontainers)
cd frontend && npm run build && npm run lint
```
