package kr.hhplus.be.server.infrastructure.persistence.lock;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    /**
     * # Method설명 : 분산락 순차적으로 획득
     * # MethodName : withMultiLock
     **/
    @Override
    public <T> T withMultiLock(List<String> lockKeys, Supplier<T> action, long timeoutMillis, int maxRetry, long sleepMillis) {
        List<String> acquiredKeys = new ArrayList<>();  // 획득한 락 key 리스트
        List<String> lockValues = new ArrayList<>();    // 각 락의 고유 value (락 해제시 사용)

        Collections.sort(lockKeys); // 항상 같은 순서로 락 획득 (사전순 정렬)

        try {
            // 각 key 별로 순서대로 락 획득 시도
            for (String lockKey : lockKeys) {
                String lockValue = UUID.randomUUID().toString();
                boolean locked = false;
                int tryCount = 0;

                // 스핀락 방식으로 최대 maxRetry번 재시도
                while (!locked && tryCount < maxRetry) {
                    locked = tryLock(lockKey, lockValue, timeoutMillis);
                    if (!locked) {
                        tryCount++;
                        Thread.sleep(sleepMillis);
                    }
                }
                if (!locked) {  // 최종적으로 락 획득 실패
                    releaseAllLocks(acquiredKeys, lockValues);  // 획득한 락 전체 해제
                    throw new ApiException(ErrorCode.FORBIDDEN, "요청이 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");
                }
                // 3. 획득한 락 리스트에 추가
                acquiredKeys.add(lockKey);
                lockValues.add(lockValue);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAllLocks(acquiredKeys, lockValues);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "락 획득 중 인터럽트 발생");
        } catch (RuntimeException e) {
            releaseAllLocks(acquiredKeys, lockValues);
            throw e;
        } finally {
            releaseAllLocks(acquiredKeys, lockValues);
        }
    }

    /**
     * # Method설명 : 락 전체 해제
     * # MethodName : releaseAllLocks
     **/
    private void releaseAllLocks(List<String> lockKeys, List<String> lockValues) {
        for (int i = 0; i < lockKeys.size(); i++) {
            try {
                unlock(lockKeys.get(i), lockValues.get(i));
            } catch (Exception ignore) {}
        }
    }
}
