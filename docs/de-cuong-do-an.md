# ĐỀ CƯƠNG TỔNG HỢP ĐỒ ÁN TỐT NGHIỆP

**Đề tài:** Xây dựng website giới thiệu và đặt phòng homestay — Homestay TVH

---

> **LƯU Ý TRƯỚC KHI DÙNG**
>
> Tài liệu này là **nội dung** của đồ án, không phải bản trình bày đã định dạng.
> Mẫu trình bày riêng của Khoa CNTT — ĐH Trà Vinh không công khai nên **phải xin
> lại giáo viên hướng dẫn**. Trong lúc chờ, tài liệu này neo theo quy định đầy
> đủ và cụ thể của **Khoa Kỹ thuật — Công nghệ, ĐH Văn Hiến** (xem bảng dưới):
>
> | Hạng mục | Quy định |
> |---|---|
> | Độ dài | **tối thiểu 60 trang** phần thuyết minh |
> | Lề | trên 2cm · dưới 2cm · **trái 3cm** · phải 1.5cm |
> | Font | **Times New Roman, size 13** · giãn dòng **1.3** · thụt đầu dòng 1.0cm |
> | Số trang | trang phụ: La Mã · phần chính: Ả Rập · bìa phụ và mục lục: không đánh |
> | Đề mục | `Chương 1` đậm → `1.1` đậm → `1.1.1` đậm nghiêng |
> | Số chương | tối thiểu 3 |
>
> Cấu trúc năm chương dưới đây là cấu trúc dùng chung ở các trường kỹ thuật Việt
> Nam. Nếu mẫu của khoa chia khác (ví dụ gộp Chương 4 và 5), chỉ cần sắp xếp lại
> các mục — nội dung không đổi.
>
> Kế hoạch hoàn thiện thành quyển nộp được nằm ở
> [`plans/260920-0511-viet-bao-cao-do-an/plan.md`](../plans/260920-0511-viet-bao-cao-do-an/plan.md).
>
> Mọi số liệu trong tài liệu này (21 bảng, 80 thao tác API, 129 test…) lấy từ hệ
> thống **đang chạy thật**, không phải ước lượng. Nếu bạn sửa thêm mã sau này,
> nhớ đếm lại.

---

## HƯỚNG DẪN ĐỌC KÝ HIỆU HÌNH ẢNH

Trong toàn bộ tài liệu, chỗ nào cần chèn hình được đánh dấu như sau:

> 🖼️ **[HÌNH X.Y] — Tên hình**
> *Cần gì:* mô tả chính xác hình phải thể hiện điều gì.
> *Lấy ở đâu:* nguồn cụ thể — ảnh chụp màn hình nào, tệp Mermaid nào, hay phải
> tự vẽ bằng công cụ gì.
> *Chú thích dưới hình:* câu chú thích đặt dưới hình trong báo cáo.

Danh sách đầy đủ tất cả hình cần chuẩn bị nằm ở **Phụ lục D** cuối tài liệu.

---

# PHẦN A — CÁC TRANG ĐẦU

Thứ tự các trang đầu (đánh số La Mã i, ii, iii… hoặc không đánh số, tuỳ mẫu khoa):

| # | Trang | Ghi chú nội dung |
|---|---|---|
| 1 | **Trang bìa chính** | Tên trường, khoa, tên đề tài, họ tên sinh viên, MSSV, lớp, GVHD, địa danh và năm. In bìa cứng theo mẫu khoa. |
| 2 | **Trang bìa phụ** | Nội dung giống bìa chính, in giấy thường. |
| 3 | **Nhiệm vụ đồ án** | Do GVHD giao, có chữ ký. Thường là mẫu in sẵn của khoa. |
| 4 | **Lời cảm ơn** | Xem bản mẫu ở Phụ lục A.1. |
| 5 | **Lời cam đoan** | **Bắt buộc ở nhiều khoa.** Xem bản mẫu ở Phụ lục A.2. |
| 6 | **Nhận xét của GVHD** | Để trống, thầy/cô viết. |
| 7 | **Nhận xét của GV phản biện** | Để trống. |
| 8 | **Mục lục** | Tối đa 3 cấp tiêu đề. Dùng chức năng tự sinh của Word — đừng gõ tay. |
| 9 | **Danh mục hình ảnh** | Tự sinh từ caption. Xem Phụ lục D để biết số lượng hình. |
| 10 | **Danh mục bảng biểu** | Tự sinh từ caption. |
| 11 | **Danh mục từ viết tắt** | Xem bản mẫu ở Phụ lục B. |

> 🖼️ **[HÌNH 0.1] — Trang bìa**
> *Cần gì:* không phải "hình", mà là trang bìa định dạng theo mẫu khoa. Nếu
> khoa cho phép, chèn logo Trường ĐH Trà Vinh ở đầu trang.

---

# PHẦN B — MỞ ĐẦU

## 1. Lý do chọn đề tài

Du lịch homestay ở Đồng bằng sông Cửu Long nói chung và Trà Vinh nói riêng đang
tăng nhanh cùng làn sóng du lịch trải nghiệm và du lịch cộng đồng. Khác với
khách sạn, homestay thường do hộ gia đình tự vận hành, quy mô vài phòng tới vài
chục phòng, và **không có bộ phận lễ tân trực 24/7**.

Thực tế vận hành của nhóm cơ sở này bộc lộ ba vấn đề:

**Thứ nhất, kênh đặt phòng phân mảnh.** Chủ homestay nhận đặt phòng qua
Facebook Messenger, Zalo, điện thoại, và đôi khi qua các sàn trung gian như
Booking.com hay Agoda. Mỗi kênh là một danh sách riêng trong đầu hoặc trên một
cuốn sổ. Không có chỗ nào cho biết "ngày 20/10 còn mấy phòng" ngoài việc tự nhớ.

**Thứ hai, rủi ro đặt trùng phòng là rủi ro thường trực.** Khi hai khách nhắn
tin gần như cùng lúc và chủ nhà trả lời "còn phòng" cho cả hai, sai sót chỉ lộ
ra vào ngày nhận phòng — lúc không còn cách khắc phục nào ngoài việc từ chối một
khách đã đi đường xa tới nơi. Đây không phải lỗi bất cẩn mà là hệ quả tất yếu
của việc quản lý lịch phòng bằng trí nhớ.

**Thứ ba, chi phí hoa hồng của sàn trung gian.** Các sàn thu 15–20% giá trị mỗi
đơn. Với homestay quy mô nhỏ, đó là phần lớn biên lợi nhuận. Nhưng rời sàn thì
mất luôn kênh tiếp cận khách.

Một website đặt phòng riêng giải quyết cả ba: gom mọi đơn về một nơi, để **cơ sở
dữ liệu** chứ không phải trí nhớ con người bảo đảm không bán trùng phòng, và
giữ lại toàn bộ doanh thu.

Về mặt học thuật, đề tài có giá trị riêng: bài toán chống đặt trùng phòng là một
bài toán **tranh chấp dữ liệu (concurrency)** kinh điển, không giải được đúng
bằng cách kiểm tra thông thường ở tầng ứng dụng. Nó buộc người làm phải hiểu về
giao dịch cơ sở dữ liệu, mức cô lập, và ràng buộc toàn vẹn ở tầng lưu trữ — những
nội dung cốt lõi của ngành mà một đồ án CRUD thông thường không chạm tới.

## 2. Mục tiêu của đề tài

### 2.1. Mục tiêu tổng quát

Xây dựng một website hoạt động được, có dữ liệu minh hoạ, giải quyết đúng bài
toán giới thiệu và đặt phòng homestay, gồm ba khối: trang bán hàng cho khách,
khu quản trị cho chủ homestay, và REST API kết nối hai khối đó.

### 2.2. Mục tiêu cụ thể

| # | Mục tiêu | Tiêu chí nghiệm thu |
|---|---|---|
| 1 | Khách tìm được phòng trống theo ngày và đặt phòng thành công | Đặt được đơn từ trang chủ tới khi có mã QR |
| 2 | **Hệ thống không bao giờ bán trùng một phòng** | Test đa luồng: nhiều luồng cùng đặt phòng cuối, đúng một luồng thắng |
| 3 | Đặt cọc qua QR VietQR, webhook tự xác nhận | Đơn tự chuyển `CONFIRMED` khi tiền về đủ |
| 4 | Không có nhánh nào để tiền của khách biến mất im lặng | Mọi khoản không khớp vào hàng đợi đối soát |
| 5 | Quản trị viên quản lý được phòng, giá, số khách và **ngày khả dụng** | Đóng phòng theo khoảng ngày, hệ thống tự mở lại |
| 6 | Giao diện đạt chuẩn trang đặt phòng thương mại | 0 lỗi WCAG 2.1 mức A + AA trên các màn hình chính |
| 7 | `docker compose up` là đủ để chạy toàn hệ thống kèm dữ liệu mẫu | Ba lệnh từ lúc clone tới lúc hệ thống chạy |
| 8 | Tài liệu kỹ thuật đầy đủ phục vụ bảo vệ | 12 tài liệu trong `docs/` |

### 2.3. Mục tiêu KHÔNG thuộc phạm vi

Nêu rõ giới hạn là một phần của thiết kế, không phải thiếu sót:

- **Không kết nối cổng thanh toán thật.** Hệ thống viết đúng luồng thật và đọc
  khoá từ biến môi trường, nhưng khi bảo vệ dùng endpoint mô phỏng. Đề bài
  không yêu cầu thanh toán thật.
- **Không có ứng dụng di động.** Website đáp ứng (responsive) trên điện thoại.
- **Không đa ngôn ngữ.** Chỉ tiếng Việt.
- **Không đa chi nhánh.** Một homestay, không phải nền tảng nhiều cơ sở.
- **Không có tính năng chat trực tuyến.**

## 3. Đối tượng và phạm vi nghiên cứu

**Đối tượng nghiên cứu:**
- Quy trình nghiệp vụ đặt phòng lưu trú ngắn ngày.
- Cơ chế bảo đảm toàn vẹn dữ liệu khi có tranh chấp trong hệ quản trị cơ sở dữ
  liệu PostgreSQL.
- Kiến trúc ứng dụng web tách rời frontend — backend qua REST API.

**Phạm vi nghiên cứu:**
- **Về nghiệp vụ:** từ lúc khách tìm phòng tới lúc trả phòng, cộng với đối soát
  thanh toán và báo cáo doanh thu. Không bao gồm kế toán, thuế, nhân sự, kho.
- **Về kỹ thuật:** Java 21 / Spring Boot 3.5.6 ở backend; Angular 21 / Tailwind
  CSS 4 ở frontend; PostgreSQL 16 làm cơ sở dữ liệu; Docker Compose để đóng gói.
- **Về dữ liệu:** dữ liệu minh hoạ tự sinh, mô phỏng một homestay 15 phòng thuộc
  4 loại phòng.

## 4. Nhiệm vụ nghiên cứu

Để đạt được các mục tiêu trên, đề tài xác định sáu nhiệm vụ cụ thể:

1. **Khảo sát và phân tích** quy trình đặt phòng thủ công hiện tại của các cơ sở
   homestay quy mô nhỏ, xác định các điểm yếu có thể tin học hoá.
2. **Nghiên cứu cơ sở lý thuyết** về toàn vẹn dữ liệu khi có tranh chấp trong hệ
   quản trị cơ sở dữ liệu quan hệ, đặc biệt là kiểu dữ liệu khoảng và ràng buộc
   loại trừ của PostgreSQL.
3. **Phân tích và thiết kế hệ thống** bằng UML: use case, lược đồ cơ sở dữ liệu,
   biểu đồ trạng thái, biểu đồ tuần tự, kiến trúc phân lớp.
4. **Xây dựng hệ thống** gồm ba khối: REST API, trang bán hàng cho khách và khu
   quản trị cho chủ homestay.
5. **Kiểm thử** bằng bộ kiểm thử tự động chạy trên cơ sở dữ liệu thật, bao gồm
   kiểm thử tranh chấp đa luồng.
6. **Đóng gói và lập tài liệu** để hệ thống chạy lại được trên máy khác và có
   thể bàn giao.

## 5. Các kết quả đạt được

| # | Kết quả | Số liệu |
|---|---|---|
| 1 | Hệ thống hoàn chỉnh ba khối, chạy được bằng ba lệnh | 4 dịch vụ Docker |
| 2 | REST API đầy đủ | **80 thao tác** trên 63 đường dẫn |
| 3 | Lược đồ cơ sở dữ liệu có ràng buộc toàn vẹn ở tầng lưu trữ | **21 bảng**, 8 migration |
| 4 | **Chống bán trùng phòng được chứng minh bằng thực nghiệm** | 3 ca kiểm thử đa luồng thật |
| 5 | Bộ kiểm thử tự động | **129 ca**, 18 lớp, tất cả xanh |
| 6 | Giao diện đạt chuẩn khả năng tiếp cận | 0 lỗi WCAG 2.1 mức A + AA |
| 7 | Dữ liệu minh hoạ đủ để trình diễn | 40 đơn trải đủ 8 trạng thái |
| 8 | Tài liệu kỹ thuật | 12 tài liệu trong `docs/` |

Kết quả có ý nghĩa nhất không phải số lượng chức năng mà là **lời giải cho bài
toán chống đặt trùng phòng**: đặt ràng buộc ở tầng cơ sở dữ liệu thay vì kiểm
tra ở tầng ứng dụng, và chứng minh nó đúng bằng kiểm thử đa luồng trên
PostgreSQL thật.

## 6. Phương pháp nghiên cứu

| Phương pháp | Áp dụng cụ thể trong đồ án |
|---|---|
| **Nghiên cứu tài liệu** | Đọc tài liệu chính thức của PostgreSQL về kiểu `daterange` và ràng buộc `EXCLUDE`; tài liệu Spring Framework về quy tắc rollback giao dịch; tài liệu SePay về webhook. |
| **Khảo sát và phân tích đối sánh** | So sánh giao diện và luồng đặt phòng của Booking.com và Agoda để rút ra mẫu thiết kế nên lấy và mẫu **không** nên lấy (xem mục 1.3). |
| **Phân tích thiết kế hướng đối tượng** | Mô hình hoá bằng UML: biểu đồ use case, biểu đồ tuần tự, biểu đồ trạng thái, biểu đồ lớp. |
| **Thực nghiệm** | Dựng hệ thống thật, chạy 129 kịch bản kiểm thử tự động trên PostgreSQL thật (không giả lập), đo kết quả bằng lệnh chứ không bằng cảm nhận. |
| **Kiểm thử đối kháng** | Chủ động tạo tình huống xấu: nhiều luồng cùng đặt phòng cuối, tiền về sau khi đơn đã hết hạn, webhook gửi lại hai lần, giả mạo header IP để vượt giới hạn tần suất. |

## 7. Ý nghĩa khoa học và thực tiễn

**Ý nghĩa thực tiễn:** hệ thống có thể đưa vào dùng thật cho một homestay quy mô
nhỏ sau khi thay dữ liệu mẫu bằng dữ liệu thật và bổ sung HTTPS. Chi phí vận
hành gần như bằng không nếu tự đặt trên một máy chủ ảo phổ thông.

**Ý nghĩa khoa học:** đồ án chứng minh bằng thực nghiệm rằng bài toán chống đặt
trùng phòng **không giải được đúng** bằng mẫu kiểm tra-rồi-ghi (`check-then-act`)
ở tầng ứng dụng, và trình bày một lời giải đặt ràng buộc ở tầng lưu trữ. Kết quả
này áp dụng lại được cho mọi bài toán "đặt chỗ theo khoảng": đặt sân bóng, đặt
phòng họp, mượn thiết bị, lịch khám bệnh.

## 8. Bố cục của đồ án

Đồ án gồm phần mở đầu, năm chương nội dung, phần kết luận và phụ lục:

- **Chương 1 — Tổng quan về đề tài:** khảo sát hiện trạng, phân tích các hệ
  thống tương tự, và xác định yêu cầu chức năng, phi chức năng.
- **Chương 2 — Cơ sở lý thuyết và công nghệ sử dụng:** trình bày nền tảng lý
  thuyết và lý do chọn từng công nghệ.
- **Chương 3 — Phân tích và thiết kế hệ thống:** mô hình hoá bằng UML, thiết kế
  cơ sở dữ liệu, thiết kế API và thiết kế giao diện.
- **Chương 4 — Xây dựng và triển khai hệ thống:** cài đặt các chức năng, mô hình
  bảo mật và quy trình đóng gói.
- **Chương 5 — Kiểm thử và đánh giá:** chiến lược kiểm thử, kết quả, đối chiếu
  với mục tiêu ban đầu và các giới hạn đã biết.
- **Kết luận và hướng phát triển.**

---

# CHƯƠNG 1 — TỔNG QUAN VỀ ĐỀ TÀI

## 1.1. Giới thiệu bài toán

Homestay TVH là một cơ sở lưu trú giả định đặt tại Trà Vinh, quy mô **15 phòng
vật lý** thuộc **4 loại phòng**: Standard, Deluxe, Family và Bungalow. Mỗi loại
phòng có giá cơ bản theo đêm, sức chứa người lớn và trẻ em riêng, bộ tiện nghi
riêng và bộ ảnh riêng.

Nghiệp vụ cần tin học hoá gồm bốn nhóm:

**Nhóm 1 — Giới thiệu.** Trình bày loại phòng, tiện nghi, hình ảnh, tin tức và
đánh giá của khách cũ, sao cho khách chưa từng nghe tên homestay vẫn hiểu được
họ sẽ nhận được gì.

**Nhóm 2 — Đặt phòng.** Khách chọn khoảng ngày và số khách, hệ thống trả về các
loại phòng còn chỗ kèm chi phí dự kiến, khách gửi yêu cầu đặt phòng và đặt cọc.

**Nhóm 3 — Quản lý vận hành.** Chủ homestay theo dõi đơn theo trạng thái, xác
nhận hoặc huỷ, đối soát tiền đã nhận, và quản lý danh mục phòng, giá, khuyến mãi,
nội dung trang chủ.

