package com.mythweave.web.config;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * ES 自动配置动态排除过滤器：
 * 当 mythweave.es.enabled=false 时，在自动配置加载前拦截并排除所有 Elasticsearch 自动配置类，
 * 从根上消除无 ES 环境下 ES 客户端 Bean 初始化导致的启动失败风险。
 * 通过 META-INF/spring.factories 注册，由 SpringFactoriesLoader 实例化（非容器 Bean）。
 */
public class EsAutoConfigurationImportFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    /**
     * ES 相关自动配置前缀（Spring Boot 3.x：
     * elasticsearch 包为 RestClient/Client 客户端，data.elasticsearch 包为数据层与仓库）
     */
    private static final String ES_AUTO_CONFIG_PREFIX = "org.springframework.boot.autoconfigure.elasticsearch.";
    private static final String ES_DATA_AUTO_CONFIG_PREFIX = "org.springframework.boot.autoconfigure.data.elasticsearch.";

    /** actuator 健康检查自动配置（单独类名） */
    private static final String ES_HEALTH_AUTO_CONFIG =
            "org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticsearchHealthContributorAutoConfiguration";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length];
        Arrays.fill(matches, true);

        // 未配置或 enabled=true：全部放行
        boolean esEnabled = environment == null
                || environment.getProperty("mythweave.es.enabled", Boolean.class, true);
        if (esEnabled) {
            return matches;
        }

        // enabled=false：仅排除 ES 自动配置
        // 注意：候选数组可能含 null（被前序 AutoConfigurationImportFilter 排除的占位），需判空
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            String className = autoConfigurationClasses[i];
            if (className != null
                    && (className.startsWith(ES_AUTO_CONFIG_PREFIX)
                        || className.startsWith(ES_DATA_AUTO_CONFIG_PREFIX)
                        || className.equals(ES_HEALTH_AUTO_CONFIG))) {
                matches[i] = false;
            }
        }
        return matches;
    }
}
