package ru.lewis.leykabot.configuration.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter implements Filter {

    // 5 soniya ichida har bir IP uchun maksimal 40 ta so'rov
    private final Cache<String, AtomicInteger> ipRateLimitCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .build();

    private static final int MAX_REQUESTS_PER_5_SECONDS = 40;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            String path = httpRequest.getRequestURI();

            // Faqat API endpointlariga cheklov qo'llaymiz
            if (path != null && path.startsWith("/api/")) {
                String clientIp = getClientIp(httpRequest);
                AtomicInteger counter = ipRateLimitCache.get(clientIp, k -> new AtomicInteger(0));

                if (counter.incrementAndGet() > MAX_REQUESTS_PER_5_SECONDS) {
                    log.warn("🚨 DDoS/Anti-Spam trigger: IP {} exceeded API rate limit on {}", clientIp, path);
                    httpResponse.setStatus(429); // 429 Too Many Requests
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    httpResponse.getWriter().write("{\"ok\":false,\"error\":\"Juda ko‘p so‘rov yuborildi. Iltimos, biroz kuting!\"}");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
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