**Nhóm 4 — Báo cáo.** Doanh thu theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ.

### Bài toán cốt lõi: không được bán trùng một phòng

Đây là ràng buộc nghiệp vụ nghiêm ngặt nhất và là trục kỹ thuật của toàn đồ án.
Phát biểu hình thức:

> Với mọi phòng vật lý *p*, không tồn tại hai đơn đặt phòng còn hiệu lực cùng
> giữ *p* mà khoảng ngày ở của chúng giao nhau.

Điểm tinh tế: "giao nhau" phải hiểu theo **khoảng nửa mở**. Khách A trả phòng
ngày 10/03 và khách B nhận phòng ngày 10/03 là **hợp lệ** — buổi sáng A trả,
buổi chiều B nhận. Nếu cài bằng phép so sánh ngày thông thường, chỉ cần lệch một
dấu bằng là hoặc mất một đêm doanh thu mỗi phòng, hoặc bán trùng.

> 🖼️ **[HÌNH 1.1] — Minh hoạ khoảng nửa mở**
> *Cần gì:* sơ đồ trục thời gian nằm ngang, hai thanh biểu diễn hai kỳ ở:
> `[08/03 → 10/03)` và `[10/03 → 12/03)`, chỉ rõ chúng chạm nhau tại 10/03
> nhưng **không chồng lấn**. Bên dưới vẽ thêm một cặp chồng lấn thật
> (`[08/03 → 11/03)` và `[10/03 → 12/03)`) tô đỏ phần giao.
> *Cách làm:* vẽ bằng draw.io hoặc PowerPoint, xuất PNG.

## 1.2. Khảo sát hiện trạng

### 1.2.1. Quy trình thủ công hiện tại

> 🖼️ **[HÌNH 1.2] — Sơ đồ quy trình đặt phòng thủ công hiện tại**
> *Cần gì:* lưu đồ (flowchart) thể hiện: Khách nhắn Facebook/Zalo → Chủ nhà mở
> sổ/nhớ lại → Trả lời còn/hết → Khách chuyển khoản → Chủ nhà ghi sổ. Đánh dấu
> **bằng màu đỏ** ba điểm rủi ro: "tra cứu bằng trí nhớ", "không có bản ghi tập
> trung", "hai khách nhắn cùng lúc".
> *Cách làm:* draw.io.

Quy trình hiện tại và nhược điểm tương ứng:

| Bước | Cách làm hiện tại | Nhược điểm |
|---|---|---|
| Tìm hiểu thông tin | Xem ảnh trên trang Facebook | Ảnh lẫn lộn, không có giá rõ ràng, không biết còn phòng hay không |
| Hỏi phòng trống | Nhắn tin, chờ chủ nhà trả lời | Phụ thuộc giờ giấc chủ nhà; khách nhắn lúc 23h phải chờ tới sáng |
| Kiểm tra lịch | Chủ nhà tra sổ hoặc nhớ | **Nguồn gốc của đặt trùng phòng** |
| Báo giá | Chủ nhà tự nhẩm | Dễ nhầm khi khách ở nhiều đêm hoặc nhiều phòng |
| Đặt cọc | Chuyển khoản, gửi ảnh biên lai | Chủ nhà phải tự đối chiếu sao kê bằng mắt |
| Xác nhận | Nhắn tin xác nhận | Không có bằng chứng nào ngoài đoạn chat |
| Quản lý | Sổ giấy hoặc file Excel | Không thống kê được; mất sổ là mất hết |

### 1.2.2. Khảo sát người dùng

Đồ án xác định **ba nhóm người dùng** với nhu cầu khác hẳn nhau:

**Khách vãng lai (chiếm đa số).** Họ muốn xem phòng, biết giá, và đặt được
phòng. Họ **không** muốn tạo tài khoản. Bắt đăng ký trước khi đặt là chỗ mất
khách nhiều nhất của một trang đặt phòng — đây là một quyết định thiết kế quan
trọng của đồ án: tài khoản là **tuỳ chọn**, không phải điều kiện.

**Khách có tài khoản.** Làm được mọi thứ khách vãng lai làm, cộng thêm việc xem
lại mọi đơn của mình ở một chỗ.

**Quản trị viên (chủ homestay và nhân viên).** Cần nhìn thấy toàn bộ đơn, xử lý
tiền, và cập nhật thông tin phòng. Họ dùng hệ thống hằng ngày nên màn hình quản
trị ưu tiên mật độ thông tin cao hơn là đẹp.

## 1.3. Khảo sát các hệ thống tương tự

Đồ án khảo sát hai nền tảng đặt phòng lớn nhất tại thị trường Việt Nam là
**Booking.com** và **Agoda**, cùng mô hình **Airbnb** cho phân khúc lưu trú cá
nhân.

> 🖼️ **[HÌNH 1.3] — Ảnh chụp màn hình trang kết quả tìm kiếm của Booking.com**
> *Cần gì:* ảnh chụp thật trang kết quả, **khoanh vùng và đánh số** các thành
> phần: (1) thanh tìm kiếm dính ở đầu trang, (2) thẻ phòng, (3) tổng tiền cả kỳ,
> (4) nhãn khan hiếm kiểu "Chỉ còn 1 phòng".
> *Lưu ý:* ghi rõ nguồn và ngày chụp dưới hình.

> 🖼️ **[HÌNH 1.4] — Ảnh chụp màn hình lịch chọn ngày của Agoda**
> *Cần gì:* ảnh chụp lịch, khoanh vùng chỗ hiển thị giá từng đêm trên ô ngày và
> chỗ ngày hết phòng bị chặn sẵn.

### 1.3.1. Bảng so sánh

| Tiêu chí | Booking.com | Agoda | Airbnb | **Homestay TVH** |
|---|---|---|---|---|
| Đối tượng | Khách sạn, mọi phân khúc | Khách sạn, mạnh ở châu Á | Lưu trú cá nhân | **Một homestay duy nhất** |
| Hoa hồng | 15–18% | 15–20% | ~3% chủ + ~14% khách | **0%** |
| Đặt không cần tài khoản | Có | Có | Không | **Có** |
| Thanh toán | Thẻ / tại nơi ở | Thẻ | Thẻ | **Chuyển khoản QR VietQR** |
| Hiện tổng tiền cả kỳ | Có | Có | Có | **Có, ở mọi bước** |
| Chặn sẵn ngày hết phòng | Có | Có | Có | **Có** |
| Nhãn khan hiếm | Có, rất mạnh | Có | Vừa phải | **Có, nhưng chỉ dùng số thật** |
| "X người đang xem" | Có | Có | Không | **Cố ý KHÔNG có** |

### 1.3.2. Rút ra: lấy gì và cố ý không lấy gì

Đây là mục thể hiện năng lực phân tích, không chỉ mô tả. Đồ án lấy các mẫu thiết
kế đã được kiểm chứng, nhưng **từ chối** nhóm mẫu gây áp lực giả (dark pattern):

| Lấy từ hai nền tảng | Cố ý làm khác |
|---|---|
| Khoảng trắng rộng, tương phản cao | **"Còn 2 phòng"** — con số lấy trực tiếp từ cơ sở dữ liệu, có ràng buộc `EXCLUDE` bảo chứng là đúng |
| **Tổng tiền cả kỳ** hiển thị ở mọi bước, không chỉ giá mỗi đêm | Đồng hồ đếm ngược **chỉ** dùng cho hạn giữ chỗ **có thật** trong cơ sở dữ liệu, không phải để thúc ép |
| Thanh điều hướng dính ở trang chi tiết | **Không có "X người đang xem"** — hệ thống không đo được số đó, nên không nói |
| Chặn sẵn ngày hết phòng trong lịch | Không có "vừa có người đặt xong" nếu không có người đặt thật |
| Khối đánh giá đặt ngay dưới phần chọn phòng | |

**Luận điểm cần nêu khi bảo vệ:** các nhãn khan hiếm trên nền tảng lớn thường
không tương ứng với dữ liệu thật. Đồ án giữ lại **hình thức** của chúng (vì
chúng thật sự giúp khách quyết định nhanh) nhưng buộc mọi con số phải có nguồn
gốc kiểm chứng được. Đây là lựa chọn đạo đức thiết kế, và nó khả thi chính vì
ràng buộc ở tầng cơ sở dữ liệu bảo đảm con số "còn N phòng" luôn đúng.

## 1.4. Xác định yêu cầu

### 1.4.1. Yêu cầu chức năng

**Nhóm A — Dành cho khách:**

| Mã | Yêu cầu |
|---|---|
| CN-01 | Xem danh sách loại phòng kèm tiện nghi, hình ảnh, giá |
| CN-02 | Xem chi tiết một loại phòng, xem thư viện ảnh |
| CN-03 | Tìm phòng trống theo khoảng ngày, số người lớn, trẻ em, số phòng |
| CN-04 | Xem lịch từng đêm: đêm nào còn phòng, đêm nào hết |
| CN-05 | Gửi yêu cầu đặt phòng, nhận mã đơn |
| CN-06 | Xem chi phí dự kiến trước khi xác nhận đơn |
| CN-07 | Áp dụng mã khuyến mãi và thấy ngay số tiền được giảm |
| CN-08 | Thanh toán đặt cọc bằng mã QR VietQR |
| CN-09 | Tra cứu đơn bằng mã đơn + số điện thoại |
| CN-10 | Huỷ đơn |
| CN-11 | Nhận thư xác nhận qua email |
| CN-12 | Viết đánh giá sau khi trả phòng |
| CN-13 | Đăng ký, đăng nhập, xem lại mọi đơn của mình |
| CN-14 | Đọc tin tức, xem thư viện ảnh của homestay |

**Nhóm B — Dành cho quản trị viên:**

| Mã | Yêu cầu |
|---|---|
| CN-15 | Đăng nhập khu quản trị; buộc đổi mật khẩu tạm ở lần đầu |
| CN-16 | Xem danh sách đơn, lọc theo trạng thái và khoảng ngày |
| CN-17 | Xem chi tiết đơn: lịch sử trạng thái, thanh toán, thư đã gửi, ghi chú |
| CN-18 | Chuyển trạng thái đơn theo máy trạng thái hợp lệ |
| CN-19 | Ghi chú nội bộ trên đơn |
| CN-20 | Đối soát thanh toán: xác nhận thủ công, xử lý khoản lệch |
| CN-21 | Quản lý loại phòng: thêm, sửa, xoá, gán tiện nghi, tải ảnh |
| CN-22 | Quản lý phòng vật lý: thêm, sửa, đổi trạng thái vận hành |
| CN-23 | **Đóng phòng theo khoảng ngày** (quản lý ngày khả dụng) |
| CN-24 | Quản lý mã khuyến mãi |
| CN-25 | Duyệt, từ chối, trả lời đánh giá của khách |
| CN-26 | Quản lý nội dung trang chủ, banner, thư viện ảnh, tin tức |
| CN-27 | Xem dashboard: doanh thu, tỉ lệ lấp đầy, tỉ lệ huỷ |
| CN-28 | Xuất báo cáo đơn ra tệp CSV |

**Nhóm C — Hệ thống tự động:**

| Mã | Yêu cầu |
|---|---|
| CN-29 | Nhận webhook từ SePay khi có tiền về, tự đối chiếu và xác nhận đơn |
| CN-30 | Tự chuyển đơn quá hạn giữ chỗ sang `EXPIRED` và nhả phòng |
| CN-31 | Gửi thư qua hàng đợi, ghi nhận mọi thư đã gửi |
| CN-32 | Xử lý tiền về muộn: mở lại đơn, thử giành lại phòng |

### 1.4.2. Yêu cầu phi chức năng

| Mã | Loại | Yêu cầu | Cách kiểm chứng |
|---|---|---|---|
| PCN-01 | **Toàn vẹn** | Không bao giờ bán trùng một phòng, kể cả khi có tranh chấp | Test đa luồng thật |
| PCN-02 | **Toàn vẹn** | Số phòng đã gán luôn khớp số phòng đơn yêu cầu | Trigger hoãn ở CSDL |
| PCN-03 | **Toàn vẹn** | Không nhánh nào để tiền biến mất im lặng | Test cửa sổ tranh chấp webhook |
| PCN-04 | **Bảo mật** | Mặc định từ chối mọi endpoint chưa khai quyền | Test ma trận 30 tiền tố |
| PCN-05 | **Bảo mật** | Chống dò mật khẩu và spam đặt phòng | Giới hạn tần suất 9 khoá |
| PCN-06 | **Bảo mật** | Không có bí mật nào nằm trong mã nguồn | Ứng dụng dừng khởi động khi thiếu biến |
| PCN-07 | **Khả dụng** | Giao diện dùng được trên điện thoại | Kiểm ở 3 độ rộng màn hình |
| PCN-08 | **Khả dụng** | Đạt WCAG 2.1 mức A và AA | Quét tự động bằng axe |
| PCN-09 | **Khả dụng** | Mọi vùng chạm ≥ 44×44 px trên di động | Đo tự động |
| PCN-10 | **Hiệu năng** | Lịch cả kỳ lấy trong **một** truy vấn, không lặp từng đêm | Đọc mã truy vấn |
| PCN-11 | **Triển khai** | Ba lệnh từ lúc clone tới lúc chạy | Thực nghiệm |
| PCN-12 | **Bảo trì** | Lược đồ CSDL do công cụ migration quản lý, migration bất biến | Test kiểm tra checksum |

## 1.5. Phạm vi và giới hạn của hệ thống

**Trong phạm vi:** toàn bộ 32 yêu cầu chức năng nêu trên, đã cài đặt và kiểm thử.

**Ngoài phạm vi (và lý do):**

| Không có | Lý do |
|---|---|
| Cổng thanh toán thật | Đề bài không yêu cầu; hệ thống viết đúng luồng thật, chỉ thiếu tài khoản thật |
| Hoàn tiền tự động | Hoàn tiền là quyết định kinh doanh; hệ thống đưa vào hàng đợi cho người xử lý |
| Ứng dụng di động | Website đã đáp ứng; làm app là một đồ án riêng |
| Nhiều chi nhánh | Bài toán khác hẳn về mô hình dữ liệu |
| HTTPS trong bản đóng gói | Cần tên miền và chứng chỉ thật; bản demo chạy HTTP trần — **đây là giới hạn đã biết, ghi rõ ở Chương 5** |

---

# CHƯƠNG 2 — CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ SỬ DỤNG

> **Lưu ý khi viết chương này:** đừng biến nó thành bản dịch tài liệu chính thức.
> Với mỗi công nghệ, viết theo ba phần: (a) nó là gì, ngắn gọn; (b) **vì sao đồ
> án chọn nó thay vì phương án khác**; (c) nó được dùng ở chỗ nào trong hệ
> thống. Phần (b) là phần hội đồng hỏi.

## 2.1. Kiến trúc ứng dụng web tách rời

### 2.1.1. Mô hình client — server và REST API

Hệ thống theo kiến trúc **tách rời (decoupled)**: frontend và backend là hai ứng
dụng độc lập, giao tiếp qua REST API trả về JSON. Frontend không truy cập cơ sở
dữ liệu; backend không sinh HTML.

So sánh với kiến trúc **nguyên khối kết xuất phía máy chủ** (ví dụ Spring MVC +
Thymeleaf, hoặc PHP thuần):

| Tiêu chí | Kết xuất phía máy chủ | **Tách rời (đồ án chọn)** |
|---|---|---|
| Độ phức tạp ban đầu | Thấp hơn | Cao hơn |
| Trải nghiệm người dùng | Tải lại cả trang | Chuyển trang không tải lại |
| Tái dùng cho ứng dụng di động | Phải viết lại API | **Dùng lại nguyên API** |
| Phân chia công việc | Khó tách | Tách rõ frontend/backend |
| Tối ưu SEO | Dễ hơn | Cần thêm công sức |

Lý do đồ án chọn tách rời: hệ thống có **hai giao diện rất khác nhau** (trang
bán hàng cho khách và khu quản trị mật độ cao). Dùng chung một API cho cả hai,
cộng với khả năng mở rộng sang ứng dụng di động sau này, bù lại chi phí phức tạp
ban đầu.

> 🖼️ **[HÌNH 2.1] — Kiến trúc tổng thể hệ thống**
> *Cần gì:* sơ đồ khối thể hiện: Trình duyệt → nginx (web) → Spring Boot (api) →
> PostgreSQL (db), cộng Mailpit và SePay ở ngoài.
> *Lấy ở đâu:* render sơ đồ Mermaid đầu tiên trong `docs/kien-truc.md`.

### 2.1.2. Nguyên tắc REST áp dụng trong đồ án

- **Tài nguyên là danh từ, thao tác là động từ HTTP:** `GET /api/room-types`,
  `POST /api/bookings`, `PATCH /api/admin/rooms/{id}`.
- **Tài nguyên con lồng vào tài nguyên cha:**
  `GET /api/admin/rooms/{id}/closures` — khoảng đóng phòng không tồn tại độc lập
  với phòng.
- **Mã trạng thái HTTP mang ngữ nghĩa:** `201` khi tạo mới, `204` khi xoá thành
  công, `409` khi xung đột trạng thái, `429` khi vượt giới hạn tần suất.
- **Lỗi theo chuẩn RFC 7807 (Problem Details):** mọi phản hồi lỗi có cấu trúc
  thống nhất kèm trường `code` do hệ thống tự thêm. **Giao diện đọc `code`,
  không đọc câu chữ mô tả** — đổi câu chữ ở backend không được phép làm hỏng
  màn hình.

## 2.2. Ngôn ngữ và nền tảng backend

### 2.2.1. Java 21

Java 21 là phiên bản hỗ trợ dài hạn (LTS). Các tính năng ngôn ngữ mà đồ án dùng
thật sự, không phải liệt kê cho đủ:

- **`record`** — dùng cho toàn bộ DTO. Một `record` bất biến, tự có
  `equals`/`hashCode`/`toString`, và không thể vô tình bị sửa giữa các tầng.
- **Lớp niêm phong (`sealed class`)** — dùng cho cây ngoại lệ nghiệp vụ. Trình
  biên dịch biết đủ danh sách lớp con, nên thêm một loại lỗi mới mà quên xử lý
  sẽ bị báo ngay.
