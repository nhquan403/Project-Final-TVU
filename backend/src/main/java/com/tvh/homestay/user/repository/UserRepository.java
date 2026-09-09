package com.tvh.homestay.user.repository;

import com.tvh.homestay.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Tài khoản CUSTOMER và ADMIN. */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Email luôn lưu chữ thường; nơi gọi phải chuẩn hoá trước khi tra. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Tăng {@code token_version}, làm chết mọi access token đang lưu hành của
     * người dùng đó.
     *
     * <p>Viết bằng UPDATE thay vì đọc–sửa–ghi là có chủ ý: hai lần đăng xuất
     * song song đọc cùng một giá trị rồi cùng ghi +1 sẽ chỉ tăng được một bậc,
     * và một trong hai token đáng lẽ phải chết vẫn còn sống.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    void incrementTokenVersion(@Param("id") Long id);
}
