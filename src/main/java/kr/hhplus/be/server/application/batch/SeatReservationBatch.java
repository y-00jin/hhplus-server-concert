package kr.hhplus.be.server.application.batch;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.concert.SeatStatus;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.ReservationStatus;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatReservationBatch {

    private final SeatReservationRepository seatReservationRepository;
    private final SeatRepository seatRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final DistributedLockRepository distributedLockRepository;

    private static final long LOCK_TIMEOUT_MILLIS = 15000;
    private static final int MAX_RETRY = 10;
    private static final long SLEEP_MILLIS = 200;


    @Scheduled(fixedDelay = 100_000)
    public void expireTempReservations() {
        LocalDateTime now = LocalDateTime.now();
        // 임시예약 상태(TEMP_RESERVED)면서 만료시각이 현재보다 지난 예약 찾기
        List<SeatReservation> expiredReservations = seatReservationRepository.findByStatusAndExpiredAtBefore(ReservationStatus.TEMP_RESERVED, now);

        for (SeatReservation reservation : expiredReservations) {

            // 락 획득
            List<String> lockKeys = Arrays.asList(
                    "reservation-lock:" + reservation.getReservationId(),
                    "seat-lock:" + reservation.getSeatId()
            );

            withMultiLock(lockKeys, () -> {
                expireReservationAndSeatTransactional(reservation.getReservationId(), reservation.getSeatId());
                return null;
            });
        }
    }

    /**
     * # Method설명 : 여러 분산락 순차적으로 획득
     * # MethodName : withMultiLock
     **/
    private <T> T withMultiLock(List<String> lockKeys, Supplier<T> action) {
        List<String> acquiredKeys = new ArrayList<>();  // 획득한 락 key 리스트
        List<String> lockValues = new ArrayList<>();    // 각 락의 고유 value (락 해제시 사용)
        try {
            // 각 key 별로 순서대로 락 획득 시도
            for (String lockKey : lockKeys) {
                String lockValue = UUID.randomUUID().toString();
                boolean locked = false;
                int tryCount = 0;
                // 스핀락 방식으로 최대 MAX_RETRY번 재시도
                while (!locked && tryCount < MAX_RETRY) {
                    locked = distributedLockRepository.tryLock(lockKey, lockValue, LOCK_TIMEOUT_MILLIS);
                    if (!locked) {  // 락 획득 실패
                        tryCount++;
                        Thread.sleep(SLEEP_MILLIS);
                    }
                }
                if (!locked) {  // 최종적으로 락 획득 실패
                    releaseAllLocks(acquiredKeys, lockValues);  // 획득한 락 전체 해제
                    log.warn("락 획득 실패: {}", lockKey);
                    return null;
                }
                // 3. 획득한 락 리스트에 추가
                acquiredKeys.add(lockKey);
                lockValues.add(lockValue);
            }
            return action.get();    // 모든 락 획득 후 비즈니스 로직 실행
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAllLocks(acquiredKeys, lockValues);
            log.error("락 획득 중 인터럽트 발생");
            return null;
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
                distributedLockRepository.unlock(lockKeys.get(i), lockValues.get(i));
            } catch (Exception ignore) {}
        }
    }


    @Transactional
    public void expireReservationAndSeatTransactional(Long reservationId, Long seatId) {

        // 1. 예약 조회
        Optional<SeatReservation> reservationOpt = seatReservationRepository.findById(reservationId);
        if (reservationOpt.isEmpty()) {
            log.warn("예약 정보 없음: {}", reservationId);
            return;
        }
        SeatReservation reservation = reservationOpt.get();
        if (reservation.getStatus() != ReservationStatus.TEMP_RESERVED) {   // 임시 예약 확인
            return;
        }

        // 2. 좌석 조회
        Optional<Seat> seatOpt = seatRepository.findById(seatId);
        if (seatOpt.isEmpty()) {
            log.warn("좌석 정보 없음: {}", seatId);
            return;
        }
        Seat seat = seatOpt.get();

        // 3. 상태 체크 및 만료 처리
        if (seat.getStatus() == SeatStatus.TEMP_RESERVED) {
            // 예약 만료 처리
            reservation.expireReservation();
            seatReservationRepository.save(reservation);

            // 좌석 만료 처리
            seat.releaseAssignment();
            seatRepository.save(seat);

            // 토큰 만료
            Long scheduleId = seat.getScheduleId();
            queueTokenRepository.findTokenIdByUserIdAndScheduleId(reservation.getUserId(), scheduleId)
                    .ifPresent(queueTokenRepository::expiresQueueToken);
        }
    }
}