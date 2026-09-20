# PHẦN I — MỞ ĐẦU

## 1. Lý do chọn đề tài

Du lịch homestay tại Đồng bằng sông Cửu Long nói chung và Trà Vinh nói riêng
đang tăng nhanh cùng làn sóng du lịch trải nghiệm và du lịch cộng đồng. Khác với
khách sạn, homestay thường do hộ gia đình tự vận hành, quy mô từ vài phòng tới
vài chục phòng, và không có bộ phận lễ tân trực suốt ngày đêm.

Thực tế vận hành của nhóm cơ sở lưu trú này bộc lộ ba vấn đề.

**Thứ nhất, kênh đặt phòng phân mảnh.** Chủ homestay nhận đặt phòng qua Facebook
Messenger, Zalo, điện thoại, và đôi khi qua các sàn trung gian. Mỗi kênh là một
danh sách riêng trong trí nhớ hoặc trên một cuốn sổ. Không có nơi nào cho biết
một ngày cụ thể còn bao nhiêu phòng ngoài việc tự nhớ lại.

**Thứ hai, rủi ro đặt trùng phòng là rủi ro thường trực.** Khi hai khách nhắn tin
gần như cùng lúc và chủ nhà trả lời còn phòng cho cả hai, sai sót chỉ lộ ra vào
ngày nhận phòng — lúc không còn cách khắc phục nào ngoài việc từ chối một khách
đã đi đường xa tới nơi. Đây không phải lỗi bất cẩn mà là hệ quả tất yếu của việc
quản lý lịch phòng bằng trí nhớ con người.

**Thứ ba, chi phí hoa hồng của sàn trung gian.** Các sàn đặt phòng trực tuyến thu
khoảng 15–20% giá trị mỗi đơn. Với homestay quy mô nhỏ, đó là phần lớn biên lợi
nhuận. Nhưng rời sàn thì mất luôn kênh tiếp cận khách.

Một website đặt phòng riêng giải quyết cả ba vấn đề: gom mọi đơn về một nơi, để
cơ sở dữ liệu chứ không phải trí nhớ con người bảo đảm không bán trùng phòng, và
giữ lại toàn bộ doanh thu.

Về mặt học thuật, đề tài có giá trị riêng. Bài toán chống đặt trùng phòng là một
bài toán tranh chấp dữ liệu kinh điển, không giải được đúng bằng cách kiểm tra
thông thường ở tầng ứng dụng. Nó buộc người thực hiện phải hiểu về giao dịch cơ
sở dữ liệu, mức cô lập, và ràng buộc toàn vẹn ở tầng lưu trữ — những nội dung
cốt lõi của ngành mà một đồ án quản lý thông thường không chạm tới.

## 2. Mục đích nghiên cứu

### 2.1. Mục đích tổng quát

Xây dựng một website hoạt động được, có dữ liệu minh hoạ, giải quyết đúng bài
toán giới thiệu và đặt phòng homestay, gồm ba khối: trang bán hàng cho khách,
khu quản trị cho chủ homestay, và giao diện lập trình ứng dụng kết nối hai khối.

### 2.2. Mục đích cụ thể

| # | Mục đích | Tiêu chí nghiệm thu |
|---|---|---|
| 1 | Khách tìm được phòng trống theo ngày và đặt phòng thành công | Đặt được đơn từ trang chủ tới khi có mã QR |
| 2 | Hệ thống không bao giờ bán trùng một phòng | Kiểm thử đa luồng: nhiều luồng cùng đặt phòng cuối, đúng một luồng thắng |
| 3 | Đặt cọc qua mã QR, webhook tự xác nhận | Đơn tự chuyển trạng thái đã xác nhận khi tiền về đủ |
| 4 | Không có nhánh nào để tiền của khách biến mất im lặng | Mọi khoản không khớp vào hàng đợi đối soát |
| 5 | Quản trị viên quản lý được phòng, giá, số khách và ngày khả dụng | Đóng phòng theo khoảng ngày, hệ thống tự mở lại |
| 6 | Giao diện đạt chuẩn trang đặt phòng thương mại | Không còn lỗi vi phạm chuẩn khả năng tiếp cận mức A và AA |
| 7 | Một lệnh là đủ để chạy toàn hệ thống kèm dữ liệu mẫu | Ba lệnh từ lúc sao chép mã nguồn tới lúc hệ thống chạy |
| 8 | Tài liệu kỹ thuật đầy đủ phục vụ bảo vệ | 12 tài liệu kỹ thuật |

