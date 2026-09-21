# ĐỀ CƯƠNG ĐỒ ÁN THỰC TẬP CHUYÊN NGÀNH

**Trường Đại học Trà Vinh — Khoa Kỹ thuật và Công nghệ**

| | |
|---|---|
| **Họ và tên sinh viên** | . . . . . . . . . . . . . . . . . . . . . . . . . . . |
| **Mã số sinh viên** | . . . . . . . . . . . . . . . |
| **Lớp** | . . . . . . . . . . . . . . . |
| **Ngành** | Công nghệ thông tin |
| **Giảng viên hướng dẫn** | . . . . . . . . . . . . . . . . . . . . . . . . . . . |
| **Thời gian thực hiện** | . . . / . . . / 20 . . .  →  . . . / . . . / 20 . . . |

---

## 1. TÊN ĐỀ TÀI

> **Xây dựng website giới thiệu và đặt phòng trực tuyến cho Homestay TVH tại
> Trà Vinh, có cơ chế chống đặt trùng phòng ở tầng cơ sở dữ liệu**

Tên đề tài nêu đủ bốn thành phần:

| Thành phần | Nội dung |
|---|---|
| **Vấn đề nghiên cứu** | Chống đặt trùng phòng khi nhiều khách đặt đồng thời |
| **Sản phẩm** | Website giới thiệu và đặt phòng trực tuyến |
| **Đối tượng áp dụng** | Homestay TVH — cơ sở lưu trú quy mô nhỏ do hộ gia đình vận hành |
| **Phạm vi không gian** | Tỉnh Trà Vinh |

---

## 2. LÝ DO CHỌN ĐỀ TÀI

### 2.1. Tính cấp thiết về thực tiễn

Du lịch homestay tại Đồng bằng sông Cửu Long nói chung và Trà Vinh nói riêng
đang tăng nhanh cùng làn sóng du lịch trải nghiệm và du lịch cộng đồng. Khác với
khách sạn, homestay thường do hộ gia đình tự vận hành, quy mô từ vài phòng tới
vài chục phòng, và không có bộ phận lễ tân trực suốt ngày đêm.

Khảo sát thực tế vận hành của nhóm cơ sở lưu trú này cho thấy **ba vấn đề**:

1. **Kênh đặt phòng phân mảnh.** Chủ homestay nhận đặt phòng qua nhiều kênh tin
   nhắn khác nhau và đôi khi qua sàn trung gian. Mỗi kênh là một danh sách riêng
   trong trí nhớ hoặc trên một cuốn sổ. Không có nơi nào cho biết một ngày cụ thể
   còn bao nhiêu phòng ngoài việc tự nhớ lại.

2. **Rủi ro đặt trùng phòng là rủi ro thường trực.** Khi hai khách liên hệ gần
   như cùng lúc và chủ nhà trả lời "còn phòng" cho cả hai, sai sót chỉ lộ ra vào
   ngày nhận phòng — lúc không còn cách khắc phục nào ngoài việc từ chối một
   khách đã đi đường xa tới nơi. Đây không phải lỗi bất cẩn mà là **hệ quả tất
   yếu** của việc quản lý lịch phòng bằng trí nhớ con người.

3. **Chi phí hoa hồng của sàn trung gian.** Các sàn đặt phòng trực tuyến thu
   khoảng 15–20% giá trị mỗi đơn. Với homestay quy mô nhỏ, đó là phần lớn biên
   lợi nhuận. Nhưng rời sàn thì mất luôn kênh tiếp cận khách.

Một website đặt phòng riêng giải quyết cả ba: gom mọi đơn về một nơi, để **cơ sở
dữ liệu** chứ không phải trí nhớ con người bảo đảm không bán trùng phòng, và giữ
lại toàn bộ doanh thu.

### 2.2. Ý nghĩa khoa học

Đề tài không dừng ở mức "làm một trang web quản lý". Bài toán cốt lõi —
**không được bán trùng một phòng khi có nhiều yêu cầu đồng thời** — là một bài
toán tranh chấp dữ liệu kinh điển, và là bài toán **không giải đúng được** bằng
cách kiểm tra thông thường ở tầng ứng dụng.