- **Khối văn bản (text block)** — dùng để viết các câu SQL nhiều dòng còn đọc
  được.

### 2.2.2. Spring Boot 3.5.6

Các mô-đun sử dụng và vai trò:

| Mô-đun | Dùng làm gì trong đồ án |
|---|---|
| Spring Web MVC | Tầng controller, ánh xạ REST |
| Spring Data JPA | Truy cập dữ liệu qua Hibernate cho phần CRUD |
| Spring JDBC (`JdbcClient`) | **Các truy vấn phòng trống viết SQL thuần** (lý do ở 2.4.3) |
| Spring Security | Xác thực JWT, phân quyền theo vai trò |
| Spring Validation | Kiểm tra dữ liệu đầu vào bằng chú thích |
| Spring Mail | Gửi thư xác nhận |
| Spring Scheduling | Bộ quét đơn quá hạn, bộ gửi thư |

**Khái niệm giao dịch (`@Transactional`) — cần nắm chắc để bảo vệ.** Một giao
dịch có bốn tính chất ACID. Điểm mà đồ án buộc phải hiểu sâu là hành vi **huỷ
giao dịch (rollback)**: khi PostgreSQL bác một câu lệnh vì vi phạm ràng buộc,
**toàn bộ giao dịch bị đánh dấu hỏng**. Mọi câu lệnh tiếp theo trong cùng giao
dịch đó trả về mã lỗi `25P02` chứ không chạy. Hệ quả trực tiếp lên thiết kế:
vòng thử gán phòng tiếp theo **bắt buộc** phải chạy trong một giao dịch MỚI.

### 2.2.3. Hibernate và `ddl-auto: validate`

Đồ án đặt `spring.jpa.hibernate.ddl-auto = validate` và **không bao giờ** dùng
`update`. Lý do:

- `update` để Hibernate tự sửa lược đồ. Nó âm thầm thêm cột, không bao giờ xoá
  cột thừa, và không ghi lại việc mình đã làm. Sau vài tháng, lược đồ trên máy
  phát triển và trên máy chạy thật khác nhau mà không ai biết khác chỗ nào.
- `validate` chỉ **kiểm tra**: mọi cột mà entity khai phải tồn tại đúng kiểu
  trong cơ sở dữ liệu. Ứng dụng khởi động được nghĩa là 20 entity khớp hoàn toàn
  với lược đồ. Lược đồ do Flyway làm chủ.

## 2.3. Ngôn ngữ và nền tảng frontend

### 2.3.1. Angular 21

Đồ án dùng Angular 21 với ba đặc điểm hiện đại:

- **Component độc lập (standalone)** — không còn `NgModule`. Mỗi component tự
  khai báo những gì nó dùng.
- **Signals** — cơ chế phản ứng mới của Angular. Một `signal` là một giá trị có
  thể theo dõi; `computed` là giá trị dẫn xuất tự cập nhật. Đồ án **không dùng
  thư viện quản lý trạng thái ngoài** (NgRx, Akita): signals đủ cho quy mô này,
  và thêm một thư viện là thêm một thứ phải giải thích khi bảo vệ.
- **Nạp lười theo tuyến (lazy loading)** — khu quản trị gồm mười một màn hình
  được tải riêng. Người vào trang đặt phòng không tải mã của những màn hình họ
  không bao giờ mở.

### 2.3.2. Tailwind CSS 4 và design system tự viết

Đồ án **không dùng** Angular Material hay Bootstrap, mà tự xây thư viện
component trên nền Tailwind CSS 4.

*Lý do:* thư viện dựng sẵn mang theo ngôn ngữ thị giác của nó. Dùng Material thì
trang web trông như một ứng dụng Google, không giống một trang đặt phòng. Tự
viết cho phép kiểm soát hoàn toàn bảng màu, khoảng cách và trạng thái — và quan
trọng hơn, nó biến "thiết kế giao diện" thành một phần có thể trình bày được của
đồ án thay vì một tuỳ chọn cấu hình.

*Khái niệm `@layer` của CSS cần hiểu:* Tailwind 4 đặt các lớp tiện ích trong
`@layer utilities`. Một quy tắc CSS **không nằm trong layer nào** luôn thắng mọi
quy tắc nằm trong layer, bất kể độ ưu tiên (specificity). Đây là nguồn gốc của
một lỗi thật gặp phải trong quá trình làm — trình bày ở mục 5.3.

> 🖼️ **[HÌNH 2.2] — Bảng token màu của hệ thống**
> *Cần gì:* ảnh chụp trang `/ui-kit` phần bảng màu, hoặc tự vẽ bảng các ô màu
> kèm tên token và mã hex.
> *Lấy ở đâu:* chạy hệ thống ở profile `demo`, mở `http://localhost/ui-kit`.

## 2.4. Hệ quản trị cơ sở dữ liệu PostgreSQL 16

> **Đây là mục quan trọng nhất của Chương 2.** Toàn bộ lời giải cho bài toán cốt
> lõi nằm ở đây.

### 2.4.1. Vì sao PostgreSQL mà không phải MySQL

Đây là câu hỏi gần như chắc chắn sẽ bị hỏi. Câu trả lời **không** phải "vì
PostgreSQL mạnh hơn", mà là một tính năng cụ thể mà MySQL không có.

### 2.4.2. Kiểu dữ liệu khoảng (range type)

PostgreSQL có kiểu `daterange` biểu diễn **một khoảng ngày** như một giá trị duy
nhất, kèm toán tử `&&` kiểm tra hai khoảng có giao nhau hay không:

```sql
SELECT daterange('2026-03-08','2026-03-10','[)')
    && daterange('2026-03-10','2026-03-12','[)');   -- false, không giao
```

Ký hiệu `'[)'` là **khoảng nửa mở**: bao gồm cận dưới, **không** bao gồm cận
trên. Đúng bằng ngữ nghĩa nghiệp vụ khách sạn — và đây là lý do quy ước nửa mở
được chọn, không phải sở thích.

### 2.4.3. Ràng buộc loại trừ `EXCLUDE USING gist`

```sql
ALTER TABLE booking_rooms
    ADD CONSTRAINT booking_rooms_no_overlap
    EXCLUDE USING gist (room_id WITH =, stay WITH &&)
    WHERE (status = 'ACTIVE');
```

Đọc là: *không được phép tồn tại hai dòng có cùng `room_id` **và** có `stay`
giao nhau, trong số các dòng đang ở trạng thái `ACTIVE`.*

**Vì sao ràng buộc này là lời giải đúng — ba lý do:**

**(1) Nó đóng khe hở tranh chấp.** Cách làm quen thuộc là:

```
SELECT ... WHERE NOT EXISTS (đơn nào giao ngày không?)   -- kiểm tra
INSERT INTO booking_rooms ...                            -- rồi ghi
```

Giữa hai câu lệnh đó có một khoảng thời gian. Hai giao dịch song song cùng đọc
"còn phòng" rồi cùng ghi — và phòng bị bán hai lần. Mẫu này gọi là
**`check-then-act`** và nó **sai về nguyên lý**, không phải sai vì viết ẩu.

Phương án khoá bi quan (`SELECT ... FOR UPDATE`) bịt được khe hở nhưng biến mọi
lượt đặt cùng một loại phòng thành hàng đợi một luồng, và không còn đúng khi ứng
dụng chạy nhiều bản song song.

Với `EXCLUDE`, PostgreSQL bác một trong hai giao dịch **ngay trong động cơ lưu
trữ**, bằng mã lỗi `SQLSTATE 23P01`. Không có khe hở nào để chen vào.

**(2) Nó đúng ngữ nghĩa nửa mở mà không cần một dòng mã ứng dụng nào.**

**(3) Nó cho phép huỷ đơn nhả phòng ngay mà vẫn giữ lịch sử.** Mệnh đề
`WHERE (status = 'ACTIVE')` làm ràng buộc chỉ soi những dòng đang giữ chỗ. Huỷ
đơn chỉ đổi trạng thái sang `RELEASED`: phòng mở lại lập tức, dòng dữ liệu vẫn
còn để tra cứu.

> 🖼️ **[HÌNH 2.3] — Minh hoạ tranh chấp đặt phòng**
> *Cần gì:* biểu đồ tuần tự hai luồng song song. Bên trái: mẫu `check-then-act`
> — cả hai cùng đọc "còn phòng", cả hai cùng ghi, kết quả **bán trùng** (tô đỏ).
> Bên phải: với `EXCLUDE` — luồng 1 ghi thành công, luồng 2 nhận `23P01` và bị
> bác (tô xanh).
> *Cách làm:* vẽ bằng draw.io hoặc Mermaid `sequenceDiagram`.

### 2.4.4. Chỉ mục GiST và phần mở rộng `btree_gist`

`EXCLUDE` cần một chỉ mục hỗ trợ toán tử `&&`. Chỉ mục B-tree thông thường chỉ
so sánh bằng và lớn/nhỏ, không so sánh được "giao nhau". **GiST** (Generalized
Search Tree) làm được.

Nhưng ràng buộc này trộn hai toán tử: `room_id WITH =` (B-tree) và
`stay WITH &&` (GiST). Phần mở rộng **`btree_gist`** cho phép đưa cả hai vào một
chỉ mục GiST duy nhất. Đây là lý do migration đầu tiên của hệ thống bắt đầu bằng
`CREATE EXTENSION IF NOT EXISTS btree_gist`.

### 2.4.5. Cột sinh tự động (generated column)

```sql
stay daterange GENERATED ALWAYS AS (daterange(check_in, check_out, '[)')) STORED
```

Cột `stay` **không bao giờ được ghi tay**; PostgreSQL tự tính từ hai cột ngày.
Nếu để tầng ứng dụng tự tính rồi ghi vào, sẽ có lúc nó tính sai — và lúc đó ràng
buộc đang canh một giá trị sai, tức là không canh gì cả.

### 2.4.6. Trigger ràng buộc hoãn (deferrable constraint trigger)

Một bất biến thứ hai: **số phòng đã gán phải khớp số phòng đơn yêu cầu**. Không
kiểm được bằng `CHECK` vì nó liên quan hai bảng.

Nếu kiểm ngay sau mỗi câu `INSERT`, một đơn hai phòng sẽ bị bác ngay sau dòng
đầu tiên (1 ≠ 2) dù cuối cùng nó đúng. Giải pháp là
`CONSTRAINT TRIGGER ... DEFERRABLE INITIALLY DEFERRED`: PostgreSQL hoãn việc
kiểm tới **thời điểm commit**, khi mọi dòng đã ghi xong.

## 2.5. Xác thực và phân quyền bằng JWT

**JWT (JSON Web Token)** là một chuỗi ký tự gồm ba phần — header, payload,
signature — nối bằng dấu chấm, đã ký bằng khoá bí mật của máy chủ. Máy chủ không
cần lưu phiên; nó chỉ cần xác minh chữ ký.

Đồ án dùng **hai loại token, hai vòng đời**:

| | Access token | Refresh token |
|---|---|---|
| Vòng đời | Ngắn (phút) | Dài (ngày) |
| Lưu ở đâu | Bộ nhớ của trang | Cookie `HttpOnly` |
| Mục đích | Gửi kèm mỗi request | Xin access token mới |
| JavaScript đọc được? | Có | **Không** |

*Vì sao tách đôi:* access token nằm trong bộ nhớ nên mã độc chèn vào trang (XSS)
lấy được, nhưng nó hết hạn sau vài phút. Refresh token sống lâu nhưng nằm trong
cookie `HttpOnly` mà JavaScript **không** đọc được.

**Thu hồi tức thì bằng `token_version`.** Nhược điểm cố hữu của JWT là không thu
hồi được trước hạn. Đồ án khắc phục bằng một số phiên bản lưu trong bảng người
dùng và nhúng vào token; đổi mật khẩu hoặc đăng xuất thì tăng số đó lên, mọi
token cũ chết ngay.

## 2.6. Quản lý phiên bản lược đồ bằng Flyway

Flyway thực thi các tệp SQL đánh số `V1__`, `V2__`, … theo thứ tự, và ghi lại
tệp nào đã chạy kèm **giá trị băm (checksum)** của nó.

**Nguyên tắc bất biến:** migration đã phát hành **không bao giờ được sửa**. Sửa
làm checksum đổi, Flyway chặn khởi động. Muốn đổi lược đồ thì **thêm** một
migration mới. Hệ thống hiện có tám migration `V1` → `V8`.

**Dữ liệu mẫu cố ý KHÔNG đi qua Flyway.** Nếu dữ liệu demo là một migration, thì
chuyển sang môi trường thật trên cùng khối dữ liệu sẽ mang theo cả 40 đơn giả.
Dữ liệu mẫu được nạp bởi một thành phần riêng chỉ kích hoạt ở profile `demo`.

## 2.7. Đóng gói bằng Docker và Docker Compose

- **Docker image** — bản đóng gói gồm ứng dụng và mọi thứ nó cần để chạy.
- **Dockerfile nhiều tầng (multi-stage)** — tầng đầu chứa công cụ biên dịch
  (Maven, Node.js), tầng sau **chỉ** chứa kết quả biên dịch. Ảnh cuối không mang
  theo trình biên dịch, nhỏ hơn nhiều và ít bề mặt tấn công hơn.
- **Docker Compose** — mô tả nhiều dịch vụ trong một tệp YAML.

Hệ thống gồm bốn dịch vụ: `web` (nginx), `api` (Spring Boot), `db` (PostgreSQL),
`mailpit` (máy chủ thư giả). **Chỉ `web` và giao diện `mailpit` mở cổng ra máy
chủ**; `api` và `db` không publish cổng nào — đây là điều kiện để tin được header
`X-Forwarded-For` (giải thích ở mục 4.4.3).

## 2.8. Thanh toán qua VietQR và SePay

**VietQR** là chuẩn mã QR thanh toán của hệ thống ngân hàng Việt Nam. Quét mã là
điền sẵn số tài khoản, số tiền và nội dung chuyển khoản.

**SePay** là dịch vụ trung gian đọc biến động số dư của tài khoản ngân hàng và
**gọi webhook** tới hệ thống khi có tiền về.

**Webhook** là cơ chế ngược với việc hỏi liên tục: thay vì hệ thống hỏi "có tiền
chưa?" mỗi vài giây, SePay chủ động gọi vào một endpoint do hệ thống cung cấp.

Ba vấn đề kỹ thuật phải giải và cách giải:

| Vấn đề | Cách giải trong đồ án |
|---|---|
| Ai cũng gọi được endpoint webhook | Xác thực bằng khoá API trong header `Apikey` |
| SePay có thể gửi lại cùng một sự kiện | Ràng buộc `UNIQUE (provider, external_id)` — gửi lại không cộng tiền hai lần |
| Tiền về sau khi đơn đã hết hạn | Mở lại đơn sang `AWAITING_REVIEW`, thử giành lại phòng; không được thì vào hàng đợi hoàn tiền |

## 2.9. Kiểm thử với Testcontainers

**Testcontainers** là thư viện khởi động một container Docker thật trong lúc
chạy test.

*Vì sao đồ án bắt buộc dùng nó:* phần lớn thứ đáng kiểm ở hệ thống này **chính
là hành vi của cơ sở dữ liệu** — ràng buộc `EXCLUDE`, kiểu `daterange`, trigger
hoãn, mã `SQLSTATE`. Cơ sở dữ liệu trong bộ nhớ như H2 **không có** những tính
năng đó. Test chạy trên H2 sẽ xanh mà không chứng minh được điều duy nhất đáng
chứng minh.

## 2.10. Bảng tổng hợp công nghệ

| Tầng | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ backend | Java | 21 (LTS) |
| Nền tảng backend | Spring Boot | 3.5.6 |
| Truy cập dữ liệu | Spring Data JPA / Hibernate, Spring JDBC | theo Boot |
| Bảo mật | Spring Security + JWT | theo Boot |
| Cơ sở dữ liệu | PostgreSQL | 16 |
| Migration | Flyway | theo Boot |
| Giới hạn tần suất | bucket4j | — |
| Lọc HTML | OWASP java-html-sanitizer | 20240325.1 |
| Kiểm thử | JUnit 5, AssertJ, Testcontainers | — |
| Ngôn ngữ frontend | TypeScript | theo Angular |
| Nền tảng frontend | Angular | 21 |
| CSS | Tailwind CSS | 4 |
| Máy chủ web | nginx | alpine |
| Đóng gói | Docker, Docker Compose | Engine 24+, Compose v2 |
| Thư (môi trường demo) | Mailpit | — |
| Thanh toán | SePay + VietQR | — |

---

# CHƯƠNG 3 — PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

## 3.1. Phân tích chức năng — biểu đồ use case

### 3.1.1. Xác định tác nhân

| Tác nhân | Mô tả | Cơ chế xác thực |
|---|---|---|
| **Khách vãng lai** | Không có tài khoản. Đặt, tra cứu và huỷ đơn bằng mã đơn + số điện thoại | Không đăng nhập. Thao tác trên một đơn cụ thể cần **mã đơn + số điện thoại** hoặc **mã truy cập** trong liên kết thư xác nhận |
| **Khách có tài khoản** | Làm mọi thứ khách vãng lai làm, thêm việc xem lại mọi đơn ở một chỗ | JWT, vai trò `CUSTOMER` |
| **Quản trị viên** | Chủ homestay và nhân viên | JWT, vai trò `ADMIN` |
| *(Tác nhân hệ thống)* | SePay gọi webhook; bộ quét định kỳ | Khoá API / nội bộ |

> 🖼️ **[HÌNH 3.1] — Biểu đồ use case tổng quát**
> *Cần gì:* biểu đồ use case UML đầy đủ với 3 tác nhân và 8 use case, có khung
> bao hệ thống.
> *Lấy ở đâu:* render sơ đồ Mermaid trong `docs/use-case.md`, hoặc vẽ lại bằng
> draw.io theo đúng ký pháp UML (hình người que + hình elip) nếu khoa yêu cầu
> ký pháp chuẩn.