## 3. Đối tượng nghiên cứu

**Đối tượng nghiên cứu** gồm ba nhóm:

1. Quy trình nghiệp vụ đặt phòng lưu trú ngắn ngày của cơ sở homestay quy mô nhỏ.
2. Cơ chế bảo đảm toàn vẹn dữ liệu khi có tranh chấp trong hệ quản trị cơ sở dữ
   liệu quan hệ PostgreSQL, cụ thể là kiểu dữ liệu khoảng và ràng buộc loại trừ.
3. Kiến trúc ứng dụng web tách rời giữa giao diện người dùng và máy chủ, giao
   tiếp qua giao diện lập trình ứng dụng theo kiểu REST.

**Phạm vi nghiên cứu:**

- *Về nghiệp vụ:* từ lúc khách tìm phòng tới lúc trả phòng, cộng với đối soát
  thanh toán và báo cáo doanh thu. Không bao gồm kế toán, thuế, nhân sự, kho.
- *Về kỹ thuật:* Java 21 và Spring Boot 3.5.6 ở tầng máy chủ; Angular 21 và
  Tailwind CSS 4 ở tầng giao diện; PostgreSQL 16 làm cơ sở dữ liệu; Docker
  Compose để đóng gói.
- *Về dữ liệu:* dữ liệu minh hoạ tự sinh, mô phỏng một homestay 15 phòng thuộc
  4 loại phòng.

**Ngoài phạm vi.** Việc nêu rõ giới hạn là một phần của thiết kế, không phải
thiếu sót:

| Không thực hiện | Lý do |
|---|---|
| Kết nối cổng thanh toán thật | Đề tài không yêu cầu; hệ thống viết đúng luồng thật và đọc khoá từ biến môi trường |
| Ứng dụng di động | Website đã đáp ứng trên điện thoại |
| Đa ngôn ngữ | Chỉ phục vụ khách trong nước |
| Nhiều chi nhánh | Bài toán khác hẳn về mô hình dữ liệu |
| Trò chuyện trực tuyến | Không thuộc lõi nghiệp vụ đặt phòng |

## 4. Nhiệm vụ nghiên cứu

Để đạt được các mục đích trên, đề tài xác định sáu nhiệm vụ cụ thể:

1. **Khảo sát và phân tích** quy trình đặt phòng thủ công hiện tại của các cơ sở
   homestay quy mô nhỏ, xác định các điểm yếu có thể tin học hoá.
2. **Nghiên cứu cơ sở lý thuyết** về toàn vẹn dữ liệu khi có tranh chấp trong hệ
   quản trị cơ sở dữ liệu quan hệ, đặc biệt là kiểu dữ liệu khoảng và ràng buộc
   loại trừ của PostgreSQL.
3. **Phân tích và thiết kế hệ thống** bằng ngôn ngữ mô hình hoá thống nhất: biểu
   đồ use case, lược đồ cơ sở dữ liệu, biểu đồ lớp, biểu đồ trạng thái, biểu đồ
   tuần tự, kiến trúc phân lớp.
4. **Xây dựng hệ thống** gồm ba khối: giao diện lập trình ứng dụng, trang bán
   hàng cho khách và khu quản trị cho chủ homestay.
5. **Kiểm thử** bằng bộ kiểm thử tự động chạy trên cơ sở dữ liệu thật, bao gồm
   kiểm thử tranh chấp đa luồng.
6. **Đóng gói và lập tài liệu** để hệ thống chạy lại được trên máy khác và có
   thể bàn giao.

## 5. Các kết quả đạt được của đề tài

| # | Kết quả | Số liệu |
|---|---|---|
| 1 | Hệ thống hoàn chỉnh ba khối, chạy được bằng ba lệnh | 4 dịch vụ trong một tệp cấu hình |
| 2 | Giao diện lập trình ứng dụng đầy đủ | **80 thao tác** trên **63 đường dẫn** |
| 3 | Lược đồ cơ sở dữ liệu có ràng buộc toàn vẹn ở tầng lưu trữ | **21 bảng**, **8 migration**, **20 thực thể** |
| 4 | Chống bán trùng phòng được chứng minh bằng thực nghiệm | 3 ca kiểm thử đa luồng thật |
| 5 | Bộ kiểm thử tự động | **129 ca**, **18 lớp**, tất cả thành công |
| 6 | Giao diện đạt chuẩn khả năng tiếp cận | Không còn lỗi vi phạm WCAG 2.1 mức A và AA [10] |
| 7 | Dữ liệu minh hoạ đủ để trình diễn | 40 đơn trải đủ 8 trạng thái |
| 8 | Tài liệu kỹ thuật | 12 tài liệu |