Mẫu quen thuộc *kiểm-tra-rồi-ghi* (đọc "còn phòng không?", nếu còn thì ghi) để
lại một khe hở giữa lúc đọc và lúc ghi. Hai luồng cùng đọc thấy "còn", cùng ghi,
và phòng bị bán hai lần. Mức cô lập giao dịch mặc định của hầu hết hệ quản trị
cơ sở dữ liệu **không đóng được** khe hở này.

Giải quyết đúng đòi hỏi hiểu biết về giao dịch, mức cô lập và **ràng buộc toàn
vẹn ở tầng lưu trữ** — những nội dung cốt lõi của ngành mà một đề tài quản lý
thông thường (thêm, sửa, xoá, tìm kiếm) không chạm tới. Đây là lý do học thuật
để chọn đề tài này thay vì một đề tài quản lý khác.

---

## 3. MỤC TIÊU VÀ CÂU HỎI NGHIÊN CỨU

### 3.1. Mục tiêu tổng quát

Xây dựng một website **hoạt động được**, có dữ liệu minh hoạ, giải quyết đúng
bài toán giới thiệu và đặt phòng homestay, trong đó tính đúng đắn của việc chống
đặt trùng phòng được **chứng minh bằng kiểm thử** chứ không phải bằng lập luận.

### 3.2. Mục tiêu cụ thể và tiêu chí nghiệm thu

Mỗi mục tiêu đi kèm **một tiêu chí đo được**, không phải một lời hứa:

| # | Mục tiêu cụ thể | Tiêu chí nghiệm thu |
|---|---|---|
| 1 | Khách tìm được phòng trống theo ngày và đặt phòng thành công | Đặt được một đơn từ trang chủ tới lúc có mã QR thanh toán |
| 2 | Hệ thống không bao giờ bán trùng một phòng | Ca kiểm thử đa luồng: nhiều giao dịch cùng đặt một phòng, **đúng một** luồng thắng |
| 3 | Đặt cọc qua mã QR, webhook ngân hàng tự xác nhận đơn | Đơn tự chuyển trạng thái khi tiền về đủ, không cần thao tác tay |
| 4 | Không có nhánh nào để tiền của khách biến mất im lặng | Mọi khoản không khớp đều vào hàng đợi đối soát thủ công |
| 5 | Quản trị viên quản lý được phòng, giá, số khách và **ngày khả dụng** | Đóng phòng theo khoảng ngày, hệ thống tự mở lại đúng hạn |
| 6 | Giao diện đạt chuẩn một trang đặt phòng thương mại | Không còn lỗi vi phạm WCAG 2.1 mức A và AA khi quét tự động |
| 7 | Một lệnh là đủ để chạy toàn hệ thống kèm dữ liệu mẫu | Ba lệnh từ lúc sao chép mã nguồn tới lúc hệ thống chạy được |
| 8 | Có tài liệu kỹ thuật để người khác tiếp nhận được dự án | Bộ tài liệu kỹ thuật đối chiếu được với hệ thống đang chạy |

### 3.3. Câu hỏi nghiên cứu

Đồ án trả lời **bốn câu hỏi**, xếp theo thứ tự phụ thuộc:

> **CH1.** Vì sao việc kiểm tra "còn phòng hay không" ở tầng ứng dụng **không
> đủ** để ngăn đặt trùng phòng, kể cả khi đã dùng giao dịch?

> **CH2.** Cơ chế nào ở **tầng cơ sở dữ liệu** bảo đảm được tính đúng đắn đó, và
> vì sao cơ chế đó đúng ngay cả khi nhiều yêu cầu tới cùng lúc?

> **CH3.** Khi cơ sở dữ liệu **từ chối** một giao dịch vì trùng lịch, tầng ứng
> dụng phải xử lý thế nào để khách vẫn đặt được phòng khác thay vì nhận lỗi?

> **CH4.** Trong luồng thanh toán không đồng bộ (khách chuyển khoản, ngân hàng
> gọi lại sau), làm thế nào để **không có nhánh nào** khiến tiền của khách biến
> mất mà không ai biết?

Bốn câu hỏi này là trục xuyên suốt: Chương 2 trả lời CH1 và CH2 về mặt lý thuyết,
Chương 3 trả lời CH3 và CH4 bằng thiết kế và cài đặt, Chương 4 chứng minh cả bốn
bằng kết quả kiểm thử.

---

## 4. ĐỐI TƯỢNG VÀ PHẠM VI

### 4.1. Đối tượng nghiên cứu

