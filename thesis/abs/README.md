# Báo cáo trình bày

| Tệp | Nội dung |
|---|---|
| `slide-bao-ve.pptx` | 20 slide, khổ 16:9, tiêu đề 44pt, nội dung 32pt |
| `slide-bao-ve.pdf` | Bản PDF của cùng bộ slide, để chiếu khi máy không có PowerPoint |

Kịch bản nói cho từng slide ở **`docs/kich-ban-bao-ve.md`**, và cũng nằm ngay
trong phần ghi chú của từng slide khi mở bằng PowerPoint.

## Dựng lại

```sh
pip install python-pptx
python3 scripts/tao-slide.py thesis/abs/slide-bao-ve.pptx
python3 scripts/tao-kich-ban.py docs/kich-ban-bao-ve.md
```

Nội dung slide nằm ở `scripts/noi-dung-slide.py` — sửa ở đó rồi chạy lại **cả
hai** script để slide và kịch bản nói không lệch nhau.

## Việc còn phải làm tay

1. **Điền thông tin sinh viên ở slide bìa**: tên GVHD, tên SVTH, mã số, lớp.
   Hiện là dấu chấm cho điền tay, giống các trang bìa của quyển báo cáo.
2. **Tập nói và bấm giờ.** Hội đồng cho 10–20 phút *kể cả hỏi đáp*, nên phần
   trình bày chỉ nên 10–12 phút — trung bình 35 giây một slide.
3. **Poster 90cm × 60cm hướng đứng** — cộng tối đa 1 điểm, chưa làm.
