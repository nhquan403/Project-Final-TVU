#!/usr/bin/env sh
# Dung giao dien roi day thang vao container nginx dang chay.
#
# Cach nay thay cho `ng serve` trong moi truong container vi hai ly do: cong
# 8080 cua api KHONG mo ra host (chi web tren cong 80), va dev server cua
# Angular khong nhan duoc ket noi o day. Dung chinh nginx cua du an thi CSP,
# duong dan /api va cach phuc vu tep tinh deu giong ban that.
set -eu
cd "$(dirname "$0")/.."
(cd frontend && npx ng build --configuration development >/dev/null)
docker compose cp frontend/dist/frontend/browser/. web:/usr/share/nginx/html/ >/dev/null
printf 'da nap lai giao dien vao container web\n'
