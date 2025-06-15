package kr.hhplus.be.server.infrastructure.persistence.lock;

import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisDistributedLockRepository implements DistributedLockRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end";

    @Override
    public boolean tryLock(String key, String value, long timeoutMillis) {
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, value, timeoutMillis, TimeUnit.MILLISECONDS);
        return success != null && success;
    }

    @Override
    public void unlock(String key, String value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(UNLOCK_SCRIPT);
        script.setResultType(Long.class);
        redisTemplate.execute(script, Collections.singletonList(key), value);
    }
}
