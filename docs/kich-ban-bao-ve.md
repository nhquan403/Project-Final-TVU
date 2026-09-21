# Kịch bản nói khi bảo vệ

Sinh tự động từ phần ghi chú của từng slide trong `scripts/noi-dung-slide.py`
bằng `scripts/tao-kich-ban.py`. Sửa ghi chú ở đó rồi chạy lại cả hai script —
đừng sửa thẳng vào tệp này.

**Thời lượng:** hội đồng cho 10–20 phút *kể cả hỏi đáp*, nên phần trình bày chỉ
nên **10–12 phút**. Với 20 slide, trung bình **35 giây một slide**. Ba slide
được phép nói lâu hơn — slide 9, 10 và 16 — nên các slide còn lại phải gọn.

| Đoạn | Slide | Thời lượng |
|---|---|---|
| Mở đầu và bài toán | 1–6 | ~3 phút |
| Vấn đề kỹ thuật then chốt | 7–10 | ~3 phút |
| Thiết kế và cài đặt | 11–15 | ~3 phút |
| Kết quả và kết luận | 16–20 | ~2 phút |

---

## Slide 1 — Bìa

**Nói:**

> Chào thầy cô. Em xin trình bày đồ án thực tập chuyên ngành: Xây dựng website giới thiệu và đặt phòng homestay cho Homestay TVH.

## Slide 2 — Nội dung trình bày

**Trên slide:** Bài toán và mục tiêu · Vấn đề kỹ thuật then chốt · Thiết kế và cài đặt · Kết quả kiểm chứng · Giới hạn và hướng phát triển

**Nói:**

> Phần trình bày gồm năm mục. Em sẽ dành nhiều thời gian nhất cho mục hai — vấn đề kỹ thuật then chốt của đề tài.

## Slide 3 — Bài toán: homestay quản lý phòng bằng trí nhớ

**Trên slide:** Đặt phòng đến từ nhiều kênh tin nhắn khác nhau · Không có nơi nào cho biết một ngày còn bao nhiêu phòng · Sàn trung gian thu 15–20% mỗi đơn

**Hình:** `hinh-1-2-quy-trinh-thu-cong.png`

**Nói:**

> Chủ homestay nhận đặt phòng qua nhiều kênh. Mỗi kênh là một danh sách riêng trong trí nhớ. Ba điểm tô đỏ trên lưu đồ là ba chỗ hỏng: tra cứu bằng trí nhớ, không có bản ghi tập trung, và hai khách nhắn cùng lúc.

## Slide 4 — Bài toán cốt lõi: không được bán trùng phòng

**Trên slide:** Hai khách liên hệ gần như cùng lúc · Chủ nhà trả lời còn phòng cho cả hai · Sai sót chỉ lộ ra vào ngày nhận phòng

**Nói:**

> Đây không phải lỗi bất cẩn. Đây là hệ quả tất yếu của việc quản lý lịch phòng bằng trí nhớ con người. Và đây chính là bài toán kỹ thuật mà đồ án tập trung giải.

## Slide 5 — Quy ước nửa mở cho khoảng ngày

**Trên slide:** Trả phòng 10/03 và nhận phòng 10/03 KHÔNG chồng lấn · Viết là [nhận, trả) — chứa ngày đầu, không chứa ngày cuối · Sai quy ước này là mất một đêm bán được của mỗi phòng

**Hình:** `hinh-1-1-khoang-nua-mo.png`

**Nói:**

> Trước khi nói chuyện chống trùng, phải thống nhất thế nào là trùng. Hai thanh trên hình chạm nhau tại ngày 10 tháng 3 nhưng không chồng lấn — khách này trả phòng buổi sáng, khách kia nhận phòng buổi chiều.

## Slide 6 — Mục tiêu và phạm vi

**Trên slide:** Ba khối: trang khách, khu quản trị, giao diện lập trình ứng dụng · Đặt cọc qua mã QR, webhook tự xác nhận · Không có nhánh nào để tiền của khách biến mất im lặng · Một lệnh là đủ để chạy toàn hệ thống kèm dữ liệu mẫu

**Nói:**

> Tám mục đích cụ thể, mỗi mục đích đi kèm một tiêu chí nghiệm thu đo được, không phải một lời hứa.

## Slide 7 — Công nghệ sử dụng

**Trên slide:** Máy chủ: Java 21, Spring Boot 3.5 · Giao diện: Angular 21, Tailwind CSS v4 · Cơ sở dữ liệu: PostgreSQL 16 — chọn vì một lý do rất cụ thể · Đóng gói: Docker Compose, bốn dịch vụ

**Nói:**

> Em chọn PostgreSQL không phải vì nó mạnh hơn MySQL. Slide sau sẽ nói rõ lý do.

## Slide 8 — Kiến trúc tổng thể

**Hình:** `hinh-2-1-kien-truc-tong-the.png`

**Nói:**

> Bốn dịch vụ. Máy chủ web là đường vào duy nhất. Cơ sở dữ liệu và hộp thư không mở cổng ra ngoài — chỉ máy chủ ứng dụng gọi tới được.

## Slide 9 — Vì sao kiểm tra ở tầng ứng dụng KHÔNG đủ

**Trên slide:** Luồng 1 đọc: còn phòng → Luồng 2 đọc: còn phòng · Cả hai cùng ghi → bán trùng · Mức cô lập Read Committed không ngăn được · Tài liệu PostgreSQL khuyên dùng ràng buộc toàn vẹn

**Hình:** `hinh-2-2-tranh-chap-dat-phong.png`

