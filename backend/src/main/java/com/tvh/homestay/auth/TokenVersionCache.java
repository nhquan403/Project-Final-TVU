package com.tvh.homestay.auth;

import com.tvh.homestay.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bộ nhớ đệm ngắn cho {@code users.token_version}.
 *
 * <p>Kiểm {@code tv} phải xảy ra ở MỌI request, nên đọc thẳng cơ sở dữ liệu mỗi
 * lần sẽ nhân số truy vấn lên theo lưu lượng. Đệm 30 giây theo id người dùng
 * đổi lấy một cửa sổ thu hồi ≤ 30 giây — đây là đánh đổi có chủ ý, và nó vẫn
 * ngắn hơn 15 phút của việc chờ token hết hạn tới 30 lần.
 *
 * <p>{@link #invalidate(Long)} được gọi ngay tại chỗ tăng {@code token_version}
 * (đăng xuất, đổi mật khẩu, khoá tài khoản), nên trong cùng một tiến trình việc
 * thu hồi có hiệu lực TỨC THÌ, không phải chờ hết 30 giây. Cửa sổ 30 giây chỉ
 * còn ý nghĩa khi chạy nhiều instance.
 */
@Component
public class TokenVersionCache {

    private static final Duration TTL = Duration.ofSeconds(30);

    private record Entry(int tokenVersion, Instant expiresAt) {}

    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();
    private final UserRepository users;
    private final Clock clock;

    public TokenVersionCache(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    /** Trả {@code null} khi không còn người dùng nào mang id đó. */
    public Integer currentTokenVersion(Long userId) {
        Instant now = clock.instant();
        Entry cached = cache.get(userId);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.tokenVersion();
        }
        Integer fresh = users.findById(userId).map(user -> user.getTokenVersion()).orElse(null);
        if (fresh != null) {
            cache.put(userId, new Entry(fresh, now.plus(TTL)));
        } else {
            cache.remove(userId);
        }
        return fresh;
    }

    public void invalidate(Long userId) {
        cache.remove(userId);
    }
}