| | |
|---|---|
| **Khách thể** | Cơ sở lưu trú quy mô nhỏ do hộ gia đình vận hành (homestay) |
| **Đối tượng nghiên cứu** | Quy trình đặt phòng trực tuyến và **cơ chế bảo đảm tính đúng đắn của việc giữ chỗ** khi có nhiều yêu cầu đồng thời |
| **Đối tượng khảo sát** | Homestay TVH (Trà Vinh) và các trang đặt phòng thương mại đang hoạt động |

### 4.2. Phạm vi nghiên cứu

| Chiều | Giới hạn |
|---|---|
| **Không gian** | Một cơ sở lưu trú tại Trà Vinh. Hệ thống phục vụ **một** homestay, không phải sàn nhiều cơ sở |
| **Thời gian** | . . . tuần, từ . . . / . . . / 20 . . . đến . . . / . . . / 20 . . . |
| **Nội dung** | Ba khối: trang bán hàng cho khách, khu quản trị cho chủ homestay, giao diện lập trình ứng dụng dùng chung |
| **Dữ liệu** | Dữ liệu minh hoạ do đề tài tự sinh, phản ánh đủ mọi trạng thái đơn. Không sử dụng dữ liệu khách hàng thật |

### 4.3. Giới hạn — những gì đề tài KHÔNG làm

Nêu rõ giới hạn ngay từ đề cương để tránh hiểu nhầm phạm vi khi nghiệm thu:

- **Không** xây dựng ứng dụng di động riêng. Giao diện web đáp ứng được trên
  điện thoại; ứng dụng di động là hướng phát triển.
- **Không** tích hợp cổng thanh toán quốc tế. Đề tài dùng hình thức chuyển khoản
  trong nước qua mã QR và webhook xác nhận.
- **Không** làm bảng giá theo từng đêm. Giá theo loại phòng; bảng giá theo mùa
  là hướng phát triển.
- **Không** đồng bộ lịch hai chiều với sàn trung gian.
- **Không** tự động hoàn tiền. Khoản thừa được ghi nhận và đưa vào hàng đợi để
  người xử lý, vì hoàn tiền tự động vượt quá phạm vi một đồ án.

---

## 5. NHIỆM VỤ NGHIÊN CỨU

Để đạt tám mục tiêu ở mục 3.2, đề tài thực hiện **sáu nhiệm vụ**:

1. **Khảo sát hiện trạng** quy trình đặt phòng thủ công của homestay và **phân
   tích các trang đặt phòng thương mại** để rút ra mẫu thiết kế nên áp dụng và
   mẫu cố ý không áp dụng.
2. **Nghiên cứu cơ sở lý thuyết** về giao dịch, mức cô lập, kiểu dữ liệu khoảng
   và ràng buộc loại trừ trong hệ quản trị cơ sở dữ liệu quan hệ.
3. **Phân tích và thiết kế hệ thống**: biểu đồ use case, lược đồ quan hệ thực
   thể, biểu đồ lớp, biểu đồ trạng thái, biểu đồ tuần tự, kiến trúc phân lớp,
   thiết kế giao diện lập trình ứng dụng và giao diện người dùng.
4. **Cài đặt hệ thống** theo thiết kế, gồm cả cơ chế chống đặt trùng phòng và
   luồng thanh toán không đồng bộ.
5. **Kiểm thử**, trọng tâm là **kiểm thử đa luồng** trên cơ sở dữ liệu thật để
   chứng minh mục tiêu số 2, cùng kiểm thử phân quyền và khả năng tiếp cận.
6. **Đóng gói và viết tài liệu** để hệ thống chạy lại được bằng vài lệnh và
   người khác tiếp nhận được dự án.

---

## 6. PHƯƠNG PHÁP NGHIÊN CỨU

### 6.1. Phương pháp nghiên cứu lý thuyết

- **Nghiên cứu tài liệu:** đọc tài liệu chính thức của hệ quản trị cơ sở dữ
  liệu về mức cô lập giao dịch và ràng buộc toàn vẹn; đọc tiêu chuẩn về khả năng
  tiếp cận nội dung web và về định dạng phản hồi lỗi của dịch vụ web.
- **Phân tích — tổng hợp:** đối chiếu các cách chống tranh chấp dữ liệu (khoá
  bi quan, khoá lạc quan, ràng buộc ở tầng lưu trữ) để chọn cách phù hợp với bài
  toán đặt phòng theo khoảng ngày.

