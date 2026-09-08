package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tin tức và bài khuyến mãi. */
public interface PostRepository extends JpaRepository<Post, Long> {
}
