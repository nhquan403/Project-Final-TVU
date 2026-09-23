# -*- coding: utf-8 -*-
"""Dung ban Word cua bao cao theo dung quy dinh trinh bay cua Khoa KT-CN DH Tra Vinh."""
import re, os, sys, struct
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING, WD_BREAK
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = '/home/user/Project-Final-TVU'
DOCS = os.path.join(ROOT, 'docs', 'bao-cao')
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

HTML_TAG = re.compile(r'<(/?)([a-zA-Z][a-zA-Z0-9]*)[^>]*>')
_da_canh_bao = set()


def lam_sach_html(text):
    """Bo the HTML con sot trong Markdown.

    Word khong hieu HTML, nen mot the <br> viet trong nguon se in ra nguyen
    van "<br>" giua trang giay. Da dinh mot lan o bang ky ten cua de cuong.
    <br> doi thanh dau xuong dong; the la thi bo di kem mot dong canh bao,
    vi im lang la cach loi nay quay lai."""
    text = re.sub(r'<br\s*/?>', '\n', text, flags=re.I)
    for m in HTML_TAG.finditer(text):
        ten = m.group(2).lower()
        if ten not in _da_canh_bao:
            _da_canh_bao.add(ten)
            print('  canh bao: bo the HTML <%s> — Word khong hieu HTML' % ten)
    return HTML_TAG.sub('', text)


def add_inline(p, text, size=13, base_bold=False):
    text = lam_sach_html(text)
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
            for i, dong in enumerate(part.split('\n')):
                if i:
                    p.add_run().add_break()
                if dong:
                    set_font(p.add_run(dong), size, bold=base_bold)

# ── danh so: moi danh sach mot day rieng ─────────────────────────────────────
# Style 'List Number' cua Word la MOT day so duy nhat cho ca tai lieu. Dung
# nguyen no thi muc "Dong gop du kien" noi tiep so cua muc "Nhiem vu" truoc do
# (1..9 roi 10, 11, 12), va danh muc tai lieu tham khao mo dau bang "13." —
# trong nhu bi thieu mat 12 muc dau. Moi danh sach vi the phai co numId rieng.
_DANH_SO = {'abstract': None}

def _phan_numbering(doc):
    return doc.part.numbering_part.element

def _tao_abstract_num(doc):
    """Dinh nghia mot kieu danh so thap phan ba cap, dung chung cho moi danh sach."""
    goc = _phan_numbering(doc)
    da_co = [int(a.get(qn('w:abstractNumId')))
             for a in goc.findall(qn('w:abstractNum'))]
    aid = max(da_co) + 1 if da_co else 0
    a = OxmlElement('w:abstractNum')
    a.set(qn('w:abstractNumId'), str(aid))
    for cap in range(3):
        lvl = OxmlElement('w:lvl'); lvl.set(qn('w:ilvl'), str(cap))
        for ten, val in (('w:start', '1'), ('w:numFmt', 'decimal'),
                         ('w:lvlText', '%%%d.' % (cap + 1)), ('w:lvlJc', 'left')):
            e = OxmlElement(ten); e.set(qn('w:val'), val); lvl.append(e)
        pPr = OxmlElement('w:pPr'); ind = OxmlElement('w:ind')
        ind.set(qn('w:left'), str(360 + 360 * cap)); ind.set(qn('w:hanging'), '360')
        pPr.append(ind); lvl.append(pPr)
        a.append(lvl)
    # Trong numbering.xml moi the w:abstractNum phai dung TRUOC moi the w:num.
    dau = goc.find(qn('w:num'))
    dau.addprevious(a) if dau is not None else goc.append(a)
    return aid

def day_so_moi(doc):
    """Cap mot numId chua dung, tuc mot bo dem bat dau lai tu 1."""
    if _DANH_SO['abstract'] is None:
        _DANH_SO['abstract'] = _tao_abstract_num(doc)
    goc = _phan_numbering(doc)
    da_co = [int(n.get(qn('w:numId'))) for n in goc.findall(qn('w:num'))]
    nid = max(da_co) + 1 if da_co else 1
    n = OxmlElement('w:num'); n.set(qn('w:numId'), str(nid))
    ab = OxmlElement('w:abstractNumId'); ab.set(qn('w:val'), str(_DANH_SO['abstract']))
    n.append(ab)
    # Bo dem nam o w:abstractNum, KHONG phai o w:num. Nhieu numId cung tro ve
    # mot abstractNum thi van dem tiep cung mot day — cap numId moi thoi la
    # chua du. Moi day vi the phai tu khai lai diem bat dau bang startOverride.
    for cap in range(3):
        ov = OxmlElement('w:lvlOverride'); ov.set(qn('w:ilvl'), str(cap))
        st = OxmlElement('w:startOverride'); st.set(qn('w:val'), '1')
        ov.append(st); n.append(ov)
    goc.append(n)
    return nid

