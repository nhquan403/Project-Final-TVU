# Bản Word của quyển báo cáo

Hai tệp ở đây được **sinh ra từ `docs/bao-cao/`**, không sửa tay. Sửa nội dung
thì sửa Markdown rồi dựng lại; sửa thẳng vào `.docx` sẽ mất ở lần dựng sau.

| Tệp | Chứa gì | Số trang đo được |
|---|---|---:|
| `bao-cao-noi-dung.docx` | MỞ ĐẦU + Chương 1 → Chương 5 — **đây là phần bị tính trần 30–50 trang** | 52 |
| `bao-cao-tltk-phu-luc.docx` | Danh mục tài liệu tham khảo + Phụ lục A–Q — *không tính vào số trang* | 30 |
| `de-cuong-do-an.docx` | **Đề cương** nộp cho GVHD *trước* khi làm — không thuộc quyển báo cáo | 10 |

Bản PDF tương ứng ở `../pdf/`.

## Dựng lại

```sh
pip install python-docx
python3 scripts/xuat-ban-word.py noi-dung thesis/doc/bao-cao-noi-dung.docx \
    "Họ tên GVHD" "Họ tên SVTH"
python3 scripts/xuat-ban-word.py phu-luc thesis/doc/bao-cao-tltk-phu-luc.docx \
    "Họ tên GVHD" "Họ tên SVTH"

# Đề cương — truyền thẳng đường dẫn tệp Markdown
python3 scripts/xuat-ban-word.py docs/de-cuong-do-an.md thesis/doc/de-cuong-do-an.docx
```

Không truyền hai tham số cuối thì chân trang để dấu chấm cho điền tay.

Script đã áp sẵn: Times New Roman 13pt, giãn dòng 1.5, cách đoạn trước và sau
6pt, khổ A4, lề trên 2cm – dưới 2cm – trái 3cm – phải 2cm, số trang góc phải
dưới, chân trang `GVHD:` bên trái và `SVTH:` bên phải. Hình được chèn theo dấu
chỗ đặt `[Hình X.Y]` kèm chú thích và dòng nguồn.

## Bốn việc còn phải làm tay trong Word

1. **Mười ba trang đầu.** Bìa chính, bìa phụ, các trang nhận xét, lời mở đầu,
   lời cảm ơn, mục lục, danh mục bảng–sơ đồ–hình, ký hiệu viết tắt, tóm tắt.
   `docs/bao-cao/00-phan-dau.md` ghi rõ từng trang gồm gì và cỡ chữ nào, theo
   biểu mẫu BM5 — phải dựng tay cho đúng mẫu, không chép sang được.
2. **Mục lục và danh mục hình.** Dùng chức năng sinh mục lục tự động của Word
   sau khi đã ghép xong; không quá bốn cấp tiểu mục.
3. **Chú thích Hình 1.3.** Ảnh chụp từ trang web bên ngoài nên dòng nguồn phải
   ghi địa chỉ và ngày chụp, không phải "Nguồn: tác giả".
4. **Hai trang cuối cùng.** Phần nội dung đã từ 66 xuống 52 trang, còn dư 2 so
   với trần 50. Mục 5.3 của `docs/huong-dan-lam-bao-cao.md` ghi rõ đã cắt những gì và
   ba việc cắt tiếp nếu thầy hướng dẫn yêu cầu đúng 50.
