# ĐỀ CƯƠNG ĐỒ ÁN THỰC TẬP CHUYÊN NGÀNH

**Đề tài:** Xây dựng website giới thiệu và đặt phòng homestay — Homestay TVH
**Khoa:** Kỹ thuật và Công nghệ — Trường Đại học Trà Vinh

---

> **TÀI LIỆU NÀY LÀ GÌ**
>
> Đây là bản hướng dẫn thi công: nó nói rõ quyển báo cáo phải có gì, mỗi phần
> nằm ở tệp nào, cần chuẩn bị bao nhiêu hình và hình đó thể hiện điều gì, cùng
> danh sách việc còn lại trước khi nộp.
>
> **Nội dung quyển báo cáo nằm ở `docs/bao-cao/`**, đã viết xong và đã theo đúng
> cấu trúc quy định. Tài liệu này không lặp lại nội dung đó.
>
> Mọi quy định trích trong tài liệu này lấy từ hai văn bản chính thức của khoa:
> *"Một số quy định về hình thức trình bày thực tập đồ án cơ sở ngành và chuyên
> ngành"* và bộ biểu mẫu **BM5**.

---

## 1. QUY ĐỊNH TRÌNH BÀY — BẢNG TRA NHANH

| Hạng mục | Quy định | Trạng thái |
|---|---|---|
| **Độ dài nội dung** | **30–50 trang A4.** Không kể bìa, lời cảm ơn, mục lục, tài liệu tham khảo, phụ lục | ⚠️ **đo thật: 52 trang** (từ 66 ban đầu) — còn dư 2 trang, xem mục 5.3 |
| Font | Times New Roman, **13pt** | Áp khi xuất Word |
| Giãn dòng | **1.5 lines** | Áp khi xuất Word |
| Cách đoạn | Before **6pt**, After **6pt** | Áp khi xuất Word |
| Lề | trên 2cm · dưới 2cm · **trái 3cm** · **phải 2cm** | Áp khi xuất Word |
| Số trang | **Góc phải dưới**, bắt đầu đánh từ Chương 1 | Áp khi xuất Word |
| Footer mỗi trang | `GVHD: ...` bên trái · `SVTH: ...` bên phải | Áp khi xuất Word |
| Đánh số chương mục | Hệ thống số **Ả Rập**, không dùng số La Mã. `Chương 3` → `3.1.` → `3.1.1.` | ✅ đã đúng |
| Mục lục | Không quá **04 cấp** tiểu mục. In đậm, in hoa tiêu đề chương và mục lớn | Sinh tự động khi xuất Word |
| Bảng, sơ đồ, hình | Đánh số theo chương. **Cuối mỗi cái phải có ghi chú, nêu rõ nguồn trích hoặc sao chụp** | ⚠️ phải viết chú thích khi chèn |
| Tài liệu tham khảo | **Định dạng IEEE**, xếp theo thứ tự từ điển, tách Tiếng Việt / Tiếng Anh | ✅ đã đúng |
| Bìa | **Bìa cứng, chữ nhũ vàng.** Tên khoa: KHOA KỸ THUẬT VÀ CÔNG NGHỆ | Khi in |
| In ấn | Khổ A4 | Khi in |

> **Ba điểm rất dễ làm sai, ghi ra để không quên:**
>
> 1. **Trần 50 trang là trần thật.** Quy định ghi "không nên vượt quá 50 trang".
>    Viết dài hơn không phải là ưu điểm.
> 2. **Lề phải 2cm, không phải 1.5cm.** Nhiều mẫu của trường khác dùng 1.5cm.
> 3. **Mỗi hình và bảng phải có ghi chú nguồn.** Ảnh chụp từ hệ thống thì ghi
>    "Nguồn: chụp từ hệ thống"; ảnh từ trang web ngoài thì ghi rõ địa chỉ và ngày
>    chụp. Đây là yêu cầu tường minh trong quy định, không phải tuỳ chọn.

---

## 2. CẤU TRÚC QUYỂN BÁO CÁO

