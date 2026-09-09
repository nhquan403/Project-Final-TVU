package com.tvh.homestay.user;

import com.tvh.homestay.user.entity.User;
import com.tvh.homestay.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Nạp tài khoản cho lần đăng nhập bằng mật khẩu.
 *
 * <p>Trả thẳng {@link org.springframework.security.core.userdetails.User} của
 * Spring với cờ {@code enabled} lấy từ cơ sở dữ liệu, nên tài khoản bị khoá bị
 * {@code DisabledException} chặn ngay trong {@code AuthenticationManager} —
 * không phải tự kiểm ở tầng service rồi có ngày quên.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository users;

    public UserDetailsServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = users.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isEnabled())
                .build();
    }
}