### 3.1.2. Danh sách use case

| Mã | Tên use case | Tác nhân chính |
|---|---|---|
| UC1 | Tìm phòng trống | Khách vãng lai, Khách có tài khoản |
| UC2 | Đặt phòng | Khách vãng lai, Khách có tài khoản |
| UC3 | Thanh toán cọc | Khách vãng lai, Khách có tài khoản |
| UC4 | Đối soát thanh toán | Quản trị viên |
| UC5 | Tra cứu và huỷ đơn | Khách vãng lai, Khách có tài khoản |
| UC6 | Quản lý đơn đặt phòng | Quản trị viên |
| UC7 | Xem lại mọi đơn của mình | Khách có tài khoản |
| UC8 | Xem báo cáo doanh thu | Quản trị viên |
| UC9 | **Đặt ngày khả dụng của phòng** | Quản trị viên |
| UC10 | Soạn nội dung trang chủ | Quản trị viên |
| UC11 | Viết đánh giá | Khách đã trả phòng |

### 3.1.3. Đặc tả use case chi tiết

> **Lưu ý:** đặc tả đầy đủ cho cả 6 use case chính nằm ở `docs/use-case.md`.
> Dưới đây trình bày mẫu đặc tả cho hai use case quan trọng nhất; khi viết báo
> cáo, chép đủ cả 6 theo cùng khuôn mẫu.

---

#### UC2 — Đặt phòng

| Mục | Nội dung |
|---|---|
| **Mã** | UC2 |
| **Tác nhân chính** | Khách vãng lai, Khách có tài khoản |
| **Mô tả** | Khách gửi yêu cầu đặt phòng cho một khoảng ngày và nhận mã đơn |
| **Tiền điều kiện** | Đã hoàn tất UC1 và chọn được một loại phòng còn chỗ |
| **Hậu điều kiện thành công** | Đơn ở trạng thái `PENDING_PAYMENT`; các phòng vật lý đã được gán; hạn giữ chỗ 15 phút bắt đầu chạy |

**Luồng sự kiện chính:**

1. Khách nhập họ tên, số điện thoại, email và ghi chú (nếu có).
2. Khách nhập mã khuyến mãi (tuỳ chọn); hệ thống kiểm tra và hiển thị **ngay**
   số tiền được giảm.
3. Khách xác nhận đơn.
4. Hệ thống mở một giao dịch, tính lại chi phí **ở phía máy chủ** (không tin số
   tiền do trình duyệt gửi lên).
5. Hệ thống chọn các phòng vật lý còn rảnh và ghi vào bảng gán phòng.
6. Cơ sở dữ liệu kiểm tra ràng buộc loại trừ. Nếu không vi phạm, giao dịch được
   commit.
7. Hệ thống sinh mã đơn, mã truy cập, thông tin thanh toán và trả về cho khách.
8. Hệ thống đưa thư xác nhận vào hàng đợi gửi.

**Luồng thay thế / ngoại lệ:**

- *4a. Dữ liệu không hợp lệ* (ngày trả ≤ ngày nhận, số khách vượt sức chứa mỗi
  phòng) → trả lỗi `INVALID_BOOKING_REQUEST` (400), không ghi gì.
- *2a. Mã khuyến mãi không hợp lệ* → `INVALID_PROMOTION` (400).
- *2b. Mã khuyến mãi hết lượt* → `PROMOTION_EXHAUSTED` (409).
- **6a. Ràng buộc loại trừ bị vi phạm** (một khách khác vừa đặt xong phòng đó
  trong tích tắc) → cơ sở dữ liệu trả `SQLSTATE 23P01`, giao dịch bị huỷ. Hệ
  thống mở **giao dịch mới** và thử phòng vật lý tiếp theo. Hết phòng để thử thì
  trả `ROOM_NOT_AVAILABLE` (409).
- *5a. Vượt giới hạn tần suất* (quá 10 đơn/phút từ cùng một IP, hoặc quá 10
  đơn/giờ từ cùng một số điện thoại) → `TOO_MANY_REQUESTS` (429).

---

#### UC9 — Đặt ngày khả dụng của phòng

| Mục | Nội dung |
|---|---|
| **Mã** | UC9 |
| **Tác nhân chính** | Quản trị viên |
| **Mô tả** | Đóng một phòng vật lý trong một khoảng ngày cụ thể; hệ thống tự mở lại khi hết khoảng |
| **Tiền điều kiện** | Đã đăng nhập với vai trò `ADMIN`; đã đổi mật khẩu tạm |
| **Hậu điều kiện** | Phòng biến mất khỏi kết quả tìm phòng trong đúng khoảng ngày đó; **không đơn nào bị huỷ** |

**Luồng sự kiện chính:**

1. Quản trị viên mở màn hình quản lý phòng và chọn một phòng vật lý.
2. Hệ thống hiển thị các khoảng đóng đang còn hiệu lực của phòng đó.
3. Quản trị viên chọn ngày bắt đầu đóng và ngày mở bán lại, nhập lý do.
4. Hệ thống ghi khoảng đóng và kiểm tra ràng buộc chống chồng lấn.
5. Hệ thống trả về danh sách các đơn đang giao với khoảng vừa đóng — **chỉ để
   cảnh báo, không huỷ đơn nào**.

**Luồng thay thế:**

- *3a. Ngày mở bán lại ≤ ngày bắt đầu* → `INVALID_ADMIN_REQUEST` (400).
- *4a. Khoảng mới chồng lên một khoảng đã có* → `CLOSURE_OVERLAP` (409).

**Điểm thiết kế đáng nêu:** hệ thống **cố ý không chặn** việc đóng một phòng
đang có đơn. Chặn thì chủ homestay không ghi nhận được sự thật "phòng này hỏng
từ ngày mai" chỉ vì hệ thống còn một đơn cũ — mà sự thật đó vẫn xảy ra dù hệ
thống có cho ghi hay không. Việc của hệ thống là **liệt kê** đúng các đơn bị ảnh
hưởng để người quyết định nhìn thấy, không phải tự quyết thay.

> 🖼️ **[HÌNH 3.2] — Ảnh chụp màn hình quản lý ngày khả dụng**
> *Cần gì:* ảnh chụp khối "Ngày không nhận khách" ở `/admin/rooms`, thể hiện
> danh sách khoảng đóng với dòng chữ dạng "5 đêm: 27/10 – 31/10 (mở lại từ
> 01/11)", và khối cảnh báo đơn bị ảnh hưởng nếu có.

## 3.2. Thiết kế cơ sở dữ liệu

### 3.2.1. Tổng quan

Lược đồ gồm **21 bảng nghiệp vụ**, dựng bởi tám migration Flyway `V1` → `V8`.

> 🖼️ **[HÌNH 3.3] — Sơ đồ quan hệ thực thể (ERD) tổng thể**
> *Cần gì:* sơ đồ ERD đầy đủ 21 bảng với khoá chính, khoá ngoại và quan hệ.
> *Lấy ở đâu:* render đoạn Mermaid `erDiagram` trong `docs/erd.md`, hoặc dùng
> DBeaver / pgAdmin kết nối vào cơ sở dữ liệu đang chạy và xuất sơ đồ.
> *Gợi ý trình bày:* nếu sơ đồ quá rậm khi in A4, tách thành nhiều hình theo
> nhóm (xem Hình 3.4 → 3.8) và giữ Hình 3.3 làm sơ đồ tổng quan.

### 3.2.2. Các nhóm bảng

| Nhóm | Migration | Các bảng | Vai trò |
|---|---|---|---|
| Người dùng và xác thực | V1 | `users`, `refresh_tokens` | Tài khoản, vai trò, phiên đăng nhập |
| Phòng và tiện nghi | V2 | `room_types`, `rooms`, `amenities`, `room_type_amenities`, `room_type_images` | Danh mục sản phẩm |
| Đặt phòng | V3 | `bookings`, `booking_rooms`, `booking_status_history` | **Lõi nghiệp vụ** |
| Thanh toán và thư | V4 | `payments`, `payment_webhook_events`, `outbound_emails` | Tiền và liên lạc |
| Khuyến mãi, đánh giá | V5 | `promotions`, `reviews` | Marketing và uy tín |
| Nội dung (CMS) | V6 | `site_contents`, `banners`, `gallery_images`, `posts` | Trang chủ, tin tức |
| Ghi chú nội bộ | V7 | `booking_notes` | Trao đổi nội bộ trên đơn |
| **Ngày khả dụng** | V8 | `room_closures` | Khoảng ngày phòng không nhận khách |

> 🖼️ **[HÌNH 3.4] — ERD nhóm đặt phòng (chi tiết)**
> *Cần gì:* sơ đồ chi tiết `bookings` — `booking_rooms` — `rooms` — `room_types`,
> ghi rõ tất cả cột, kiểu dữ liệu, và **đánh dấu nổi bật** cột sinh `stay` cùng
> ràng buộc `booking_rooms_no_overlap`. Đây là hình quan trọng nhất của chương.

### 3.2.3. Chi tiết bảng cốt lõi

> **Lưu ý khi viết báo cáo:** khoa thường yêu cầu mô tả **từng bảng** theo mẫu
> bảng ba cột (Tên cột / Kiểu dữ liệu / Mô tả – Ràng buộc). Chép cấu trúc từ các
> tệp migration trong `backend/src/main/resources/db/migration/`. Dưới đây làm
> mẫu cho hai bảng quan trọng nhất; các bảng còn lại làm tương tự.

**Bảng `bookings` — đơn đặt phòng**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `code` | varchar | Mã đơn, UNIQUE. Khách dùng để tra cứu |
| `access_token` | varchar | **Mã truy cập** — bí mật thao tác, khác mã đơn |
| `room_type_id` | bigint | Khoá ngoại tới `room_types` |
| `check_in`, `check_out` | date | `CHECK (check_out > check_in)` |
| `room_quantity` | int | Số phòng đặt |
| `adults`, `children` | int | Số khách |
| `status` | varchar | 8 giá trị, có `CHECK` liệt kê |
| `payment_status` | varchar | Có `CHECK` liệt kê |
| `total_amount`, `deposit_amount` | numeric | `CHECK (>= 0)` |
| `hold_expires_at` | timestamptz | Hạn giữ chỗ |
| `user_id` | bigint | NULL với khách vãng lai |

**Bảng `booking_rooms` — gán phòng vật lý cho đơn**

| Tên cột | Kiểu | Mô tả và ràng buộc |
|---|---|---|
| `id` | bigserial | Khoá chính |
| `booking_id` | bigint | Khoá ngoại, `ON DELETE CASCADE` |
| `room_id` | bigint | Khoá ngoại tới `rooms` |
| `status` | varchar | `ACTIVE` hoặc `RELEASED` |
| `stay` | daterange | **`GENERATED ALWAYS AS (daterange(check_in, check_out, '[)')) STORED`** |

Ràng buộc quyết định của toàn hệ thống nằm trên bảng này:

```sql
EXCLUDE USING gist (room_id WITH =, stay WITH &&) WHERE (status = 'ACTIVE')
```

### 3.2.4. Hai lần dùng `EXCLUDE USING gist`

Điểm đáng nêu khi bảo vệ: cùng một cơ chế được dùng cho **hai bài toán khác
nhau**, chứng tỏ lựa chọn PostgreSQL không phải để dùng một lần.

| Lần | Bảng | Chống điều gì |
|---|---|---|
| 1 | `booking_rooms` | Hai đơn cùng giữ một phòng trong những ngày giao nhau |
| 2 | `room_closures` | Hai khoảng đóng chồng nhau trên cùng một phòng |

Lần thứ hai giải quyết một lỗi rất khó chẩn đoán từ phía người dùng: nếu cho
phép hai khoảng đóng chồng nhau, màn hình quản trị hiện hai dòng nói cùng một
điều, và **xoá một dòng không mở lại được phòng** — người dùng bấm xoá, thấy
dòng biến mất, rồi vẫn không bán được phòng mà không hiểu vì sao.

### 3.2.5. Chuẩn hoá dữ liệu

Lược đồ đạt **dạng chuẩn 3 (3NF)**. Cần nêu hai điểm khi bảo vệ:

**Tách `room_types` và `rooms` là quyết định mô hình hoá quan trọng nhất.** Khách
đặt một **loại phòng** ("Deluxe"), nhưng ràng buộc chống trùng lịch phải đặt lên
**phòng vật lý** ("phòng 201"). Gộp hai khái niệm vào một bảng thì hoặc không
chống trùng được, hoặc buộc khách phải chọn đúng số phòng — điều không nền tảng
đặt phòng nào làm.

**Một chỗ cố ý phi chuẩn hoá:** cột `guest_name_snapshot` trong bảng `reviews`
lưu bản sao tên khách tại thời điểm đánh giá, thay vì tham chiếu. Lý do: tên
hiển thị trên đánh giá công khai phải **đóng băng** tại thời điểm viết; khách
đổi tên tài khoản sau này không được phép làm đổi nội dung đánh giá đã đăng.

## 3.3. Thiết kế máy trạng thái đơn đặt phòng

Đơn đặt phòng có **tám trạng thái**. Mọi lần chuyển trạng thái đều đi qua một
thành phần duy nhất — `BookingStateMachine` — vì mỗi lần chuyển kéo theo ba việc
phụ rất dễ quên: ghi nhật ký, nhả phòng khi đơn không diễn ra, và hoàn lượt
khuyến mãi.

| Trạng thái | Ý nghĩa |
|---|---|
| `PENDING_PAYMENT` | Vừa tạo, đang giữ chỗ chờ tiền cọc |
| `AWAITING_REVIEW` | Có tiền nhưng không khớp — cần người đối soát |
| `CONFIRMED` | Đã nhận đủ cọc, phòng được giữ chắc chắn |
| `CHECKED_IN` | Khách đã nhận phòng |
| `CHECKED_OUT` | Khách đã trả phòng |
| `CANCELLED` | Khách hoặc quản trị viên huỷ |
| `EXPIRED` | Quá hạn giữ chỗ mà không có tiền |
| `NO_SHOW` | Đã xác nhận nhưng khách không đến |

> 🖼️ **[HÌNH 3.5] — Biểu đồ trạng thái đơn đặt phòng**
> *Cần gì:* biểu đồ trạng thái UML đầy đủ 8 trạng thái và mọi đường chuyển.
> **Đánh dấu nổi bật hai đường** `CANCELLED → AWAITING_REVIEW` và
> `EXPIRED → AWAITING_REVIEW` — đó là nhánh cứu tiền khi tiền về muộn.
> *Lấy ở đâu:* render đoạn `stateDiagram-v2` trong `docs/luong-dat-phong.md`.

**Một quy tắc phản trực giác cần giải thích khi bảo vệ:** trạng thái
`CHECKED_OUT` **không** nhả phòng. Trực giác nói "khách trả phòng rồi thì phòng
phải được trả về kho". Nhưng nhả phòng nghĩa là chuyển dòng gán phòng sang
`RELEASED`, mà trigger kiểm tra số phòng lại đòi đơn phải luôn giữ đúng số phòng
đã đặt — nên nhả phòng lúc trả phòng làm cơ sở dữ liệu bác cả giao dịch. Quy tắc
này sinh ra từ một lỗi thật gặp phải trong quá trình làm, và có một ca kiểm thử
riêng canh nó.

## 3.4. Thiết kế luồng xử lý — biểu đồ tuần tự

> 🖼️ **[HÌNH 3.6] — Biểu đồ tuần tự: đặt phòng**
> *Cần gì:* biểu đồ tuần tự từ lúc khách chọn ngày tới lúc nhận mã QR, gồm các
> đối tượng: Khách, Trình duyệt (Angular), API (Spring Boot), Cơ sở dữ liệu.
> *Lấy ở đâu:* `docs/luong-dat-phong.md`, mục "Đặt phòng".

> 🖼️ **[HÌNH 3.7] — Biểu đồ tuần tự: thanh toán và webhook**
> *Cần gì:* biểu đồ tuần tự gồm: Khách, Ngân hàng, SePay, API, Cơ sở dữ liệu —
> thể hiện luồng từ lúc khách quét QR chuyển khoản tới lúc đơn tự chuyển
> `CONFIRMED`, và vòng hỏi trạng thái của trình duyệt.
> *Lấy ở đâu:* `docs/luong-dat-phong.md`, mục "Thanh toán và webhook".

> 🖼️ **[HÌNH 3.8] — Biểu đồ tuần tự: nhánh tiền về muộn**
> *Cần gì:* biểu đồ tuần tự cho trường hợp tiền về **sau khi** đơn đã `EXPIRED`
> — mở lại đơn, thử giành lại phòng, và hai nhánh kết quả (giành được →
> `CONFIRMED`; không giành được → hàng đợi hoàn tiền).

## 3.5. Thiết kế kiến trúc phần mềm

### 3.5.1. Phân lớp phía backend

> 🖼️ **[HÌNH 3.9] — Sơ đồ phân lớp backend**
> *Cần gì:* sơ đồ bốn lớp Controller → Service → Repository → Cơ sở dữ liệu,
> cộng hai thành phần tách riêng `BookingStateMachine` và `BookingPricingService`.
> *Lấy ở đâu:* render sơ đồ Mermaid thứ hai trong `docs/kien-truc.md`.

| Lớp | Trách nhiệm | **Không** được làm gì |
|---|---|---|
| **Controller** | Nhận HTTP, kiểm tra định dạng đầu vào, ánh xạ sang DTO | Không chứa quy tắc nghiệp vụ |
| **Service** | Quy tắc nghiệp vụ, ranh giới giao dịch | Không biết gì về HTTP |
| **Repository** | Truy cập dữ liệu (JPA, và SQL thuần khi cần) | Không chứa quy tắc nghiệp vụ |
| **Cơ sở dữ liệu** | **Lớp bảo vệ cuối cùng** bằng ràng buộc | — |

**Hai thành phần gom một chỗ, và lý do giống nhau:**

