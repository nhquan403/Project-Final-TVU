package com.tvh.homestay.cms.repository;

import com.tvh.homestay.cms.entity.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tin tức và bài khuyến mãi. */
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByOrderByIdDesc();

    /** Bài ĐÃ xuất bản, mới trước. Bài nháp không bao giờ ra API công khai. */
    List<Post> findByPublishedTrueOrderByPublishedAtDescIdDesc();

    List<Post> findByPublishedTrueOrderByPublishedAtDescIdDesc(Limit limit);

    Optional<Post> findBySlugIgnoreCaseAndPublishedTrue(String slug);

    Optional<Post> findBySlugIgnoreCase(String slug);
}
