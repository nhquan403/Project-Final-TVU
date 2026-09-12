package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.GalleryImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Thư viện ảnh của homestay. */
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {

    List<GalleryImage> findAllByOrderByDisplayOrderAscIdAsc();

    List<GalleryImage> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
