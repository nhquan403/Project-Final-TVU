# -*- coding: utf-8 -*-
"""Dung ban Word cua bao cao tu Markdown o docs/bao-cao/.

Ap dung dung quy dinh trinh bay cua Khoa Ky thuat va Cong nghe, DH Tra Vinh:
Times New Roman 13pt, gian dong 1.5, cach doan truoc/sau 6pt, kho A4, le tren
2cm - duoi 2cm - trai 3cm - phai 2cm, so trang goc phai duoi, chan trang mang
"GVHD:" ben trai va "SVTH:" ben phai.

Cach dung:

    pip install python-docx
    python3 scripts/xuat-ban-word.py noi-dung thesis/doc/bao-cao-noi-dung.docx \
        "Ho ten GVHD" "Ho ten SVTH"
    python3 scripts/xuat-ban-word.py phu-luc  thesis/doc/bao-cao-tltk-phu-luc.docx

Ba che do: "noi-dung" (MO DAU + Chuong 1-5, day la phan bi tinh 30-50 trang),
"phu-luc" (tai lieu tham khao + phu luc, khong tinh trang) va "day-du" (ca hai).

Tep docs/bao-cao/00-phan-dau.md KHONG duoc xuat: do la ban dac ta cach trinh bay
cac trang bia theo bieu mau BM5, phai dung tay trong Word theo dung bieu mau.

Hinh duoc chen tu docs/images/bao-cao/ theo dau cho dat [Hinh X.Y], kem chu
thich "Hinh X.Y - <mo ta> (Nguon: tac gia)" lay tu chinh cau dan trong phan chu.
Rieng Hinh 1.3 chup tu trang web ben ngoai nen phai tu sua lai dong nguon.
"""
import re, os, sys, struct
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

DOCS = '/home/user/Project-Final-TVU/docs/bao-cao'
IMGS = '/home/user/Project-Final-TVU/docs/images/bao-cao'

# ── kich thuoc PNG doc truc tiep tu IHDR, khong can thu vien anh ──────────────
def png_size(path):
    with open(path, 'rb') as f:
        head = f.read(26)
    if head[:8] != b'\x89PNG\r\n\x1a\n':
        return None
    w, h = struct.unpack('>II', head[16:24])
    return w, h

def figure_files():
    out = {}
    for f in os.listdir(IMGS):
        m = re.match(r'hinh-([0-9a-z]+)-([0-9]+)-', f)
        if m:
            out[f"{m.group(1).upper()}.{m.group(2)}"] = os.path.join(IMGS, f)
    return out

FIGS = figure_files()

CAPTIONS = {}
def load_captions():
    """Lay mo ta hinh tu cau dan '(Hình X.Y) the hien ...' trong chinh phan chu."""
    for f in sorted(os.listdir(DOCS)):
        if not f.endswith('.md'): continue
        for line in open(os.path.join(DOCS, f), encoding='utf-8'):
            m = re.match(r'Hình ([0-9A-Z]+\.[0-9]+) thể hiện (.+)', line.strip())
            if m and m.group(1) not in CAPTIONS:
                t = m.group(2).rstrip('.').strip()
                t = re.sub(r'\s*,?\s*(trong đó|và Hình).*$', '', t)
                CAPTIONS[m.group(1)] = t[0].upper() + t[1:]
load_captions()

# ── dinh dang chung ──────────────────────────────────────────────────────────
FONT = 'Times New Roman'

def set_font(run, size=13, bold=False, italic=False, mono=False):
    name = 'Consolas' if mono else FONT
    run.font.name = name          # python-docx dat rFonts dung vi tri trong rPr
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic
    rFonts = run._element.get_or_add_rPr().get_or_add_rFonts()
    for a in ('w:cs', 'w:eastAsia'):
        rFonts.set(qn(a), name)

def body_format(p, size=13, spacing=1.5, before=6, after=6,
                align=WD_ALIGN_PARAGRAPH.JUSTIFY):
    pf = p.paragraph_format
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.line_spacing = spacing
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.alignment = align

INLINE = re.compile(r'(\*\*.+?\*\*|\*[^*]+?\*|`[^`]+?`)')

def add_inline(p, text, size=13, base_bold=False):
    text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)   # bo lien ket
    for part in INLINE.split(text):
        if not part: continue
        if part.startswith('**') and part.endswith('**'):
            set_font(p.add_run(part[2:-2]), size, bold=True)
        elif part.startswith('*') and part.endswith('*') and len(part) > 2:
            set_font(p.add_run(part[1:-1]), size, italic=True)
        elif part.startswith('`') and part.endswith('`'):
            set_font(p.add_run(part[1:-1]), size - 1.5, mono=True)
        else:
            set_font(p.add_run(part), size, bold=base_bold)

def shade(cell, hexcolor):
    tcPr = cell._tc.get_or_add_tcPr()
    sh = OxmlElement('w:shd'); sh.set(qn('w:val'),'clear'); sh.set(qn('w:fill'), hexcolor)
    tcPr.append(sh)

