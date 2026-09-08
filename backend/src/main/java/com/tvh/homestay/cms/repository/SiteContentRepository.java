package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.SiteContent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Khối nội dung trên landing page. */
public interface SiteContentRepository extends JpaRepository<SiteContent, Long> {
}