### 6.2. Phương pháp nghiên cứu thực tiễn

- **Khảo sát:** quan sát quy trình đặt phòng thủ công hiện tại và ghi lại các
  điểm phát sinh rủi ro.
- **Phân tích hệ thống tương tự:** khảo sát trang đặt phòng thương mại đang hoạt
  động để rút ra yêu cầu về trải nghiệm đặt phòng.
- **Thực nghiệm:** dựng hệ thống chạy thật rồi đo kết quả, thay vì chỉ lập luận.

### 6.3. Phương pháp kiểm chứng

Đây là phần quyết định chất lượng đề tài, nên nêu cụ thể:

| Điều cần chứng minh | Cách chứng minh |
|---|---|
| Không bán trùng phòng | **Kiểm thử đa luồng** trên cơ sở dữ liệu thật (không giả lập bằng cơ sở dữ liệu trong bộ nhớ, vì ràng buộc cần kiểm lại không tồn tại ở đó): cho nhiều giao dịch cùng đặt một phòng và kiểm tra đúng một luồng thắng |
| Mặc định từ chối mọi endpoint chưa khai quyền | Kiểm thử **ma trận phân quyền** duyệt toàn bộ tiền tố đường dẫn |
| Tiền không biến mất im lặng | Kiểm thử **cửa sổ tranh chấp** của webhook: tiền về muộn, tiền thiếu, tiền thừa, webhook gửi lại |
| Đạt chuẩn khả năng tiếp cận | **Quét tự động** bằng công cụ kiểm tra khả năng tiếp cận trên toàn bộ màn hình |
| Triển khai lại được | Chạy lại quy trình triển khai từ đầu trên máy sạch |

### 6.4. Công nghệ và công cụ dự kiến

| Tầng | Công nghệ | Lý do chọn |
|---|---|---|
| **Cơ sở dữ liệu** | PostgreSQL | **Lý do quyết định:** có kiểu dữ liệu khoảng ngày và ràng buộc loại trừ — hai thứ mà bài toán cốt lõi cần và các hệ quản trị phổ biến khác không có |
| **Máy chủ ứng dụng** | Java + Spring Boot | Hệ sinh thái trưởng thành cho giao dịch cơ sở dữ liệu và bảo mật |
| **Giao diện** | Angular + Tailwind CSS | Kiến trúc tách rời, dùng lại được giao diện lập trình ứng dụng nếu sau này làm ứng dụng di động |
| **Quản lý lược đồ** | Công cụ migration | Lược đồ có lịch sử, tái lập được, không sửa bằng tay |
| **Kiểm thử** | Kiểm thử tích hợp trên cơ sở dữ liệu thật dựng trong container | Ràng buộc cần kiểm là tính năng của hệ quản trị, giả lập thì không kiểm được |
| **Đóng gói** | Docker Compose | Chạy toàn hệ thống bằng một lệnh, không phụ thuộc máy người chấm |
| **Quản lý mã nguồn** | Git + GitHub | Theo yêu cầu mục 4 của quy định khoa |

---

## 7. ĐÓNG GÓP DỰ KIẾN CỦA ĐỀ TÀI

1. **Về thực tiễn:** một website đặt phòng chạy được, đóng gói sẵn, homestay quy
   mô nhỏ có thể dùng ngay mà không phải trả hoa hồng cho sàn trung gian.
2. **Về kỹ thuật:** một lời giải **kiểm chứng được** cho bài toán chống đặt
   trùng phòng, đặt ở tầng cơ sở dữ liệu thay vì tầng ứng dụng, kèm bộ kiểm thử
   đa luồng chứng minh tính đúng đắn.
3. **Về tài liệu:** bộ tài liệu kỹ thuật và báo cáo phân tích rõ **vì sao** cách
   làm thông thường không đủ — phần có giá trị tham khảo cho các đề tài sau gặp
   bài toán tranh chấp dữ liệu tương tự.

---

## 8. KẾT CẤU DỰ KIẾN CỦA ĐỒ ÁN

Theo đúng tên chương quy định trong văn bản *"Một số quy định về hình thức trình
bày thực tập đồ án cơ sở ngành và chuyên ngành"* của Khoa Kỹ thuật và Công nghệ.

**MỞ ĐẦU** — Lí do chọn đề tài · Mục đích nghiên cứu · Đối tượng nghiên cứu ·
Phạm vi nghiên cứu

