package com.nexus.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-client-IP rate limiter for the unauthenticated auth endpoints
 * ({@code /api/auth/login} and {@code /api/auth/register}) — the brute-force /
 * credential-stuffing surface. Runs at the front of the filter chain so abusive
 * traffic is rejected cheaply, before security or controller work.
 *
 * <p>Backed by an in-memory {@link TokenBucket} per IP. This is correct for a single
 * instance; across a horizontally-scaled fleet each node would hold its own buckets, so
 * a shared store (e.g. Redis via Bucket4j) would be the production evolution — documented
 * in the architecture notes. Conditional on {@code nexus.ratelimit.enabled} so tests and
 * environments can opt out.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "nexus.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final RateLimitProperties properties;
    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(LOGIN_PATH.equals(uri) || REGISTER_PATH.equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientId = clientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientId, key ->
                new TokenBucket(properties.getCapacity(), properties.getRefillTokens(), properties.getRefillPeriod()));

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            meterRegistry.counter("nexus.ratelimit.rejected").increment();
            writeTooManyRequests(response);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        long retryAfterSeconds = Math.max(1,
                properties.getRefillPeriod().getSeconds() / Math.max(1, properties.getCapacity()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                        + "\"detail\":\"Rate limit exceeded. Please retry later.\"}");
    }
}
