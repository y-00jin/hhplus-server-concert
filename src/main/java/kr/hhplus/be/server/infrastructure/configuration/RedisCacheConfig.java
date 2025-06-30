package kr.hhplus.be.server.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RedisCacheProperties.class)
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf, RedisCacheProperties redisCacheProperties) {
        // 각 캐시별로 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        redisCacheProperties.getConfigs().forEach((cacheName, detail) -> {
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(detail.getTtl()));
            cacheConfigs.put(cacheName, config);
        });

        // 기본 TTL 설정 (30분)
        return RedisCacheManager.builder(cf).cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(1800000))).withInitialCacheConfigurations(cacheConfigs).build();
    }

}
