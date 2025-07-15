package kr.hhplus.be.server.lock.domain;

import java.util.List;
import java.util.function.Supplier;

public interface DistributedLockRepository {
    boolean tryLock(String key, String value, long timeoutMillis);
    void unlock(String key, String value);
    <T> T withMultiLock(List<String> lockKeys, Supplier<T> action, long timeoutMillis, int maxRetry, long sleepMillis);
}