### 2.1. Các trang đầu — theo mục 1 "CẤU TRÚC" của quy định

| Thứ tự | Trang | Biểu mẫu | Nằm ở tệp |
|---|---|---|---|
| 1 | Bìa chính | BM5, bìa cứng chữ nhũ vàng | `00-phan-dau.md` mục 1 |
| 2 | Bìa phụ (trang lót) | BM5, giấy thường | `00-phan-dau.md` mục 2 |
| 3 | Nhận xét của cơ quan thực tập | BM5 *(nếu có)* | `00-phan-dau.md` mục 3 |
| 4 | Nhận xét của GVHD — trang tự do | BM5 | `00-phan-dau.md` mục 4 |
| 5 | Bản nhận xét của GVHD — mẫu UBND | BM5 | `00-phan-dau.md` mục 4 |
| 6 | Nhận xét của giảng viên chấm — trang tự do | BM5 | `00-phan-dau.md` mục 5 |
| 7 | Bản nhận xét của cán bộ chấm — mẫu UBND | BM5 | `00-phan-dau.md` mục 5 |
| 8 | **LỜI MỞ ĐẦU** | BM5, xếp sau trang lót | `00-phan-dau.md` mục 6 |
| 9 | **LỜI CẢM ƠN** | BM5, xếp sau lời mở đầu | `00-phan-dau.md` mục 7 |
| 10 | **MỤC LỤC** | Sinh tự động, tối đa 4 cấp | `00-phan-dau.md` mục 8 |
| 11 | **DANH MỤC CÁC BẢNG, SƠ ĐỒ, HÌNH** | BM5, gộp một danh mục | `00-phan-dau.md` mục 9 |
| 12 | **KÍ HIỆU CÁC CỤM TỪ VIẾT TẮT** | BM5, xếp theo bảng chữ cái | `00-phan-dau.md` mục 10 |
| 13 | **TÓM TẮT ĐỒ ÁN** | Quy định mục 2 "BỐ CỤC" | `00-phan-dau.md` mục 11 |

> **Không có Lời cam đoan.** Quy định của khoa không yêu cầu trang này. Bản đề
> cương trước có, và đã gỡ bỏ.

### 2.2. Nội dung — năm chương theo đúng tên quy định

| Phần | Tên theo quy định | Tệp | Số từ | ≈ trang |
|---|---|---|---:|---:|
| — | **MỞ ĐẦU** | `01-mo-dau.md` | 995 | 2,8 |
| 1 | **CHƯƠNG 1. TỔNG QUAN** | `02-chuong-1.md` | 2 054 | 5,9 |
| 2 | **CHƯƠNG 2. NGHIÊN CỨU LÝ THUYẾT** | `03-chuong-2.md` | 2 862 | 8,2 |
| 3 | **CHƯƠNG 3. HIỆN THỰC HÓA NGHIÊN CỨU** | `04a-` + `04b-` | 7 561 | 21,6 |
| 4 | **CHƯƠNG 4. KẾT QUẢ NGHIÊN CỨU** | `05-chuong-4.md` | 2 615 | 7,5 |
| 5 | **CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN** | `06-chuong-5.md` | 582 | 1,7 |
| — | **DANH MỤC TÀI LIỆU THAM KHẢO** | `07-tai-lieu-tham-khao.md` | — | *không tính* |
| — | **PHỤ LỤC** | `08a-` + `08b-` | — | *không tính* |

**Mỗi chương phải chứa gì, theo đúng câu chữ của quy định:**