- `BookingStateMachine` — nơi **duy nhất** đổi trạng thái đơn.
- `BookingPricingService` — nơi **duy nhất** tính tiền. Hai công thức tính tiền
  song song sẽ lệch nhau ngay lần đầu có khuyến mãi hoặc làm tròn, và bên lệch
  là bên khách nhìn thấy.

**Frontend không tính tiền.** Mọi con số hiển thị đều lấy từ phản hồi API, kể cả
số tiền giảm khi khách thử mã khuyến mãi.

### 3.5.2. Cấu trúc gói phía backend

Tổ chức theo **tính năng (feature)**, không theo loại kỹ thuật:

```
com.tvh.homestay
├── auth/          — đăng nhập, JWT, refresh token
├── user/          — người dùng
├── room/          — loại phòng, phòng, tiện nghi, khoảng đóng
├── availability/  — truy vấn phòng trống
├── booking/       — đơn, máy trạng thái, gán phòng, tính tiền
├── payment/       — SePay, webhook, đối soát
├── promotion/     — mã khuyến mãi
├── review/        — đánh giá
├── cms/           — nội dung trang chủ
├── admin/         — controller và service khu quản trị
├── report/        — dashboard, xuất CSV
├── storage/       — tải ảnh lên
├── mail/          — hàng đợi thư
├── common/        — xử lý ngoại lệ, tiện ích dùng chung
└── demo/          — nạp dữ liệu mẫu (chỉ profile demo)
```

*Lý do chọn cách này:* gom theo loại kỹ thuật (`controllers/`, `services/`,
`repositories/`) làm một thay đổi nghiệp vụ nhỏ phải sửa file ở ba thư mục xa
nhau. Gom theo tính năng thì mọi thứ liên quan tới đặt phòng nằm cạnh nhau.

### 3.5.3. Cấu trúc thư mục phía frontend

```
src/app
├── core/        — service gọi API, interceptor, guard
├── shared/ui/   — thư viện component tự viết (19 component)
├── features/
│   ├── landing/ — trang khách (trang chủ, phòng, đặt phòng, tra cứu)
│   ├── auth/    — đăng nhập, đăng ký
│   └── admin/   — 11 màn hình quản trị (nạp lười)
└── styles/      — token màu, lớp nền
```

## 3.6. Thiết kế REST API

Hệ thống phơi bày **80 thao tác trên 63 đường dẫn**. Con số này đếm từ tài liệu
OpenAPI của hệ thống **đang chạy**, không chép từ kế hoạch.

### 3.6.1. Nguyên tắc phân quyền: mặc định là đóng

`SecurityConfig` kết thúc bằng `anyRequest().denyAll()`. Một endpoint mới mà
người viết quên khai quyền sẽ **bị từ chối**, chứ không lọt ra công khai. Đây là
lựa chọn "fail closed" thay vì "fail open".

Có một lớp kiểm thử giữ một **ma trận 30 tiền tố đường dẫn** và kiểm hai điều:
mọi endpoint đã đăng ký phải nằm trong ma trận, và **cấm** khai một dòng bao
`/api/admin` gộp hết. Liệt kê từng nhóm buộc người thêm endpoint mới phải nghĩ
về quyền của nó, thay vì được che miễn phí.

### 3.6.2. Bảng endpoint theo nhóm

> **Lưu ý:** bảng đầy đủ 80 thao tác nằm ở `docs/api.md`. Báo cáo nên đưa bảng
> tóm tắt theo nhóm như dưới đây, và đẩy bảng đầy đủ xuống Phụ lục.

| Nhóm | Số thao tác | Quyền | Ví dụ |
|---|---|---|---|
| Công khai — tra cứu, nội dung | 14 | Không cần đăng nhập | `GET /api/room-types` |
| Công khai — đơn đặt phòng | 6 | Mã đơn + SĐT hoặc mã truy cập | `POST /api/bookings` |
| Xác thực | 8 | Hỗn hợp | `POST /api/auth/login` |
| Quản trị — đơn và thanh toán | 8 | `ADMIN` | `GET /api/admin/bookings` |
| Quản trị — danh mục | ~20 | `ADMIN` | `POST /api/admin/rooms/{id}/closures` |
| Quản trị — nội dung, đánh giá | ~20 | `ADMIN` | `PUT /api/admin/content/sections/{key}` |
| Quản trị — báo cáo | 2 | `ADMIN` | `GET /api/admin/dashboard` |
| Chỉ có ở profile demo | 2 | — | `/swagger-ui/**` |

> 🖼️ **[HÌNH 3.10] — Giao diện Swagger UI**
> *Cần gì:* ảnh chụp `http://localhost/swagger-ui/index.html` với danh sách các
> nhóm endpoint đang thu gọn, thấy rõ số lượng.

### 3.6.3. Thiết kế mã lỗi

Giao diện đọc trường `code`, **không** đọc câu chữ mô tả.

| `code` | HTTP | Ý nghĩa |
|---|---|---|
| `ROOM_NOT_AVAILABLE` | 409 | Phòng vừa được đặt hết cho khoảng ngày này |
| `INVALID_PROMOTION` | 400 | Mã không hợp lệ hoặc chưa đủ điều kiện |
| `PROMOTION_EXHAUSTED` | 409 | Mã đã hết lượt |
| `BOOKING_NOT_FOUND` | 404 | Không có đơn khớp mã + số điện thoại |
| `INVALID_ACCESS_TOKEN` | 403 | Mã truy cập sai |
| `INVALID_STATE_TRANSITION` | 409 | Bước chuyển trạng thái không hợp lệ |
| `CLOSURE_OVERLAP` | 409 | Khoảng đóng phòng chồng lên khoảng đã có |
| `PASSWORD_CHANGE_REQUIRED` | 403 | Phải đổi mật khẩu tạm trước |
| `TOO_MANY_REQUESTS` | 429 | Vượt giới hạn tần suất |

## 3.7. Thiết kế giao diện người dùng

### 3.7.1. Hệ thống thiết kế (design system)

Bảng màu là **định nghĩa thương hiệu** của dự án, không phải phương án tạm. Mọi
mã màu tập trung ở một tệp token duy nhất; có một script chạy trong quy trình
kiểm tra **chặn mọi mã màu viết thẳng** ở nơi khác.

> 🖼️ **[HÌNH 3.11] — Trang `/ui-kit` — thư viện component**
> *Cần gì:* ảnh chụp trang `/ui-kit` (chỉ có ở profile `demo`), thể hiện các
> component: nút, ô nhập, thẻ phòng, lịch chọn ngày, nhãn trạng thái…
> *Gợi ý:* chụp thành 2–3 ảnh nếu trang quá dài.

### 3.7.2. Quy ước tương tác bắt buộc

| Quy ước | Lý do |
|---|---|
| Vùng chạm ≥ 44×44 px trên di động | Ngón tay không chính xác như con trỏ chuột |
| Chữ không bao giờ dưới 16px trên di động | Dưới mức này trình duyệt iOS tự phóng to khi chạm ô nhập |
| Lỗi **không bao giờ** chỉ báo bằng màu | Người mù màu không phân biệt được; luôn kèm biểu tượng và chữ |
| Viền focus rõ ràng, không bao giờ bị xoá | Người dùng bàn phím cần biết đang ở đâu |
| Tôn trọng tuỳ chọn giảm chuyển động của hệ điều hành | Người bị rối loạn tiền đình |
| **Chặn sẵn thay vì báo lỗi sau** | Ngày hết phòng bị chặn ngay trong lịch, không để khách chọn rồi mới báo |

### 3.7.3. Sơ đồ điều hướng

> 🖼️ **[HÌNH 3.12] — Sơ đồ điều hướng trang khách (sitemap)**
> *Cần gì:* sơ đồ cây các trang công khai: Trang chủ → Danh sách phòng → Chi
> tiết phòng → Luồng đặt phòng (3 bước) → Thanh toán QR → Hoàn tất; nhánh Tra
> cứu đơn; nhánh Tin tức; nhánh Đăng nhập.

> 🖼️ **[HÌNH 3.13] — Sơ đồ điều hướng khu quản trị**
> *Cần gì:* sơ đồ 11 màn hình quản trị và quan hệ giữa chúng.

---

# CHƯƠNG 4 — XÂY DỰNG VÀ TRIỂN KHAI HỆ THỐNG

## 4.1. Môi trường phát triển

| Thành phần | Công cụ |
|---|---|
| Hệ điều hành | Linux / Windows / macOS |
| IDE backend | IntelliJ IDEA hoặc VS Code |
| IDE frontend | VS Code |
| Quản lý phụ thuộc | Maven (backend), npm (frontend) |
| Quản lý mã nguồn | Git, lưu trữ trên GitHub |
| Cơ sở dữ liệu cục bộ | PostgreSQL 16 trong Docker |
| Kiểm thử API | Swagger UI, curl |

**Yêu cầu tối thiểu để chạy hệ thống:**

| Công cụ | Phiên bản | Ghi chú |
|---|---|---|
| Docker | có Compose v2 | Đường chạy khuyến nghị |
| JDK | 21 | Chỉ khi chạy thủ công |
| Node.js | 20.19+ hoặc 22.12+ | Angular 21 yêu cầu |
| PostgreSQL | 16 | Chỉ khi chạy thủ công — **bắt buộc**, cần `btree_gist` |

## 4.2. Quy trình phát triển

Đồ án được thực hiện theo **mười giai đoạn (phase)** tuần tự, tổng khối lượng
ước tính khoảng 97 giờ công. Mỗi giai đoạn có tài liệu kế hoạch riêng, tiêu chí
nghiệm thu riêng, và chỉ được đóng khi mọi tiêu chí đã kiểm chứng được bằng lệnh.

| Phase | Nội dung | Khối lượng |
|---|---|---|
| 1 | Khởi tạo dự án và hạ tầng phát triển | 5h |
| 2 | Hệ thống thiết kế và thư viện component | 12h |
| 3 | Lược đồ cơ sở dữ liệu và migration | 8h |
| 4 | Xác thực và phân quyền (JWT) | 7h |
| 5 | Lõi đặt phòng và chống trùng lịch | 11h |
| 6 | Thanh toán QR, webhook và thư | 10h |
| 7 | Khu quản trị và dashboard | 14h |
| 8 | Trang khách, quản lý nội dung và đánh giá | 16h |
| 9 | Đóng gói, dữ liệu mẫu và tài liệu | 7h |
| 10 | **Lịch khoá phòng theo khoảng ngày** | 7h |

> 🖼️ **[HÌNH 4.1] — Sơ đồ phụ thuộc giữa các giai đoạn**
> *Cần gì:* lưu đồ thể hiện thứ tự và quan hệ phụ thuộc giữa 10 phase, chỉ rõ
> hai cặp có thể làm song song (Phase 2 ∥ Phase 3; Phase 4 ∥ Phase 5).
> *Lấy ở đâu:* render Mermaid trong `plans/260907-1304-homestay-tvh-booking/plan.md`.

> 🖼️ **[HÌNH 4.2] — Biểu đồ Gantt tiến độ thực hiện**
> *Cần gì:* biểu đồ Gantt các giai đoạn theo thời gian thực tế.
> *Gợi ý:* nhiều khoa yêu cầu hình này. Dựng bằng Mermaid `gantt` hoặc Excel.

## 4.3. Cài đặt các chức năng chính

### 4.3.1. Chức năng tìm phòng trống

Đây là chức năng có yêu cầu kỹ thuật cao nhất phía truy vấn. Hệ thống có **bốn
truy vấn phòng trống** phục vụ bốn màn hình khác nhau, tất cả nằm trong một lớp
truy cập dữ liệu duy nhất:

| Truy vấn | Phục vụ | Hậu quả nếu sai |
|---|---|---|
| Đếm phòng trống theo loại | Trang danh sách phòng, bước 2 đặt phòng | Khách thấy sai số phòng còn lại |
| **Chọn phòng vật lý để gán** | **Lúc tạo đơn** | **Gán nhầm phòng — lỗi im lặng, chỉ lộ khi khách tới** |
| Lịch từng đêm toàn homestay | Thanh tìm phòng trang chủ | Lịch chặn sai ngày |
| Lịch từng đêm một loại phòng | Trang chi tiết phòng | Lịch chặn sai ngày |

**Điểm kỹ thuật đáng trình bày:** truy vấn đếm phòng trống **đếm trực tiếp** số
phòng vừa khả dụng vừa rảnh, thay vì lấy tổng trừ đi số đã đặt.

Cách "tổng trừ đã đặt" **sai** ở chỗ nó trừ mọi đơn đang giữ chỗ của loại phòng
đó — kể cả đơn nằm trên phòng đã chuyển sang bảo trì. Một phòng bảo trì đang có
đơn cũ bị trừ **hai lần**: một lần vì nó không còn khả dụng, một lần nữa vì nó
vẫn có đơn. Website báo thiếu phòng và mất doanh thu âm thầm.

Ngoài ra, truy vấn lịch toàn homestay gộp **cả khoảng ngày vào một câu lệnh**
thay vì lặp từng đêm. Khoảng tối đa là 120 ngày, và 120 lượt đi về cơ sở dữ liệu
cho một lần mở lịch là đủ để người dùng thấy lịch giật.

> 🖼️ **[HÌNH 4.3] — Màn hình trang chủ với thanh tìm phòng**
> *Cần gì:* ảnh chụp trang chủ đầy đủ, thấy rõ thanh tìm phòng và lịch đang mở
> với một số ngày bị chặn.
> *Lấy ở đâu:* đã có sẵn tại `docs/images/landing.png`, hoặc chụp lại.

> 🖼️ **[HÌNH 4.4] — Kết quả tìm phòng**
> *Cần gì:* ảnh chụp danh sách loại phòng còn chỗ, thấy rõ nhãn "còn N phòng"
> và **tổng tiền cả kỳ**.

> 🖼️ **[HÌNH 4.5] — Trang chi tiết loại phòng**
> *Cần gì:* ảnh chụp trang chi tiết với thư viện ảnh, danh sách tiện nghi được
> nhóm theo loại (tiện nghi trong phòng / tiện ích chung), và lịch giá.

### 4.3.2. Chức năng đặt phòng và vòng thử gán phòng

Thuật toán gán phòng là nơi lý thuyết ở Chương 2 biến thành mã:

```
Với mỗi phòng ứng viên còn rảnh:
    Mở một giao dịch MỚI
    Thử ghi dòng gán phòng
    Nếu cơ sở dữ liệu trả SQLSTATE 23P01:
        → một khách khác vừa chiếm mất phòng này
        → huỷ giao dịch, thử phòng tiếp theo
    Nếu thành công:
        → commit và kết thúc
Hết phòng để thử → trả lỗi ROOM_NOT_AVAILABLE (409)
```

**Điểm bắt buộc phải nhấn mạnh khi bảo vệ:** dòng "Mở một giao dịch MỚI" không
phải tuỳ chọn. Khi PostgreSQL bác một câu lệnh vì vi phạm ràng buộc, toàn bộ
giao dịch bị đánh dấu hỏng; mọi câu lệnh tiếp theo trong cùng giao dịch đó trả
về `25P02` chứ không chạy. Thử phòng tiếp theo trong cùng giao dịch là **không
thể thành công**.

Hệ thống cũng chỉ bắt **đúng** mã `23P01`. Bắt chung mọi lỗi toàn vẹn dữ liệu sẽ
khiến một lỗi khoá ngoại — tức là lỗi lập trình thật — bị báo cho khách thành
"hết phòng", và lỗi đó không bao giờ được ai phát hiện.

> 🖼️ **[HÌNH 4.6] — Luồng đặt phòng ba bước**
> *Cần gì:* ba ảnh chụp ghép lại: (1) chọn ngày và số khách, (2) chọn loại
> phòng, (3) nhập thông tin khách và áp mã khuyến mãi.

### 4.3.3. Chức năng thanh toán

Hệ thống sinh mã QR theo chuẩn VietQR, kèm nội dung chuyển khoản gồm **mã đơn +
hai chữ số lần thử**. Hai chữ số lần thử cho phép phân biệt các lần chuyển khoản
khác nhau của cùng một đơn.

Màn hình QR có đồng hồ đếm ngược 15 phút **tương ứng với hạn giữ chỗ có thật
trong cơ sở dữ liệu** — không phải đồng hồ trang trí để thúc ép.

> 🖼️ **[HÌNH 4.7] — Màn hình thanh toán QR**
> *Cần gì:* ảnh chụp màn hình QR với mã QR, thông tin chuyển khoản dạng chữ
> (phương án dự phòng khi mạng chập chờn), và đồng hồ đếm ngược.

**Xử lý webhook** — bảng quyết định khi tiền về:

| Tình huống | Xử lý |
|---|---|
| Đủ tiền cọc | → `CONFIRMED`, gửi thư xác nhận |
| Thiếu tiền | → `AWAITING_REVIEW`, đánh dấu cần đối soát |
| Thừa tiền | → đánh dấu cần hoàn lại phần thừa |
| Webhook gửi lại lần hai | Bỏ qua, không cộng tiền hai lần |
| Không khớp đơn nào | Vẫn ghi vào nhật ký webhook để người đối soát |
| Tiền về sau khi đơn đã đóng | Mở lại `AWAITING_REVIEW`, thử giành lại phòng |

**Nguyên tắc thiết kế xuyên suốt:** *không nhánh nào để tiền của khách biến mất
im lặng.* Mọi khoản tiền không xử lý tự động được đều vào hàng đợi đối soát thủ
công, chứ không bị bỏ qua.

### 4.3.4. Chức năng quản lý ngày khả dụng (Phase 10)

Trước giai đoạn này, hệ thống chỉ có công tắc trạng thái vận hành cho phòng:
`AVAILABLE` / `MAINTENANCE` / `OUT_OF_SERVICE`. Công tắc đó **không có ngày**.
Chủ homestay muốn nói "phòng 201 sơn lại từ 20/10 đến 25/10" thì phải tự nhớ tắt
rồi tự nhớ bật — và ngày quên bật là ngày mất doanh thu mà không có gì báo.

Giải pháp: bảng `room_closures` với cột sinh `blocked daterange` dùng cùng quy
ước nửa mở `[)`, cộng ràng buộc `EXCLUDE` chống chồng lấn, và một điều kiện lọc
thêm vào **cả bốn** truy vấn phòng trống.

