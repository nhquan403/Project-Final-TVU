package com.tvh.homestay.booking.repository;

import com.tvh.homestay.booking.entity.Booking;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Đơn đặt phòng. */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByCode(String code);

    Optional<Booking> findByCode(String code);

    /** Danh sách đơn của một tài khoản. Lọc theo id lấy từ token, không tin client. */
    Page<Booking> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Booking> findByCodeAndUserId(String code, Long userId);
}
