package com.novelcraft.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.sentinel")
public class SentinelConfig {
    /** 每个分片的章节数 */
    private int chunkSize = 10;

    /** 线程池大小 */
    private int threadPoolSize = 4;

    /** 增量检测阈值：自上次巡查后新增章节超过此值才触发增量扫描 */
    private int incrementalThreshold = 5;

    /** 伏笔超期阈值：超过此章节数未回收则告警 */
    private int overdueThreshold = 10;
}