**Phần thưởng kiến trúc đáng nêu khi bảo vệ:** phía khách **không phải sửa một
dòng mã nào**. Trang chủ, danh sách phòng, trang chi tiết và luồng đặt phòng đều
đọc phòng trống qua bốn truy vấn đó, nên sửa ở tầng dữ liệu là cả bốn màn hình
tự đúng theo. Đây là kết quả của việc gom mọi truy vấn phòng trống vào một lớp
duy nhất từ Phase 5 — một điểm kiến trúc, không phải may mắn.

### 4.3.5. Khu quản trị

Khu quản trị gồm **11 màn hình**, nạp lười theo tuyến.

> 🖼️ **[HÌNH 4.8] — Dashboard quản trị**
> *Cần gì:* ảnh chụp trang tổng quan với biểu đồ doanh thu theo tháng, tỉ lệ lấp
> đầy, tỉ lệ huỷ.
> *Lấy ở đâu:* đã có sẵn tại `docs/images/admin-dashboard.png`.

> 🖼️ **[HÌNH 4.9] — Màn hình đối soát thanh toán**
> *Cần gì:* ảnh chụp danh sách các khoản cần đối soát, thấy rõ khoản thiếu tiền
> và khoản thừa tiền.
> *Lấy ở đâu:* đã có sẵn tại `docs/images/admin-payments.png`.

> 🖼️ **[HÌNH 4.10] — Màn hình danh sách đơn đặt phòng**
> *Cần gì:* ảnh chụp bảng đơn với bộ lọc theo trạng thái, thấy đủ nhiều trạng
> thái khác nhau trong cùng một ảnh.

> 🖼️ **[HÌNH 4.11] — Màn hình chi tiết đơn**
> *Cần gì:* ảnh chụp trang chi tiết một đơn, thấy rõ dòng thời gian lịch sử
> trạng thái, các lần thanh toán, thư đã gửi và ghi chú nội bộ.

> 🖼️ **[HÌNH 4.12] — Màn hình quản lý nội dung trang chủ (CMS)**
> *Cần gì:* ảnh chụp màn hình sửa một khối nội dung, có ô soạn thảo HTML.

### 4.3.6. Quản lý nội dung và lọc HTML

Khu quản trị cho phép soạn nội dung HTML cho trang chủ và tin tức. Đây là một
bề mặt tấn công XSS cổ điển.

Hệ thống lọc HTML **ở tầng vào** (trước khi lưu) bằng thư viện
`owasp-java-html-sanitizer` với **danh sách thẻ cho phép tường minh** — không
phải danh sách thẻ cấm. Lý do: danh sách cấm luôn thiếu; thẻ nguy hiểm mới xuất
hiện là danh sách cấm lại lỗi thời, còn danh sách cho phép thì vẫn đúng.

Đối với nội dung do **khách** nhập (ví dụ đánh giá), hệ thống không lọc HTML mà
hiển thị dạng **văn bản thuần**, vì khách không có lý do gì để cần định dạng.

## 4.4. Cài đặt mô hình bảo mật

### 4.4.1. Xác thực và buộc đổi mật khẩu tạm

Tài khoản quản trị của bản demo được sinh với **mật khẩu ngẫu nhiên, chỉ in một
lần vào log container** — không ghi vào bất kỳ tệp nào trong mã nguồn.

Tài khoản đó mang cờ "bắt buộc đổi mật khẩu". Một bộ lọc chặn **mọi** đường dẫn
trừ ba đường tối thiểu (xem thông tin bản thân, đổi mật khẩu, đăng xuất) cho tới
khi mật khẩu được đổi. Lý do: mật khẩu tạm đã đi qua log nên **không còn là bí
mật**.

> 🖼️ **[HÌNH 4.13] — Màn hình buộc đổi mật khẩu lần đầu**
> *Cần gì:* ảnh chụp màn hình đổi mật khẩu hiện ra ngay sau lần đăng nhập đầu,
> kèm thông báo giải thích vì sao bắt buộc.

### 4.4.2. Mã đơn ≠ mã truy cập

Một quyết định bảo mật tinh tế đáng trình bày: hệ thống dùng **hai mã khác nhau**
cho một đơn.

**Mã đơn** xuất hiện trên sao kê ngân hàng (vì nó nằm trong nội dung chuyển
khoản). Nghĩa là bất kỳ ai nhìn thấy sao kê đều biết mã đơn của người khác.

**Mã truy cập** là bí mật thao tác, chỉ có trong liên kết ở thư xác nhận.

Vì thế endpoint xem trạng thái thanh toán **bắt buộc** mã truy cập: nếu chỉ cần
mã đơn là xem được, ai thấy sao kê cũng theo dõi được đơn của người khác.

### 4.4.3. Giới hạn tần suất sau nginx

Hệ thống giới hạn tần suất theo **chín khoá** khác nhau:

| Khoá | Hạn mức | Áp cho |
|---|---|---|
| `ip:auth` | 10 / phút | Mọi endpoint xác thực |
| `email:login` | 5 / phút | Đăng nhập, theo email trong thân request |
| `ip:availability` | 60 / phút | Truy vấn phòng trống |
| `ip:promo` | 20 / phút | Kiểm tra mã khuyến mãi |
| `ip:booking-create` | 10 / phút | Tạo đơn |
| `phone:booking` | 10 / giờ | Tạo đơn, theo số điện thoại |
| `ip:lookup` | 10 / phút | Tra cứu đơn |
| `code:lookup` | 5 / giờ | Tra cứu đơn, theo mã đơn |
| `ip:cancel` | 10 / phút | Huỷ đơn |

**Vấn đề kỹ thuật phải giải:** ứng dụng nằm sau nginx, nên mọi request đến đều
mang địa chỉ IP của container nginx. Giới hạn theo IP đó là giới hạn **toàn bộ
người dùng chung một hạn mức**.

Giải pháp là để nginx **ghi đè** header `X-Forwarded-For` bằng địa chỉ thật của
người gọi. Nhưng header này do client gửi lên nên **giả mạo được** — trừ khi
nginx ghi đè chứ không nối thêm, **và** cổng của ứng dụng không mở ra ngoài để
không ai gọi thẳng vào được. Hai điều kiện này phải đi cùng nhau; thiếu một là
lớp giới hạn tần suất trở thành trang trí.

### 4.4.4. Quản lý bí mật

Hai biến `JWT_SECRET` và `SEPAY_WEBHOOK_API_KEY` **không có giá trị mặc định ở
bất kỳ đâu**: không trong tệp cấu hình, không trong tệp `.env` mẫu, không trong
tệp Compose. Ứng dụng **dừng khởi động** khi thiếu, ở mọi profile.

*Lý do:* kho mã nguồn này công khai. Một giá trị mặc định "an toàn cho demo" nằm
trong kho đồng nghĩa với việc bất kỳ ai clone về cũng **tự ký được JWT vai trò
ADMIN** cho mọi bản triển khai dùng kho này.

Một script riêng là đường **duy nhất** tạo ra các giá trị thật, sinh ngẫu nhiên
bằng `openssl` vào tệp `.env` với quyền truy cập hạn chế.

## 4.5. Đóng gói và triển khai

### 4.5.1. Kiến trúc Docker Compose

Bốn dịch vụ:

| Dịch vụ | Ảnh nền | Cổng mở ra máy chủ |
|---|---|---|
| `web` | `nginx:alpine` | **80** |
| `api` | `eclipse-temurin:21-jre-alpine` | không |
| `db` | `postgres:16` | không |
| `mailpit` | `mailpit` | **8025** (giao diện xem thư) |

**Việc `api` và `db` không mở cổng là một quyết định bảo mật**, không phải tối
giản cấu hình — đó là điều kiện để tin được header `X-Forwarded-For` như đã nói
ở 4.4.3.

Ảnh `api` chạy bằng **người dùng không phải root**, và được dựng bằng Dockerfile
nhiều tầng nên ảnh cuối không chứa Maven.

### 4.5.2. Cấu hình nginx

nginx phải chuyển tiếp **bốn nhóm đường dẫn** chứ không chỉ `/api`:

| Đường dẫn | Nếu quên chuyển tiếp |
|---|---|
| `/api/**` | Toàn bộ hệ thống không chạy |
| `/swagger-ui/**` | Trả về trang Angular kèm mã 200 — **trông y như thành công** |
| `/v3/api-docs/**` | Như trên |
| `/uploads/**` | Mọi ảnh khách tải lên trả 404 |

Cả bốn khối phải dùng cú pháp `location ^~` (tiền tố ưu tiên), không phải
`location` thường. Nguyên nhân: khối cuối tệp xử lý tệp tĩnh là một **location
chính quy**, và nginx cho location chính quy thắng mọi location tiền tố thường.
Không có `^~` thì ảnh tải lên rơi vào khối tĩnh và trả 404.

### 4.5.3. Hai profile

Hệ thống chỉ có **hai** profile, cố ý không nhiều hơn:

| Profile | Dùng khi | Gồm gì |
|---|---|---|
| `demo` | Phát triển và bản đem đi bảo vệ | Dữ liệu mẫu, Swagger UI, trang `/ui-kit` |
| `prod` | Triển khai thật | Không dữ liệu mẫu, không Swagger, không `/ui-kit` |

Đổi profile trên cùng một khối dữ liệu là **an toàn** — đây chính là lý do dữ
liệu mẫu không nằm trong lịch sử Flyway.

### 4.5.4. Dữ liệu mẫu

Profile `demo` nạp sẵn: 4 loại phòng, 15 phòng vật lý, 12 tiện nghi, **40 đơn
đặt phòng trải đủ tám trạng thái**, 3 khoản cần đối soát, 3 mã khuyến mãi (còn
hạn / hết hạn / hết lượt), 8 đánh giá, 1 khoảng đóng phòng, và nội dung trang
chủ đầy đủ.

**Mọi ngày trong dữ liệu mẫu là tương đối** (`CURRENT_DATE ± n`), nên bộ dữ liệu
không cũ đi theo thời gian — hôm bảo vệ vẫn có đơn trong tương lai để trình diễn.

### 4.5.5. Xử lý múi giờ

Toàn hệ thống chạy `Asia/Ho_Chi_Minh`, đặt ở **bốn tầng độc lập**: biến `TZ` của
container, tham số JVM, cấu hình serialize JSON, và cấu hình JDBC.

Chỉ đặt cấu hình JSON là **không đủ** — thuộc tính đó chỉ chi phối cách dữ liệu
được chuyển thành JSON, không đổi múi giờ mặc định của JVM. Hệ quả nếu làm sai:
đơn tạo trong khung 00:00–07:00 giờ Việt Nam bị gom nhóm vào **tháng trước** ở
dashboard, trong khi đồng hồ đếm ngược trên màn hình thanh toán vẫn đúng — sai
một nửa nên rất khó nghi ngờ.

Endpoint kiểm tra sức khoẻ phơi bày cả múi giờ ứng dụng lẫn múi giờ mặc định của
JVM để kiểm tra được điều này bằng mắt.

### 4.5.6. Quy trình triển khai ba lệnh

```bash
git clone <địa-chỉ-kho> homestay-tvh && cd homestay-tvh
./scripts/init-env.sh          # sinh bí mật ngẫu nhiên
docker compose up -d --build   # dựng và chạy 4 dịch vụ
```

> 🖼️ **[HÌNH 4.14] — Kết quả lệnh `docker compose ps`**
> *Cần gì:* ảnh chụp terminal thể hiện cả 4 dịch vụ ở trạng thái `healthy`.

> 🖼️ **[HÌNH 4.15] — Hộp thư Mailpit**
> *Cần gì:* ảnh chụp `http://localhost:8025` với thư xác nhận đặt phòng đã gửi,
> mở ra thấy nội dung thư.

---

# CHƯƠNG 5 — KIỂM THỬ VÀ ĐÁNH GIÁ

## 5.1. Chiến lược kiểm thử

Hệ thống có **129 ca kiểm thử tự động trong 18 lớp**, tất cả đều xanh.

### 5.1.1. Vì sao không dùng cơ sở dữ liệu giả lập

Mọi ca kiểm thử tích hợp dựng một **PostgreSQL 16 thật** bằng Testcontainers.
Không giả lập cơ sở dữ liệu, vì phần lớn thứ đáng kiểm ở dự án này **chính là
hành vi của cơ sở dữ liệu**: ràng buộc `EXCLUDE`, kiểu `daterange`, trigger
hoãn, mã `SQLSTATE`. Giả lập chúng là kiểm thử một thứ không tồn tại.

### 5.1.2. Các cấp độ kiểm thử

| Cấp độ | Ví dụ lớp | Kiểm điều gì |
|---|---|---|
| **Kiểm thử đơn vị** | Lọc HTML, xuất CSV | Logic thuần, không cần cơ sở dữ liệu |
| **Kiểm thử tích hợp** | Hầu hết các lớp | Nhiều tầng + cơ sở dữ liệu thật |
| **Kiểm thử tranh chấp** | Đặt phòng đa luồng, đua thanh toán | Nhiều luồng chạy song song thật |
| **Kiểm thử lược đồ** | Migration, ràng buộc | Chính cấu trúc cơ sở dữ liệu |

## 5.2. Kết quả kiểm thử tự động

> 🖼️ **[HÌNH 5.1] — Kết quả chạy `./mvnw verify`**
> *Cần gì:* ảnh chụp terminal thể hiện dòng
> `Tests run: 129, Failures: 0, Errors: 0, Skipped: 0` và `BUILD SUCCESS`.
> **Đây là hình bắt buộc phải có.**

### 5.2.1. Bảng tổng hợp theo lớp

| Lớp kiểm thử | Số ca | Chứng minh điều gì |
|---|---:|---|
| `SchemaMigrationTest` | 8 | Migration dựng đúng 21 bảng; mọi migration thành công; không migration nào bị sửa sau phát hành |
| `SchemaConstraintIT` | 9 | Từng ràng buộc `CHECK` thật sự chặn: email không chữ thường, trạng thái sai chính tả, số tiền âm, ngày trả ≤ ngày nhận |
| `SqlStatesIT` | 2 | Hai mã lỗi `23P01` và `25P02` được dịch đúng |
| **`BookingConcurrencyIT`** | 3 | **Lớp quan trọng nhất.** Nhiều luồng thật cùng đặt phòng cuối — đúng một luồng thắng |
| `AvailabilityQueryIT` | 6 | Đếm phòng trống đúng trong các tình huống dễ đếm sai |
| `BookingLifecycleIT` | 8 | Bảng chuyển trạng thái; `CHECKED_OUT` không nhả phòng |
| `BookingExpiryIT` | 4 | Đơn quá hạn chuyển `EXPIRED`; đơn đã có tiền **không** bị quét |
| **`RoomClosureIT`** | 9 | Khoảng đóng phòng trừ đúng phòng, đúng đêm, ở cả bốn truy vấn |
| `SepayWebhookIT` | 10 | Sai khoá API bị từ chối; webhook gửi lại không cộng tiền hai lần |
| **`PaymentRaceIT`** | 5 | Cửa sổ tranh chấp webhook ↔ bộ quét hết hạn |
| `AuthFlowIT` | 7 | Đăng ký, đăng nhập, làm mới token; token cũ chết ngay khi đổi mật khẩu |
| **`EndpointAuthorizationIT`** | 3 | Mọi endpoint nằm trong ma trận 30 tiền tố; cấm khai dòng bao |
| `ContentSanitizerTest` | 17 | Bộ lọc HTML chặn `<script>`, `javascript:`, `<iframe>` |
| `PublicContentIT` | 7 | API công khai chỉ trả nội dung đã đăng |
| `ReviewIT` | 8 | Đánh giá cần xác thực; tên khách chụp **từ đơn**, không nhận từ client |
| `DashboardServiceIT` | 6 | Doanh thu, tỉ lệ lấp đầy, tỉ lệ huỷ tính đúng |
| `CsvExportTest` | 10 | Xuất CSV thoát đúng dấu phẩy, dấu nháy, xuống dòng |
| `ImageUploadIT` | 7 | Từ chối kiểu tệp sai và tệp quá lớn; đổi tên thành UUID |
| **Tổng** | **129** | |

### 5.2.2. Ba ca kiểm thử đáng trình bày chi tiết khi bảo vệ

**(1) Kiểm thử tranh chấp đặt phòng.** Nhiều luồng thật chạy song song cùng đặt
phòng cuối cùng. Kết quả bắt buộc: **đúng một luồng thắng**, không có hai dòng
gán phòng chồng lấn trong cơ sở dữ liệu. Đây là bằng chứng trực tiếp cho mục
tiêu số 2 của đồ án.

**(2) Kiểm thử phòng đang đóng không bao giờ được gán.** Ba truy vấn phòng trống
chỉ ảnh hưởng thứ khách *nhìn thấy*; sót truy vấn chọn phòng vật lý thì **đơn
vẫn tạo được và phòng đang sửa chữa vẫn bị gán** — lỗi im lặng, chỉ lộ ra khi
khách tới nhận phòng. Ca kiểm thử này canh đúng chỗ đó.

**(3) Kiểm thử tiền về muộn.** Tiền về **sau khi** đơn đã hết hạn: đơn mở lại
sang `AWAITING_REVIEW`, hệ thống thử giành lại phòng; không giành được thì vào
hàng đợi hoàn tiền. Đây là bằng chứng cho tiêu chí "không nhánh nào để tiền biến
mất im lặng".

## 5.3. Các lỗi thực tế phát hiện trong quá trình làm

> **Đây là mục làm điểm.** Hội đồng đánh giá cao sinh viên trình bày được lỗi
> thật đã gặp và cách chẩn đoán, hơn là một báo cáo chỉ có thành công.