| Chương | Quy định yêu cầu | Đã đáp ứng bằng |
|---|---|---|
| **MỞ ĐẦU** | Lí do chọn đề tài, mục đích, đối tượng và phạm vi nghiên cứu | 4 mục đúng thứ tự đó |
| **1. TỔNG QUAN** | Giới thiệu tổng quan về vấn đề sẽ tập trung nghiên cứu và giải quyết | Bài toán, khảo sát hiện trạng, so sánh hệ thống tương tự, xác định yêu cầu |
| **2. NGHIÊN CỨU LÝ THUYẾT** | Cơ sở lí thuyết, lí luận, giả thiết khoa học, các công cụ, công nghệ, phần mềm được sử dụng | Kiến trúc tách rời, Java/Spring, Angular, **PostgreSQL: mức cô lập, kiểu khoảng, ràng buộc loại trừ, GiST**, JWT, Flyway, Docker, webhook, Testcontainers |
| **3. HIỆN THỰC HÓA NGHIÊN CỨU** | Mô tả các bước nghiên cứu đã tiến hành, các bản thiết kế, cách thức cài đặt. Đề tài ứng dụng phải có **hồ sơ thiết kế, cài đặt theo các dạng lược đồ, mô hình phổ biến trong ngành** | Use case, ERD, biểu đồ lớp, biểu đồ trạng thái, biểu đồ tuần tự, kiến trúc phân lớp, thiết kế API, thiết kế giao diện, cài đặt từng chức năng, bảo mật, đóng gói |
| **4. KẾT QUẢ NGHIÊN CỨU** | Kết quả đạt được. **Có thể đánh giá hiệu năng, trải nghiệm người dùng, hoặc trình bày giao diện chức năng** | 129 ca kiểm thử, 11 lỗi thực tế đã khắc phục, khả năng tiếp cận, bảo mật, đối chiếu mục đích, 7 giới hạn đã biết |
| **5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN** | Kết quả đạt được, đóng góp mới, đề xuất mới. **Phần kết luận ngắn gọn, không có lời bàn và bình luận thêm** | Kết luận gọn + 11 hướng phát triển chia ngắn/trung/dài hạn |

---

## 3. DANH SÁCH HÌNH

> **Cập nhật: 30/31 hình đã dựng xong**, nằm ở `docs/images/bao-cao/`. Bảng dưới
> đây giữ lại để biết mỗi hình phải thể hiện điều gì — dùng khi viết chú thích
> và khi kiểm tra lại hình trước lúc nộp. Trạng thái từng hình xem ở cuối mục 3.
>
> Số hình đã giảm từ 40 xuống 28 cho vừa trần 50 trang, sau đó thêm bốn hình
> (4.3 và A.1–A.3) khi rà soát lại phần chữ, thành 31.
>
> **Mỗi hình bắt buộc có chú thích ghi rõ nguồn** — yêu cầu tường minh của quy
> định. Mẫu chú thích: `Hình 3.2 — Sơ đồ quan hệ thực thể (Nguồn: tác giả)` hoặc
> `Hình 1.3 — Trang kết quả tìm kiếm Booking.com (Nguồn: booking.com, chụp ngày .../...)`.

### Nhóm A — Sơ đồ tự vẽ minh hoạ lý thuyết (4 hình) ⭐⭐⭐

*Nhóm khó nhất nhưng làm điểm cao nhất, vì nó chứng tỏ người viết hiểu bản chất
chứ không chỉ mô tả. Vẽ bằng draw.io hoặc PowerPoint rồi xuất PNG.*

| Hình | Nội dung cần thể hiện | Nằm ở |
|---|---|---|
| **1.1** | Khoảng nửa mở `[)`. Trục thời gian ngang, hai thanh `[08/03 → 10/03)` và `[10/03 → 12/03)` chạm nhau tại 10/03 nhưng **không chồng lấn**. Bên dưới vẽ một cặp chồng lấn thật, tô đỏ phần giao | Ch1, mục 1.1.2 |
| **1.2** | Lưu đồ quy trình đặt phòng **thủ công** hiện nay: khách nhắn tin → chủ nhà tra sổ/nhớ → trả lời còn hết → chuyển khoản → ghi sổ. **Tô đỏ ba điểm rủi ro**: tra cứu bằng trí nhớ, không có bản ghi tập trung, hai khách nhắn cùng lúc | Ch1, mục 1.2.1 |
| **2.2** | **Hình quan trọng nhất cả đồ án.** Hai cột song song. Cột trái: mẫu kiểm-tra-rồi-ghi — hai luồng cùng đọc "còn phòng", cùng ghi, kết quả **bán trùng** (đỏ). Cột phải: với ràng buộc loại trừ — luồng 1 ghi thành công, luồng 2 nhận `23P01` và bị bác (xanh) | Ch2, mục 2.4.3 |
| **3.11** | Sơ đồ phụ thuộc giữa 10 giai đoạn, chỉ rõ hai cặp làm song song được | Ch3, mục 3.7 |

