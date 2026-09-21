/**
 * Quay video trinh dien toan he thong tren ban dang chay that.
 *
 * Di het mot vong: khach tim phong (co ngay bi chan san trong lich), dat
 * phong, chuyen khoan gia lap qua ĐUNG webhook that, thu xac nhan vao hop
 * thu, roi sang khu quan tri xem don, doi soat va ngay kha dung.
 *
 *   docker compose down -v && docker compose up -d
 *   docker compose logs api | grep "Mat khau"
 *   node scripts/quay-demo.mjs http://localhost <mat-khau-quan-tri>
 *
 * PHAI dung CSDL sach truoc moi lan quay. Kich ban di qua buoc buoc doi mat
 * khau tam, nen chay lan thu hai voi cung mat khau cu se dang nhap that bai —
 * va nua sau video thanh anh chup trang dang nhap. Co chot kiem tra chan viec
 * do, nhung dung lai CSDL van la cach re nhat.
 *
 * Video ra dang .webm; truyen duong dan ffmpeg qua FFMPEG=... de doi sang mp4.
 */
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const BASE = process.argv[2] || 'http://localhost';
const ADMIN_PW = process.argv[3] || process.env.ADMIN_PW;
const OUTDIR = process.env.OUTDIR || path.join(ROOT, 'thesis', 'abs');
const CHROME = process.env.CHROMIUM_PATH || '/opt/pw-browsers/chromium-1194/chrome-linux/chrome';
// Ban ffmpeg di kem Playwright chi co VP8, khong co H.264 — dung no thi khong
// ra duoc .mp4. Uu tien ffmpeg cua he thong; khong co thi bo qua buoc doi va
// giu nguyen .webm.
const FFMPEG = process.env.FFMPEG || '/usr/bin/ffmpeg';
const SHOTS = process.env.SHOTS || path.join(OUTDIR, 'demo-anh');

if (!ADMIN_PW) {
  console.error('Thieu mat khau quan tri. Xem: docker compose logs api | grep "Mat khau"');
  process.exit(1);
}

const iso = (n) => new Date(Date.now() + 86400000 * n).toISOString().slice(0, 10);
const NEW_PW = 'Demo' + Math.random().toString(36).slice(2, 10) + '!A1';

// Thanh chu thich chay duoi video. Cai bang addInitScript nen no song qua moi
// lan chuyen trang, va khong phai the <script> noi tuyen nen khong dinh CSP.
const CAPTION_INIT = `
window.__capText = window.__capText || '';
window.__cap = function (t) {
  window.__capText = t;
  let el = document.getElementById('__demo_cap');
  if (!el) {
    el = document.createElement('div');
    el.id = '__demo_cap';
    Object.assign(el.style, {
      position: 'fixed', left: '0', right: '0', bottom: '0', zIndex: '2147483647',
      background: 'rgba(15,92,76,.96)', color: '#fff', padding: '14px 26px',
      font: '600 19px/1.4 system-ui,-apple-system,"Segoe UI",sans-serif',
      letterSpacing: '.01em', boxShadow: '0 -2px 18px rgba(0,0,0,.25)',
      display: 'flex', alignItems: 'center', gap: '14px',
    });
    (document.body || document.documentElement).appendChild(el);
  }
  el.textContent = t;
};
document.addEventListener('DOMContentLoaded', function () {
  if (window.__capText) window.__cap(window.__capText);
});
`;

const log = (s) => console.log('  ' + s);

