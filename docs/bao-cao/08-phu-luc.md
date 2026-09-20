# PHỤ LỤC

## Phụ lục A — Mã nguồn migration tạo ràng buộc chống trùng lịch

Trích từ tệp `V3__bookings_and_exclusion.sql`:

```sql
CREATE TABLE booking_rooms (
    id         bigserial   PRIMARY KEY,
    booking_id bigint      NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    room_id    bigint      NOT NULL REFERENCES rooms (id),
    check_in   date        NOT NULL,
    check_out  date        NOT NULL,
    stay       daterange   GENERATED ALWAYS AS
                           (daterange(check_in, check_out, '[)')) STORED,
    status     varchar(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT ck_booking_rooms_dates  CHECK (check_out > check_in),
    CONSTRAINT ck_booking_rooms_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);

ALTER TABLE booking_rooms
    ADD CONSTRAINT booking_rooms_no_overlap
    EXCLUDE USING gist (room_id WITH =, stay WITH &&)
    WHERE (status = 'ACTIVE');
```

## Phụ lục B — Trigger ràng buộc hoãn

Trích từ cùng tệp migration:

```sql
CREATE FUNCTION assert_booking_room_count(p_booking_id bigint) RETURNS void AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM bookings b
        WHERE b.id = p_booking_id
          AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'NO_SHOW')
          AND b.room_quantity <> (
              SELECT count(*)
              FROM booking_rooms br
              WHERE br.booking_id = b.id
                AND br.status = 'ACTIVE')
    ) THEN
        RAISE EXCEPTION 'booking_rooms khong khop room_quantity';
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER booking_room_count_check
    AFTER INSERT OR UPDATE OR DELETE ON booking_rooms
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_booking_room_count();
```

Trigger được đặt ở **cả hai phía**: khi sửa bảng gán phòng, và khi sửa số lượng
phòng trên đơn. Đặt một phía là để lọt trường hợp còn lại.

## Phụ lục C — Bảng endpoint đầy đủ

Bảng đầy đủ 80 thao tác trên 63 đường dẫn được liệt kê trong tài liệu kỹ thuật
`docs/api.md` của dự án, và xem được trực tiếp qua giao diện tài liệu tương tác
của hệ thống đang chạy ở cấu hình trình diễn.

Cách đếm lại số thao tác từ hệ thống đang chạy:

```bash
curl -s localhost/v3/api-docs | python3 -c "
import json,sys
d = json.load(sys.stdin)
methods = ('get','post','put','delete','patch')
print(sum(len([m for m in v if m in methods]) for v in d['paths'].values()))"
```

## Phụ lục D — Migration bảng khoảng đóng phòng

Trích từ tệp `V8__room_closures.sql`:

```sql
CREATE TABLE room_closures (
    id         bigserial   PRIMARY KEY,
    room_id    bigint      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    from_date  date        NOT NULL,
    to_date    date        NOT NULL,
    blocked    daterange   GENERATED ALWAYS AS
                           (daterange(from_date, to_date, '[)')) STORED,
    reason     varchar(300),
    created_by bigint      REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_room_closures_dates CHECK (to_date > from_date)
);

ALTER TABLE room_closures
    ADD CONSTRAINT room_closures_no_overlap
    EXCLUDE USING gist (room_id WITH =, blocked WITH &&);

CREATE INDEX idx_room_closures_room ON room_closures (room_id, from_date);
```

## Phụ lục E — Cấu hình đóng gói

Tệp cấu hình đóng gói `docker-compose.yml` và cấu hình máy chủ web
`frontend/nginx.conf` được lưu trong kho mã nguồn của dự án.

Điểm đáng lưu ý trong cấu hình máy chủ web là cú pháp tiền tố ưu tiên ở cả bốn
khối chuyển tiếp:

```nginx
location ^~ /api/         { ... }
location ^~ /swagger-ui/  { ... }
location ^~ /v3/api-docs/ { ... }
location ^~ /uploads/     { ... }
```

## Phụ lục F — Lệnh kiểm chứng

| Mục đích | Lệnh |
|---|---|
| Chạy toàn bộ kiểm thử | `cd backend && ./mvnw verify` |
| Biên dịch tầng giao diện | `cd frontend && npm run build` |
| Kiểm tra quy ước mã nguồn | `cd frontend && npm run lint` |
| Dựng và chạy toàn hệ thống | `docker compose up -d --build` |
| Kiểm tra trạng thái các dịch vụ | `docker compose ps` |
| Đếm số bảng trong lược đồ | `SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history';` |
