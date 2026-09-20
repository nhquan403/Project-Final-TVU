# CÁC TRANG ĐẦU

## Trang bìa

BỘ GIÁO DỤC VÀ ĐÀO TẠO
TRƯỜNG ĐẠI HỌC TRÀ VINH
KHOA CÔNG NGHỆ THÔNG TIN

*(logo trường)*

**ĐỒ ÁN TỐT NGHIỆP**

**XÂY DỰNG WEBSITE GIỚI THIỆU VÀ ĐẶT PHÒNG HOMESTAY
— HOMESTAY TVH —**

Giảng viên hướng dẫn: ...............................
Sinh viên thực hiện: ...............................
Mã số sinh viên: ...............................
Lớp: ...............................

TRÀ VINH – 20...

---

## Lời cảm ơn

Em xin gửi lời cảm ơn chân thành đến Thầy/Cô ......................... đã tận
tình hướng dẫn, góp ý và tạo điều kiện cho em hoàn thành đồ án tốt nghiệp này.
Những nhận xét thẳng thắn của Thầy/Cô trong các buổi báo cáo tiến độ đã giúp em
nhận ra nhiều thiếu sót mà bản thân không tự thấy được.

Em cũng xin cảm ơn quý Thầy/Cô Khoa Công nghệ Thông tin — Trường Đại học Trà
Vinh đã truyền đạt kiến thức nền tảng trong suốt quá trình học tập. Đặc biệt,
những kiến thức về cơ sở dữ liệu và hệ thống phân tán đã trực tiếp giúp em giải
quyết bài toán cốt lõi của đề tài này.

Cuối cùng, em xin cảm ơn gia đình và những người đã luôn ủng hộ em.

Do thời gian và kiến thức còn hạn chế, đồ án chắc chắn còn nhiều thiếu sót. Em
rất mong nhận được ý kiến đóng góp của quý Thầy/Cô.

Em xin chân thành cảm ơn!

*Trà Vinh, ngày ... tháng ... năm 20...*
*Sinh viên thực hiện*

---

## Lời cam đoan

Em xin cam đoan đồ án tốt nghiệp **"Xây dựng website giới thiệu và đặt phòng
homestay — Homestay TVH"** là công trình do chính em thực hiện dưới sự hướng dẫn
của Thầy/Cô ..........................

Toàn bộ mã nguồn của hệ thống do em tự viết. Các thư viện, framework và công cụ
mã nguồn mở được sử dụng đều được nêu rõ trong Chương 2 và trong danh mục tài
liệu tham khảo. Số liệu và kết quả kiểm thử trình bày trong đồ án được lấy trực
tiếp từ hệ thống đang chạy, trung thực và chưa từng được công bố trong bất kỳ
công trình nào khác.

Những nội dung tham khảo từ tài liệu bên ngoài đều được trích dẫn đầy đủ nguồn
gốc theo đúng quy định.

Em xin chịu hoàn toàn trách nhiệm về lời cam đoan này.

*Trà Vinh, ngày ... tháng ... năm 20...*
*Sinh viên thực hiện*
*(Ký và ghi rõ họ tên)*

---

## Nhận xét của giảng viên hướng dẫn

*(Để trống — giảng viên hướng dẫn ghi)*

## Nhận xét của giảng viên phản biện

*(Để trống — giảng viên phản biện ghi)*

---

## Mục lục

*(Sinh tự động bằng chức năng Table of Contents của trình soạn thảo, tối đa ba
cấp tiêu đề)*

## Danh mục hình ảnh

*(Sinh tự động từ caption. Đồ án gồm 40 hình)*

## Danh mục bảng biểu

*(Sinh tự động từ caption)*

---

## Danh mục từ viết tắt

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
| GiST | Generalized Search Tree | Cây tìm kiếm tổng quát — loại chỉ mục của PostgreSQL |
| HTML | HyperText Markup Language | Ngôn ngữ đánh dấu siêu văn bản |
| HTTP/HTTPS | HyperText Transfer Protocol (Secure) | Giao thức truyền siêu văn bản (bảo mật) |
| IDE | Integrated Development Environment | Môi trường phát triển tích hợp |
| JDBC | Java Database Connectivity | Giao diện kết nối cơ sở dữ liệu của Java |
| JPA | Jakarta Persistence API | Chuẩn ánh xạ đối tượng — quan hệ của Java |
| JSON | JavaScript Object Notation | Định dạng trao đổi dữ liệu |
| JVM | Java Virtual Machine | Máy ảo Java |
| JWT | JSON Web Token | Chuẩn token xác thực |
| LTS | Long-Term Support | Phiên bản hỗ trợ dài hạn |
| MVCC | Multi-Version Concurrency Control | Điều khiển tương tranh đa phiên bản |
| ORM | Object-Relational Mapping | Ánh xạ đối tượng — quan hệ |
| QR | Quick Response (code) | Mã phản hồi nhanh |
| RBAC | Role-Based Access Control | Kiểm soát truy cập theo vai trò |
| REST | REpresentational State Transfer | Kiểu kiến trúc giao diện lập trình ứng dụng |
| RFC | Request For Comments | Tài liệu tiêu chuẩn của tổ chức IETF |
| SMTP | Simple Mail Transfer Protocol | Giao thức gửi thư |
| SQL | Structured Query Language | Ngôn ngữ truy vấn có cấu trúc |
| UI/UX | User Interface / User Experience | Giao diện / Trải nghiệm người dùng |
| UML | Unified Modeling Language | Ngôn ngữ mô hình hoá thống nhất |
| UUID | Universally Unique Identifier | Định danh duy nhất toàn cục |
| WCAG | Web Content Accessibility Guidelines | Hướng dẫn khả năng tiếp cận nội dung web |
| XSS | Cross-Site Scripting | Tấn công chèn mã kịch bản |
| 3NF | Third Normal Form | Dạng chuẩn 3 |
