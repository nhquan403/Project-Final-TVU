package com.tvh.homestay.common;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Giới hạn tần suất theo ma trận của Phase 4.
 *
 * <p>Bucket lưu trong bộ nhớ tiến trình. Trong phạm vi đồ án (một instance) là
 * đủ; chạy nhiều instance thì mỗi instance có bộ đếm riêng và hạn mức thực tế
 * nhân lên theo số instance. Muốn scale thì chuyển sang Redis — điều này được
 * ghi rõ thay vì để người đọc tự phát hiện.
 *
 * <p>Vượt hạn trả 429 kèm {@code Retry-After} tính từ thời điểm bucket có lại
 * lượt, để client biết chờ bao lâu thay vì thử lại ngay và bị chặn tiếp.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitKeyResolver resolver;

    public RateLimitFilter(RateLimitKeyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        HttpServletRequest effective = request;
        byte[] body = null;
        if (resolver.needsBody(request)) {
            CachedBodyRequest cached = new CachedBodyRequest(request);
            effective = cached;
            body = cached.getBody();
        }

        List<RateLimitKeyResolver.Limit> limits = resolver.resolve(effective, body);
        for (RateLimitKeyResolver.Limit limit : limits) {
            ConsumptionProbe probe = bucketFor(limit).tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                reject(response, Duration.ofNanos(probe.getNanosToWaitForRefill()));
                return;
            }
        }
        chain.doFilter(effective, response);
    }

    private Bucket bucketFor(RateLimitKeyResolver.Limit limit) {
        return buckets.computeIfAbsent(limit.key(), key -> Bucket.builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit.capacity())
                        .refillGreedy(limit.capacity(), limit.window()))
                .build());
    }

    private void reject(HttpServletResponse response, Duration retryAfter) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter.toSeconds())));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"TOO_MANY_REQUESTS","status":429,\
                "detail":"Quá nhiều yêu cầu. Thử lại sau ít phút."}""");
    }
}
