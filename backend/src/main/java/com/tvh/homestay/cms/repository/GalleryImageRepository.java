package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;

/** Thư viện ảnh. */
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {
}