**CHƯƠNG 1. TỔNG QUAN**
- 1.1. Giới thiệu bài toán — mô tả cơ sở, bài toán cốt lõi không được bán trùng phòng
- 1.2. Khảo sát hiện trạng — quy trình thủ công, khảo sát người dùng
- 1.3. Khảo sát các hệ thống tương tự — so sánh, mẫu áp dụng và mẫu cố ý không áp dụng
- 1.4. Xác định yêu cầu — yêu cầu chức năng và phi chức năng
- 1.5. Phạm vi và giới hạn của hệ thống

**CHƯƠNG 2. NGHIÊN CỨU LÝ THUYẾT**
- 2.1. Kiến trúc ứng dụng web tách rời
- 2.2. Ngôn ngữ và nền tảng tầng máy chủ
- 2.3. Nền tảng tầng giao diện
- **2.4. Cơ sở dữ liệu quan hệ — trọng tâm lý thuyết của đề tài**
  - 2.4.1. Giao dịch và mức cô lập → *trả lời CH1*
  - 2.4.2. Kiểu dữ liệu khoảng và quy ước nửa mở
  - 2.4.3. Ràng buộc loại trừ → *trả lời CH2*
  - 2.4.4. Chỉ mục tổng quát và lý do cần nó
- 2.5. Xác thực và phân quyền
- 2.6. Quản lý lược đồ bằng migration
- 2.7. Thanh toán không đồng bộ và webhook
- 2.8. Kiểm thử tích hợp trên cơ sở dữ liệu thật
- 2.9. Đóng gói bằng container

**CHƯƠNG 3. HIỆN THỰC HÓA NGHIÊN CỨU**
- 3.1. Phân tích chức năng — biểu đồ use case
- 3.2. Thiết kế cơ sở dữ liệu — lược đồ quan hệ thực thể, hai lần dùng ràng buộc loại trừ
- 3.3. Thiết kế lớp và máy trạng thái đơn đặt phòng
- 3.4. Thiết kế kiến trúc phần mềm
- 3.5. Thiết kế giao diện lập trình ứng dụng
- 3.6. Thiết kế giao diện người dùng
- 3.7. Môi trường và quy trình phát triển
- 3.8. Cài đặt các chức năng chính → *trả lời CH3*
- 3.9. Cài đặt mô hình bảo mật
- 3.10. Đóng gói và triển khai

**CHƯƠNG 4. KẾT QUẢ NGHIÊN CỨU**
- 4.1. Chức năng đã hoàn thành
- 4.2. Kết quả kiểm thử tự động
- 4.3. Các lỗi thực tế phát hiện trong quá trình thực hiện
- 4.4. Kiểm thử phi chức năng — khả năng tiếp cận, bảo mật
- 4.5. Đối chiếu với mục đích đã đặt ra → *trả lời CH4*
- 4.6. Những điều chưa kiểm chứng được
- 4.7. Các giới hạn đã biết của hệ thống

**CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN**

**DANH MỤC TÀI LIỆU THAM KHẢO** · **PHỤ LỤC**

> **Độ dài dự kiến:** 30–50 trang phần nội dung (không kể bìa, lời cảm ơn, mục
> lục, tài liệu tham khảo, phụ lục), đúng quy định của khoa.

---

## 9. KẾ HOẠCH VÀ TIẾN ĐỘ THỰC HIỆN