Kết quả có ý nghĩa nhất không phải số lượng chức năng mà là **lời giải cho bài
toán chống đặt trùng phòng**: đặt ràng buộc ở tầng cơ sở dữ liệu thay vì kiểm
tra ở tầng ứng dụng, và chứng minh nó đúng bằng kiểm thử đa luồng trên
PostgreSQL thật.

## 6. Phương pháp nghiên cứu

| Phương pháp | Áp dụng cụ thể trong đề tài |
|---|---|
| **Nghiên cứu tài liệu** | Đọc tài liệu chính thức của PostgreSQL về kiểu khoảng [1], mức cô lập giao dịch [2] và chỉ mục GiST [3]; tài liệu Spring Framework về quy tắc huỷ giao dịch [4]; tài liệu tích hợp webhook của nhà cung cấp thanh toán [8] |
| **Khảo sát và phân tích đối sánh** | So sánh giao diện và luồng đặt phòng của hai nền tảng đặt phòng lớn để rút ra mẫu thiết kế nên áp dụng và mẫu không nên áp dụng [13][14] |
| **Phân tích thiết kế hướng đối tượng** | Mô hình hoá bằng UML: biểu đồ use case, biểu đồ lớp, biểu đồ tuần tự, biểu đồ trạng thái |
| **Thực nghiệm** | Dựng hệ thống thật, chạy 129 kịch bản kiểm thử tự động trên PostgreSQL thật, đo kết quả bằng lệnh |
| **Kiểm thử đối kháng** | Chủ động tạo tình huống xấu: nhiều luồng cùng đặt phòng cuối, tiền về sau khi đơn đã hết hạn, webhook gửi lại hai lần, giả mạo địa chỉ để vượt giới hạn tần suất |

## 7. Ý nghĩa khoa học và thực tiễn

**Ý nghĩa thực tiễn.** Hệ thống có thể đưa vào sử dụng thật cho một homestay quy
mô nhỏ sau khi thay dữ liệu mẫu bằng dữ liệu thật và bổ sung giao thức bảo mật
HTTPS. Chi phí vận hành gần như bằng không nếu tự đặt trên một máy chủ ảo phổ
thông.

**Ý nghĩa khoa học.** Đề tài chứng minh bằng thực nghiệm rằng bài toán chống đặt
trùng phòng không giải được đúng bằng mẫu kiểm tra rồi ghi ở tầng ứng dụng, và
trình bày một lời giải đặt ràng buộc ở tầng lưu trữ. Kết quả này áp dụng lại
được cho mọi bài toán đặt chỗ theo khoảng: đặt sân thể thao, đặt phòng họp, mượn
thiết bị, lịch khám bệnh.

## 8. Kết cấu của đồ án

Đồ án gồm phần mở đầu, năm chương nội dung, phần kết luận và phụ lục:

- **Chương 1 — Tổng quan về đề tài:** khảo sát hiện trạng, phân tích các hệ
  thống tương tự, và xác định yêu cầu chức năng, phi chức năng.
- **Chương 2 — Cơ sở lý thuyết và công nghệ sử dụng:** trình bày nền tảng lý
  thuyết và lý do lựa chọn từng công nghệ.
- **Chương 3 — Phân tích và thiết kế hệ thống:** mô hình hoá bằng UML, thiết kế
  cơ sở dữ liệu, thiết kế giao diện lập trình ứng dụng và thiết kế giao diện
  người dùng.
- **Chương 4 — Xây dựng và triển khai hệ thống:** cài đặt các chức năng, mô hình
  bảo mật và quy trình đóng gói.
- **Chương 5 — Kiểm thử và đánh giá:** chiến lược kiểm thử, kết quả, đối chiếu
  với mục đích ban đầu và các giới hạn đã biết.
- **Kết luận và đề nghị.**
