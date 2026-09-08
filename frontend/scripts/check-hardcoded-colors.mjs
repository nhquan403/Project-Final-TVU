#!/usr/bin/env node
/**
 * Chặn mã màu nằm ngoài bảng token.
 *
 * Vì sao cần: xoá bảng màu mặc định của Tailwind mới chặn được `text-blue-500`,
 * nhưng không chặn được ai đó viết thẳng `style="color:#3b82f6"` hay
 * `background: rgb(59 130 246)` trong component. Hai lớp đó bổ sung cho nhau.
 *
 * Chỉ `src/styles/tokens.css` được phép chứa mã màu — đó là định nghĩa thương
 * hiệu. Mọi nơi khác phải đọc token.
 */
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const ROOT = new URL('..', import.meta.url).pathname;
const SRC = join(ROOT, 'src');

/** File duy nhất được phép chứa mã màu. */
const ALLOWED = [join('src', 'styles', 'tokens.css')];

const EXTENSIONS = ['.ts', '.html', '.css'];

/** Hex 3 hoặc 6 ký tự, rgb()/rgba(), hsl()/hsla(). */
const PATTERNS = [
  { name: 'mã hex', regex: /#[0-9a-fA-F]{3}(?:[0-9a-fA-F]{3})?\b/g },
  { name: 'rgb()', regex: /\brgba?\s*\(/g },
  { name: 'hsl()', regex: /\bhsla?\s*\(/g },
];

/** Bỏ qua dòng có chú thích `color-lint-ignore` kèm lý do. */
const IGNORE_MARKER = 'color-lint-ignore';

function* walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      yield* walk(full);
    } else if (EXTENSIONS.some((ext) => entry.endsWith(ext))) {
      yield full;
    }
  }
}

const findings = [];

for (const file of walk(SRC)) {
  const rel = relative(ROOT, file);
  if (ALLOWED.includes(rel) || ALLOWED.includes(rel.split('/').join(sep))) {
    continue;
  }
  const lines = readFileSync(file, 'utf8').split('\n');
  lines.forEach((line, index) => {
    if (line.includes(IGNORE_MARKER)) {
      return;
    }
    for (const { name, regex } of PATTERNS) {
      regex.lastIndex = 0;
      const match = regex.exec(line);
      if (match) {
        findings.push({ file: rel, line: index + 1, kind: name, text: match[0] });
      }
    }
  });
}

if (findings.length > 0) {
  console.error(`Tìm thấy ${findings.length} mã màu ngoài tokens.css:\n`);
  for (const finding of findings) {
    console.error(`  ${finding.file}:${finding.line}  ${finding.kind}  ${finding.text}`);
  }
  console.error('\nDùng token trong src/styles/tokens.css thay vì viết mã màu trực tiếp.');
  process.exit(1);
}

console.log('Không có mã màu nào ngoài tokens.css.');
