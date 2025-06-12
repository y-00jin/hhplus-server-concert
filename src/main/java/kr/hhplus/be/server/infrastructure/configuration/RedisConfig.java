package kr.hhplus.be.server.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * # Method설명 : Redus 연결 팩토리 설정
     * # MethodName : redisConnectionFactory
     **/
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    /**
     * # Method설명 : Spring에서 Redis에 데이터를 저장/조회할 때 사용하는 주요 객체
     * # MethodName : redisTemplate
     **/
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        template.setKeySerializer(new StringRedisSerializer()); // Key는 문자열로 저장
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());  // Value는 JSON 형태로 직렬화 (LocalDateTime 등도 지원)

        template.setHashKeySerializer(new StringRedisSerializer()); // Hash 자료구조의 Key도 문자열로 저장
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());  // Hash의 Value도 JSON 형태로 직렬화

        template.afterPropertiesSet();  // 빈 등록 직후 초기화
        return template;
    }
}