# -*- coding: utf-8 -*-
"""Dung bo slide bao ve tu noi dung o slides-data.py.

Khuon: 16:9, tieu de 44pt, noi dung 32pt, chu Times New Roman cho dong bo voi
quyen bao cao. Hinh lay tu docs/images/bao-cao. Kich ban noi di kem tung slide
o phan ghi chu cua nguoi trinh bay.

Cach dung:
    pip install python-pptx
    python3 scripts/tao-slide.py thesis/abs/slide-bao-ve.pptx
"""
import os, sys, struct
from pptx import Presentation
from pptx.util import Cm, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
IMGS = os.path.join(ROOT, 'docs', 'images', 'bao-cao')

# Bang mau lay tu tokens.css cua chinh du an, de slide va san pham cung mot he mau.
XANH   = RGBColor(0x0F, 0x5C, 0x4C)
DAM    = RGBColor(0x1C, 0x19, 0x17)
NHAT   = RGBColor(0x6B, 0x62, 0x5A)
NEN    = RGBColor(0xFB, 0xF9, 0xF6)
TRANG  = RGBColor(0xFF, 0xFF, 0xFF)
CAM    = RGBColor(0xB4, 0x41, 0x0E)

FONT = 'Times New Roman'
W, H = Cm(33.87), Cm(19.05)          # 16:9

def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(26)
    return struct.unpack('>II', head[16:24]) if head[:8] == b'\x89PNG\r\n\x1a\n' else None

def style(run, size, bold=False, color=DAM, italic=False):
    run.font.name = FONT
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color

def bg(slide, color):
    f = slide.background.fill
    f.solid()
    f.fore_color.rgb = color

def hanging(p, left_cm=0.9):
    """Thut dong treo: dong thu hai tro di thang hang voi chu, khong ve sat le.

    python-pptx chua mo API cho marL/indent nen dat thang vao XML cua doan."""
    pPr = p._pPr if p._pPr is not None else p._p.get_or_add_pPr()
    pPr.set('marL', str(int(left_cm * 360000)))
    pPr.set('indent', str(-int(left_cm * 360000)))


def textbox(slide, l, t, w, h, anchor=MSO_ANCHOR.TOP):
    tb = slide.shapes.add_textbox(l, t, w, h)
    tf = tb.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    return tf

def add_notes(slide, text):
    slide.notes_slide.notes_text_frame.text = text

def cover(prs, notes):
    s = prs.slides.add_slide(prs.slide_layouts[6])
    bg(s, XANH)
    tf = textbox(s, Cm(2.5), Cm(3.2), W - Cm(5), Cm(12), MSO_ANCHOR.MIDDLE)
    for txt, size, bold, color, space in [
        ('TRƯỜNG ĐẠI HỌC TRÀ VINH', 20, False, RGBColor(0xC8, 0xDE, 0xD6), 4),
        ('KHOA KỸ THUẬT VÀ CÔNG NGHỆ', 20, False, RGBColor(0xC8, 0xDE, 0xD6), 26),
        ('ĐỒ ÁN THỰC TẬP CHUYÊN NGÀNH', 24, False, RGBColor(0xC8, 0xDE, 0xD6), 12),
        ('XÂY DỰNG WEBSITE GIỚI THIỆU\nVÀ ĐẶT PHÒNG HOMESTAY TVH', 40, True, TRANG, 30),
        ('Giảng viên hướng dẫn:  . . . . . . . . . . . . . . . . . . . .', 20, False, RGBColor(0xC8, 0xDE, 0xD6), 4),
        ('Sinh viên thực hiện:  . . . . . . . . . . . . . . . . . . . .', 20, False, RGBColor(0xC8, 0xDE, 0xD6), 4),
        ('Mã số sinh viên:  . . . . . . . . . . .      Lớp:  . . . . . . . . . . .', 20, False, RGBColor(0xC8, 0xDE, 0xD6), 0),
    ]:
        for i, line in enumerate(txt.split('\n')):
            p = tf.paragraphs[0] if (tf.paragraphs[0].runs == [] and not tf.paragraphs[0].text and len(tf.paragraphs) == 1) else tf.add_paragraph()
            p.alignment = PP_ALIGN.CENTER
            p.space_after = Pt(space if i == len(txt.split('\n')) - 1 else 0)
            style(p.add_run(), size, bold, color)
            p.runs[0].text = line
    add_notes(s, notes)