| Tuần | Nội dung công việc | Sản phẩm phải có | Ghi chú |
|:---:|---|---|---|
| **1** | Khảo sát hiện trạng, phân tích hệ thống tương tự, chốt yêu cầu | Danh sách yêu cầu chức năng và phi chức năng | |
| **2** | Nghiên cứu lý thuyết về giao dịch, mức cô lập, ràng buộc loại trừ | Bản ghi chú lý thuyết trả lời CH1, CH2 | Quyết định chọn hệ quản trị CSDL |
| **3** | Thiết kế cơ sở dữ liệu và kiến trúc hệ thống | Lược đồ quan hệ thực thể, biểu đồ use case, biểu đồ lớp | **Mốc: duyệt thiết kế với GVHD** |
| **4** | Cài đặt lược đồ và **ràng buộc chống đặt trùng phòng** | Migration chạy được, ca kiểm thử đa luồng đầu tiên | Mốc quan trọng nhất |
| **5** | Cài đặt xác thực, phân quyền, truy vấn phòng trống | Đăng nhập, ma trận phân quyền | |
| **6** | Cài đặt luồng đặt phòng và vòng thử gán phòng | Đặt được đơn từ đầu tới cuối | Trả lời CH3 |
| **7** | Cài đặt thanh toán, webhook, hàng đợi thư | Đơn tự xác nhận khi tiền về | Trả lời CH4 |
| **8** | Cài đặt khu quản trị và trang bán hàng cho khách | Giao diện hoàn chỉnh hai khối | |
| **9** | Quản lý ngày khả dụng, đóng gói bằng container, dữ liệu mẫu | Chạy toàn hệ thống bằng một lệnh | |
| **10** | Kiểm thử toàn diện, sửa lỗi, đo khả năng tiếp cận | Bộ kiểm thử đạt 100%, không còn lỗi vi phạm | |
| **11** | Viết báo cáo: mở đầu và Chương 1, 2 | Bản thảo hai chương đầu | **Mốc: nộp bản thảo cho GVHD** |
| **12** | Viết báo cáo: Chương 3, 4, 5; chuẩn bị hình minh hoạ | Bản thảo đầy đủ + bộ hình | |
| **13** | Hoàn thiện, xuất Word đúng định dạng, kiểm số trang | Quyển báo cáo hoàn chỉnh | Đếm trang trên bản đã xuất |
| **14** | Làm slide, poster, tập bảo vệ và trả lời phản biện | Slide 15–20 trang, poster, kịch bản nói | **Mốc: nộp** |

> **Cách dùng bảng này:** điền ngày bắt đầu thật của từng tuần vào cột đầu trước
> khi nộp. Ba dòng in đậm là ba mốc cần GVHD duyệt trước khi đi tiếp.
>
> **Ràng buộc từ quy định khoa:** commit lên GitHub **ít nhất một lần mỗi tuần**
> và nộp báo cáo tiến độ hàng tuần vào thư mục `progress-report/`. Lịch sử commit
> là **một tiêu chí chấm điểm tiến độ** — không có commit thì dù có báo cáo tiến
> độ vẫn bị xem như không hoàn thành.

---

## 10. TÀI LIỆU THAM KHẢO DỰ KIẾN

**Tiếng Anh**

1. Tài liệu chính thức của PostgreSQL — chương về **Transaction Isolation**.
2. Tài liệu chính thức của PostgreSQL — chương về **Range Types** và
   **Constraints** (ràng buộc loại trừ).
3. Tài liệu chính thức của PostgreSQL — chương về **Index Types** (chỉ mục tổng quát).
4. Tài liệu chính thức của **Spring Framework** — quản lý giao dịch.
5. Tài liệu chính thức của **Angular**.
6. **RFC 7807** — Problem Details for HTTP APIs.
7. **RFC 7519** — JSON Web Token.
8. **WCAG 2.1** — Web Content Accessibility Guidelines, W3C.
9. **OWASP Top Ten** — rủi ro bảo mật ứng dụng web.
10. Tài liệu chính thức của **Docker Compose** và **Testcontainers**.

**Tiếng Việt**

11. Văn bản *"Một số quy định về hình thức trình bày thực tập đồ án cơ sở ngành
    và chuyên ngành"* — Khoa Kỹ thuật và Công nghệ, Trường Đại học Trà Vinh.
12. Bộ biểu mẫu **BM5** — Khoa Kỹ thuật và Công nghệ, Trường Đại học Trà Vinh.

> Danh mục đầy đủ theo định dạng IEEE, xếp theo thứ tự từ điển và tách riêng
> tiếng Việt với tiếng Anh, sẽ hoàn thiện trong quyển báo cáo.

---

\pagebreak

## 11. Ý KIẾN CỦA GIẢNG VIÊN HƯỚNG DẪN

☐ Đồng ý cho thực hiện    ☐ Cần chỉnh sửa    ☐ Không đồng ý

Nội dung cần chỉnh sửa:

...........................................................................

...........................................................................

...........................................................................

...........................................................................

...........................................................................

...........................................................................

| Sinh viên thực hiện | Giảng viên hướng dẫn |
|:---:|:---:|
| *(Ký và ghi rõ họ tên)* | *(Ký và ghi rõ họ tên)* |
|  |  |

*Trà Vinh, ngày . . . tháng . . . năm 20 . . .*