**Nói:**

> Đây là slide quan trọng nhất. Cột trái là mẫu kiểm-tra-rồi-ghi: hai luồng cùng đọc thấy còn phòng, cùng ghi, kết quả bán trùng. Khoảng trống giữa lúc đọc và lúc ghi là khe hở không bịt được bằng logic thông thường.

## Slide 10 — Giải pháp: để cơ sở dữ liệu quyết định

**Trên slide:** EXCLUDE USING gist (room_id WITH =, stay WITH &&) · Cột stay sinh tự động: daterange(nhận, trả, '[)') · Luồng thứ hai nhận lỗi 23P01 và bị bác — ngay trong giao dịch · MySQL không có daterange lẫn ràng buộc loại trừ

**Nói:**

> Ràng buộc loại trừ là thứ MySQL không có. Chỉ số GiST cho phép so sánh chồng lấn — việc mà chỉ số B-tree không làm được. Khi hai giao dịch cùng ghi, PostgreSQL bác một cái, và hệ thống bắt đúng mã lỗi đó rồi thử phòng tiếp theo trong một giao dịch MỚI.

## Slide 11 — Cơ sở dữ liệu: 21 bảng, 8 migration

**Hình:** `hinh-3-3-erd-dat-phong.png`

**Nói:**

> Đây là nhóm bảng đặt phòng — nơi đặt ràng buộc quan trọng nhất. Lược đồ do công cụ migration quản lý, và migration là bất biến: đã chạy thì không sửa, chỉ thêm bản mới.

## Slide 12 — Máy trạng thái đơn đặt phòng

**Hình:** `hinh-3-5-trang-thai-don.png`

**Nói:**

> Tám trạng thái. Hai đường đáng chú ý: đơn đã huỷ và đơn đã hết hạn vẫn quay lại được trạng thái chờ đối soát, khi tiền của khách về muộn. Không có nhánh nào để tiền biến mất im lặng.

## Slide 13 — Thanh toán: mã QR và webhook

**Hình:** `hinh-3-7-tuan-tu-thanh-toan.png`

**Nói:**

> Khách quét mã QR chuyển khoản. Nhà cung cấp gọi webhook. Hệ thống kiểm năm lớp trước khi cho phép một đồng nào ảnh hưởng tới dữ liệu: khoá xác thực, số tài khoản, chiều tiền, chống xử lý trùng, và số tiền.

## Slide 14 — Giao diện khách: ba bước đặt phòng

**Hình:** `hinh-3-14-ba-buoc-dat-phong.png`

**Nói:**

> Ba bước: chọn ngày, chọn phòng, xác nhận. Giá hiển thị là tổng cả kỳ chứ không phải giá một đêm — đây là điều khách thực sự muốn biết. Ngày đã hết phòng bị chặn ngay trong lịch, không để khách chọn rồi mới báo lỗi.

## Slide 15 — Khu quản trị: 11 màn hình

**Hình:** `hinh-3-16-dashboard.png`

**Nói:**

> Trang tổng quan: giá trị booking theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ, và số khoản tiền đang chờ đối soát. Khu quản trị ưu tiên mật độ thông tin vì chủ homestay dùng nó hằng ngày.

## Slide 16 — Kết quả kiểm thử

**Trên slide:** 129 ca kiểm thử, tất cả đạt · Kiểm thử đa luồng với PostgreSQL thật, không giả lập · 11 lỗi thực tế đã tìm ra và khắc phục · Ma trận phân quyền: 30 tiền tố đường dẫn đều được khai

**Nói:**

> Kiểm thử tích hợp chạy trên PostgreSQL thật qua Testcontainers, vì ràng buộc loại trừ là tính năng của PostgreSQL — giả lập bằng cơ sở dữ liệu trong bộ nhớ thì đúng thứ cần kiểm lại không tồn tại.

## Slide 17 — Khả năng tiếp cận và triển khai

**Trên slide:** Quét 17 màn hình: 0 vi phạm WCAG 2.1 mức A và AA · Kiểm ở ba độ rộng màn hình, vùng chạm tối thiểu 44×44 điểm ảnh · docker compose up -d: bốn dịch vụ đều healthy

**Hình:** `hinh-3-20-docker-compose-ps.png`

**Nói:**

> Quét tự động ngay trên bản đóng gói đang chạy, đi qua đúng máy chủ web và đúng chính sách bảo mật nội dung của bản triển khai.

## Slide 18 — Giới hạn đã biết

**Trên slide:** Chưa có bảng giá theo từng đêm — giá hiện là giá cố định theo loại phòng · Tầng giao diện chưa có bộ kiểm thử tự động thường trực · Hoàn tiền chưa tự động, chỉ ghi nhận để người xử lý

**Nói:**

> Em nêu rõ giới hạn thay vì giấu. Ba giới hạn này đều là chỗ mở rộng được, không phải chỗ thiết kế sai.

## Slide 19 — Hướng phát triển

**Trên slide:** Bảng giá theo mùa và theo ngày trong tuần · Ứng dụng di động — dùng lại nguyên giao diện lập trình ứng dụng · Đồng bộ lịch hai chiều với sàn trung gian

**Nói:**

> Kiến trúc tách rời cho phép ứng dụng di động dùng lại nguyên giao diện lập trình ứng dụng hiện có, không phải viết lại tầng máy chủ.

## Slide 20 — Cảm ơn

**Nói:**

> Em xin cảm ơn thầy cô đã lắng nghe. Em sẵn sàng trả lời câu hỏi.
