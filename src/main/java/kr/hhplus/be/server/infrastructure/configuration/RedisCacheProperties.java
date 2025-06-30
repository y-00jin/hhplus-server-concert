package kr.hhplus.be.server.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.cache.redis")
public class RedisCacheProperties {
    private Map<String, CacheDetail> configs;

    @Getter
    @Setter
    public static class CacheDetail {
        private long ttl;
        private long maxIdleTime;
    }
}