### Nhóm B — Sơ đồ UML và kiến trúc (10 hình) ⭐⭐⭐

*Hầu hết đã có sẵn mã Mermaid trong `docs/`. Dán vào **mermaid.live** rồi xuất
PNG độ phân giải cao. Nếu khoa yêu cầu ký pháp UML chuẩn (đặc biệt biểu đồ use
case), nên vẽ lại bằng draw.io hoặc StarUML.*

| Hình | Nội dung | Nguồn mã sẵn có |
|---|---|---|
| **2.1** | Kiến trúc tổng thể 4 dịch vụ khi triển khai | `docs/kien-truc.md`, sơ đồ đầu |
| **3.1** | Biểu đồ use case tổng quát: 3 tác nhân, 11 use case, có khung bao hệ thống | `docs/use-case.md` |
| **3.2** | **ERD tổng thể 21 bảng.** Nếu in A4 quá rậm thì tách nhóm, giữ hình này làm tổng quan | `docs/erd.md`, hoặc xuất từ DBeaver/pgAdmin |
| **3.3** | ERD **chi tiết nhóm đặt phòng**: `bookings` — `booking_rooms` — `rooms` — `room_types`, đủ cột và kiểu, **nhấn mạnh cột sinh `stay` và ràng buộc `booking_rooms_no_overlap`** | Vẽ lại từ `V3__bookings_and_exclusion.sql` |
| **3.4** | Biểu đồ lớp các thực thể cốt lõi nhóm đặt phòng và thanh toán | Đọc từ `backend/.../entity/` |
| **3.5** | Biểu đồ trạng thái 8 trạng thái đơn. **Đánh dấu nổi bật hai đường** `CANCELLED → AWAITING_REVIEW` và `EXPIRED → AWAITING_REVIEW` | `docs/luong-dat-phong.md` |
| **3.6** | Biểu đồ tuần tự **đặt phòng**: khách, trình duyệt, máy chủ, cơ sở dữ liệu | `docs/luong-dat-phong.md` |
| **3.7** | Biểu đồ tuần tự **thanh toán và webhook**: thêm ngân hàng và nhà cung cấp trung gian | `docs/luong-dat-phong.md` |
| **3.8** | Biểu đồ tuần tự **nhánh tiền về muộn**, hai kết quả có thể xảy ra | `docs/luong-dat-phong.md` |
| **3.9** | Sơ đồ phân lớp tầng máy chủ, kèm hai thành phần tách riêng | `docs/kien-truc.md`, sơ đồ hai |

### Nhóm C — Ảnh chụp màn hình hệ thống (12 hình) ⭐⭐⭐

*Chạy `docker compose up -d`, mở trình duyệt, chụp. Chụp ở độ phân giải cao, cắt
gọn, không để lẫn thanh tác vụ hệ điều hành.*

