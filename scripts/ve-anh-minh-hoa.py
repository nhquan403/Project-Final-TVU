#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Sinh 26 tệp SVG minh hoạ cho dữ liệu mẫu của Homestay TVH.

Vì sao là một bộ SINH RA chứ không phải 26 tệp viết tay: bảng màu đổi thì cả
26 tấm phải đổi theo cùng một lúc, nếu không vài tấm mang màu cũ và trông như
ảnh của một trang khác lọt vào. Bộ sinh giữ mọi tấm cùng một bảng màu, cùng
cách dựng lớp xa–gần, cùng hướng nắng.

CÁC TẤM NÀY LÀ BẢN TẠM. Đích đến là ảnh chụp thật của homestay: chép ảnh vào
thư mục này với đúng tên tệp cũ là xong, không phải sửa một dòng mã nào.

Chạy:  python3 scripts/ve-anh-minh-hoa.py
"""
import os

RA = os.path.join(os.path.dirname(__file__), '..', 'frontend', 'public', 'images', 'demo')

# ── Bảng màu ───────────────────────────────────────────────────────────────
# Lấy tinh thần từ token nhưng KHÔNG phải cùng một giá trị: token là màu của
# giao diện (phải đạt ngưỡng tương phản với chữ), còn đây là màu của cảnh vật.
TROI_TREN, TROI_GIUA, TROI_DUOI = '#F7E6D0', '#F0D6B8', '#E5C3A6'
NANG      = '#FAEBD2'
NUOC_TREN, NUOC_DUOI = '#2E757A', '#12484F'
LA_GAN, LA_VUA, LA_XA = '#1F5449', '#3C7A5F', '#89AC96'
CO_TREN, CO_DUOI = '#478161', '#2C5E46'
GO_SANG, GO_TOI = '#8A6240', '#5C3E29'
GO_SAU = '#6E4E34'   # chân sau của bàn ghế — tối hơn mặt, sáng hơn chân trước
MAI       = '#6B4A32'
TUONG     = '#F6EADA'
TUONG_BONG = '#E8D8C3'
SAN       = '#DCC9AE'
GACH      = '#A83E1E'
XANH      = '#0E5C63'
VAI       = '#F2E7D6'
VAI_BONG  = '#DCCDB6'
KIM_LOAI  = '#8A8275'


def tep(ten, w, h, nhan, than):
    """Ghi một tệp SVG. Không vẽ chữ vào ảnh — xem README trong thư mục này."""
    noi_dung = (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" '
        f'width="{w}" height="{h}" role="img" aria-label="{nhan}">\n'
        f'{than}\n</svg>\n'
    )
    with open(os.path.join(RA, ten), 'w', encoding='utf-8') as f:
        f.write(noi_dung)


def dai_mau(id_, tren, duoi):
    return (f'<linearGradient id="{id_}" x1="0" y1="0" x2="0" y2="1">'
            f'<stop offset="0" stop-color="{tren}"/>'
            f'<stop offset="1" stop-color="{duoi}"/></linearGradient>')


# ── Mảnh cảnh dùng lại ─────────────────────────────────────────────────────
def troi(w, h, mat_troi=None):
    """Nền trời. Mặt trời là hai vòng tròn lồng nhau: lõi sáng và quầng mờ —
    một vòng tròn đặc trông như miếng dán, hai vòng thì có không khí."""
    g = (f'<defs>{dai_mau("troi", TROI_TREN, TROI_DUOI)}'
         f'{dai_mau("nuoc", NUOC_TREN, NUOC_DUOI)}'
         f'{dai_mau("co", CO_TREN, CO_DUOI)}</defs>'
         f'<rect width="{w}" height="{h}" fill="url(#troi)"/>')
    if mat_troi:
        x, y, r = mat_troi
        g += (f'<circle cx="{x}" cy="{y}" r="{r*1.7:.0f}" fill="{NANG}" opacity=".38"/>'
              f'<circle cx="{x}" cy="{y}" r="{r}" fill="{NANG}"/>')
    return g


def day_cay_xa(w, y):
    """Hàng cây phía xa, nhạt và bệt — nó chỉ để tạo chiều sâu, không để nhìn."""
    return (f'<path d="M0 {y} Q{w*0.14:.0f} {y-34} {w*0.3:.0f} {y-8} '
            f'T{w*0.58:.0f} {y-12} Q{w*0.74:.0f} {y-40} {w*0.88:.0f} {y-6} '
            f'T{w} {y-16} L{w} {y+70} L0 {y+70}Z" fill="{LA_XA}" opacity=".8"/>')


def dua(x, chan, cao, mau=LA_GAN):
    """Cây dừa: thân cong nhẹ về bên phải, năm tàu lá toả đều."""
    ngon = chan - cao
    tan = ''
    for goc, rx, ry in ((-26, cao*0.34, cao*0.075), (26, cao*0.34, cao*0.075),
                        (-70, cao*0.31, cao*0.068), (70, cao*0.31, cao*0.068),
                        (0, cao*0.30, cao*0.06)):
        tan += (f'<ellipse cx="{x}" cy="{ngon}" rx="{rx:.0f}" ry="{ry:.0f}" '
                f'transform="rotate({goc} {x} {ngon})"/>')
    return (f'<g fill="{mau}">'
            f'<path d="M{x-8} {chan}c-{cao*0.05:.0f}-{cao*0.55:.0f} {cao*0.02:.0f}-{cao*0.8:.0f} '
            f'{cao*0.16:.0f}-{cao*0.95:.0f}l{cao*0.1:.0f} {cao*0.05:.0f}'
            f'c-{cao*0.12:.0f} {cao*0.28:.0f}-{cao*0.17:.0f} {cao*0.56:.0f}-{cao*0.13:.0f} {cao*0.9:.0f}z"/>'
            f'{tan}</g>')


def cay_la(x, y, r, mau=LA_VUA):
    """Cây tán tròn, ba khối lệch nhau cho đỡ tròn trịa như quả bóng."""
    return (f'<g fill="{mau}">'
            f'<rect x="{x-r*0.09:.0f}" y="{y}" width="{r*0.18:.0f}" height="{r*0.95:.0f}" fill="{GO_TOI}"/>'
            f'<circle cx="{x}" cy="{y-r*0.15:.0f}" r="{r}"/>'
            f'<circle cx="{x-r*0.62:.0f}" cy="{y+r*0.12:.0f}" r="{r*0.68:.0f}"/>'
            f'<circle cx="{x+r*0.6:.0f}" cy="{y+r*0.05:.0f}" r="{r*0.6:.0f}"/></g>')


def nha_san(x, y_mai, w, h_than):
    """Nhà sàn mái lá: mái dốc, hai mảng sáng–tối để mái có khối."""
    dinh_x, dinh_y = x + w/2, y_mai - w*0.36
    y_than = y_mai
    return (
        f'<polygon points="{x-w*0.07:.0f},{y_mai} {dinh_x:.0f},{dinh_y:.0f} {x+w*1.07:.0f},{y_mai}" fill="{MAI}"/>'
        f'<polygon points="{x-w*0.07:.0f},{y_mai} {dinh_x:.0f},{dinh_y:.0f} {dinh_x:.0f},{y_mai}" fill="{GO_SANG}"/>'
        f'<rect x="{x}" y="{y_than}" width="{w}" height="{h_than}" fill="{TUONG}"/>'
        f'<rect x="{x}" y="{y_than}" width="{w}" height="{h_than}" fill="none" stroke="{MAI}" stroke-width="{w*0.018:.1f}"/>'
        f'<rect x="{x+w*0.12:.0f}" y="{y_than+h_than*0.22:.0f}" width="{w*0.24:.0f}" height="{h_than*0.44:.0f}" fill="{XANH}"/>'
        f'<rect x="{x+w*0.64:.0f}" y="{y_than+h_than*0.22:.0f}" width="{w*0.24:.0f}" height="{h_than*0.44:.0f}" fill="{XANH}"/>'
        f'<rect x="{x+w*0.06:.0f}" y="{y_than+h_than:.0f}" width="{w*0.05:.0f}" height="{h_than*0.34:.0f}" fill="{GO_TOI}"/>'
        f'<rect x="{x+w*0.89:.0f}" y="{y_than+h_than:.0f}" width="{w*0.05:.0f}" height="{h_than*0.34:.0f}" fill="{GO_TOI}"/>'
    )


def mat_nuoc(w, h, y):
    """Dải nước cộng vài vệt phản chiếu. Vệt nằm ngang và mờ — nước lặng của
    một nhánh sông, không phải sóng biển."""
    v = ''
    for i, (x1, x2, yy) in enumerate((
            (w*0.06, w*0.34, y + (h-y)*0.22), (w*0.5, w*0.86, y + (h-y)*0.4),
            (w*0.2, w*0.72, y + (h-y)*0.62), (w*0.42, w*0.95, y + (h-y)*0.84))):
        v += (f'<line x1="{x1:.0f}" y1="{yy:.0f}" x2="{x2:.0f}" y2="{yy:.0f}" '
              f'stroke="{NANG}" stroke-width="{h*0.008:.0f}" stroke-linecap="round" opacity=".26"/>')
    return f'<rect y="{y}" width="{w}" height="{h-y}" fill="url(#nuoc)"/>{v}'


def bai_co(w, h, y):
    return (f'<path d="M0 {y} Q{w*0.24:.0f} {y-30} {w*0.5:.0f} {y-6} '
            f'T{w} {y-18} L{w} {h} L0 {h}Z" fill="url(#co)"/>')


def xuong(x, y, w):
    """Chiếc xuồng ba lá."""
    return (f'<g><path d="M{x} {y} q{w*0.36:.0f} {w*0.17:.0f} {w} 0 '
            f'l-{w*0.13:.0f} {w*0.17:.0f} q-{w*0.35:.0f} {w*0.11:.0f} -{w*0.72:.0f} 0z" fill="{GO_TOI}"/>'
            f'<path d="M{x+w*0.23:.0f} {y-w*0.07:.0f} q{w*0.26:.0f} -{w*0.13:.0f} {w*0.5:.0f} 0" '
            f'stroke="{GACH}" stroke-width="{w*0.035:.0f}" fill="none" stroke-linecap="round"/></g>')


# ── Cảnh trong phòng ───────────────────────────────────────────────────────
def phong_nen(w, h, y_san=None):
    """Tường, sàn, chân tường và vệt nắng hắt chéo.

    Vạch chân tường là chi tiết nhỏ nhưng quyết định: không có nó thì tường và
    sàn chỉ là hai mảng màu chồng lên nhau, và căn phòng đọc ra là một tấm nền
    chứ không phải một không gian có chiều sâu."""
    y = y_san or h * 0.66
    return (f'<rect width="{w}" height="{h}" fill="{TUONG}"/>'
            f'<rect y="{y:.0f}" width="{w}" height="{h-y:.0f}" fill="{SAN}"/>'
            f'<rect y="{y-h*0.022:.0f}" width="{w}" height="{h*0.022:.0f}" fill="{TUONG_BONG}"/>'
            f'<polygon points="{w*0.08:.0f},{h} {w*0.36:.0f},{y:.0f} {w*0.56:.0f},{y:.0f} '
            f'{w*0.34:.0f},{h}" fill="{NANG}" opacity=".45"/>')


def cua_so(x, y, w, h, canh_ngoai=True):
    """Cửa sổ có khung gỗ; ô kính nhìn ra mảng xanh nếu canh_ngoai."""
    nen = LA_VUA if canh_ngoai else TROI_GIUA
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{nen}"/>'
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="none" stroke="{GO_SANG}" stroke-width="{w*0.05:.0f}"/>'
            f'<line x1="{x+w/2:.0f}" y1="{y}" x2="{x+w/2:.0f}" y2="{y+h}" stroke="{GO_SANG}" stroke-width="{w*0.04:.0f}"/>'
            f'<line x1="{x}" y1="{y+h*0.5:.0f}" x2="{x+w}" y2="{y+h*0.5:.0f}" stroke="{GO_SANG}" stroke-width="{w*0.03:.0f}"/>')


def giuong(x, y_san, w, goi=2):
    """Giường nhìn ngang, đầu giường dựa tường bên trái.

    Ba con số quyết định nó ra cái giường hay ra cái băng ghế: nệm phải DÀY
    (bản trước mỏng bằng một phần ba nên ba phòng ngủ đều trông như có băng
    ghế dài), gối phải NẰM TRÊN mặt nệm chứ không lơ lửng phía trên, và chăn
    phải phủ trong lòng nệm chứ không tràn ra ngoài."""
    h_nem = w * 0.20          # nệm — phần dày nhất
    h_be = w * 0.09           # bệ gỗ đỡ nệm
    h_chan = w * 0.13         # chân giường
    h_dau = w * 0.46          # đầu giường, tính từ sàn
    y_nem = y_san - h_chan - h_be - h_nem
    return (
        # bóng đổ dưới gầm, vẽ trước để mọi thứ khác nằm đè lên
        f'<ellipse cx="{x+w*0.52:.0f}" cy="{y_san:.0f}" rx="{w*0.54:.0f}" ry="{w*0.026:.0f}" '
        f'fill="{TUONG_BONG}" opacity=".6"/>'
        # đầu giường
        f'<rect x="{x:.0f}" y="{y_san-h_dau:.0f}" width="{w*0.075:.0f}" height="{h_dau:.0f}" '
        f'rx="{w*0.012:.0f}" fill="{GO_TOI}"/>'
        # chân giường hai đầu
        f'<rect x="{x+w*0.03:.0f}" y="{y_san-h_chan:.0f}" width="{w*0.05:.0f}" height="{h_chan:.0f}" fill="{GO_TOI}"/>'
        f'<rect x="{x+w*0.92:.0f}" y="{y_san-h_chan:.0f}" width="{w*0.05:.0f}" height="{h_chan:.0f}" fill="{GO_TOI}"/>'
        # bệ gỗ
        f'<rect x="{x+w*0.02:.0f}" y="{y_san-h_chan-h_be:.0f}" width="{w*0.96:.0f}" height="{h_be:.0f}" fill="{GO_SANG}"/>'
        # nệm
        f'<rect x="{x+w*0.055:.0f}" y="{y_nem:.0f}" width="{w*0.92:.0f}" height="{h_nem:.0f}" '
        f'rx="{h_nem*0.22:.0f}" fill="{VAI}"/>'
        f'<line x1="{x+w*0.055:.0f}" y1="{y_nem+h_nem*0.55:.0f}" x2="{x+w*0.975:.0f}" '
        f'y2="{y_nem+h_nem*0.55:.0f}" stroke="{VAI_BONG}" stroke-width="{h_nem*0.10:.1f}"/>'
        # chăn gấp, nằm gọn trong lòng nệm về phía chân giường
        f'<rect x="{x+w*0.60:.0f}" y="{y_nem-h_nem*0.10:.0f}" width="{w*0.35:.0f}" '
        f'height="{h_nem*0.72:.0f}" rx="{h_nem*0.14:.0f}" fill="{GACH}" opacity=".82"/>'
        # gối: đáy gối chạm mặt nệm
        + ''.join(
            f'<rect x="{x+w*(0.12+i*0.185):.0f}" y="{y_nem-h_nem*0.62:.0f}" width="{w*0.165:.0f}" '
            f'height="{h_nem*0.66:.0f}" rx="{h_nem*0.30:.0f}" fill="{TUONG}"/>'
            f'<rect x="{x+w*(0.12+i*0.185):.0f}" y="{y_nem-h_nem*0.62:.0f}" width="{w*0.165:.0f}" '
            f'height="{h_nem*0.66:.0f}" rx="{h_nem*0.30:.0f}" fill="none" stroke="{VAI_BONG}" '
            f'stroke-width="{w*0.005:.1f}"/>'
            for i in range(goi))
    )


def tu_dau_giuong(x, y_san, w):
    """Tủ đầu giường: mặt tủ, một ngăn kéo, hai chân."""
    h = w * 0.78
    return (f'<rect x="{x:.0f}" y="{y_san-h:.0f}" width="{w:.0f}" height="{h*0.72:.0f}" fill="{GO_SANG}"/>'
            f'<rect x="{x+w*0.12:.0f}" y="{y_san-h*0.62:.0f}" width="{w*0.76:.0f}" '
            f'height="{h*0.2:.0f}" fill="{GO_TOI}" opacity=".5"/>'
            f'<rect x="{x+w*0.08:.0f}" y="{y_san-h*0.28:.0f}" width="{w*0.1:.0f}" '
            f'height="{h*0.28:.0f}" fill="{GO_TOI}"/>'
            f'<rect x="{x+w*0.82:.0f}" y="{y_san-h*0.28:.0f}" width="{w*0.1:.0f}" '
            f'height="{h*0.28:.0f}" fill="{GO_TOI}"/>')


def tham(x, y, w, h):
    """Thảm trải sàn, bo góc, có đường viền trong."""
    return (f'<rect x="{x:.0f}" y="{y:.0f}" width="{w:.0f}" height="{h:.0f}" rx="{h*0.08:.0f}" '
            f'fill="{VAI_BONG}" opacity=".75"/>'
            f'<rect x="{x+w*0.05:.0f}" y="{y+h*0.14:.0f}" width="{w*0.9:.0f}" height="{h*0.72:.0f}" '
            f'rx="{h*0.06:.0f}" fill="none" stroke="{GACH}" stroke-width="{h*0.05:.0f}" opacity=".45"/>')


def ban(x, y_mat, w, cao):
    """Bàn gỗ nhìn ngang: mặt bàn dày, bốn chân (hai chân trước đậm, hai chân
    sau nhạt hơn để có chiều sâu)."""
    d = cao * 0.11
    return (f'<rect x="{x+w*0.12:.0f}" y="{y_mat+d:.0f}" width="{w*0.045:.0f}" '
            f'height="{cao-d:.0f}" fill="{GO_SAU}"/>'
            f'<rect x="{x+w*0.82:.0f}" y="{y_mat+d:.0f}" width="{w*0.045:.0f}" '
            f'height="{cao-d:.0f}" fill="{GO_SAU}"/>'
            f'<rect x="{x:.0f}" y="{y_mat:.0f}" width="{w:.0f}" height="{d:.0f}" '
            f'rx="{d*0.2:.0f}" fill="{GO_SANG}"/>'
            f'<rect x="{x+w*0.05:.0f}" y="{y_mat+d:.0f}" width="{w*0.055:.0f}" '
            f'height="{cao-d:.0f}" fill="{GO_TOI}"/>'
            f'<rect x="{x+w*0.895:.0f}" y="{y_mat+d:.0f}" width="{w*0.055:.0f}" '
            f'height="{cao-d:.0f}" fill="{GO_TOI}"/>')


def ghe(x, y_mat, w, cao):
    """Ghế tựa nhìn ngang. `y_mat` là cao độ MẶT NGỒI, không phải đỉnh lưng ghế
    — nhờ vậy ghế đặt cạnh bàn thì mặt ngồi luôn thấp hơn mặt bàn đúng một
    khoảng hợp lý, thay vì phải canh bằng mắt ở từng cảnh."""
    d = w * 0.09
    y_tua = y_mat - cao * 0.52
    return (f'<rect x="{x:.0f}" y="{y_tua:.0f}" width="{w*0.11:.0f}" '
            f'height="{y_mat-y_tua+cao*0.06:.0f}" rx="{w*0.03:.0f}" fill="{GO_TOI}"/>'
            f'<rect x="{x:.0f}" y="{y_mat:.0f}" width="{w:.0f}" height="{d:.0f}" '
            f'rx="{d*0.25:.0f}" fill="{GO_SANG}"/>'
            f'<rect x="{x+w*0.06:.0f}" y="{y_mat+d:.0f}" width="{w*0.09:.0f}" '
            f'height="{cao*0.48:.0f}" fill="{GO_TOI}"/>'
            f'<rect x="{x+w*0.84:.0f}" y="{y_mat+d:.0f}" width="{w*0.09:.0f}" '
            f'height="{cao*0.48:.0f}" fill="{GO_TOI}"/>')


def chau_cay(x, y, r):
    return (f'<path d="M{x-r:.0f} {y} l{r*0.22:.0f} {r*0.95:.0f} h{r*1.56:.0f} l{r*0.22:.0f} -{r*0.95:.0f}z" fill="{GACH}"/>'
            f'<circle cx="{x}" cy="{y-r*0.7:.0f}" r="{r*0.78:.0f}" fill="{LA_VUA}"/>'
            f'<circle cx="{x-r*0.55:.0f}" cy="{y-r*0.35:.0f}" r="{r*0.5:.0f}" fill="{LA_GAN}"/>'
            f'<circle cx="{x+r*0.52:.0f}" cy="{y-r*0.4:.0f}" r="{r*0.46:.0f}" fill="{LA_GAN}"/>')


def den(x, y, h):
    """Đèn bàn: chao đèn hình thang cộng một quầng sáng mờ."""
    return (f'<circle cx="{x}" cy="{y-h*0.55:.0f}" r="{h*0.7:.0f}" fill="{NANG}" opacity=".45"/>'
            f'<path d="M{x-h*0.34:.0f} {y-h*0.42:.0f} l{h*0.12:.0f} -{h*0.32:.0f} h{h*0.44:.0f} '
            f'l{h*0.12:.0f} {h*0.32:.0f}z" fill="{GACH}"/>'
            f'<rect x="{x-h*0.04:.0f}" y="{y-h*0.42:.0f}" width="{h*0.08:.0f}" height="{h*0.36:.0f}" fill="{KIM_LOAI}"/>'
            f'<rect x="{x-h*0.18:.0f}" y="{y-h*0.08:.0f}" width="{h*0.36:.0f}" height="{h*0.08:.0f}" fill="{KIM_LOAI}"/>')


def xe_dap(x, y, r):
    return (f'<g fill="none" stroke="{XANH}" stroke-width="{r*0.16:.0f}" stroke-linecap="round">'
            f'<circle cx="{x}" cy="{y}" r="{r}"/><circle cx="{x+r*3.1:.0f}" cy="{y}" r="{r}"/>'
            f'<path d="M{x} {y} l{r*1.1:.0f} -{r*1.9:.0f} h{r*1.25:.0f} l{r*0.85:.0f} {r*1.9:.0f}'
            f' M{x+r*1.1:.0f} {y-r*1.9:.0f} l-{r*0.3:.0f} {r*1.9:.0f} h{r*1.55:.0f}"/>'
            f'<path d="M{x+r*2.2:.0f} {y-r*2.1:.0f} h{r*0.8:.0f}"/></g>')


# ════════════════════════════════════════════════════════════════════════════
#  26 cảnh
# ════════════════════════════════════════════════════════════════════════════
def ve_tat_ca():
    # ── hero: nhà sàn bên sông lúc hoàng hôn ────────────────────────────────
    W, H = 1600, 900
    than = (troi(W, H, (1150, 300, 84)) + day_cay_xa(W, 545)
            + dua(250, 620, 330) + dua(430, 625, 235)
            + nha_san(880, 400, 470, 200)
            + mat_nuoc(W, H, 622) + xuong(640, 706, 250))
    tep('hero.svg', W, H, 'Nhà sàn Homestay TVH bên sông, hàng dừa và chiếc xuồng nhỏ', than)

    W, H = 800, 600
    # ── about: vườn sau nhà ─────────────────────────────────────────────────
    than = (troi(W, H, (180, 130, 56)) + day_cay_xa(W, 330)
            + bai_co(W, H, 360) + cay_la(620, 300, 96) + cay_la(700, 350, 66, LA_GAN)
            + nha_san(200, 250, 250, 118)
            + chau_cay(120, 520, 34) + chau_cay(196, 540, 26)
            + f'<path d="M300 600 Q400 500 520 430" stroke="{SAN}" stroke-width="64" fill="none" stroke-linecap="round"/>')
    tep('about.svg', W, H, 'Vườn sau nhà', than)

    # ── standard-1: phòng đôi, cửa sổ hướng lối đi ──────────────────────────
    YS = 396
    than = (phong_nen(W, H, YS) + cua_so(470, 96, 240, 200)
            + tham(120, 440, 520, 130)
            + giuong(90, YS, 330) + tu_dau_giuong(438, YS, 96)
            + den(486, YS - 74, 66) + chau_cay(706, 500, 42))
    tep('standard-1.svg', W, H, 'Phòng Standard', than)

    # ── standard-2: góc bàn làm việc ────────────────────────────────────────
    YS = 400
    than = (phong_nen(W, H, YS) + cua_so(110, 92, 220, 190)
            + ban(150, 330, 380, 170) + ghe(400, 352, 130, 178)
            + den(200, 330, 80)
            + f'<rect x="300" y="292" width="140" height="40" rx="4" fill="{XANH}"/>'
            + f'<rect x="318" y="332" width="104" height="8" fill="{GO_TOI}"/>'
            + chau_cay(660, 470, 46))
    tep('standard-2.svg', W, H, 'Góc bàn làm việc', than)

    # ── deluxe-1: phòng rộng hơn, hai cửa sổ ────────────────────────────────
    YS = 392
    than = (phong_nen(W, H, YS) + cua_so(70, 92, 200, 180) + cua_so(530, 92, 200, 180)
            + tham(150, 440, 520, 136)
            + giuong(210, YS, 380) + tu_dau_giuong(120, YS, 84)
            + den(162, YS - 66, 60) + chau_cay(712, 508, 46))
    tep('deluxe-1.svg', W, H, 'Phòng Deluxe', than)

    # ── deluxe-2: ban công hướng vườn ───────────────────────────────────────
    YS = 432
    than = (troi(W, H, (620, 120, 60)) + day_cay_xa(W, 300) + bai_co(W, H, 344)
            + cay_la(170, 236, 90) + cay_la(654, 276, 70, LA_GAN)
            + f'<rect y="{YS}" width="{W}" height="{H-YS}" fill="{SAN}"/>'
            + f'<rect y="{YS-12}" width="{W}" height="14" fill="{GO_SANG}"/>'
            # lan can ban công
            + f'<rect y="330" width="{W}" height="12" fill="{GO_SANG}"/>'
            + ''.join(f'<rect x="{x}" y="342" width="10" height="78" fill="{GO_SANG}"/>'
                      for x in range(34, W, 92))
            + ghe(214, 486, 130, 156) + ban(372, 466, 190, 146)
            + chau_cay(676, 566, 38))
    tep('deluxe-2.svg', W, H, 'Ban công hướng vườn', than)

    # ── deluxe-3: phòng tắm riêng ───────────────────────────────────────────
    YS = 424
    than = (f'<rect width="{W}" height="{H}" fill="{TUONG}"/>'
            # tường ốp gạch: kẻ cả ngang lẫn dọc, nếu chỉ kẻ ngang thì ra vách gỗ
            + ''.join(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}" stroke="{TUONG_BONG}" stroke-width="3"/>'
                      for y in range(76, YS, 58))
            + ''.join(f'<line x1="{x}" y1="0" x2="{x}" y2="{YS}" stroke="{TUONG_BONG}" stroke-width="3"/>'
                      for x in range(58, W, 58))
            + f'<rect y="{YS}" width="{W}" height="{H-YS}" fill="{SAN}"/>'
            # buồng tắm kính ở bên phải
            + f'<rect x="470" y="110" width="270" height="{YS-110}" fill="{LA_XA}" opacity=".30"/>'
            + f'<rect x="470" y="110" width="270" height="{YS-110}" fill="none" stroke="{KIM_LOAI}" stroke-width="7"/>'
            + f'<rect x="596" y="168" width="9" height="120" fill="{KIM_LOAI}"/>'
            + f'<ellipse cx="600" cy="164" rx="40" ry="13" fill="{KIM_LOAI}"/>'
            + ''.join(f'<line x1="{x}" y1="182" x2="{x-10}" y2="296" stroke="{NUOC_TREN}" '
                      f'stroke-width="5" opacity=".45" stroke-linecap="round"/>'
                      for x in range(570, 636, 16))
            # bồn rửa và gương
            + f'<rect x="96" y="150" width="200" height="140" rx="8" fill="{LA_XA}" opacity=".45"/>'
            + f'<rect x="96" y="150" width="200" height="140" rx="8" fill="none" stroke="{GO_SANG}" stroke-width="10"/>'
            + f'<rect x="80" y="330" width="232" height="26" rx="10" fill="{TUONG}"/>'
            + f'<rect x="80" y="330" width="232" height="26" rx="10" fill="none" stroke="{KIM_LOAI}" stroke-width="5"/>'
            + f'<rect x="130" y="356" width="132" height="{YS-356}" fill="{GO_SANG}"/>'
            + f'<rect x="189" y="296" width="12" height="38" fill="{KIM_LOAI}"/>'
            + f'<path d="M195 296 q0 -26 30 -26" stroke="{KIM_LOAI}" stroke-width="11" fill="none"/>'
            # khăn tắm vắt trên thanh
            + f'<rect x="334" y="296" width="116" height="9" rx="4" fill="{KIM_LOAI}"/>'
            + f'<rect x="344" y="302" width="46" height="106" rx="7" fill="{VAI}"/>'
            + f'<line x1="344" y1="332" x2="390" y2="332" stroke="{VAI_BONG}" stroke-width="5"/>'
            + f'<rect x="396" y="302" width="46" height="88" rx="7" fill="{GACH}" opacity=".6"/>'
            + f'<line x1="396" y1="328" x2="442" y2="328" stroke="{GACH}" stroke-width="5"/>'
            + chau_cay(720, 520, 36))
    tep('deluxe-3.svg', W, H, 'Phòng tắm riêng', than)

    # ── family-1: phòng gia đình, hai giường ────────────────────────────────
    YS = 400
    than = (phong_nen(W, H, YS) + cua_so(316, 84, 176, 156)
            + tham(60, 448, 680, 126)
            + giuong(40, YS, 290) + giuong(470, YS, 290)
            + tu_dau_giuong(350, YS, 96) + den(398, YS - 74, 62))
    tep('family-1.svg', W, H, 'Phòng gia đình', than)

    # ── family-2: bàn ăn trong phòng ────────────────────────────────────────
    YS = 470
    than = (phong_nen(W, H, YS) + cua_so(516, 100, 210, 176)
            + tham(110, 452, 560, 128)
            + ghe(126, 386, 118, 200) + ghe(578, 386, 118, 200)
            + ban(200, 340, 400, 180)
            + f'<ellipse cx="300" cy="348" rx="46" ry="12" fill="{VAI}"/>'
            + f'<ellipse cx="300" cy="345" rx="28" ry="7" fill="{GACH}" opacity=".75"/>'
            + f'<ellipse cx="452" cy="350" rx="38" ry="10" fill="{VAI}"/>'
            + chau_cay(716, 540, 36))
    tep('family-2.svg', W, H, 'Bàn ăn trong phòng', than)

    # ── bungalow-1: bungalow ven sông ───────────────────────────────────────
    than = (troi(W, H, (640, 130, 64)) + day_cay_xa(W, 330)
            + dua(150, 430, 250) + nha_san(380, 300, 300, 140)
            + mat_nuoc(W, H, 440) + xuong(120, 500, 150))
    tep('bungalow-1.svg', W, H, 'Bungalow ven sông', than)

    # ── bungalow-2: hiên trước bungalow ─────────────────────────────────────
    YS = 430
    than = (troi(W, H, (150, 140, 56)) + day_cay_xa(W, 300) + mat_nuoc(W, H, 340)
            + f'<rect y="{YS}" width="{W}" height="{H-YS}" fill="{SAN}"/>'
            + f'<rect y="{YS-12}" width="{W}" height="14" fill="{GO_SANG}"/>'
            # lan can chắn giữa hiên và sông
            + f'<rect y="336" width="{W}" height="12" fill="{GO_SANG}"/>'
            + ''.join(f'<rect x="{x}" y="348" width="10" height="70" fill="{GO_SANG}"/>'
                      for x in range(36, W, 96))
            # mái che nửa trên, đỡ bằng hai cột thật
            + f'<rect y="58" width="{W}" height="30" fill="{MAI}"/>'
            + f'<rect x="56" y="88" width="20" height="248" fill="{GO_SANG}"/>'
            + f'<rect x="724" y="88" width="20" height="248" fill="{GO_SANG}"/>'
            + ghe(180, 470, 140, 170) + ghe(500, 470, 140, 170)
            + ban(348, 452, 130, 148) + chau_cay(680, 570, 34))
    tep('bungalow-2.svg', W, H, 'Hiên trước bungalow', than)

    # ── gallery-1: lối vào nhà chính ────────────────────────────────────────
    than = (troi(W, H, (400, 110, 58)) + day_cay_xa(W, 300) + bai_co(W, H, 340)
            + dua(110, 420, 260) + dua(700, 430, 210)
            + nha_san(300, 280, 220, 110)
            + f'<path d="M330 600 L400 400 L470 400 L500 600Z" fill="{SAN}"/>'
            + ''.join(f'<line x1="{340+i*6}" y1="{560-i*40}" x2="{492-i*6}" y2="{560-i*40}" '
                      f'stroke="{TUONG_BONG}" stroke-width="5"/>' for i in range(4)))
    tep('gallery-1.svg', W, H, 'Lối vào nhà chính', than)

    # ── gallery-2: vườn xoài ────────────────────────────────────────────────
    than = (troi(W, H, (660, 110, 52)) + day_cay_xa(W, 310) + bai_co(W, H, 350)
            + cay_la(170, 240, 110) + cay_la(400, 280, 92) + cay_la(620, 250, 100)
            + ''.join(f'<circle cx="{x}" cy="{y}" r="11" fill="{GACH}"/>'
                      for x, y in ((150,290),(196,262),(384,320),(430,300),(600,296),(648,276))))
    tep('gallery-2.svg', W, H, 'Vườn xoài', than)

    # ── gallery-3: bến sông ─────────────────────────────────────────────────
    than = (troi(W, H, (600, 120, 66)) + day_cay_xa(W, 296)
            + mat_nuoc(W, H, 336) + dua(110, 348, 230)
            + ''.join(f'<rect x="{x}" y="352" width="14" height="118" fill="{GO_TOI}"/>'
                      for x in range(330, 700, 74))
            + f'<rect x="318" y="334" width="400" height="20" fill="{GO_SANG}"/>'
            + xuong(150, 470, 170))
    tep('gallery-3.svg', W, H, 'Bến sông', than)

    # ── gallery-4: hiên bungalow lúc chiều ──────────────────────────────────
    YS = 440
    than = (troi(W, H, (690, 200, 92)) + day_cay_xa(W, 310) + mat_nuoc(W, H, 350)
            + f'<rect y="{YS}" width="{W}" height="{H-YS}" fill="{SAN}"/>'
            + f'<rect y="{YS-12}" width="{W}" height="14" fill="{GO_SANG}"/>'
            + f'<rect y="344" width="{W}" height="12" fill="{GO_SANG}"/>'
            + ''.join(f'<rect x="{x}" y="356" width="10" height="72" fill="{GO_SANG}"/>'
                      for x in range(30, W, 98))
            + f'<rect y="70" width="{W}" height="28" fill="{MAI}"/>'
            + f'<rect x="64" y="98" width="20" height="246" fill="{GO_SANG}"/>'
            + ghe(230, 484, 160, 168) + ban(440, 462, 150, 150)
            + den(150, 470, 60) + chau_cay(700, 566, 32))
    tep('gallery-4.svg', W, H, 'Hiên bungalow lúc chiều', than)

    # ── gallery-5: tô bún nước lèo ──────────────────────────────────────────
    than = (f'<rect width="{W}" height="{H}" fill="{SAN}"/>'
            + ''.join(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}" stroke="{TUONG_BONG}" stroke-width="6" opacity=".55"/>'
                      for y in range(80, 600, 96))
            + f'<ellipse cx="400" cy="330" rx="210" ry="60" fill="{TUONG_BONG}" opacity=".5"/>'
            + f'<path d="M212 300 a188 188 0 0 0 376 0z" fill="{TUONG}"/>'
            + f'<ellipse cx="400" cy="300" rx="188" ry="62" fill="{XANH}"/>'
            + f'<ellipse cx="400" cy="300" rx="150" ry="48" fill="{GACH}" opacity=".85"/>'
            + ''.join(f'<path d="M{x} 286 q26 -18 52 0" stroke="{TUONG}" stroke-width="9" fill="none" stroke-linecap="round"/>'
                      for x in (296, 360, 424))
            + f'<circle cx="470" cy="300" r="20" fill="{LA_VUA}"/>'
            + f'<rect x="596" y="210" width="16" height="170" rx="8" fill="{GO_SANG}" transform="rotate(14 604 295)"/>'
            + f'<rect x="632" y="210" width="16" height="170" rx="8" fill="{GO_SANG}" transform="rotate(14 640 295)"/>')
    tep('gallery-5.svg', W, H, 'Bún nước lèo', than)

    # ── gallery-6: bàn ăn ngoài hiên ────────────────────────────────────────
    YS = 424
    than = (troi(W, H, (150, 110, 50)) + day_cay_xa(W, 288) + bai_co(W, H, 330)
            + f'<rect y="{YS}" width="{W}" height="{H-YS}" fill="{SAN}"/>'
            + ghe(96, 386, 124, 196) + ghe(600, 386, 124, 196)
            + ban(192, 338, 420, 190)
            + f'<ellipse cx="290" cy="346" rx="44" ry="12" fill="{VAI}"/>'
            + f'<ellipse cx="420" cy="346" rx="44" ry="12" fill="{VAI}"/>'
            + f'<rect x="500" y="310" width="24" height="34" rx="4" fill="{GACH}" opacity=".8"/>')
    tep('gallery-6.svg', W, H, 'Bàn ăn ngoài hiên', than)

    # ── gallery-7: xe đạp cho khách mượn ────────────────────────────────────
    than = (troi(W, H, (660, 120, 56)) + day_cay_xa(W, 300) + bai_co(W, H, 350)
            + f'<rect y="450" width="{W}" height="150" fill="{SAN}"/>'
            + xe_dap(120, 470, 62) + xe_dap(420, 470, 62)
            + cay_la(700, 290, 86))
    tep('gallery-7.svg', W, H, 'Xe đạp cho khách mượn', than)

    # ── gallery-8: đường đất đỏ vào cổng ────────────────────────────────────
    than = (troi(W, H, (400, 110, 60)) + day_cay_xa(W, 300) + bai_co(W, H, 340)
            + f'<path d="M300 340 L500 340 L720 600 L80 600Z" fill="{GACH}" opacity=".78"/>'
            + dua(150, 400, 270) + dua(660, 405, 240)
            + f'<rect x="322" y="230" width="16" height="120" fill="{GO_TOI}"/>'
            + f'<rect x="462" y="230" width="16" height="120" fill="{GO_TOI}"/>'
            + f'<rect x="306" y="212" width="188" height="20" fill="{MAI}"/>')
    tep('gallery-8.svg', W, H, 'Đường đất đỏ vào cổng', than)

    # ── map: đường vào homestay ─────────────────────────────────────────────
    than = (f'<rect width="{W}" height="{H}" fill="{TUONG}"/>'
            + f'<defs>{dai_mau("nuoc", NUOC_TREN, NUOC_DUOI)}</defs>'
            # sông chạy dọc mép dưới, homestay nằm TRÊN BỜ chứ không dưới nước
            + f'<path d="M0 520 Q220 486 430 520 T800 504 L800 600 L0 600Z" fill="url(#nuoc)" opacity=".9"/>'
            # đường lớn nằm ngang phía trên
            + f'<rect y="104" width="{W}" height="54" fill="{SAN}"/>'
            + ''.join(f'<line x1="{x}" y1="131" x2="{x+34}" y2="131" stroke="{TUONG}" stroke-width="5"/>'
                      for x in range(20, W, 76))
            # đường nhánh nối thẳng từ đường lớn xuống tới homestay
            + f'<path d="M540 158 L540 300 Q540 372 452 400" stroke="{SAN}" stroke-width="42" '
            f'fill="none" stroke-linecap="round"/>'
            + cay_la(150, 300, 62) + cay_la(690, 320, 54) + cay_la(250, 430, 44)
            + nha_san(330, 372, 160, 74)
            + f'<circle cx="410" cy="330" r="26" fill="{GACH}"/>'
            + f'<circle cx="410" cy="330" r="9" fill="{TUONG}"/>')
    tep('map.svg', W, H, 'Đường vào homestay', than)

    # ── banner 960×360 ──────────────────────────────────────────────────────
    BW, BH = 960, 360
    than = (troi(BW, BH, (800, 90, 52)) + day_cay_xa(BW, 210)
            + bai_co(BW, BH, 250) + dua(140, 300, 210) + nha_san(560, 180, 220, 96))
    tep('banner-1.svg', BW, BH, 'Ảnh minh hoạ ưu đãi khách đặt lần đầu', than)

    than = (troi(BW, BH, (170, 100, 56)) + day_cay_xa(BW, 200)
            + mat_nuoc(BW, BH, 236) + xe_dap(560, 268, 44) + dua(760, 250, 190))
    tep('banner-2.svg', BW, BH, 'Ảnh minh hoạ ưu đãi đặt ba đêm', than)

    than = (troi(BW, BH, (480, 95, 58)) + day_cay_xa(BW, 205) + bai_co(BW, BH, 245)
            + cay_la(200, 170, 74) + cay_la(760, 180, 66)
            + ''.join(f'<circle cx="{x}" cy="{y}" r="9" fill="{GACH}"/>'
                      for x, y in ((180,200),(224,178),(742,212),(786,190))))
    tep('banner-3.svg', BW, BH, 'Ảnh minh hoạ ưu đãi Tết đã kết thúc', than)

    # ── ảnh bìa bài viết 960×540 ────────────────────────────────────────────
    PW, PH = 960, 540
    than = (troi(PW, PH, (760, 130, 72)) + day_cay_xa(PW, 300)
            + bai_co(PW, PH, 340) + dua(160, 420, 280) + nha_san(560, 280, 240, 110))
    tep('post-1.svg', PW, PH, 'Đi Trà Vinh mùa nào đẹp', than)

    than = (f'<rect width="{PW}" height="{PH}" fill="{SAN}"/>'
            + ''.join(f'<line x1="0" y1="{y}" x2="{PW}" y2="{y}" stroke="{TUONG_BONG}" stroke-width="6" opacity=".5"/>'
                      for y in range(60, 540, 90))
            + f'<path d="M300 268 a180 180 0 0 0 360 0z" fill="{TUONG}"/>'
            + f'<ellipse cx="480" cy="268" rx="180" ry="58" fill="{XANH}"/>'
            + f'<ellipse cx="480" cy="268" rx="142" ry="44" fill="{GACH}" opacity=".85"/>'
            + f'<circle cx="548" cy="266" r="19" fill="{LA_VUA}"/>'
            + f'<ellipse cx="200" cy="400" rx="86" ry="26" fill="{TUONG}"/>'
            + f'<ellipse cx="770" cy="392" rx="76" ry="24" fill="{TUONG}"/>')
    tep('post-2.svg', PW, PH, 'Năm món nên ăn', than)

    than = (troi(PW, PH, (200, 120, 64)) + day_cay_xa(PW, 300) + bai_co(PW, PH, 340)
            + f'<path d="M0 470 Q300 430 620 470 T960 452 L960 540 L0 540Z" fill="{SAN}"/>'
            + xe_dap(300, 452, 56) + dua(780, 400, 250) + cay_la(120, 290, 74))
    tep('post-3.svg', PW, PH, 'Đạp xe vòng cù lao', than)


if __name__ == '__main__':
    ve_tat_ca()
    tep_ra = sorted(f for f in os.listdir(RA) if f.endswith('.svg'))
    tong = sum(os.path.getsize(os.path.join(RA, f)) for f in tep_ra)
    print(f'da ve {len(tep_ra)} tep, tong {tong/1024:.1f} KB')