def add_figure(doc, num, width_cm):
    path = FIGS.get(num)
    if not path:
        p = doc.add_paragraph(); body_format(p, align=WD_ALIGN_PARAGRAPH.CENTER)
        set_font(p.add_run(f'[ CHUA CO HINH {num} ]'), 13, bold=True)
        return
    size = png_size(path)
    w_cm = width_cm
    if size:
        w_px, h_px = size
        # khong de mot hinh cao qua 17cm, neu khong no chiem tron trang
        w_cm = min(w_cm, 15.0)
        if h_px / w_px * w_cm > 11:
            w_cm = 11 * w_px / h_px
    p = doc.add_paragraph(); body_format(p, before=6, after=2, align=WD_ALIGN_PARAGRAPH.CENTER)
    p.add_run().add_picture(path, width=Cm(w_cm))
    cap = doc.add_paragraph()
    body_format(cap, size=12, spacing=1.0, before=2, after=8, align=WD_ALIGN_PARAGRAPH.CENTER)
    desc = CAPTIONS.get(num, '')
    nguon = 'Nguồn: tác giả'
    set_font(cap.add_run(f'Hình {num}'), 12, bold=True)
    set_font(cap.add_run(f' — {desc} ({nguon})' if desc else f' ({nguon})'), 12, italic=True)

def add_table(doc, rows, width_cm):
    ncol = max(len(r) for r in rows)
    rows = [r + [''] * (ncol - len(r)) for r in rows]
    t = doc.add_table(rows=len(rows), cols=ncol)
    t.style = 'Table Grid'
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = True
    for i, row in enumerate(rows):
        for j, cell_text in enumerate(row):
            cell = t.cell(i, j)
            cell.text = ''
            p = cell.paragraphs[0]
            body_format(p, size=11, spacing=1.0, before=1, after=1,
                        align=WD_ALIGN_PARAGRAPH.LEFT)
            add_inline(p, cell_text, size=11, base_bold=(i == 0))
            if i == 0:
                shade(cell, 'EFEDE7')
    return t

def add_code(doc, lines):
    p = doc.add_paragraph()
    body_format(p, size=10.5, spacing=1.0, before=6, after=6, align=WD_ALIGN_PARAGRAPH.LEFT)
    p.paragraph_format.left_indent = Cm(0.6)
    for i, l in enumerate(lines):
        if i: p.add_run().add_break()
        set_font(p.add_run(l), 10.5, mono=True)

TABLE_SEP = re.compile(r'^\s*\|?[\s:\-|]+\|[\s:\-|]*$')

def render_markdown(doc, path, width_cm, first_chapter_break=True):
    lines = open(path, encoding='utf-8').read().split('\n')
    i, para, first_h1 = 0, [], True
    def flush():
        nonlocal para
        if para:
            p = doc.add_paragraph(); body_format(p)
            add_inline(p, ' '.join(para))
            para = []
    while i < len(lines):
        line = lines[i]
        s = line.strip()
        if not s:
            flush(); i += 1; continue
        m = re.match(r'^(#{1,4})\s+(.*)', s)
        if m:
            flush()
            lvl, txt = len(m.group(1)), m.group(2).strip()
            p = doc.add_paragraph()
            if lvl == 1:
                if not first_h1 or first_chapter_break:
                    p.paragraph_format.page_break_before = True
                first_h1 = False
                body_format(p, size=14, spacing=1.5, before=0, after=12,
                            align=WD_ALIGN_PARAGRAPH.CENTER)
                set_font(p.add_run(txt.upper()), 14, bold=True)
            else:
                sizes = {2: 13.5, 3: 13, 4: 13}
                body_format(p, size=sizes[lvl], spacing=1.5, before=12, after=6,
                            align=WD_ALIGN_PARAGRAPH.LEFT)
                set_font(p.add_run(txt), sizes[lvl], bold=True)
                p.paragraph_format.keep_with_next = True
            i += 1; continue
        if s.startswith('```'):
            flush(); i += 1; buf = []
            while i < len(lines) and not lines[i].strip().startswith('```'):
                buf.append(lines[i]); i += 1
            i += 1
            add_code(doc, buf); continue
        if re.match(r'^\[Hình ([0-9A-Z]+\.[0-9]+)\]$', s):
            flush()
            add_figure(doc, re.match(r'^\[Hình ([0-9A-Z]+\.[0-9]+)\]$', s).group(1), width_cm)
            i += 1; continue
        if s.startswith('|'):
            flush(); rows = []
            while i < len(lines) and lines[i].strip().startswith('|'):
                raw = lines[i].strip()
                if not TABLE_SEP.match(raw):
                    cells = [c.strip() for c in raw.strip('|').split('|')]
                    rows.append(cells)
                i += 1
            if rows: add_table(doc, rows, width_cm)
            continue
        if s in ('---', '***', '___'):
            flush(); i += 1; continue
        if s.startswith('>'):
            flush(); buf = []
            while i < len(lines) and lines[i].strip().startswith('>'):
                buf.append(lines[i].strip().lstrip('>').strip()); i += 1
            txt = ' '.join(x for x in buf if x)
            if txt:
                p = doc.add_paragraph(); body_format(p, before=8, after=8)
                p.paragraph_format.left_indent = Cm(0.8)
                add_inline(p, txt)
            continue
        m = re.match(r'^([-*+]|\d+\.)\s+(.*)', s)
        if m:
            flush()
            style = 'List Bullet' if m.group(1) in '-*+' else 'List Number'
            buf = [m.group(2)]; i += 1
            while i < len(lines):
                nxt = lines[i]
                if nxt.strip() and not re.match(r'^([-*+]|\d+\.)\s+', nxt.strip()) \
                   and nxt.startswith(('  ', '\t')):
                    buf.append(nxt.strip()); i += 1
                else:
                    break
            p = doc.add_paragraph(style=style)
            body_format(p, spacing=1.5, before=3, after=3)
            add_inline(p, ' '.join(buf))
            continue
        para.append(s); i += 1
    flush()