| # | Hiện tượng | Nguyên nhân gốc | Cách sửa |
|---|---|---|---|
| 1 | Giao dịch thử phòng tiếp theo luôn thất bại | Bắt `23P01` rồi thử tiếp **trong cùng giao dịch**; PostgreSQL đã huỷ giao dịch nên mọi lệnh sau trả `25P02` | Mỗi lượt thử chạy trong một giao dịch mới |
| 2 | Website báo thiếu phòng | Truy vấn "tổng trừ đã đặt" trừ **hai lần** phòng bảo trì đang có đơn | Đếm trực tiếp phòng vừa khả dụng vừa rảnh |
| 3 | Đơn bị huỷ khi khách trả phòng | Nhả phòng lúc `CHECKED_OUT` làm trigger kiểm số phòng bác giao dịch | `CHECKED_OUT` không nhả phòng |
| 4 | **Toàn bộ trang web hiển thị không có CSS** | Chính sách bảo mật nội dung (CSP) chặn thuộc tính `onload` mà công cụ tối ưu CSS tự sinh | Tắt tính năng tối ưu đó thay vì nới lỏng CSP |
| 5 | **Mọi nút bấm chính có độ tương phản ~2:1** | Tệp CSS nền **không nằm trong `@layer`** nên thắng mọi lớp tiện ích của Tailwind bất kể độ ưu tiên | Bọc tệp nền trong `@layer base` |
| 6 | Ảnh tải lên trả 404 | Khối `location` chính quy của nginx thắng khối tiền tố thường | Dùng `location ^~` cho bốn khối proxy |
| 7 | Màn hình thanh toán trắng trơn | Component gọi hàm đọc tham số đầu vào **trong hàm khởi tạo**, trước khi Angular gán tham số | Chuyển sang `ngOnInit` |
| 8 | Bộ nạp dữ liệu mẫu cộng dồn mỗi lần khởi động | Điều kiện idempotent khoá theo ngày tương đối nên mỗi ngày lại khớp khác | Đổi điều kiện sang khoá toàn bảng |
| 9 | Khu quản trị đóng nhầm phòng không hiển thị | Khối giao diện giữ id phòng cũ sau khi bộ lọc của màn hình cha thay đổi | Đối chiếu id với danh sách phòng đang hiển thị |

**Điểm đáng nêu về lỗi số 4 và 5:** cả hai chỉ phát hiện được khi **chụp màn
hình bằng trình duyệt thật**. Việc biên dịch thành công không nói gì về việc
trang có hiển thị đúng hay không.

**Điểm đáng nêu về lỗi số 9:** lỗi này do một vòng rà soát mã độc lập tìm ra
**sau khi** tác giả đã tự kiểm thử và cho rằng chức năng đã hoàn chỉnh.

> 🖼️ **[HÌNH 5.2] — So sánh trước/sau lỗi tương phản nút bấm**
> *Cần gì:* hai ảnh chụp cạnh nhau: nút bấm chữ tối trên nền tối (~2:1) và nút
> bấm chữ trắng trên nền xanh (đạt chuẩn). Ghi rõ tỉ số tương phản đo được.

## 5.4. Kiểm thử phi chức năng

### 5.4.1. Khả năng tiếp cận (Accessibility)

Quét tự động bằng công cụ `axe` trên 7 màn hình công khai và 6 màn hình quản
trị: **0 lỗi vi phạm WCAG 2.0 và 2.1 mức A + AA**.

Đo vùng chạm ở 3 độ rộng màn hình: **0 vùng chạm dưới 44×44 px**.

> 🖼️ **[HÌNH 5.3] — Kết quả quét khả năng tiếp cận**
> *Cần gì:* ảnh chụp kết quả quét axe (qua tiện ích trình duyệt axe DevTools
> hoặc Lighthouse) thể hiện 0 vi phạm.

### 5.4.2. Kiểm thử bảo mật

| Kịch bản tấn công | Kết quả |
|---|---|
| Giả mạo header `X-Forwarded-For` để vượt giới hạn tần suất | **Thất bại** — vẫn bị chặn ở đúng ngưỡng |
| Gọi endpoint quản trị không có token | 401 |
| Gọi endpoint quản trị bằng token `CUSTOMER` | 403 |
| Dùng token cũ sau khi đã đổi mật khẩu | 401 — token cũ chết ngay |
| Gọi webhook không có khoá API | Bị từ chối |
| Gửi lại cùng một webhook lần hai | Bỏ qua, không cộng tiền hai lần |
| Chèn `<script>` vào nội dung CMS | Bị bộ lọc gỡ bỏ |
| Xem trạng thái thanh toán chỉ bằng mã đơn | Bị từ chối — cần mã truy cập |

### 5.4.3. Kiểm thử đáp ứng (Responsive)

> 🖼️ **[HÌNH 5.4] — Giao diện trên ba kích thước màn hình**
> *Cần gì:* ba ảnh chụp cùng một trang (trang chủ hoặc chi tiết phòng) ở độ rộng
> điện thoại, máy tính bảng và máy tính để bàn, ghép cạnh nhau.
> *Cách làm:* dùng chế độ giả lập thiết bị của DevTools.

## 5.5. Kiểm thử thủ công trước khi bảo vệ

Danh sách tối thiểu nên chạy tay trên bản đóng gói:

1. Trang chủ có nội dung thật: 4 loại phòng, thư viện ảnh, đánh giá, tin tức.
2. Đặt một đơn từ đầu đến khi ra mã QR.
3. Nhấn F5 ở màn hình QR — vẫn thấy QR và đồng hồ đếm ngược.
4. Tra cứu đơn bằng mã + số điện thoại; thử sai số điện thoại → bị từ chối.
5. Huỷ đơn; kiểm tra phòng mở lại (đặt lại đúng ngày đó được).
6. Đăng nhập quản trị → bị buộc đổi mật khẩu → vào dashboard.
7. Màn hình đối soát có sẵn khoản cần xử lý.
8. Sửa khối nội dung trang chủ → mở lại trang chủ thấy đổi.
9. Mở Mailpit xem thư xác nhận đã gửi.
10. Đóng một phòng vài ngày → tìm phòng đúng khoảng đó thấy số phòng giảm 1,
    đêm mở lại **không** giảm.

## 5.6. Đánh giá kết quả đạt được

### 5.6.1. Đối chiếu với mục tiêu ban đầu

| # | Mục tiêu | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | Khách tìm và đặt phòng thành công | ✅ Đạt | Kiểm thử thủ công + 6 ca tự động |
| 2 | Không bao giờ bán trùng phòng | ✅ Đạt | 3 ca kiểm thử đa luồng thật |
| 3 | QR VietQR + webhook tự xác nhận | ✅ Đạt | 10 ca kiểm thử webhook |
| 4 | Không để tiền biến mất im lặng | ✅ Đạt | 5 ca kiểm thử đua thanh toán |
| 5 | Quản lý phòng, giá, số khách, **ngày khả dụng** | ✅ Đạt | 9 ca `RoomClosureIT` |
| 6 | Giao diện đạt chuẩn thương mại | ✅ Đạt | 0 lỗi WCAG A + AA |
| 7 | `docker compose up` là đủ | ⚠️ Đạt có điều kiện | Xem mục 5.6.3 |
| 8 | Tài liệu kỹ thuật đầy đủ | ✅ Đạt | 12 tài liệu trong `docs/` |

### 5.6.2. Đối chiếu với đề bài của môn học

| Yêu cầu đề bài | Đáp ứng |
|---|---|
| Hiển thị phòng, tiện nghi và hình ảnh | ✅ |
| Tìm phòng theo ngày | ✅ |
| Gửi yêu cầu đặt phòng | ✅ |
| Quản lý trạng thái đơn | ✅ Tám trạng thái, máy trạng thái có kiểm thử |
| Quản trị thông tin phòng | ✅ |
| Quản trị viên cập nhật **phòng, giá, số lượng khách và ngày khả dụng** | ✅ Đủ bốn vế |
| Hệ thống kiểm tra trùng ngày | ✅ Ở tầng cơ sở dữ liệu |
| Tính chi phí dự kiến | ✅ Một nơi tính tiền duy nhất |
| Không yêu cầu thanh toán thật | ✅ Có endpoint mô phỏng |
| Có dữ liệu minh hoạ để trình diễn | ✅ 40 đơn đủ tám trạng thái |

### 5.6.3. Những điều chưa kiểm chứng được

Trung thực về giới hạn của chính quá trình kiểm thử:

- Quá trình phát triển chạy trong môi trường có proxy chặn kho phụ thuộc bên
  ngoài từ trong container dựng ảnh, nên **bước `docker compose build` chưa
  được chạy trọn vẹn một lần** trong môi trường đó. Hệ thống đã được kiểm chứng
  bằng PostgreSQL thật cộng ứng dụng chạy trực tiếp. **Cần chạy lại đủ ba lệnh
  trên máy có mạng bình thường trước hôm bảo vệ.**
- Frontend **không có bộ kiểm thử tự động thường trực**. Chất lượng được canh
  bằng biên dịch, kiểm kiểu, kiểm lint và kiểm thử bằng trình duyệt thật ở từng
  giai đoạn. **Hồi quy giao diện hiện phải phát hiện bằng mắt** — đây là giới
  hạn đã biết.

## 5.7. Các giới hạn đã biết của hệ thống

> **Mục này nên giữ nguyên trong báo cáo.** Nêu rõ giới hạn mình biết là dấu
> hiệu của người hiểu hệ thống mình làm; giấu đi rồi bị hội đồng tìm ra thì tệ
> hơn nhiều.

| # | Giới hạn | Ảnh hưởng | Hướng khắc phục |
|---|---|---|---|
| 1 | Giới hạn tần suất lưu trong bộ nhớ tiến trình | Chỉ đúng khi chạy **một** bản ứng dụng | Chuyển sang Redis khi mở rộng |
| 2 | Webhook xác thực bằng khoá API tĩnh, không phải chữ ký | Khoá lộ thì giả mạo được | Dùng chữ ký HMAC khi nhà cung cấp hỗ trợ |
| 3 | Không có CSRF token | Rủi ro thấp vì token nằm trong header, không phải cookie | Thêm nếu chuyển sang xác thực bằng cookie |
| 4 | Hoàn tiền là thao tác thủ công | Người quản trị phải tự chuyển khoản lại | Tích hợp API hoàn tiền của nhà cung cấp |
| 5 | Không có nhật ký kiểm toán cho thao tác quản trị | Không truy được ai đã sửa gì | Thêm bảng audit log |
| 6 | Mật khẩu chỉ yêu cầu độ dài, không yêu cầu độ phức tạp | Người dùng đặt mật khẩu yếu được | Thêm kiểm tra độ mạnh |
| 7 | **Không có HTTPS trong bản đóng gói** | Mật khẩu và token đi qua mạng dạng rõ | Thêm chứng chỉ Let's Encrypt khi có tên miền |

**Giới hạn số 7 là quan trọng nhất** nếu đưa hệ thống ra dùng thật. Bản đóng gói
phục vụ HTTP trần vì nó chạy trên `localhost` khi trình diễn.

---

# KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

## 1. Kết quả đạt được

Đồ án đã xây dựng hoàn chỉnh một website giới thiệu và đặt phòng homestay gồm ba
khối: trang bán hàng cho khách, khu quản trị cho chủ homestay, và REST API với
80 thao tác. Hệ thống chạy được bằng ba lệnh, kèm dữ liệu minh hoạ đủ để trình
diễn mọi trạng thái nghiệp vụ.

**Về mặt kỹ thuật**, kết quả đáng kể nhất không phải số lượng chức năng mà là
**cách giải bài toán chống đặt trùng phòng**: đặt ràng buộc ở tầng cơ sở dữ liệu
bằng `EXCLUDE USING gist` trên kiểu `daterange`, thay vì kiểm tra ở tầng ứng
dụng. Lời giải này được chứng minh bằng kiểm thử đa luồng thật trên PostgreSQL
thật, không phải bằng lập luận.

Cùng cơ chế đó được dùng lần thứ hai cho bài toán khác — chống chồng lấn khoảng
đóng phòng — cho thấy lựa chọn công nghệ không phải để dùng một lần.

**Về mặt quy trình**, đồ án chứng minh giá trị của việc **kiểm chứng bằng bằng
chứng thay vì bằng khẳng định**. Chín lỗi thật được ghi lại ở mục 5.3, trong đó
có những lỗi mà việc biên dịch thành công hoàn toàn không phát hiện được.

## 2. Những điều học được

- Mẫu `check-then-act` ở tầng ứng dụng **không giải được** bài toán tranh chấp.
  Đây là hiểu biết mang tính nguyên lý, áp dụng lại được cho mọi bài toán đặt
  chỗ theo khoảng.
- Hành vi huỷ giao dịch của cơ sở dữ liệu quan hệ ảnh hưởng trực tiếp lên thiết
  kế mã ứng dụng, không phải chi tiết có thể bỏ qua.
- Kiểm thử phải chạy trên hạ tầng thật khi thứ cần kiểm chính là hành vi của hạ
  tầng đó.
- Biên dịch thành công không nói gì về việc giao diện có hiển thị đúng hay không.

## 3. Hướng phát triển

**Ngắn hạn (cần làm trước khi dùng thật):**

1. Bổ sung HTTPS bằng chứng chỉ Let's Encrypt.
2. Thêm nhật ký kiểm toán cho thao tác quản trị.
3. Kết nối tài khoản SePay thật.
4. Thay dữ liệu mẫu bằng dữ liệu thật của homestay.

**Trung hạn:**

5. Chuyển giới hạn tần suất sang Redis để chạy được nhiều bản ứng dụng.
6. Bộ kiểm thử giao diện tự động để chống hồi quy.
7. Bảng giá theo mùa — hiện giá cố định theo loại phòng, chưa có giá cuối tuần
   hay giá lễ tết.
8. Đồng bộ lịch hai chiều với Booking.com và Agoda qua chuẩn iCal, để homestay
   vừa bán trên sàn vừa bán trên web riêng mà không đặt trùng.

**Dài hạn:**

9. Ứng dụng di động dùng lại nguyên REST API hiện có.
10. Mở rộng thành nền tảng nhiều chi nhánh.
11. Gợi ý phòng dựa trên lịch sử đặt của khách.

---

# TÀI LIỆU THAM KHẢO

> **Lưu ý định dạng:** sắp xếp theo thứ tự xuất hiện trong bài hoặc theo bảng
> chữ cái, tuỳ quy định của khoa. Dưới đây theo nhóm cho dễ tra; khi đưa vào báo
> cáo cần đánh số liên tục `[1]`, `[2]`, … và **trích dẫn trong bài**.

**Tài liệu kỹ thuật chính thức**

[1] PostgreSQL Global Development Group, *PostgreSQL 16 Documentation — Range Types*.
    https://www.postgresql.org/docs/16/rangetypes.html

[2] PostgreSQL Global Development Group, *PostgreSQL 16 Documentation — SELECT*.
    https://www.postgresql.org/docs/16/sql-select.html

[3] VMware, *Spring Framework Reference — Rolling Back a Declarative Transaction*.
    https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html

[4] VMware, *Spring Boot Reference Documentation*.

[5] Google, *Angular Documentation — Signals, Standalone Components*.
    https://angular.dev

[6] Tailwind Labs, *Tailwind CSS v4 Documentation*.

[7] Nginx Inc., *Module ngx_http_proxy_module*.
    https://nginx.org/en/docs/http/ngx_http_proxy_module.html

[8] SePay, *Tài liệu tích hợp Webhooks*.
    https://docs.sepay.vn/tich-hop-webhooks.html

**Tiêu chuẩn**

[9] M. Nottingham, E. Wilde, S. Dalal, *RFC 7807 — Problem Details for HTTP APIs*, IETF.

[10] W3C, *Web Content Accessibility Guidelines (WCAG) 2.1*.
     https://www.w3.org/TR/WCAG21/

[11] OWASP Foundation, *OWASP Top Ten*.
     https://owasp.org/www-project-top-ten/

[12] OWASP Foundation, *CSV Injection*.
     https://owasp.org/www-community/attacks/CSV_Injection

**Thiết kế và trải nghiệm người dùng**

[13] Baymard Institute, *Travel Accommodations 2024 Benchmark*.
     https://baymard.com/blog/travel-accommodations-2024-benchmark

[14] Snappymob, *UI/UX Audit: Booking.com vs Agoda*.
     https://blog.snappymob.com/ui-ux-audit-booking-com-vs-agoda

**Sách tham khảo (gợi ý bổ sung — đọc rồi mới đưa vào)**

[15] M. Fowler, *Patterns of Enterprise Application Architecture*, Addison-Wesley.

[16] R. C. Martin, *Clean Architecture*, Prentice Hall.

[17] M. Kleppmann, *Designing Data-Intensive Applications*, O'Reilly Media.

> ⚠️ **Cảnh báo:** đừng liệt kê sách chưa đọc. Hội đồng có thể hỏi "trong cuốn
> [17] tác giả nói gì về giao dịch?".

---

# PHỤ LỤC

## Phụ lục A — Mẫu lời cảm ơn và lời cam đoan

### A.1. Lời cảm ơn

> Sửa lại cho đúng tên thầy/cô và cảm xúc của bạn. Đừng dùng nguyên văn.

*Em xin gửi lời cảm ơn chân thành đến Thầy/Cô [HỌ TÊN GVHD] đã tận tình hướng
dẫn, góp ý và tạo điều kiện cho em hoàn thành đồ án tốt nghiệp này. Những nhận
xét thẳng thắn của Thầy/Cô trong các buổi báo cáo tiến độ đã giúp em nhận ra
nhiều thiếu sót mà bản thân không tự thấy được.*

*Em cũng xin cảm ơn quý Thầy/Cô Khoa Công nghệ Thông tin — Trường Đại học Trà
Vinh đã truyền đạt kiến thức nền tảng trong suốt quá trình học tập. Đặc biệt,
những kiến thức về cơ sở dữ liệu và hệ thống phân tán đã trực tiếp giúp em giải
quyết bài toán cốt lõi của đề tài này.*

*Cuối cùng, em xin cảm ơn gia đình và bạn bè đã luôn ủng hộ em.*

*Do thời gian và kiến thức còn hạn chế, đồ án chắc chắn còn nhiều thiếu sót. Em
rất mong nhận được ý kiến đóng góp của quý Thầy/Cô.*

