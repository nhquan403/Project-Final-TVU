package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.SiteContent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nội dung tĩnh của trang, tra theo khoá section. */
public interface SiteContentRepository extends JpaRepository<SiteContent, Long> {

    Optional<SiteContent> findBySectionKey(String sectionKey);

    List<SiteContent> findAllByOrderBySectionKeyAsc();
}