def thanks(prs, notes):
    s = prs.slides.add_slide(prs.slide_layouts[6])
    bg(s, XANH)
    tf = textbox(s, Cm(2.5), Cm(6), W - Cm(5), Cm(7), MSO_ANCHOR.MIDDLE)
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    style(p.add_run(), 54, True, TRANG); p.runs[0].text = 'Cảm ơn thầy cô đã lắng nghe'
    p2 = tf.add_paragraph(); p2.alignment = PP_ALIGN.CENTER; p2.space_before = Pt(24)
    style(p2.add_run(), 26, False, RGBColor(0xC8, 0xDE, 0xD6))
    p2.runs[0].text = 'Em xin sẵn sàng trả lời câu hỏi'
    add_notes(s, notes)

def content(prs, title, bullets, image, notes):
    s = prs.slides.add_slide(prs.slide_layouts[6])
    bg(s, NEN)
    # dai mau o canh tren, thay cho mot tieu de tran trui
    bar = s.shapes.add_shape(1, Cm(0), Cm(0), W, Cm(0.4))
    bar.fill.solid(); bar.fill.fore_color.rgb = XANH; bar.line.fill.background()

    tf = textbox(s, Cm(1.8), Cm(1.1), W - Cm(3.6), Cm(2.6))
    p = tf.paragraphs[0]
    size = 36 if len(title) > 42 else (40 if len(title) > 32 else 44)
    style(p.add_run(), size, True, XANH)
    p.runs[0].text = title

    top = Cm(4.4)
    has_img = image and os.path.exists(os.path.join(IMGS, image))

    if bullets:
        bw = (W - Cm(3.6)) if not has_img else Cm(14.5)
        tf2 = textbox(s, Cm(1.8), top, bw, H - top - Cm(1.4))
        first = True
        for b in bullets:
            p = tf2.paragraphs[0] if first else tf2.add_paragraph()
            first = False
            p.space_after = Pt(16)
            hanging(p)
            r = p.add_run(); r.text = '▪  ' + b
            style(r, 28 if has_img else 32, False, DAM)

    if has_img:
        path = os.path.join(IMGS, image)
        sz = png_size(path)
        # tinh bang so cm thuan, roi moi doi sang don vi cua thu vien mot lan
        box_l_cm, box_w_cm = (17.0, 15.2) if bullets else (3.0, W.cm - 6.0)
        box_t_cm, box_h_cm = top.cm, H.cm - top.cm - 1.4
        if sz:
            w_px, h_px = sz
            w_cm = box_w_cm
            h_cm = box_w_cm * h_px / w_px
            if h_cm > box_h_cm:                 # cao qua khung thi thu theo chieu cao
                h_cm = box_h_cm
                w_cm = box_h_cm * w_px / h_px
            s.shapes.add_picture(path,
                                 Cm(box_l_cm + (box_w_cm - w_cm) / 2),
                                 Cm(box_t_cm + (box_h_cm - h_cm) / 2),
                                 width=Cm(w_cm), height=Cm(h_cm))
        else:
            s.shapes.add_picture(path, Cm(box_l_cm), Cm(box_t_cm), width=Cm(box_w_cm))

    add_notes(s, notes)

def main(out):
    ns = {}
    exec(open(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                           'noi-dung-slide.py'), encoding='utf-8').read(), ns)
    prs = Presentation()
    prs.slide_width, prs.slide_height = W, H
    for title, bullets, image, notes in ns['SLIDES']:
        if title == '__COVER__':
            cover(prs, notes)
        elif title == '__THANKS__':
            thanks(prs, notes)
        else:
            content(prs, title, bullets, image, notes)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    prs.save(out)
    print('  -> %s  (%d slide, %.0f KB)' % (out, len(prs.slides._sldIdLst), os.path.getsize(out) / 1024))
    kb = os.path.join(ROOT, 'docs', 'kich-ban-bao-ve.md')
    if os.path.exists(kb):
        print('     kich ban noi: %s (chay scripts/tao-kich-ban.py de sinh lai)' % kb)

if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, 'thesis', 'abs', 'slide-bao-ve.pptx'))