| Hình | Nội dung cần thấy rõ trong ảnh | Ghi chú |
|---|---|---|
| **1.3** | Trang kết quả tìm kiếm Booking.com, **khoanh vùng đánh số 4 thành phần**: thanh tìm kiếm cố định, thẻ phòng, tổng tiền cả kỳ, nhãn khan hiếm | ⚠️ Ảnh bên ngoài — **bắt buộc ghi nguồn và ngày chụp** |
| **3.10** | Giao diện tài liệu API tương tác, thấy rõ các nhóm endpoint | `localhost/swagger-ui/index.html` |
| **3.12** | Trang chủ + thanh tìm phòng, **lịch đang mở với vài ngày bị chặn sẵn** | Có sẵn `docs/images/landing.png`, nên chụp lại |
| **3.13** | Kết quả tìm phòng, thấy **nhãn "còn N phòng"** và **tổng tiền cả kỳ** | |
| **3.14** | Ba bước luồng đặt phòng, **ghép 3 ảnh thành 1 hình** | |
| **3.15** | Màn hình thanh toán: mã QR + thông tin chuyển khoản dạng chữ + đồng hồ đếm ngược | |
| **3.16** | Trang tổng quan quản trị: biểu đồ doanh thu, tỉ lệ lấp đầy, tỉ lệ huỷ | Có sẵn `docs/images/admin-dashboard.png` |
| **3.17** | Màn hình đối soát thanh toán, thấy khoản thiếu tiền và thừa tiền | Có sẵn `docs/images/admin-payments.png` |
| **3.18** | Danh sách đơn kèm bộ lọc, **thấy nhiều trạng thái khác nhau trong cùng một ảnh** | |
| **3.19** | Chi tiết đơn: dòng thời gian lịch sử trạng thái, thanh toán, thư đã gửi, ghi chú | |
| **3.21** | Hộp thư Mailpit với thư xác nhận đã gửi, mở ra thấy nội dung | `localhost:8025` |
| **4.2** | Giao diện trên **ba kích thước màn hình** ghép cạnh nhau: điện thoại, máy tính bảng, máy tính | Chế độ giả lập thiết bị của DevTools |

### Nhóm D — Ảnh kết quả đo đạc (2 hình) ⭐⭐⭐

| Hình | Nội dung | Cách lấy |
|---|---|---|
| **3.20** | Kết quả `docker compose ps` — cả 4 dịch vụ `healthy` | Chụp terminal |
| **4.1** | Kết quả quét khả năng tiếp cận, thể hiện **0 vi phạm** | axe DevTools hoặc Lighthouse |

> **Hình bắt buộc phải có mà danh sách trên chưa nêu:** ảnh chụp kết quả
> `./mvnw verify` với dòng `Tests run: 129, Failures: 0`. Hình này hiện nằm
> trong phần chữ của Chương 4 dưới dạng số liệu. Nếu muốn đưa thành hình thì
> thêm vào Chương 4 và đánh số `4.3`.

### Tổng kết công việc chuẩn bị hình

**Trạng thái hiện tại: 30/31 hình đã có trong `docs/images/bao-cao/`.**

| Nhóm | Số hình | Trạng thái |
|---|---:|---|
| A — Sơ đồ tự vẽ minh hoạ lý thuyết | 4 | xong — dựng bằng Mermaid |
| B — Sơ đồ UML và kiến trúc | 10 | xong — dựng bằng Mermaid |
| C — Ảnh chụp màn hình | 12 | xong 11, **còn Hình 1.3** |
| D — Ảnh kết quả đo đạc | 2 | xong |
| Thêm: 4.3 độ tương phản, A.1–A.3 phụ lục | 4 | xong |
| **Tổng** | **31** | **còn 1 hình** |

Hình duy nhất còn thiếu là **1.3** — ảnh trang kết quả tìm kiếm của Booking.com.
Đây là trang web bên ngoài nên phải tự mở trình duyệt chụp, khoanh vùng đánh số
bốn thành phần, rồi ghi nguồn và ngày chụp ngay dưới hình.

Bảng đối chiếu đầy đủ *hình ↔ tên tệp ↔ cách tạo ra* nằm ở
`docs/images/bao-cao/README.md`.

So với danh sách 28 hình ở trên có ba điểm khác, đều đã đồng bộ lại trong phần
chữ của báo cáo:

- Thêm **Hình 4.3** — đối chiếu độ tương phản nút chính trước và sau khi sửa.
  Chương 4 vốn dẫn tới một số hình không tồn tại.
- Thêm **Hình A.1–A.3** cho phụ lục: màn hình ngày khả dụng, bước buộc đổi mật
  khẩu tạm, trang chi tiết loại phòng. Hình ngày khả dụng trước đó bị đánh
  trùng số với sơ đồ quan hệ thực thể.