def gan_day_so(p, nid, cap=0):
    """Gan doan van vao day so nid. Dung get_or_add_* de the w:numPr roi
    dung VI TRI ma lich do OOXML quy dinh trong w:pPr — chen sai cho thi
    LibreOffice tu choi mo tep, khong bao loi gi ro rang."""
    numPr = p._element.get_or_add_pPr().get_or_add_numPr()
    numPr.get_or_add_ilvl().val = cap
    numPr.get_or_add_numId().val = nid

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
        if h_px / w_px * w_cm > 10:
            w_cm = 10 * w_px / h_px
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
    # Bang dai vo sang trang sau thi trang do mat dong tieu de, va nguoi doc
    # khong con biet cot nao la cot nao. Danh dau dong dau la dong tieu de de
    # Word tu lap lai no o moi trang.
    trPr = t.rows[0]._tr.get_or_add_trPr()
    th = OxmlElement('w:tblHeader')
    th.set(qn('w:val'), 'true')
    trPr.append(th)
    # Dong hoan toan rong giu mot chieu cao toi thieu: do la cach mot bang ky
    # ten chua duoc cho de ky. Khong dat thi dong rong cao bang mot dong chu.
    for i, row in enumerate(rows):
        if i and not any(c.strip() for c in row):
            pr = t.rows[i]._tr.get_or_add_trPr()
            h = OxmlElement('w:trHeight')
            h.set(qn('w:val'), '1400')     # ~2,5cm — vua mot cho ky ten
            h.set(qn('w:hRule'), 'atLeast')
            pr.append(h)
    return t

def add_code(doc, lines):
    p = doc.add_paragraph()
    body_format(p, size=10.5, spacing=1.0, before=6, after=6, align=WD_ALIGN_PARAGRAPH.LEFT)
    p.paragraph_format.left_indent = Cm(0.6)
    for i, l in enumerate(lines):
        if i: p.add_run().add_break()
        set_font(p.add_run(l), 10.5, mono=True)

# Dong ke phan cach cua bang PHAI co it nhat mot dau gach. Mau cu chi doi
# "toan ky tu gach, hai cham, khoang trang va gach dung", nen mot dong RONG
# kieu "|  |  |" cung khop va bi bo di im lang — dung ba dong trong chua cho
# ky ten cuoi de cuong bien mat ma khong bao gi.
TABLE_SEP = re.compile(r'^\s*\|?[\s:\-|]*-[\s:\-|]*\|[\s:\-|]*$')

def render_markdown(doc, path, width_cm, first_chapter_break=True):
    lines = open(path, encoding='utf-8').read().split('\n')
    i, para, first_h1 = 0, [], True
    # numId cua danh sach so dang mo; None nghia la doan truoc khong phai
    # muc danh so, nen muc tiep theo mo mot day moi bat dau tu 1.
    day_so_dang_mo = None
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
            # Dong trong KHONG dong day so: mot danh sach "thua" (co dong trong
            # giua cac muc) van la mot danh sach.
            flush(); i += 1; continue
        if not re.match(r'^\d+\.\s+', s):
            # Bat cu khoi nao khac — tieu de, bang, gach dau dong, doan van —
            # deu ket thuc day so dang mo. Dong noi tiep cua mot muc danh sach
            # khong toi duoc day: nhanh danh sach o duoi da nuot chung.
            day_so_dang_mo = None
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
        if s == '\\pagebreak':
            # Dau ngat trang tuong minh. Can cho nhung khoi PHAI nam tron mot
            # trang — vi du khoi ky ten: vo doi giua bang va dong ngay thang
            # thi to giay nhin nhu in hong.
            flush()
            p = doc.add_paragraph()
            p.paragraph_format.page_break_before = True
            p.paragraph_format.space_after = Pt(0)
            i += 1; continue
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
            if style == 'List Number':
                if day_so_dang_mo is None:
                    day_so_dang_mo = day_so_moi(doc)
                gan_day_so(p, day_so_dang_mo)
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
    # Che do thu tu: mot duong dan tep Markdown bat ky, de xuat de cuong hoac
    # bat cu tai lieu nao khac theo cung mot dinh dang.
    if which.endswith('.md'):
        files = [which if os.path.isabs(which) else os.path.join(ROOT, which)]
    else:
        files = {'noi-dung': CONTENT, 'phu-luc': SAU, 'day-du': CONTENT + SAU}[which]
    doc, w = new_doc(gvhd, svth)
    for n, f in enumerate(files):
        path = f if os.path.isabs(f) else os.path.join(DOCS, f)
        render_markdown(doc, path, w, first_chapter_break=(n > 0))
    doc.save(out)
    print(f'  -> {out}  ({len(files)} tệp, {os.path.getsize(out)/1024:.0f} KB)')
    print(f'  hình có chú thích: {len(CAPTIONS)}/{len(FIGS)}')
