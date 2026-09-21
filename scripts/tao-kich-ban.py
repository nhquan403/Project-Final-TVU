# -*- coding: utf-8 -*-
"""Sinh kich ban noi khi bao ve tu ghi chu cua tung slide.

Ghi chu nam o scripts/noi-dung-slide.py — cung mot nguon su that voi ban .pptx,
nen kich ban va slide khong bao gio lech nhau.

    python3 scripts/tao-kich-ban.py [docs/kich-ban-bao-ve.md]
"""
import os, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEN = {'__COVER__': 'Bìa', '__THANKS__': 'Cảm ơn'}

HEADER = """# Kịch bản nói khi bảo vệ

Sinh tự động từ phần ghi chú của từng slide trong `scripts/noi-dung-slide.py`
bằng `scripts/tao-kich-ban.py`. Sửa ghi chú ở đó rồi chạy lại cả hai script —
đừng sửa thẳng vào tệp này.

**Thời lượng:** hội đồng cho 10–20 phút *kể cả hỏi đáp*, nên phần trình bày chỉ
nên **10–12 phút**. Với 20 slide, trung bình **35 giây một slide**. Ba slide
được phép nói lâu hơn — slide 9, 10 và 16 — nên các slide còn lại phải gọn.

| Đoạn | Slide | Thời lượng |
|---|---|---|
| Mở đầu và bài toán | 1–6 | ~3 phút |
| Vấn đề kỹ thuật then chốt | 7–10 | ~3 phút |
| Thiết kế và cài đặt | 11–15 | ~3 phút |
| Kết quả và kết luận | 16–20 | ~2 phút |

---
"""


def main(out):
    ns = {}
    exec(open(os.path.join(ROOT, 'scripts', 'noi-dung-slide.py'), encoding='utf-8').read(), ns)
    parts = [HEADER]
    for i, (title, bullets, img, notes) in enumerate(ns['SLIDES'], 1):
        parts.append('## Slide %d — %s\n' % (i, TEN.get(title, title)))
        if bullets:
            parts.append('**Trên slide:** ' + ' · '.join(bullets) + '\n')
        if img:
            parts.append('**Hình:** `%s`\n' % img)
        parts.append('**Nói:**\n')
        parts.append('> ' + notes.replace('\n', '\n> ') + '\n')
    open(out, 'w', encoding='utf-8').write('\n'.join(parts))
    print('  -> %s  (%d slide)' % (out, len(ns['SLIDES'])))


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'docs', 'kich-ban-bao-ve.md'))
