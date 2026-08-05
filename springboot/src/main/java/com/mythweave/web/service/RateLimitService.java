package com.mythweave.web.service;

import com.mythweave.web.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 接口频率限制：
 * - 滑动窗口计数（Redis 优先，Redis 不可用时降级为本地内存计数）
 * - 连续违规达到阈值后触发熔断冷却（成功请求自动清零违规计数）
 * mythweave.security.enabled=false 时全部放行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties securityProperties;

    /** Redis 不可用时的本地降级计数窗口 */
    private static class LocalWindow {
        long windowStart = System.currentTimeMillis();
        int count = 0;
    }

    private final Map<Long, LocalWindow> localCounters = new ConcurrentHashMap<>();
    /** 熔断冷却截止时间：userId -> 时间戳 */
    private final Map<Long, Long> circuitBreakers = new ConcurrentHashMap<>();
    /** 连续违规计数：userId -> 次数 */
    private final Map<Long, AtomicInteger> violations = new ConcurrentHashMap<>();

    /**
     * 频率限制检查
     *
     * @param userId 用户ID
     * @param isVip  是否 VIP 用户
     * @return pass-放行（含剩余次数） / blocked-拦截（含原因）
     */
    public RateLimitResult checkRateLimit(Long userId, boolean isVip) {
        if (!securityProperties.isEnabled() || userId == null) {
            return RateLimitResult.pass(Integer.MAX_VALUE);
        }

        // 1. 熔断检查
        Long blockedUntil = circuitBreakers.get(userId);
        if (blockedUntil != null) {
            if (System.currentTimeMillis() < blockedUntil) {
                return RateLimitResult.blocked("请求过于频繁，请稍后再试");
            }
            // 冷却结束：清除熔断与违规计数
            circuitBreakers.remove(userId);
            violations.remove(userId);
        }

        SecurityProperties.RateLimit cfg = securityProperties.getRateLimit();
        int limit = isVip ? cfg.getVipLimit() : cfg.getFreeLimit();
        int windowSeconds = cfg.getWindowSeconds();

        // 2. 计数（Redis 优先，异常降级本地）
        int count = incrementCount(userId, windowSeconds);

        // 3. 超限：登记违规，达到阈值熔断
        if (count > limit) {
            registerViolation(userId, cfg);
            return RateLimitResult.blocked("提问频率过高，请稍后再试");
        }

        // 4. 成功：清零违规计数（仅连续违规触发熔断）
        violations.remove(userId);
        return RateLimitResult.pass(limit - count);
    }

    /**
     * 递增窗口计数：Redis 可用走 Redis（原子 + 自动过期），
     * Redis 异常时降级为本地内存窗口（进程内有效，重启重置）
     */
    private int incrementCount(Long userId, int windowSeconds) {
        try {
            String key = "ratelimit:user:" + userId;
            Long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount != null && newCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return newCount != null ? newCount.intValue() : 0;
        } catch (Exception e) {
            return localIncrement(userId, windowSeconds);
        }
    }

    private int localIncrement(Long userId, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;
        LocalWindow window = localCounters.computeIfAbsent(userId, k -> new LocalWindow());
        synchronized (window) {
            if (now - window.windowStart >= windowMillis) {
                window.windowStart = now;
                window.count = 0;
            }
            return ++window.count;
        }
    }

    /** 连续违规计数，达到阈值进入熔断冷却 */
    private void registerViolation(Long userId, SecurityProperties.RateLimit cfg) {
        int v = violations.computeIfAbsent(userId, k -> new AtomicInteger()).incrementAndGet();
        if (v >= cfg.getViolationThreshold()) {
            circuitBreakers.put(userId, System.currentTimeMillis() + cfg.getCooldownSeconds() * 1000L);
            violations.remove(userId);
            log.warn("用户 {} 连续违规 {} 次，触发熔断 {}s", userId, v, cfg.getCooldownSeconds());
        } else {
            log.warn("用户 {} 限流违规 {}/{} 次", userId, v, cfg.getViolationThreshold());
        }
    }

    public static class RateLimitResult {
        private final boolean passed;
        private final boolean blocked;
        private final String reason;
        private final int remaining;

        public RateLimitResult(boolean passed, boolean blocked, String reason, int remaining) {
            this.passed = passed;
            this.blocked = blocked;
            this.reason = reason;
            this.remaining = remaining;
        }

        public static RateLimitResult pass(int remaining) {
            return new RateLimitResult(true, false, null, remaining);
        }

        public static RateLimitResult blocked(String reason) {
            return new RateLimitResult(false, true, reason, 0);
        }

        public boolean isPassed() { return passed; }
        public boolean isBlocked() { return blocked; }
        public String getReason() { return reason; }
        public int getRemaining() { return remaining; }
    }
}