- Ảnh chụp giao diện lấy ở tỉ lệ điểm ảnh gấp hai hoặc gấp ba, không phải ảnh
  chụp màn hình thường, để in A4 không bị rỗ.

---

## 4. YÊU CẦU VỀ GITHUB — MỤC 4 CỦA QUY ĐỊNH

Quy định có một mục riêng về quản lý đồ án bằng GitHub, và **lịch sử commit là
tiêu chí chấm điểm tiến độ**.

| Yêu cầu | Trạng thái |
|---|---|
| Tên repo `cn-<malop>-<hotenkhongdau>-<shortname>` | ❌ đang là `Project-Final-TVU` — **cần đổi** |
| Mời GVHD làm Collaborator trong tuần đầu | ❓ cần thực hiện |
| `README.md` cập nhật liên tục, có **thông tin liên lạc (email, điện thoại)** | ⚠️ đã có README nhưng **thiếu thông tin liên lạc** |
| `progress-report/` **[bắt buộc]** — báo cáo tiến độ hàng tuần | ✅ đã tạo |
| `thesis/` **[bắt buộc]** với `doc/ pdf/ html/ abs/ refs/` | ✅ đã tạo |
| `setup/` — tệp cài đặt và dữ liệu thử | ✅ đã tạo |
| `src/` — mã nguồn | ⚠️ mã nguồn đang ở `backend/` và `frontend/` |
| `docker/` — tệp triển khai Docker | ⚠️ đang ở gốc (`docker-compose.yml`) |
| Commit **ít nhất một lần mỗi tuần** | ⚠️ 37 commit nhưng chỉ trải **2 tuần** |

> **Hai việc cần quyết định, không tự làm được:**
>
> **Đổi tên repo.** Cần mã lớp và họ tên không dấu. Ví dụ:
> `cn-da21tta-nguyenhongquan-homestay-springboot`. Đổi tên trên GitHub làm đổi
> URL của repo.
>
> **Lịch sử commit chỉ trải 2 tuần** là rủi ro cho điểm tiến độ. Quy định ghi rõ:
> *"Nếu trong lịch sử commit không ghi nhận tiến độ cập nhật dự án (dù có báo
> cáo tiến độ) xem như sinh viên không hoàn thành báo cáo tiến độ."* Lịch sử
> không sửa được một cách trung thực — nên hỏi GVHD hướng xử lý.
>
> **Về `src/` và `docker/`:** quy định nói "các thư mục **có thể** là", tức là
> gợi ý chứ không bắt buộc. Cấu trúc `backend/` + `frontend/` hiện tại rõ ràng
> hơn với một dự án hai tầng, và `README.md` đã giải thích. Nếu GVHD yêu cầu
> đúng tên thì đổi được, nhưng sẽ làm hỏng mọi đường dẫn trong tài liệu.

---

## 5. VIỆC CÒN LẠI TRƯỚC KHI NỘP

### 5.1. Bắt buộc

- [ ] **Điền thông tin cá nhân** vào bìa: họ tên, MSSV, lớp, khoá, tên GVHD
      → sửa `docs/bao-cao/00-phan-dau.md` mục 1 và 2
- [ ] **Bổ sung thông tin liên lạc** (email, điện thoại) vào `README.md` gốc
- [ ] **Đổi tên repo** theo cú pháp quy định
- [ ] **Mời GVHD** làm Collaborator
- [ ] Chuẩn bị **28 hình** theo mục 3
- [x] Viết **chú thích nêu nguồn** cho từng hình — script chèn tự động; **còn Hình 1.3 phải tự sửa dòng nguồn**, và chú thích cho các bảng vẫn phải viết tay trong Word
- [x] Xuất Word, áp đúng định dạng ở mục 1 — `scripts/xuat-ban-word.py`
- [x] **Đếm số trang thật.** Đã đo: **52 trang** (từ 66). Còn dư 2 so với trần
      50 — ba việc cắt tiếp ở mục 5.3
