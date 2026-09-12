package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.Banner;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Băng-rôn khuyến mãi trên trang chủ. */
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByOrderByDisplayOrderAscIdAsc();

    /**
     * Băng-rôn ĐANG hiển thị được tại thời điểm {@code now}.
     *
     * <p>Lọc cả cửa sổ thời gian, không chỉ cờ {@code active}: một chiến dịch
     * đã hết hạn mà vẫn hiện trên trang chủ là lời hứa ưu đãi không còn giá trị,
     * và khách đọc nó rồi vào đặt phòng sẽ thấy giá khác.
     */
    @Query("""
            select b from Banner b
            where b.active = true
              and (b.startsAt is null or b.startsAt <= :now)
              and (b.endsAt is null or b.endsAt > :now)
            order by b.displayOrder, b.id
            """)
    List<Banner> findVisible(@Param("now") OffsetDateTime now);
}
