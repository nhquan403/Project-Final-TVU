# -*- coding: utf-8 -*-
"""Dung bang commit cho bao cao tien do hang tuan tu lich su git that.

Quy dinh cua khoa lay LICH SU COMMIT lam tieu chi cham diem tien do, nen bang
commit trong bao cao tuan phai khop tung dong voi git log — khong chep tay.

    python3 scripts/tao-bao-cao-tuan.py 38

In ra bang Markdown cua tuan ISO do, dan thang vao muc 5 cua bao cao tuan.
Khong truyen so tuan thi liet ke cac tuan co commit.
"""
import subprocess, sys, collections

def log(fmt, *extra):
    out = subprocess.run(['git', 'log', '--date=format:%G-W%V|%Y-%m-%d',
                          '--pretty=' + fmt, *extra],
                         capture_output=True, text=True, check=True)
    return out.stdout.rstrip('\n').split('\n')

def tuan_co_commit():
    d = collections.Counter()
    ngay = collections.defaultdict(list)
    for line in log('%ad'):
        w, day = line.split('|')
        d[w] += 1
        ngay[w].append(day)
    return d, ngay

def main():
    dem, ngay = tuan_co_commit()
    if len(sys.argv) < 2:
        print('Cac tuan co commit:')
        for w in sorted(dem):
            ds = sorted(ngay[w])
            print('  %s  %2d commit  (%s -> %s)' % (w, dem[w], ds[0], ds[-1]))
        return
    w = sys.argv[1] if sys.argv[1].startswith('20') else '2026-W%02d' % int(sys.argv[1])
    rows = [l for l in log('%ad|%h|%s|%an', '--reverse') if l.startswith(w + '|')]
    if not rows:
        print('Khong co commit nao trong tuan', w); return
    ds = sorted(ngay[w])
    print('**Kỳ báo cáo:** từ %s đến %s — **%d commit**\n' % (ds[0], ds[-1], len(rows)))
    print('| Mã commit | Nội dung |')
    print('|---|---|')
    for r in rows:
        _, _, h, subj, _author = (r.split('|') + [''])[:5]
        print('| `%s` | %s |' % (h, subj))

if __name__ == '__main__':
    main()
