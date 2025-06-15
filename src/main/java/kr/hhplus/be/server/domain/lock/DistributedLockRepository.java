package kr.hhplus.be.server.domain.lock;

public interface DistributedLockRepository {
    boolean tryLock(String key, String value, long timeoutMillis);
    void unlock(String key, String value);
}
