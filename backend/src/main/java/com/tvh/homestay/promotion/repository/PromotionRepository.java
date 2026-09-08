package com.tvh.homestay.promotion.repository;

import com.tvh.homestay.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

/** Mã giảm giá. */
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
}
