package com.tvh.homestay.user.repository;

import com.tvh.homestay.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tài khoản CUSTOMER và ADMIN. */
public interface UserRepository extends JpaRepository<User, Long> {
}
