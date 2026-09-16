# Header chuyển tiếp, dùng chung cho mọi `location` proxy.
#
# nginx `proxy_pass` mặc định chỉ đặt Host và Connection. Thiếu những dòng dưới
# đây thì `request.getRemoteAddr()` trả IP của container nginx cho MỌI khách, và
# giới hạn 10 request/phút theo IP của Phase 4 biến thành giới hạn toàn hệ
# thống: một vòng curl khoá tính năng đăng nhập của tất cả mọi người.
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;

# GHI ĐÈ, không nối thêm — và đây là điểm khác biệt quan trọng nhất trong tệp.
#
# Biến `$proxy_add_x_forwarded_for` quen thuộc hơn, nhưng nó NỐI giá trị khách
# tự gửi lên vào đầu chuỗi. Spring (forward-headers-strategy: framework) lấy
# phần tử ĐẦU TIÊN làm IP khách, nên kẻ tấn công chỉ cần gửi kèm
# `X-Forwarded-For: 1.2.3.4` và đổi số mỗi lần là mỗi request có một khoá rate
# limit riêng — giới hạn 10 lần đăng nhập/phút vô hiệu hoàn toàn.
#
# nginx là biên duy nhất chạm Internet ở đây, nên nó là nơi duy nhất biết IP
# thật. `$remote_addr` là IP của kết nối TCP: không giả được bằng header.
proxy_set_header X-Forwarded-For   $remote_addr;
proxy_set_header X-Forwarded-Proto $scheme;
