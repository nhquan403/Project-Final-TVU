#!/usr/bin/env sh
#
# Sinh tệp .env cho docker compose. Chạy một lần trên máy mới:
#
#     ./scripts/init-env.sh && docker compose up -d --build
#
# Vì sao cần script này thay vì một .env.example có sẵn giá trị: repo này công
# khai. Một JWT_SECRET mặc định nằm trong repo là chìa khoá để BẤT KỲ AI cũng
# tự ký được token role=ADMIN cho MỌI bản triển khai dùng repo này. Nên
# .env.example chỉ liệt kê TÊN biến với giá trị trống, và script này là đường
# duy nhất tạo ra giá trị thật.
#
# Script KHÔNG đọc giá trị từ .env.example (tệp đó trống theo thiết kế) và
# không bao giờ ghi ngược vào đó.

set -eu

ROOT="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
ENV_FILE="$ROOT/.env"

if [ -f "$ENV_FILE" ]; then
    echo "Đã có $ENV_FILE — giữ nguyên, không ghi đè."
    echo "Muốn sinh bí mật mới thì xoá tệp đó rồi chạy lại."
    exit 0
fi

if ! command -v openssl > /dev/null 2>&1; then
    echo "Cần openssl để sinh bí mật ngẫu nhiên. Cài openssl rồi chạy lại." >&2
    exit 1
fi

# umask trước khi tạo tệp, không phải chmod sau: giữa lúc tạo và lúc chmod có
# một khoảnh khắc tệp bí mật đọc được bởi mọi người dùng trên máy.
umask 077

{
    echo "# Sinh tự động bởi scripts/init-env.sh — KHÔNG commit tệp này."
    echo "# .gitignore đã loại trừ .env; đừng gỡ dòng đó ra."
    echo ""
    echo "# Mật khẩu cơ sở dữ liệu. Chỉ dùng trong mạng nội bộ của compose."
    echo "POSTGRES_DB=homestay"
    echo "POSTGRES_USER=homestay"
    echo "POSTGRES_PASSWORD=$(openssl rand -base64 24 | tr -d '\n/+=')"
    echo ""
    echo "# Khoá ký JWT. 48 byte ngẫu nhiên."
    echo "JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')"
    echo ""
    echo "# Khoá xác thực webhook SePay. Khi dùng SePay thật, thay bằng giá trị"
    echo "# khai trong bảng điều khiển SePay."
    echo "SEPAY_WEBHOOK_API_KEY=$(openssl rand -hex 32)"
    echo ""
    echo "# Tài khoản nhận tiền hiện trên mã QR. Giá trị dưới đây là GIẢ, chỉ để"
    echo "# QR dựng được hình; thay bằng số tài khoản thật trước khi nhận tiền."
    echo "SEPAY_ACCOUNT_NUMBER=0123456789"
    echo "SEPAY_BANK_CODE=MBBank"
    echo ""
    echo "# Cổng công khai. Đổi khi cổng 80 đã có thứ khác chiếm."
    echo "WEB_PORT=80"
    echo "MAILPIT_PORT=8025"
} > "$ENV_FILE"

echo "Đã tạo $ENV_FILE với bí mật sinh ngẫu nhiên."
echo ""
echo "Tiếp theo:  docker compose up -d --build"
echo ""
echo "Mật khẩu quản trị demo KHÔNG nằm trong tệp này. Nó được sinh lúc nạp dữ"
echo "liệu mẫu và chỉ in ra log container:"
echo ""
echo "    docker compose logs api | grep -i 'mat khau admin demo'"