*Em xin chân thành cảm ơn!*

*Trà Vinh, ngày ... tháng ... năm 20...*
*Sinh viên thực hiện*

### A.2. Lời cam đoan

> **Nhiều khoa bắt buộc có trang này.** Nó là cam kết về tính trung thực của
> công trình — đọc kỹ trước khi ký tên, đừng chép máy móc.

*Em xin cam đoan đồ án tốt nghiệp **"Xây dựng website giới thiệu và đặt phòng
homestay — Homestay TVH"** là công trình do chính em thực hiện dưới sự hướng dẫn
của Thầy/Cô [HỌ TÊN GVHD].*

*Toàn bộ mã nguồn của hệ thống do em tự viết. Các thư viện, framework và công cụ
mã nguồn mở được sử dụng đều được nêu rõ trong Chương 2 và trong danh mục tài
liệu tham khảo. Số liệu và kết quả kiểm thử trình bày trong đồ án được lấy trực
tiếp từ hệ thống đang chạy, trung thực và chưa từng được công bố trong bất kỳ
công trình nào khác.*

*Những nội dung tham khảo từ tài liệu bên ngoài đều được trích dẫn đầy đủ nguồn
gốc theo đúng quy định.*

*Em xin chịu hoàn toàn trách nhiệm về lời cam đoan này.*

*Trà Vinh, ngày ... tháng ... năm 20...*
*Sinh viên thực hiện*
*(Ký và ghi rõ họ tên)*

> ⚠️ **Nếu bạn có dùng công cụ hỗ trợ (kể cả AI) trong quá trình làm, hãy hỏi
> GVHD xem khoa yêu cầu khai báo thế nào.** Quy định về việc này khác nhau giữa
> các trường và đang thay đổi nhanh. Cam đoan sai sự thật là rủi ro lớn hơn
> nhiều so với việc khai báo trung thực.

## Phụ lục B — Danh mục từ viết tắt

| Từ viết tắt | Tiếng Anh đầy đủ | Nghĩa tiếng Việt |
|---|---|---|
| ACID | Atomicity, Consistency, Isolation, Durability | Bốn tính chất của giao dịch cơ sở dữ liệu |
| API | Application Programming Interface | Giao diện lập trình ứng dụng |
| CMS | Content Management System | Hệ quản trị nội dung |
| CRUD | Create, Read, Update, Delete | Bốn thao tác dữ liệu cơ bản |
| CSP | Content Security Policy | Chính sách bảo mật nội dung |
| CSRF | Cross-Site Request Forgery | Giả mạo yêu cầu liên trang |
| CSS | Cascading Style Sheets | Ngôn ngữ định kiểu trang web |
| CSV | Comma-Separated Values | Định dạng tệp phân cách bằng dấu phẩy |
| DTO | Data Transfer Object | Đối tượng truyền dữ liệu giữa các tầng |
| ERD | Entity Relationship Diagram | Sơ đồ quan hệ thực thể |
| GiST | Generalized Search Tree | Loại chỉ mục tổng quát của PostgreSQL |
| HTML | HyperText Markup Language | Ngôn ngữ đánh dấu siêu văn bản |
| HTTP/HTTPS | HyperText Transfer Protocol (Secure) | Giao thức truyền siêu văn bản (bảo mật) |
| IDE | Integrated Development Environment | Môi trường phát triển tích hợp |
| JDBC | Java Database Connectivity | Giao diện kết nối cơ sở dữ liệu của Java |
| JPA | Jakarta Persistence API | Chuẩn ánh xạ đối tượng — quan hệ của Java |
| JSON | JavaScript Object Notation | Định dạng trao đổi dữ liệu |
| JVM | Java Virtual Machine | Máy ảo Java |
| JWT | JSON Web Token | Chuẩn token xác thực |
| LTS | Long-Term Support | Phiên bản hỗ trợ dài hạn |
| ORM | Object-Relational Mapping | Ánh xạ đối tượng — quan hệ |
| QR | Quick Response (code) | Mã phản hồi nhanh |
| RBAC | Role-Based Access Control | Kiểm soát truy cập theo vai trò |
| REST | REpresentational State Transfer | Kiểu kiến trúc API |
| RFC | Request For Comments | Tài liệu tiêu chuẩn của IETF |
| SMTP | Simple Mail Transfer Protocol | Giao thức gửi thư |
| SQL | Structured Query Language | Ngôn ngữ truy vấn có cấu trúc |
| UI/UX | User Interface / User Experience | Giao diện / Trải nghiệm người dùng |
| UML | Unified Modeling Language | Ngôn ngữ mô hình hoá thống nhất |
| UUID | Universally Unique Identifier | Định danh duy nhất toàn cục |
| WCAG | Web Content Accessibility Guidelines | Hướng dẫn khả năng tiếp cận nội dung web |
| XSS | Cross-Site Scripting | Tấn công chèn mã kịch bản |
| 3NF | Third Normal Form | Dạng chuẩn 3 |

## Phụ lục C — Nội dung nên đưa vào phụ lục kỹ thuật

> **Nguyên tắc:** phụ lục **không được dài hơn phần chính**. Chỉ đưa vào những
> đoạn mã thật sự minh hoạ cho lập luận trong bài, không chép cả dự án.

| Phụ lục | Nội dung | Nguồn trong kho mã |
|---|---|---|
| C.1 | Mã nguồn migration tạo ràng buộc chống trùng lịch | `backend/.../db/migration/V3__bookings_and_exclusion.sql` |
| C.2 | Mã nguồn migration bảng khoảng đóng phòng | `backend/.../db/migration/V8__room_closures.sql` |
| C.3 | Bốn truy vấn phòng trống | `backend/.../availability/AvailabilityRepository.java` |
| C.4 | Vòng thử gán phòng và xử lý `23P01` | `backend/.../booking/RoomAllocator.java` |
| C.5 | Ca kiểm thử tranh chấp đa luồng | `backend/.../booking/BookingConcurrencyIT.java` |
| C.6 | Bảng đầy đủ 80 endpoint | `docs/api.md` |
| C.7 | Tệp `docker-compose.yml` | Gốc kho mã |
| C.8 | Cấu hình nginx | `frontend/nginx.conf` |

---

## Phụ lục D — ⭐ DANH SÁCH ĐẦY ĐỦ HÌNH ẢNH CẦN CHUẨN BỊ

> **Đây là danh sách việc cần làm của bạn.** Tổng cộng **40 hình**. Cột "Ưu
> tiên" cho biết hình nào bắt buộc phải có (⭐⭐⭐), nên có (⭐⭐), có thì tốt (⭐).

### Nhóm 1 — Ảnh chụp màn hình hệ thống (15 hình)

*Cách làm chung: chạy `docker compose up -d`, mở trình duyệt, chụp bằng phím
PrtSc hoặc công cụ Snipping Tool. **Chụp ở độ phân giải cao** và cắt gọn, đừng
để lẫn thanh tác vụ của hệ điều hành.*

| Hình | Nội dung | Ưu tiên | Ghi chú |
|---|---|:---:|---|
| 4.3 | Trang chủ + thanh tìm phòng | ⭐⭐⭐ | Đã có sẵn `docs/images/landing.png` |
| 4.4 | Kết quả tìm phòng | ⭐⭐⭐ | Phải thấy "còn N phòng" và tổng tiền cả kỳ |
| 4.5 | Trang chi tiết loại phòng | ⭐⭐⭐ | Thấy thư viện ảnh + tiện nghi theo nhóm |
| 4.6 | Luồng đặt phòng 3 bước | ⭐⭐⭐ | Ghép 3 ảnh thành 1 hình |
| 4.7 | Màn hình thanh toán QR | ⭐⭐⭐ | Thấy QR + đồng hồ đếm ngược |
| 4.8 | Dashboard quản trị | ⭐⭐⭐ | Đã có sẵn `docs/images/admin-dashboard.png` |
| 4.9 | Màn hình đối soát thanh toán | ⭐⭐⭐ | Đã có sẵn `docs/images/admin-payments.png` |
| 4.10 | Danh sách đơn đặt phòng | ⭐⭐⭐ | Thấy nhiều trạng thái khác nhau |
| 4.11 | Chi tiết đơn | ⭐⭐ | Thấy dòng thời gian lịch sử trạng thái |
| 4.12 | Màn hình CMS | ⭐⭐ | |
| 4.13 | Buộc đổi mật khẩu lần đầu | ⭐⭐ | |
| 4.15 | Hộp thư Mailpit | ⭐⭐ | Mở `localhost:8025` |
| 3.2 | Màn hình quản lý ngày khả dụng | ⭐⭐⭐ | Thấy dòng "N đêm: … (mở lại từ …)" |
| 3.10 | Swagger UI | ⭐⭐ | `localhost/swagger-ui/index.html` |
| 3.11 | Trang `/ui-kit` | ⭐⭐ | Chỉ có ở profile `demo` |

### Nhóm 2 — Sơ đồ UML và kiến trúc (11 hình)

*Cách làm chung: hầu hết đã có sẵn dưới dạng mã Mermaid trong thư mục `docs/`.
Chép đoạn mã, dán vào **https://mermaid.live**, chỉnh cho đẹp rồi bấm "Export
PNG" ở độ phân giải cao. Nếu khoa yêu cầu **đúng ký pháp UML chuẩn** (đặc biệt
là biểu đồ use case), nên vẽ lại bằng **draw.io** hoặc **StarUML**.*

| Hình | Nội dung | Ưu tiên | Nguồn |
|---|---|:---:|---|
| 3.1 | Biểu đồ use case tổng quát | ⭐⭐⭐ | `docs/use-case.md` |
| 3.3 | ERD tổng thể 21 bảng | ⭐⭐⭐ | `docs/erd.md` hoặc xuất từ DBeaver |
| 3.4 | ERD nhóm đặt phòng (chi tiết) | ⭐⭐⭐ | Vẽ lại, nhấn mạnh cột `stay` + ràng buộc |
| 3.5 | Biểu đồ trạng thái đơn (8 trạng thái) | ⭐⭐⭐ | `docs/luong-dat-phong.md` |
| 3.6 | Biểu đồ tuần tự: đặt phòng | ⭐⭐⭐ | `docs/luong-dat-phong.md` |
| 3.7 | Biểu đồ tuần tự: thanh toán + webhook | ⭐⭐⭐ | `docs/luong-dat-phong.md` |
| 3.8 | Biểu đồ tuần tự: tiền về muộn | ⭐⭐ | `docs/luong-dat-phong.md` |
| 3.9 | Sơ đồ phân lớp backend | ⭐⭐ | `docs/kien-truc.md` |
| 2.1 | Kiến trúc tổng thể (4 dịch vụ Docker) | ⭐⭐⭐ | `docs/kien-truc.md` |
| 3.12 | Sitemap trang khách | ⭐⭐ | Tự vẽ bằng draw.io |
| 3.13 | Sitemap khu quản trị | ⭐ | Tự vẽ |

### Nhóm 3 — Sơ đồ tự vẽ minh hoạ lý thuyết (5 hình)

*Đây là nhóm **khó nhất nhưng làm điểm cao nhất**, vì nó chứng tỏ bạn hiểu chứ
không chỉ làm theo. Vẽ bằng draw.io hoặc PowerPoint.*

| Hình | Nội dung | Ưu tiên | Gợi ý vẽ |
|---|---|:---:|---|
| 1.1 | Minh hoạ khoảng nửa mở `[)` | ⭐⭐⭐ | Hai thanh trên trục thời gian, chạm nhau nhưng không chồng |
| 1.2 | Quy trình đặt phòng thủ công hiện tại | ⭐⭐⭐ | Lưu đồ, tô đỏ 3 điểm rủi ro |
| 2.3 | Minh hoạ tranh chấp đặt phòng | ⭐⭐⭐ | Hai cột song song: `check-then-act` (đỏ) vs `EXCLUDE` (xanh) |
| 4.1 | Sơ đồ phụ thuộc 10 giai đoạn | ⭐⭐ | `plans/260907-1304-homestay-tvh-booking/plan.md` |
| 4.2 | Biểu đồ Gantt tiến độ | ⭐⭐ | Mermaid `gantt` hoặc Excel |

### Nhóm 4 — Ảnh chụp kết quả kiểm thử và đo đạc (5 hình)

| Hình | Nội dung | Ưu tiên | Cách lấy |
|---|---|:---:|---|
| 5.1 | Kết quả `./mvnw verify` — 129 test xanh | ⭐⭐⭐ | Chụp terminal. **Bắt buộc phải có.** |
| 5.3 | Kết quả quét khả năng tiếp cận (axe) | ⭐⭐ | Tiện ích axe DevTools hoặc Lighthouse |
| 5.4 | Giao diện trên 3 kích thước màn hình | ⭐⭐⭐ | Chế độ giả lập thiết bị của DevTools |
| 5.2 | So sánh trước/sau lỗi tương phản nút | ⭐ | Nếu còn giữ ảnh cũ; không có thì bỏ |
| 4.14 | `docker compose ps` — 4 dịch vụ healthy | ⭐⭐ | Chụp terminal |

### Nhóm 5 — Ảnh tham khảo từ bên ngoài (3 hình)

*⚠️ **Bắt buộc ghi rõ nguồn và ngày chụp** dưới mỗi hình. Đây là ảnh của bên
thứ ba, dùng với mục đích phân tích học thuật.*

| Hình | Nội dung | Ưu tiên |
|---|---|:---:|
| 1.3 | Trang kết quả tìm kiếm Booking.com (có khoanh vùng đánh số) | ⭐⭐ |
| 1.4 | Lịch chọn ngày của Agoda (có khoanh vùng) | ⭐⭐ |
| 0.1 | Trang bìa theo mẫu khoa (kèm logo trường nếu được phép) | ⭐⭐⭐ |

### Tổng kết công việc chuẩn bị hình

| Nhóm | Số hình | Ước lượng thời gian |
|---|---:|---|
| Ảnh chụp màn hình hệ thống | 15 | ~2 giờ (3 ảnh đã có sẵn) |
| Sơ đồ UML và kiến trúc | 11 | ~3 giờ (hầu hết đã có mã Mermaid) |
| Sơ đồ tự vẽ minh hoạ lý thuyết | 5 | ~3 giờ (khó nhất) |
| Kết quả kiểm thử và đo đạc | 5 | ~1 giờ |
| Ảnh tham khảo bên ngoài | 3 | ~30 phút |
| **Tổng** | **40** | **~9–10 giờ** |

---

## Phụ lục E — Việc cần làm trước khi nộp

- [ ] Xin **mẫu trình bày chính thức** của Khoa CNTT — ĐH Trà Vinh và định dạng
      lại toàn bộ tài liệu theo đúng mẫu đó
- [ ] Đối chiếu cấu trúc 5 chương với mẫu của khoa; sắp xếp lại nếu khoa chia khác
- [ ] Chuẩn bị đủ **40 hình** theo Phụ lục D
- [ ] Đánh số và viết chú thích cho **mọi** hình và bảng
- [ ] Tạo mục lục tự động (đừng gõ tay)
- [ ] Tạo danh mục hình và danh mục bảng tự động
- [ ] Thêm **trích dẫn trong bài** cho mọi tài liệu tham khảo (`[1]`, `[2]`, …)
- [ ] **Chạy lại đủ ba lệnh triển khai trên máy có mạng bình thường** và chụp
      ảnh kết quả (xem mục 5.6.3)
- [ ] Chạy lại `./mvnw verify` và chụp ảnh kết quả mới nhất
- [ ] Kiểm tra lại mọi con số trong bài (21 bảng, 80 thao tác, 129 test) nếu có
      sửa thêm mã sau ngày viết tài liệu này
- [ ] Đọc lại toàn bộ để loại câu chữ mang giọng tài liệu kỹ thuật nội bộ
- [ ] Nhờ một người ngoài ngành đọc phần Mở đầu — nếu họ không hiểu bài toán,
      viết lại
- [ ] Chuẩn bị slide bảo vệ (khoảng 15–20 slide)
- [ ] **Tập trả lời ba câu gần như chắc chắn bị hỏi** (xem Phụ lục F)

## Phụ lục F — Ba câu hỏi gần như chắc chắn bị hỏi

**Câu 1: "Vì sao chọn PostgreSQL mà không phải MySQL?"**

Không trả lời "vì PostgreSQL mạnh hơn". Trả lời bằng tính năng cụ thể: bài toán
cốt lõi cần kiểu `daterange` và ràng buộc `EXCLUDE USING gist` — MySQL không có
cả hai. Không có chúng thì phải chống trùng lịch ở tầng ứng dụng, mà cách đó có
khe hở tranh chấp không bịt được bằng logic thông thường. Dẫn chứng: ca kiểm thử
đa luồng.

**Câu 2: "Nếu hai khách đặt cùng lúc thì sao?"**

Đây là câu hỏi trọng tâm. Trả lời theo ba bước: (1) mô tả khe hở của mẫu
`check-then-act`; (2) giải thích PostgreSQL bác một trong hai giao dịch ngay
trong động cơ lưu trữ bằng mã `23P01`; (3) nói rõ hệ thống bắt đúng mã đó và thử
phòng tiếp theo **trong một giao dịch mới**, vì giao dịch cũ đã hỏng. Nếu được,
mở Hình 2.3 ra chỉ.

**Câu 3: "Hệ thống còn thiếu gì? Điểm yếu ở đâu?"**

Đừng nói "em nghĩ là đã đầy đủ". Mở thẳng mục 5.7 và trình bày bảy giới hạn đã
biết, nhấn mạnh giới hạn số 7 (chưa có HTTPS) là quan trọng nhất nếu đưa ra dùng
thật. Biết rõ điểm yếu của hệ thống mình làm là dấu hiệu của người hiểu nó.

---

*Tài liệu này được sinh từ hệ thống Homestay TVH tại thời điểm hoàn tất Phase 10.
Mọi số liệu lấy từ hệ thống đang chạy. Nếu mã nguồn thay đổi sau ngày này, cần
đếm và kiểm chứng lại.*
