# ── Tầng dựng ────────────────────────────────────────────────────────────────
# Chép package*.json trước rồi mới chép mã nguồn, cùng lý do với backend: sửa
# một component không được phép làm npm tải lại toàn bộ cây phụ thuộc.
FROM node:22-alpine AS builder
WORKDIR /src

COPY package.json package-lock.json ./
# `npm ci` chứ không `npm install`: nó cài đúng những gì lockfile ghi và thất
# bại nếu lockfile lệch package.json. Trong một lần dựng ảnh, "tự sửa lockfile"
# là hành vi sai — bản dựng phải tái lập được.
#
# Kiểm tra `ng` ngay sau khi cài KHÔNG phải thừa. Khi npm chết giữa chừng vì
# lỗi "Exit handler never called!" — quan sát được ở cả 10.8.2 lẫn 10.9.8 — nó
# vẫn THOÁT VỚI MÃ 0 trong khi cây phụ thuộc cài dở. Không có dòng kiểm tra
# này, Docker đi tiếp và chỉ vỡ ở bước sau với thông báo `ng: not found`, không
# nhắc gì tới nguyên nhân thật.
RUN npm ci && test -x node_modules/.bin/ng

COPY . .
# `ng build` mặc định dùng cấu hình production, và cấu hình đó thay
# ui-kit.routes.ts bằng bản rỗng — trang UI kit nội bộ không đi ra bản phát hành.
RUN npm run build

# ── Tầng chạy ────────────────────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Cấu hình mặc định của nginx phục vụ trang "Welcome to nginx" ở cùng cổng.
# Xoá nó đi, nếu không hai server block cùng nghe :80 và cái thắng là cái nào
# nạp trước — một thứ không nên phụ thuộc vào.
RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/homestay.conf
COPY proxy-headers.inc /etc/nginx/conf.d/proxy-headers.inc
COPY --from=builder /src/dist/frontend/browser /usr/share/nginx/html

ENV TZ=Asia/Ho_Chi_Minh
EXPOSE 80

# Ảnh nginx chính thức đã có ENTRYPOINT/CMD đúng; không ghi đè.
