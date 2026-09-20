# MỞ ĐẦU

## 1. Lí do chọn đề tài

Du lịch homestay tại Đồng bằng sông Cửu Long nói chung và Trà Vinh nói riêng
đang tăng nhanh cùng làn sóng du lịch trải nghiệm và du lịch cộng đồng. Khác với
khách sạn, homestay thường do hộ gia đình tự vận hành, quy mô từ vài phòng tới
vài chục phòng, và không có bộ phận lễ tân trực suốt ngày đêm.

Thực tế vận hành của nhóm cơ sở lưu trú này bộc lộ ba vấn đề.

**Thứ nhất, kênh đặt phòng phân mảnh.** Chủ homestay nhận đặt phòng qua nhiều
kênh tin nhắn khác nhau và đôi khi qua sàn trung gian. Mỗi kênh là một danh sách
riêng trong trí nhớ hoặc trên một cuốn sổ. Không có nơi nào cho biết một ngày cụ
thể còn bao nhiêu phòng ngoài việc tự nhớ lại.

**Thứ hai, rủi ro đặt trùng phòng là rủi ro thường trực.** Khi hai khách liên hệ
gần như cùng lúc và chủ nhà trả lời còn phòng cho cả hai, sai sót chỉ lộ ra vào
ngày nhận phòng, lúc không còn cách khắc phục nào ngoài việc từ chối một khách
đã đi đường xa tới nơi. Đây không phải lỗi bất cẩn mà là hệ quả tất yếu của việc
quản lý lịch phòng bằng trí nhớ con người.

**Thứ ba, chi phí hoa hồng của sàn trung gian.** Các sàn đặt phòng trực tuyến
thu khoảng 15–20% giá trị mỗi đơn. Với homestay quy mô nhỏ, đó là phần lớn biên
lợi nhuận. Nhưng rời sàn thì mất luôn kênh tiếp cận khách.

Một website đặt phòng riêng giải quyết cả ba vấn đề: gom mọi đơn về một nơi, để
cơ sở dữ liệu chứ không phải trí nhớ con người bảo đảm không bán trùng phòng, và
giữ lại toàn bộ doanh thu.

Về mặt học thuật, đề tài có giá trị riêng. Bài toán chống đặt trùng phòng là một
bài toán tranh chấp dữ liệu kinh điển, không giải được đúng bằng cách kiểm tra
thông thường ở tầng ứng dụng. Nó đòi hỏi hiểu biết về giao dịch cơ sở dữ liệu,
mức cô lập và ràng buộc toàn vẹn ở tầng lưu trữ — những nội dung cốt lõi của
ngành mà một đề tài quản lý thông thường không chạm tới.

## 2. Mục đích nghiên cứu

Xây dựng một website hoạt động được, có dữ liệu minh hoạ, giải quyết đúng bài
toán giới thiệu và đặt phòng homestay, gồm ba khối: trang bán hàng cho khách,
khu quản trị cho chủ homestay, và giao diện lập trình ứng dụng kết nối hai khối.

Tám mục đích cụ thể cùng tiêu chí nghiệm thu tương ứng:

| # | Mục đích | Tiêu chí nghiệm thu |
|---|---|---|
| 1 | Khách tìm được phòng trống theo ngày và đặt phòng thành công | Đặt được đơn từ trang chủ tới khi có mã QR |
| 2 | Hệ thống không bao giờ bán trùng một phòng | Kiểm thử đa luồng: đúng một luồng thắng |
| 3 | Đặt cọc qua mã QR, webhook tự xác nhận | Đơn tự chuyển trạng thái khi tiền về đủ |
| 4 | Không có nhánh nào để tiền của khách biến mất im lặng | Mọi khoản không khớp vào hàng đợi đối soát |
| 5 | Quản trị viên quản lý được phòng, giá, số khách và ngày khả dụng | Đóng phòng theo khoảng ngày, hệ thống tự mở lại |
| 6 | Giao diện đạt chuẩn trang đặt phòng thương mại | Không còn lỗi vi phạm WCAG mức A và AA |
| 7 | Một lệnh là đủ để chạy toàn hệ thống kèm dữ liệu mẫu | Ba lệnh từ lúc sao chép mã nguồn tới lúc chạy |
| 8 | Tài liệu kỹ thuật đầy đủ | 12 tài liệu kỹ thuật kèm mã nguồn |

## 3. Đối tượng nghiên cứu

Đề tài nghiên cứu ba đối tượng:

1. **Quy trình nghiệp vụ** đặt phòng lưu trú ngắn ngày của cơ sở homestay quy mô
   nhỏ, từ lúc khách tìm phòng tới lúc trả phòng.
2. **Cơ chế bảo đảm toàn vẹn dữ liệu khi có tranh chấp** trong hệ quản trị cơ sở
   dữ liệu quan hệ PostgreSQL, cụ thể là kiểu dữ liệu khoảng, ràng buộc loại trừ
   và chỉ mục GiST.
3. **Kiến trúc ứng dụng web tách rời** giữa tầng giao diện và tầng máy chủ, giao
   tiếp qua giao diện lập trình ứng dụng theo kiểu REST.

## 4. Phạm vi nghiên cứu

**Về nghiệp vụ:** từ lúc khách tìm phòng tới lúc trả phòng, cộng với đối soát
thanh toán và báo cáo doanh thu. Không bao gồm kế toán, thuế, nhân sự, kho.

**Về kỹ thuật:** Java 21 và Spring Boot 3.5.6 ở tầng máy chủ; Angular 21 và
Tailwind CSS 4 ở tầng giao diện; PostgreSQL 16 làm cơ sở dữ liệu; Docker Compose
để đóng gói.

**Về dữ liệu:** dữ liệu minh hoạ tự sinh, mô phỏng một homestay 15 phòng thuộc
4 loại phòng.

**Ngoài phạm vi.** Việc nêu rõ giới hạn là một phần của thiết kế:

| Không thực hiện | Lý do |
|---|---|
| Kết nối cổng thanh toán thật | Đề tài không yêu cầu; hệ thống viết đúng luồng thật và đọc khoá từ biến môi trường |
| Hoàn tiền tự động | Hoàn tiền là quyết định kinh doanh; hệ thống đưa vào hàng đợi cho người xử lý |
| Ứng dụng di động | Website đã đáp ứng trên điện thoại |
| Đa ngôn ngữ và đa chi nhánh | Không thuộc lõi nghiệp vụ của một cơ sở đơn lẻ |
| Giao thức bảo mật HTTPS trong bản đóng gói | Cần tên miền và chứng chỉ thật; là giới hạn đã biết, trình bày ở mục 4.6 |
