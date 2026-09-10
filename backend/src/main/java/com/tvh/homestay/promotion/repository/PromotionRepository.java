package com.tvh.homestay.promotion.repository;

import com.tvh.homestay.promotion.entity.Promotion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Mã giảm giá. */
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    /**
     * Tiêu thụ một lượt, có điều kiện, trong MỘT câu lệnh.
     *
     * <p>Đọc rồi ghi ở hai bước riêng là mở cửa cho hai yêu cầu song song cùng
     * thấy "còn lượt" rồi cùng tăng — mã 10 lượt bị dùng 11 lần.
     *
     * <p>Mệnh đề {@code usage_limit IS NULL OR} là BẮT BUỘC. Thiếu vế đầu thì
     * với mã không giới hạn, biểu thức so sánh cho ra NULL, không dòng nào được
     * cập nhật, và hệ thống từ chối sạch mọi mã vô hạn.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Promotion p set p.usedCount = p.usedCount + 1
             where p.id = :id
               and (p.usageLimit is null or p.usedCount < p.usageLimit)
            """)
    int consumeOne(@Param("id") Long id);

    /**
     * Hoàn lại một lượt khi đơn vào trạng thái kết thúc.
     *
     * <p>Không có bước này thì một mã 10 lượt bị đốt sạch bởi 10 người bấm đặt
     * rồi bỏ ngang trong 15 phút. Điều kiện {@code usedCount > 0} giữ cho con
     * số không bao giờ âm.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Promotion p set p.usedCount = p.usedCount - 1 where p.id = :id and p.usedCount > 0")
    int releaseOne(@Param("id") Long id);
}
