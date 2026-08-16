package ru.lewis.leykabot.configuration.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, AtomicInteger> ipRateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .build();

    private static final int MAX_REQUESTS_PER_5_SECONDS = 40;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/")) {
            String clientIp = getClientIp(request);
            AtomicInteger counter = ipRateLimitCache.get(clientIp, k -> new AtomicInteger(0));

            if (counter.incrementAndGet() > MAX_REQUESTS_PER_5_SECONDS) {
                log.warn("🚨 DDoS/Anti-Spam trigger: IP {} exceeded API rate limit on {}", clientIp, path);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"ok\":false,\"error\":\"Juda ko‘p so‘rov yuborildi. Iltimos, biroz kuting!\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = req.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }
}
