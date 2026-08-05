package com.mythweave.web.service;

import com.mythweave.web.config.EsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ES 连接状态管理器（熔断器）：
 * - 连接失败后进入冷却期，冷却期内所有 ES 调用直接短路返回，避免每次等待网络超时
 * - 冷却期结束自动乐观重试，ES 恢复后自动回到可用状态
 * - 支持 mythweave.es.enabled=false 完全禁用（所有调用恒降级）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsConnectionState {

    private final EsProperties esProperties;

    /** 当前是否确认可用 */
    private volatile boolean available = false;
    /** 最近一次失败原因 */
    private volatile String lastError = "";
    /** 熔断冷却截止时间戳（毫秒） */
    private volatile long cooldownUntil = 0L;
    /** 防止并发重复探测 */
    private final AtomicBoolean probing = new AtomicBoolean(false);

    /**
     * ES 模块是否启用（mythweave.es.enabled）
     */
    public boolean isEnabled() {
        return esProperties.isEnabled();
    }

    /**
     * 对外报告的真实可用状态（用于系统状态展示）
     */
    public boolean isAvailable() {
        return esProperties.isEnabled() && available;
    }

    /**
     * 内部调用入口判断：是否允许发起一次 ES 请求。
     * 已确认可用 → 放行；熔断冷却期内 → 短路；冷却期结束 → 乐观放行一次（失败会再次熔断）
     */
    public boolean isUsable() {
        if (!esProperties.isEnabled()) return false;
        if (available) return true;
        return System.currentTimeMillis() >= cooldownUntil;
    }

    /**
     * 尝试开始一次探测（并发安全，仅允许单线程探测）
     */
    public boolean tryBeginProbe() {
        if (!esProperties.isEnabled()) return false;
        return probing.compareAndSet(false, true);
    }

    /**
     * 标记 ES 可用（探测或请求成功后调用）
     */
    public void markAvailable() {
        probing.set(false);
        available = true;
        cooldownUntil = 0L;
        lastError = "";
        log.info("Elasticsearch 连接正常，向量检索已启用");
    }

    /**
     * 标记 ES 不可用并进入熔断冷却（连接/请求失败后调用）
     */
    public void markUnavailable(String error) {
        probing.set(false);
        available = false;
        cooldownUntil = System.currentTimeMillis() + esProperties.getRetryAfter().toMillis();
        lastError = error == null ? "" : error;
        log.warn("Elasticsearch 不可用（{}），向量检索已降级，{}s 后自动重试",
                lastError, esProperties.getRetryAfter().toSeconds());
    }

    /**
     * 最近一次失败原因
     */
    public String getLastError() {
        return lastError;
    }
}