async function main() {
  fs.mkdirSync(OUTDIR, { recursive: true });
  fs.mkdirSync(SHOTS, { recursive: true });
  const videoDir = path.join(OUTDIR, '.video-tmp');
  fs.rmSync(videoDir, { recursive: true, force: true });

  const browser = await chromium.launch({ executablePath: CHROME });
  const ctx = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    locale: 'vi-VN',
    recordVideo: { dir: videoDir, size: { width: 1440, height: 900 } },
  });
  await ctx.addInitScript(CAPTION_INIT);
  const page = await ctx.newPage();

  let shotNo = 0;
  const cap = async (t, ms = 2200) => {
    await page.evaluate((x) => window.__cap(x), t).catch(() => {});
    await page.waitForTimeout(ms);
  };
  const shot = async (name) => {
    shotNo += 1;
    const p = path.join(SHOTS, `${String(shotNo).padStart(2, '0')}-${name}.png`);
    await page.screenshot({ path: p });
  };
  const go = async (p) => {
    await page.goto(BASE + p, { waitUntil: 'networkidle' }).catch(() => {});
    await page.waitForTimeout(900);
  };

  // ── 1. Trang chu ───────────────────────────────────────────────────────
  await go('/');
  await cap('Homestay TVH — trang chủ. Thanh tìm phòng nằm ngay đầu trang.', 3000);
  await shot('trang-chu');

  // ── 2. Lich co ngay bi chan san ────────────────────────────────────────
  await go('/dat-phong');
  await cap('Bước 1: chọn ngày. Mở lịch ra.', 1800);
  await page.locator('[aria-haspopup=dialog]').first().click();
  await page.waitForTimeout(900);

  const soldOut = iso(12);
  // Lat toi thang co cuoi tuan kin phong de nhin thay o ngay bi chan.
  for (let i = 0; i < 6; i++) {
    if (await page.locator(`[data-date="${soldOut}"]`).count()) break;
    await page.getByLabel('Tháng sau').click().catch(() => {});
    await page.waitForTimeout(450);
  }
  await cap('Ngày hết phòng bị chặn sẵn trong lịch — không bấm chọn được.', 4200);
  await shot('lich-ngay-bi-chan');

  const d1 = iso(20), d2 = iso(23);
  const pick = async (key) => {
    for (let i = 0; i < 8; i++) {
      const c = page.locator(`[data-date="${key}"]`);
      if (await c.count() && await c.first().isEnabled()) { await c.first().click(); return true; }
      await page.getByLabel('Tháng sau').click().catch(() => {});
      await page.waitForTimeout(400);
    }
    return false;
  };
  await pick(d1); await pick(d2);
  await page.keyboard.press('Escape');
  await cap('Chọn một khoảng ngày còn phòng.', 1600);
  await page.getByRole('button', { name: /xem phòng còn trống/i }).click();
  await page.waitForTimeout(2200);

  // ── 3. Chon phong ──────────────────────────────────────────────────────
  await cap('Bước 2: giá hiển thị là TỔNG CẢ KỲ, không phải giá một đêm.', 4000);
  await shot('chon-phong');
  await page.getByRole('button', { name: /^chọn/i }).first().click();
  await page.waitForTimeout(1800);

  // ── 4. Xac nhan ────────────────────────────────────────────────────────
  await cap('Bước 3: điền thông tin. Không bắt tạo tài khoản.', 2000);
  await page.getByLabel(/họ.*tên/i).first().fill('Nguyễn Hoàng Quân');
  await page.getByLabel(/điện thoại/i).first().fill('0912345678');
  await page.getByLabel(/email/i).first().fill('quan.demo@example.vn');
  await page.locator('body').click({ position: { x: 5, y: 5 } }).catch(() => {});
  await cap('Tóm tắt đơn bên phải khớp đúng những gì đã chọn.', 3200);
  await shot('xac-nhan-don');

  await page.getByRole('button', { name: /đặt phòng và chuyển sang thanh toán/i }).click();
  await page.waitForTimeout(5000);

  // ── 5. Man hinh thanh toan ─────────────────────────────────────────────
  const code = (page.url().match(/thanh-toan\/([A-Z0-9-]+)/i) || [])[1] || '';
  log('ma don: ' + code);
  await cap('Mã QR chuyển khoản, và đồng hồ đếm ngược 15 phút giữ chỗ THẬT.', 4500);
  await shot('thanh-toan-qr');

  // ── 6. Webhook that xac nhan don ───────────────────────────────────────
  await cap('Giả lập ngân hàng gọi webhook — đi đúng đường xử lý thật.', 2600);
  const content = code + '01';
  const env = Object.fromEntries(
    fs.readFileSync(path.join(ROOT, '.env'), 'utf8')
      .split('\n').filter((l) => l.includes('=') && !l.trim().startsWith('#'))
      .map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()]));
  const now = new Date().toISOString().slice(0, 19).replace('T', ' ');
  // Lay so tien tu DUNG dong "So tien" trong danh sach dinh nghia, khong quet
  // ca trang bang bieu thuc chinh quy: quet ca trang bat trung con so dau tien
  // co chu "d" dung sau, va so do khong phai tien coc. Chuyen sai so tien thi
  // don roi vao nhanh THIEU TIEN, va phan trinh dien chung minh nham thu khac.
  const amount = await page.evaluate(() => {
    const dt = [...document.querySelectorAll('dt')]
      .find((x) => /^\s*Số tiền\s*$/.test(x.textContent));
    const dd = dt && dt.parentElement.querySelector('dd');
    return dd ? Number(dd.textContent.replace(/[^\d]/g, '')) : 0;
  });
  if (!amount) throw new Error('Khong doc duoc so tien coc tren man hinh thanh toan');
  log('tien coc: ' + amount.toLocaleString('vi-VN') + ' d');
  execFileSync('curl', ['-s', '-o', '/dev/null', '-X', 'POST', `${BASE}/api/payments/webhook/sepay`,
    '-H', `Authorization: Apikey ${env.SEPAY_WEBHOOK_API_KEY}`,
    '-H', 'Content-Type: application/json',
    '--data', JSON.stringify({
      id: Math.floor(Math.random() * 9e8) + 1e5, gateway: 'MBBank', transactionDate: now,
      accountNumber: env.SEPAY_ACCOUNT_NUMBER, subAccount: null, code: null,
      content, transferType: 'in', description: 'CK tu khach',
      transferAmount: amount, accumulated: 0, referenceCode: 'FT' + Date.now(),
    })]);
  await cap('Màn hình tự chuyển khi tiền về — khách không phải bấm gì thêm.', 9000);
  // Chot kiem tra thu hai: chuyen dung so tien thi don phai sang DA XAC NHAN.
  // Chuyen thieu thi don sang CHO DOI SOAT — cung la nhanh that cua he thong,
  // nhung khong phai nhanh ma loi chu thich dang noi toi.
  // Man hinh thanh toan tu chuyen sang trang hoan tat khi tien ve du. Bat theo
  // dung cau chu cua trang do, khong doan.
  const xacNhan = await page.getByText(/đặt phòng thành công|đã xác nhận/i).count();
  if (!xacNhan) {
    const tt = await page.locator('body').innerText();
    throw new Error('Don khong sang trang thai da xac nhan sau webhook. Tren man hinh: '
      + tt.replace(/\s+/g, ' ').slice(0, 200));
  }
  await shot('da-xac-nhan');

  // ── 7. Thu xac nhan trong hop thu ──────────────────────────────────────
  await cap('Thư xác nhận đi qua hàng đợi thư rồi tới hộp thư của khách.', 2000);
  for (let i = 0; i < 8; i++) {
    await page.goto('http://localhost:8025/', { waitUntil: 'networkidle' }).catch(() => {});
    await page.waitForTimeout(2500);
    if (await page.getByText(new RegExp(code)).count()) break;
  }
  await page.getByText(new RegExp(code)).first().click({ timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(2500);
  await cap('Thư xác nhận: mã đơn, ngày ở, tiền cọc và số còn lại.', 5000);
  await shot('thu-xac-nhan');

  // ── 8. Tra cuu don ─────────────────────────────────────────────────────
  await go('/tra-cuu');
  await cap('Khách tra cứu đơn bằng mã đơn và số điện thoại — không cần tài khoản.', 2400);
  await page.getByLabel(/mã đơn/i).first().fill(code).catch(() => {});
  await page.getByLabel(/điện thoại/i).first().fill('0912345678').catch(() => {});
  await page.getByRole('button', { name: /tra cứu|tìm/i }).first().click().catch(() => {});
  await page.waitForTimeout(3000);
  await cap('Đơn hiện đúng trạng thái đã xác nhận.', 3500);
  await shot('tra-cuu-don');

  // ── 9. Khu quan tri ────────────────────────────────────────────────────
  await go('/admin');
  await cap('Khu quản trị — đăng nhập bằng tài khoản dựng sẵn.', 2000);
  if (await page.locator('input[type=email]').count()) {
    await page.locator('input[type=email]').fill('admin@tvh.local');
    await page.locator('input[type=password]').first().fill(ADMIN_PW);
    await page.getByRole('button', { name: /đăng nhập/i }).click();
    await page.waitForTimeout(4000);
  }
  if (/doi-mat-khau/.test(page.url())) {
    await cap('Tài khoản dựng sẵn BẮT BUỘC đổi mật khẩu ở lần đăng nhập đầu.', 3800);
    await shot('buoc-doi-mat-khau');
    const ps = page.locator('input[type=password]'); const n = await ps.count();
    await ps.nth(0).fill(ADMIN_PW); await ps.nth(1).fill(NEW_PW);
    if (n > 2) await ps.nth(2).fill(NEW_PW);
    await page.getByRole('button', { name: /đổi|lưu|xác nhận|cập nhật/i }).first().click();
    await page.waitForTimeout(4500);
    // Mat khau moi KHONG ghi vao repo. Ghi ra thu muc tam cua he dieu hanh va
    // chi in duong dan, de no khong lot vao git va khong hien ra man hinh.
    const pwFile = path.join(os.tmpdir(), 'homestay-demo-admin.pw');
    fs.writeFileSync(pwFile, NEW_PW, { mode: 0o600 });
    log('mat khau quan tri moi da ghi vao: ' + pwFile);
  }

  await go('/admin');
  // Chot kiem tra: neu mat khau sai thi trang van la form dang nhap, va toan bo
  // nua sau video se la anh chup trang dang nhap — dung kich thuoc tep, trong
  // hop ly, va sai hoan toan. Da dinh mot lan roi nen chan thang o day.
  if (await page.locator('input[type=password]').count()) {
    throw new Error(
      'Chua vao duoc khu quan tri — mat khau sai hoac da bi doi. ' +
      'Dung lai CSDL sach: docker compose down -v && docker compose up -d, ' +
      'roi lay mat khau moi tu: docker compose logs api | grep "Mat khau"');
  }
  await cap('Tổng quan: doanh thu theo tháng, tỉ lệ lấp đầy, tỉ lệ huỷ, hàng chờ đối soát.', 5000);
  await shot('tong-quan');

  await go('/admin/bookings');
  await cap('Danh sách đơn — nhiều trạng thái khác nhau trong cùng một màn hình.', 4500);
  await shot('danh-sach-don');

  const link = page.locator('table tbody a').first();
  await link.click({ timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(3000);
  await cap('Chi tiết đơn: dòng thời gian trạng thái, thanh toán, hộp thư đi, ghi chú nội bộ.', 5500);
  await shot('chi-tiet-don');

  await go('/admin/payments');
  await cap('Đối soát: khoản THIẾU tiền chờ khách bù, khoản THỪA tiền phải hoàn lại.', 5000);
  await shot('doi-soat');

  await go('/admin/rooms');
  // Chon mot phong DA CO khoang dong, neu khong thi khu nay chi hien form
  // trong va nguoi xem khong thay duoc danh sach khoang dong trong that.
  const roomSelect = page.getByLabel('Phòng').last();
  const withClosure = await roomSelect.locator('option').filter({ hasText: /^10[1-5] —/ }).first()
    .getAttribute('value').catch(() => null);
  if (withClosure) await roomSelect.selectOption(withClosure).catch(() => {});
  await page.waitForTimeout(2200);
  await page.evaluate(() => {
    const h = [...document.querySelectorAll('h2,h3')]
      .find((x) => /ngày không nhận khách|ngày khả dụng/i.test(x.textContent));
    if (h) window.scrollBy(0, h.getBoundingClientRect().top - 90);
  });
  await cap('Ngày khả dụng: đóng phòng theo khoảng ngày, hệ thống tự mở lại.', 5500);
  await shot('ngay-kha-dung');

  await go('/admin/reviews');
  await cap('Đánh giá chờ duyệt — mỗi đơn chỉ được đánh giá một lần, do CSDL ép.', 4000);
  await shot('danh-gia');

  await go('/admin');
  await cap('Hết phần trình diễn. Toàn bộ chạy trên bản đóng gói docker compose.', 3500);

  await ctx.close();
  await browser.close();

  // ── Xuat video ─────────────────────────────────────────────────────────
  const webm = fs.readdirSync(videoDir).find((f) => f.endsWith('.webm'));
  const src = path.join(videoDir, webm);
  // Ban .webm chi giu lai khi khong doi duoc sang .mp4 — hai ban cung noi dung
  // trong repo la thua, va ban webm nang gap nam lan.
  const outWebm = path.join(OUTDIR, 'demo-homestay-tvh.webm');
  let outMp4 = null;
  if (fs.existsSync(FFMPEG)) {
    const target = path.join(OUTDIR, 'demo-homestay-tvh.mp4');
    try {
      execFileSync(FFMPEG, ['-y', '-loglevel', 'error', '-i', src,
        '-c:v', 'libx264', '-preset', 'medium', '-crf', '26',
        '-pix_fmt', 'yuv420p', '-movflags', '+faststart', target], { stdio: 'ignore' });
      outMp4 = target;
    } catch {
      fs.copyFileSync(src, outWebm);
      log('khong doi duoc sang .mp4 (ffmpeg thieu bo ma hoa H.264) — giu ban .webm');
    }
  } else {
    fs.copyFileSync(src, outWebm);
    log('khong thay ffmpeg — giu ban .webm');
  }
  fs.rmSync(videoDir, { recursive: true, force: true });

  log(`ma don da dat: ${code}`);
  for (const f of [outMp4 || outWebm].filter(Boolean)) {
    log(`${f}  ${(fs.statSync(f).size / 1024 / 1024).toFixed(1)} MB`);
  }
  log(`${shotNo} anh o ${SHOTS}`);
}

main().catch((e) => { console.error(e); process.exit(1); });