- [ ] Sinh mục lục, danh mục bảng/sơ đồ/hình tự động
- [x] Xuất PDF, đặt vào `thesis/pdf/`, bản Word vào `thesis/doc/`
- [ ] In bìa cứng chữ nhũ vàng
- [ ] Nộp link GitHub qua biểu mẫu của bộ môn
- [ ] Fork repo về tài khoản chung của bộ môn (cùng GVHD)

### 5.2. Nên làm — cộng điểm

- [ ] **Poster 90cm × 60cm, hướng đứng** → cộng tối đa **1 điểm**, và được tham
      gia buổi showcase. Nộp cùng quyển báo cáo
- [x] Slide báo cáo, đặt vào `thesis/abs/` — **20 slide**, kèm kịch bản nói cho
      từng slide ở `docs/kich-ban-bao-ve.md`. Còn phải điền thông tin sinh viên
      ở slide bìa và tập bấm giờ cho đúng 10–12 phút
- [ ] Chạy lại ba lệnh triển khai trên máy có mạng bình thường và chụp kết quả
      *(xem giới hạn đã biết ở mục 4.6.3 của báo cáo)*

### 5.3. Số trang: đã cắt những gì, còn lại gì

Số đo thật, lấy bằng `scripts/xuat-ban-word.py` rồi chuyển sang PDF và đếm trang:
**66 trang → 52 trang.** Trần quy định là 50, nên còn dư **2 trang**.

**Đã làm — không mất một câu chữ nào:**

| Việc | Tiết kiệm |
|---|---|
| Chỉnh cách trình bày: bỏ đoạn trống thừa sau mỗi bảng, ô bảng sát hơn, giới hạn chiều cao hình ở 10cm, tiêu đề dính với đoạn sau | 9 trang |
| Chuyển đặc tả **UC1, UC2** xuống Phụ lục A — phụ lục giờ đủ cả 11 use case | 2 trang |
| Chuyển **danh sách cột của bốn bảng cốt lõi** xuống Phụ lục B — phụ lục giờ đủ cả 21 bảng. Phần giải thích *vì sao* từng ràng buộc tồn tại vẫn ở Chương 3 | 2 trang |
| Chuyển **bảng 12 yêu cầu phi chức năng** xuống Phụ lục O, **cây gói** xuống Phụ lục P, **bảng 9 khoá giới hạn tần suất** xuống Phụ lục Q | 1 trang |

**Đã làm — có sửa câu chữ:**

- Mục **2.1**: gộp hai đoạn so sánh kiến trúc nguyên khối thành một.
- Mục **1.2.2**: ba đoạn mô tả nhóm người dùng gộp thành một bảng ba cột.
- Mục **3.8.3** và **3.8.4**: rút gọn phần mở đầu.

**Nếu thầy hướng dẫn yêu cầu đúng 50 trang, cắt tiếp theo thứ tự này:**

1. Chuyển **bảng endpoint theo nhóm** ở mục 3.5.2 xuống Phụ lục E (đã có bảng
   đầy đủ ở đó) — khoảng nửa trang.
2. Rút mục **3.8.5** còn một đoạn, chuyển hai trong bốn ảnh khu quản trị
   (Hình 3.17, 3.18) xuống phụ lục — khoảng một trang.
3. Chuyển **bảng 32 yêu cầu chức năng** ở mục 1.4.1 xuống Phụ lục K (đã có bảng
   đầy đủ ở đó) — khoảng nửa trang.

**Không cắt** mục 2.4 (PostgreSQL), mục 4.3 (mười một lỗi thực tế) và mục 4.7
(bảy giới hạn đã biết). Ba mục này là phần làm điểm cao nhất.

---

## 6. BA CÂU HỎI GẦN NHƯ CHẮC CHẮN BỊ HỎI

Hội đồng đánh giá gồm **02 thành viên**: một do bộ môn phân công và một là GVHD.
Biểu mẫu nhận xét của cán bộ chấm có hẳn một mục **"CÁC VẤN ĐỀ CẦN LÀM RÕ"**,
nghĩa là câu hỏi phản biện được chuẩn bị trước và ghi ra giấy.

**Câu 1 — "Vì sao chọn PostgreSQL mà không phải MySQL?"**

