package com.tvh.homestay.room.entity;

import com.tvh.homestay.common.BaseCreatedEntity;
import com.tvh.homestay.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Một khoảng ngày phòng vật lý không nhận khách.
 *
 * <p>Khác {@link RoomStatus} ở đúng một điểm và điểm đó là lý do bảng này tồn
 * tại: trạng thái phòng không có ngày, còn khoảng đóng thì có. Bảo trì ba ngày
 * không phải là "ngừng khai thác" — dùng trạng thái cho việc đó nghĩa là phải
 * nhớ bật lại bằng tay.
 *
 * <p>Cột {@code blocked} CỐ Ý không được ánh xạ: nó là cột sinh tự động trong
 * cơ sở dữ liệu, và bốn truy vấn phòng trống đọc nó bằng SQL thuần chứ không
 * qua JPA. Ánh xạ một cột {@code GENERATED ALWAYS} chỉ tạo ra một đường để ghi
 * đè nó.
 */
@Entity
@Table(name = "room_closures")
@Getter
@Setter
public class RoomClosure extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /** Đêm đầu tiên bị chặn. */
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /** Ngày mở bán lại — đêm này KHÔNG bị chặn. Quy ước nửa mở {@code [)}. */
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(length = 300)
    private String reason;

    /** NULL khi tài khoản tạo khoảng đóng về sau bị xoá. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