def footer_and_numbering(section, gvhd, svth):
    f = section.footer
    f.is_linked_to_previous = False
    p = f.paragraphs[0]
    p.text = ''
    pf = p.paragraph_format
    pf.space_before = Pt(0); pf.space_after = Pt(0)
    # hai diem dung tab: giua (SVTH) va phai (so trang); dung API cua python-docx
    from docx.enum.text import WD_TAB_ALIGNMENT
    pf.tab_stops.add_tab_stop(Cm(8), WD_TAB_ALIGNMENT.CENTER)
    pf.tab_stops.add_tab_stop(Cm(16), WD_TAB_ALIGNMENT.RIGHT)
    set_font(p.add_run(f'GVHD: {gvhd}'), 11)
    set_font(p.add_run('\t'), 11)
    set_font(p.add_run(f'SVTH: {svth}'), 11)
    set_font(p.add_run('\t'), 11)
    r = p.add_run(); set_font(r, 11)
    for el, attrs, txt in ((('w:fldChar'), {'w:fldCharType': 'begin'}, None),
                           (('w:instrText'), {'xml:space': 'preserve'}, ' PAGE '),
                           (('w:fldChar'), {'w:fldCharType': 'end'}, None)):
        e = OxmlElement(el)
        for k, v in attrs.items(): e.set(qn(k), v)
        if txt: e.text = txt
        r._r.append(e)

def new_doc(gvhd='...', svth='...'):
    doc = Document()
    st = doc.styles['Normal']
    st.font.name = FONT; st.font.size = Pt(13)
    st.element.rPr.rFonts.set(qn('w:eastAsia'), FONT)
    pf = st.paragraph_format
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE; pf.line_spacing = 1.5
    pf.space_before = Pt(6); pf.space_after = Pt(6)
    s = doc.sections[0]
    s.page_width, s.page_height = Cm(21), Cm(29.7)
    s.top_margin, s.bottom_margin = Cm(2), Cm(2)
    s.left_margin, s.right_margin = Cm(3), Cm(2)
    footer_and_numbering(s, gvhd, svth)
    return doc, 21 - 3 - 2   # be ngang vung chu, cm

if __name__ == '__main__':
    which = sys.argv[1]           # 'noi-dung' | 'day-du'
    out   = sys.argv[2]
    gvhd  = sys.argv[3] if len(sys.argv) > 3 else '..........................'
    svth  = sys.argv[4] if len(sys.argv) > 4 else '..........................'
    CONTENT = ['01-mo-dau.md','02-chuong-1.md','03-chuong-2.md',
               '04a-chuong-3-thiet-ke.md','04b-chuong-3-cai-dat.md',
               '05-chuong-4.md','06-chuong-5.md']
    SAU = ['07-tai-lieu-tham-khao.md','08a-phu-luc-dac-ta.md','08b-phu-luc-ky-thuat.md']
    # '00-phan-dau.md' KHONG xuat ra Word: tep do la ban dac ta cach trinh bay
    # cac trang bia theo BM5 (kem chu thich co chu, kieu chu), phai dung tay
    # trong Word theo dung bieu mau chu khong phai chep sang.
    files = {'noi-dung': CONTENT, 'phu-luc': SAU, 'day-du': CONTENT + SAU}[which]
    doc, w = new_doc(gvhd, svth)
    for n, f in enumerate(files):
        render_markdown(doc, os.path.join(DOCS, f), w, first_chapter_break=(n > 0))
    doc.save(out)
    print(f'  -> {out}  ({len(files)} tệp, {os.path.getsize(out)/1024:.0f} KB)')
    print(f'  hình có chú thích: {len(CAPTIONS)}/{len(FIGS)}')