Không trả lời "vì PostgreSQL mạnh hơn". Trả lời bằng tính năng cụ thể: bài toán
cốt lõi cần kiểu `daterange` và ràng buộc `EXCLUDE USING gist`, MySQL không có cả
hai. Không có chúng thì phải chống trùng lịch ở tầng ứng dụng, mà cách đó có khe
hở tranh chấp không bịt được bằng logic thông thường. Dẫn chứng: ca kiểm thử đa
luồng trong `BookingConcurrencyIT`.

**Câu 2 — "Nếu hai khách đặt cùng lúc thì sao?"**

Đây là câu trọng tâm. Trả lời ba bước:
1. Mô tả khe hở của mẫu kiểm-tra-rồi-ghi.
2. PostgreSQL bác một trong hai giao dịch **ngay trong động cơ lưu trữ** bằng mã
   `SQLSTATE 23P01`.
3. Hệ thống bắt đúng mã đó và thử phòng tiếp theo **trong một giao dịch mới**, vì
   giao dịch cũ đã hỏng và mọi lệnh sau chỉ trả `25P02`.

Mở Hình 2.2 ra chỉ khi trả lời.

**Câu 3 — "Hệ thống còn thiếu gì? Điểm yếu ở đâu?"**

Đừng nói "em nghĩ đã đầy đủ". Mở mục 4.7 và trình bày bảy giới hạn đã biết, nhấn
mạnh giới hạn số 7 — chưa có HTTPS — là quan trọng nhất nếu đưa ra dùng thật.
Biết rõ điểm yếu của hệ thống mình làm là dấu hiệu của người hiểu nó.

---

## 7. BẢN ĐỒ TỆP

```
docs/
├── de-cuong-do-an.md          ← tài liệu này: hướng dẫn thi công
├── bao-cao/                   ← NỘI DUNG QUYỂN BÁO CÁO
│   ├── README.md              ← thứ tự ghép tệp + quy định định dạng
│   ├── 00-phan-dau.md         ← bìa, nhận xét, lời mở đầu, danh mục, tóm tắt
│   ├── 01-mo-dau.md           ← MỞ ĐẦU (4 mục)
│   ├── 02-chuong-1.md         ← CHƯƠNG 1. TỔNG QUAN
│   ├── 03-chuong-2.md         ← CHƯƠNG 2. NGHIÊN CỨU LÝ THUYẾT
│   ├── 04a-chuong-3-thiet-ke.md   ← CHƯƠNG 3, mục 3.1–3.6
│   ├── 04b-chuong-3-cai-dat.md    ← CHƯƠNG 3, mục 3.7–3.10
│   ├── 05-chuong-4.md         ← CHƯƠNG 4. KẾT QUẢ NGHIÊN CỨU
│   ├── 06-chuong-5.md         ← CHƯƠNG 5. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN
│   ├── 07-tai-lieu-tham-khao.md   ← IEEE, 14 mục
│   ├── 08a-phu-luc-dac-ta.md  ← Phụ lục A–B: 11 use case, 21 bảng
│   └── 08b-phu-luc-ky-thuat.md← Phụ lục C–O: mã nguồn, bảng tra cứu
└── (12 tài liệu kỹ thuật khác — nguồn số liệu và mã Mermaid)

progress-report/               ← [bắt buộc] báo cáo tiến độ hàng tuần
thesis/                        ← [bắt buộc] doc/ pdf/ html/ abs/ refs/
setup/                         ← hướng dẫn cài đặt và dữ liệu thử
```

**Lệnh ghép toàn bộ thành một tệp để xuất Word:**

```bash
cd docs/bao-cao && cat 00-phan-dau.md 01-mo-dau.md 02-chuong-1.md \
  03-chuong-2.md 04a-chuong-3-thiet-ke.md 04b-chuong-3-cai-dat.md \
  05-chuong-4.md 06-chuong-5.md 07-tai-lieu-tham-khao.md \
  08a-phu-luc-dac-ta.md 08b-phu-luc-ky-thuat.md > bao-cao-day-du.md
```
