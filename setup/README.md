# Cài đặt và dữ liệu thử

Thư mục theo gợi ý ở mục 4.3 của quy định: *"chứa các tập tin dùng để install
chương trình thi hành, các tập tin chứa dữ liệu thử tương ứng với kết quả đã báo
cáo trong Đồ án trước Hội đồng."*

## Cách cài đặt

Hệ thống chạy bằng **ba lệnh**:

```bash
git clone <địa-chỉ-repo> homestay-tvh && cd homestay-tvh
./scripts/init-env.sh          # sinh bí mật ngẫu nhiên vào .env
docker compose up -d --build   # dựng và chạy 4 dịch vụ
```

Hướng dẫn đầy đủ, gồm cả đường chạy thủ công không cần Docker và bảng xử lý sự
cố: [`docs/cai-dat.md`](../docs/cai-dat.md).

## Dữ liệu thử

Dữ liệu minh hoạ **không nằm trong thư mục này** mà được nạp tự động khi chạy ở
cấu hình `demo`, từ tệp:

```
backend/src/main/resources/db/seed/demo-data.sql
```

Lý do dữ liệu mẫu không đi qua công cụ migration: nếu nó là một migration thì
việc chuyển sang cấu hình thật trên cùng khối dữ liệu sẽ mang theo cả 40 đơn
giả. Giải thích đầy đủ trong lớp `DemoDataSeeder`.

Dữ liệu nạp sẵn: 4 loại phòng, 15 phòng vật lý, 12 tiện nghi, **40 đơn đặt phòng
trải đủ tám trạng thái**, 3 khoản cần đối soát, 3 mã khuyến mãi, 8 đánh giá,
1 khoảng đóng phòng, và nội dung trang chủ đầy đủ.

Mọi ngày trong dữ liệu mẫu là **tương đối** so với ngày hiện tại, nên bộ dữ liệu
không cũ đi theo thời gian.

## Sơ đồ triển khai

Xem [`docs/kien-truc.md`](../docs/kien-truc.md) — mục "Thành phần khi chạy bằng
Docker".
