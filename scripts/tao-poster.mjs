/**
 * Dung poster 60cm x 90cm huong dung cho buoi showcase.
 *
 * Quy dinh cua khoa: poster 90cm x 60cm huong dung, cong toi da 1 diem.
 * Dung Chromium in ra PDF dung kich thuoc vat ly, nen dem di in la ra dung co.
 *
 *   node scripts/tao-poster.mjs [thesis/abs/poster.pdf]
 *
 * Khong dung font tai qua mang: may in va may render deu phai ra cung mot ket
 * qua, nen chi dung font co san trong he thong.
 */
import { chromium } from 'playwright';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const IMGS = path.join(ROOT, 'docs', 'images', 'bao-cao');
const OUT = process.argv[2] || path.join(ROOT, 'thesis', 'abs', 'poster.pdf');
const CHROME = process.env.CHROMIUM_PATH || '/opt/pw-browsers/chromium-1194/chrome-linux/chrome';

const img = (f) => `file://${path.join(IMGS, f)}`;

const html = `<!doctype html><meta charset="utf-8">
<style>
  @page { size: 60cm 90cm; margin: 0; }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  :root {
    --xanh: #0F5C4C; --xanh-nhat: #E6EFEC; --dam: #1C1917; --nhat: #6B625A;
    --nen: #FBF9F6; --cam: #B4410E; --vien: #D6CFC4;
  }
  body { width: 60cm; height: 90cm; background: var(--nen); color: var(--dam);
         font: 15pt/1.45 "Liberation Serif","Times New Roman",serif; overflow: hidden; }
  .sans { font-family: "DejaVu Sans","Liberation Sans",sans-serif; }

  header { background: var(--xanh); color: #fff; padding: 1.5cm 2cm 1.3cm; text-align: center; }
  header .truong { font-size: 15pt; letter-spacing: .06em; color: #C8DED6; }
  header h1 { font-size: 41pt; line-height: 1.15; margin: .5cm 0 .35cm;
              font-family: "DejaVu Sans","Liberation Sans",sans-serif; }
  header .ai { font-size: 15pt; color: #C8DED6; }
  header .ai b { color: #fff; }

  main { padding: 1.1cm 1.5cm; display: grid; grid-template-columns: 1fr 1fr;
         gap: .9cm 1.1cm; align-content: start; }
  section { background: #fff; border: 1.5pt solid var(--vien); border-radius: 6pt;
            padding: .65cm .8cm .75cm; display: flex; flex-direction: column; }
  /* Doan cuoi bi day xuong day o o cao hon, thay vi de mot khoang trang lung lo */
  .day { margin-top: auto; padding-top: .3cm; }
  section.wide { grid-column: 1 / -1; }
  h2 { font-size: 19pt; color: var(--xanh); margin-bottom: .35cm;
       font-family: "DejaVu Sans","Liberation Sans",sans-serif;
       border-bottom: 2.5pt solid var(--xanh); padding-bottom: .15cm; }
  ul { list-style: none; }
  li { padding-left: .75cm; text-indent: -.75cm; margin-bottom: .22cm; }
  li::before { content: "▪"; color: var(--xanh); font-weight: bold; padding-right: .3cm; }
  p + p { margin-top: .25cm; }
  .nho { font-size: 13pt; color: var(--nhat); }
  /* Anh phai vua CHIEU CAO thi poster moi dung 90cm. Chieu rong tu co lai theo
     ti le goc, nen khong anh nao bi bop meo. */
  img { display: block; margin: 0 auto; max-width: 100%;
        border: 1pt solid var(--vien); border-radius: 4pt; }
  .anh2 img  { max-height: 22.5cm; }
  .anh-vua img { max-height: 10.2cm; }
  .anh3 img  { max-height: 10.5cm; }
  figcaption { font-size: 12.5pt; color: var(--nhat); text-align: center; padding-top: .15cm; }

  code, pre { font-family: "DejaVu Sans Mono","Liberation Mono",monospace; }
  pre { background: #1d1f21; color: #e8e6e3; padding: .45cm .55cm; border-radius: 5pt;
        font-size: 13.5pt; line-height: 1.5; overflow: hidden; white-space: pre-wrap; }
  pre .k { color: #7ec699; }

  .so { display: grid; grid-template-columns: repeat(4, 1fr); gap: .55cm; text-align: center; }
  .so div { background: var(--xanh-nhat); border-radius: 5pt; padding: .45cm .2cm; }
  .so b { display: block; font-size: 30pt; color: var(--xanh); line-height: 1.1;
          font-family: "DejaVu Sans","Liberation Sans",sans-serif; }
  .so span { font-size: 13pt; color: var(--nhat); }

  .anh3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: .5cm; align-items: end; }
  .anh2 { display: grid; grid-template-columns: 1fr 1fr; gap: .6cm; align-items: center; }

  footer { position: absolute; bottom: 0; left: 0; right: 0; background: var(--xanh);
           color: #C8DED6; font-size: 13.5pt; padding: .5cm 2cm; display: flex;
           justify-content: space-between; }
</style>

<header>
  <div class="truong">TRƯỜNG ĐẠI HỌC TRÀ VINH · KHOA KỸ THUẬT VÀ CÔNG NGHỆ</div>
  <h1>XÂY DỰNG WEBSITE GIỚI THIỆU<br>VÀ ĐẶT PHÒNG HOMESTAY TVH</h1>
  <div class="ai">Sinh viên thực hiện: <b>. . . . . . . . . . . . . . . . . . . .</b>
    &nbsp;·&nbsp; Giảng viên hướng dẫn: <b>. . . . . . . . . . . . . . . . . . . .</b></div>
</header>

<main>
  <section>
    <h2>Bài toán</h2>
    <ul>
      <li>Homestay nhận đặt phòng qua nhiều kênh tin nhắn, lịch phòng nằm trong
          trí nhớ chủ nhà.</li>
      <li><b>Hai khách nhắn gần như cùng lúc</b> thì chủ nhà trả lời còn phòng cho
          cả hai — sai sót chỉ lộ ra vào ngày nhận phòng.</li>
      <li>Sàn trung gian thu 15–20% giá trị mỗi đơn, phần lớn biên lợi nhuận của
          homestay quy mô nhỏ.</li>
    </ul>
  </section>

  <section>
    <h2>Mục tiêu</h2>
    <ul>
      <li>Ba khối: trang bán hàng cho khách, khu quản trị, giao diện lập trình
          ứng dụng dùng chung.</li>
      <li>Đặt cọc bằng mã QR, webhook ngân hàng tự xác nhận đơn.</li>
      <li><b>Không có nhánh nào để tiền của khách biến mất im lặng.</b></li>
      <li>Một lệnh là đủ để chạy toàn hệ thống kèm dữ liệu mẫu.</li>
    </ul>
  </section>

  <section class="wide">
    <h2>Vấn đề kỹ thuật then chốt — và vì sao kiểm tra ở tầng ứng dụng không đủ</h2>
    <div class="anh2">
      <div>
        <p>Mẫu <i>kiểm-tra-rồi-ghi</i> ở tầng ứng dụng để lại một khe hở: giữa lúc
        đọc "còn phòng" và lúc ghi, một luồng khác kịp ghi xong. Mức cô lập
        <code>Read Committed</code> không đóng được khe hở này, và tài liệu
        PostgreSQL khuyên dùng <b>ràng buộc toàn vẹn</b> thay vì logic ứng dụng.</p>
        <p>Đề tài để <b>cơ sở dữ liệu</b> ra quyết định cuối cùng:</p>
        <pre><span class="k">EXCLUDE USING</span> gist (
    room_id <span class="k">WITH</span> =,
    stay    <span class="k">WITH</span> &amp;&amp;
) <span class="k">WHERE</span> (status = <span class="k">'ACTIVE'</span>)</pre>
        <p class="nho">Cột <code>stay</code> sinh tự động từ
        <code>daterange(nhận, trả, '[)')</code> — quy ước nửa mở, nên trả phòng và
        nhận phòng cùng ngày <b>không</b> tính là chồng lấn. Luồng thua nhận
        <code>SQLSTATE 23P01</code> và được thử lại phòng khác <b>trong một giao
        dịch mới</b>. MySQL không có cả kiểu khoảng lẫn ràng buộc loại trừ.</p>
      </div>
      <figure>
        <img src="${img('hinh-2-2-tranh-chap-dat-phong.png')}">
        <figcaption>Cùng một tình huống: trái là bán trùng, phải là bị cơ sở dữ liệu bác</figcaption>
      </figure>
    </div>
  </section>

  <section>
    <h2>Kiến trúc</h2>
    <figure class="anh-vua">
      <img src="${img('hinh-2-1-kien-truc-tong-the.png')}">
      <figcaption>Bốn dịch vụ; máy chủ web là đường vào duy nhất</figcaption>
    </figure>
    <p class="nho day">Java 21 · Spring Boot 3.5 · Angular 21 ·
    Tailwind CSS v4 · PostgreSQL 16 · Docker Compose</p>
  </section>

  <section>
    <h2>Cơ sở dữ liệu</h2>
    <figure class="anh-vua">
      <img src="${img('hinh-3-3-erd-dat-phong.png')}">
      <figcaption>Nhóm bảng đặt phòng — nơi đặt ràng buộc quyết định</figcaption>
    </figure>
    <p class="nho day">21 bảng nghiệp vụ, 8 migration bất biến.
    Ràng buộc loại trừ dùng <b>hai lần</b>: chống đặt trùng phòng và chống chồng
    lấn khoảng ngày bảo trì.</p>
  </section>

  <section class="wide">
    <h2>Giao diện</h2>
    <div class="anh3">
      <figure><img src="${img('hinh-3-13-ket-qua-tim-phong.png')}">
        <figcaption>Tìm phòng — giá là tổng cả kỳ</figcaption></figure>
      <figure><img src="${img('hinh-3-15-thanh-toan.png')}">
        <figcaption>Đặt cọc bằng mã QR, đếm ngược 15 phút</figcaption></figure>
      <figure><img src="${img('hinh-3-16-dashboard.png')}">
        <figcaption>Khu quản trị — 11 màn hình</figcaption></figure>
    </div>
  </section>

  <section class="wide">
    <h2>Kết quả kiểm chứng</h2>
    <div class="so">
      <div><b>129</b><span>ca kiểm thử, tất cả đạt</span></div>
      <div><b>0</b><span>vi phạm WCAG 2.1 A và AA<br>trên 17 màn hình</span></div>
      <div><b>11</b><span>lỗi thực tế đã tìm ra<br>và khắc phục</span></div>
      <div><b>3</b><span>lệnh từ lúc sao chép mã<br>tới lúc chạy được</span></div>
    </div>
    <p style="margin-top:.45cm">Kiểm thử tích hợp chạy trên <b>PostgreSQL thật</b>
    qua Testcontainers chứ không giả lập — ràng buộc loại trừ là tính năng của
    PostgreSQL, giả lập bằng cơ sở dữ liệu trong bộ nhớ thì đúng thứ cần kiểm
    lại không tồn tại. Ca kiểm thử đa luồng cho hai giao dịch cùng đặt một phòng:
    <b>đúng một luồng thắng</b>, luồng kia nhận <code>ROOM_NOT_AVAILABLE</code>.</p>
  </section>
</main>

<footer>
  <span>Đồ án thực tập chuyên ngành · Mã nguồn và tài liệu kỹ thuật công khai trên GitHub</span>
  <span>Trà Vinh, . . . . / . . . .</span>
</footer>`;

const tmp = path.join(ROOT, '.poster.tmp.html');
fs.writeFileSync(tmp, html);
const browser = await chromium.launch({ executablePath: CHROME });
const page = await browser.newPage();
await page.goto('file://' + tmp, { waitUntil: 'networkidle' });
await page.waitForTimeout(900);
fs.mkdirSync(path.dirname(OUT), { recursive: true });
await page.pdf({ path: OUT, width: '60cm', height: '90cm', printBackground: true, pageRanges: '1' });
// Ban PNG de xem nhanh: chup dung khung 60cm x 90cm o 96dpi (2268 x 3402 px),
// KHONG dung fullPage — fullPage se chup ca phan tran ra ngoai trang in va cho
// cam giac sai rang poster vua trang.
const png = OUT.replace(/\.pdf$/, '-xem-truoc.png');
await page.setViewportSize({ width: 2268, height: 3402 });
await page.waitForTimeout(400);
await page.screenshot({ path: png });
await browser.close();
fs.unlinkSync(tmp);
for (const f of [OUT, png]) console.log(`  -> ${f}  ${(fs.statSync(f).size / 1024).toFixed(0)} KB`);
