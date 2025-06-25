package kr.hhplus.be.server.application.payment;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.domain.payment.Payment;
import kr.hhplus.be.server.domain.payment.PaymentRepository;
import kr.hhplus.be.server.domain.payment.PaymentStatus;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserBalance;
import kr.hhplus.be.server.domain.user.UserBalanceRepository;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Service
public class PaymentService {   // 좌석 예약 서비스

    private final PaymentRepository paymentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final SeatRepository seatRepository;
    private final QueueTokenRepository queueTokenRepository;
    private final DistributedLockRepository distributedLockRepository;


    private static final long LOCK_TIMEOUT_MILLIS = 15000;
    private static final int MAX_RETRY = 10;
    private static final long SLEEP_MILLIS = 200;


    /**
     * # Method설명 : 예약한 좌석 결제
     * # MethodName : payment
     **/
    public Payment payment(Long userId, Long reservationId) {

        validateUser(userId);   // 사용자
        Long seatId = seatReservationRepository.findSeatIdById(reservationId);  // lock-key에 쓰기위한 좌석 ID 조회
        List<String> lockKeys = Arrays.asList(
                "reservation-lock:" + reservationId,
                "seat-lock:" + seatId,
                "userBalance-lock:" + userId
        );

        return withMultiLock(lockKeys, () -> paymentTransactional(userId, reservationId, seatId));

    }


    // 트랜잭션 내 결제 처리
    @Transactional
    protected Payment paymentTransactional(Long userId, Long reservationId, Long seatId) {
//        try {
            // 1. 검증 및 데이터 조회
            SeatReservation seatReservation = validateSeatReservation(reservationId, userId);
            Seat seat = validateSeat(seatId);
            int amount = seat.getPrice();

            // 2. 비즈니스 처리
            useBalance(userId, amount);     // 잔액 차감
            Payment payment = savePayment(userId, seatReservation.getReservationId(), amount); // 결제 생성
            confirmReservation(seatReservation); // 예약 확정
            confirmSeat(seat);                  // 좌석 확정

            // 3. 후처리
            expireQueueToken(userId, seat.getScheduleId());  // 토큰 만료
            return payment;
//        } catch (DataIntegrityViolationException e) {
//            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, String.format("해당 예약(%d)은 이미 결제되었거나 결제가 불가능한 상태입니다.", reservationId));
//        }
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
                    locked = distributedLockRepository.tryLock(lockKey, lockValue, LOCK_TIMEOUT_MILLIS);    // 락 획득
                    if (!locked) {  // 락 획득 실패
                        tryCount++;
                        Thread.sleep(SLEEP_MILLIS);
                    }
                }
                if (!locked) {  // 최종적으로 락 획득 실패
                    releaseAllLocks(acquiredKeys, lockValues);  // 획득한 락 전체 해제
                    throw new ApiException(ErrorCode.FORBIDDEN, "결제 요청이 많아 처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요.");
                }
                // 3. 획득한 락 리스트에 추가
                acquiredKeys.add(lockKey);
                lockValues.add(lockValue);
            }
            return action.get();    // 모든 락 획득 후 비즈니스 로직 실행
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseAllLocks(acquiredKeys, lockValues);
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "락 대기 중 인터럽트 발생");
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


    /**
     * # Method설명 : 사용자 userId 검증
     * # MethodName : validateUser
     **/
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 예약 정보 검증
     * # MethodName : validateSeatReservation
     **/
    private SeatReservation validateSeatReservation(Long reservationId, Long userId) {

        // 예약 lock 획득
        SeatReservation seatReservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, String.format("예약 정보(%d)를 찾을 수 없습니다.", reservationId)));

        // 예약자 검증
        seatReservation.validateOwner(userId);

        // 결제 가능 상태 검증 (임시 예약 상태가 아님 : 이미 예약 완료, 취소, 만료 등)
        seatReservation.validateAvailableToPay();

        return seatReservation;
    }

    /**
     * # Method설명 : 좌석 정보 검증
     * # MethodName : validateSeat
     **/
    private Seat validateSeat(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "좌석 정보("+seatId+")를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 잔액 사용 내역 생성
     * # MethodName : useBalance
     **/
    private void useBalance(Long userId, int amount) {

        // 현재 잔액 조회 (lock 획득)
        long currentBalance = userBalanceRepository.findCurrentBalanceByUserId(userId);
        UserBalance userBalance = UserBalance.use(userId, amount, currentBalance);
        userBalanceRepository.save(userBalance);    // 잔액 사용 내역 생성
    }

    /**
     * # Method설명 : 결제 내역 생성
     * # MethodName : savePayment
     **/
    private Payment savePayment(Long userId, Long reservationId, int amount) {
        Payment payment = Payment.create(
                userId,
                reservationId,
                amount,    // 콘서트 좌석 금액
                PaymentStatus.SUCCESS
        );
        return paymentRepository.save(payment); // 결제 내역 생성
    }

    /**
     * # Method설명 :  예약 내역 - 예약 확정 변경
     * # MethodName : confirmReservation
     **/
    private void confirmReservation(SeatReservation seatReservation) {
        seatReservation.confirmReservation();
        seatReservationRepository.save(seatReservation);
    }

    /**
     * # Method설명 : 좌석 - 예약 확정 변경
     * # MethodName : confirmSeat
     **/
    private void confirmSeat(Seat seat) {
        seat.confirmReservation();
        seatRepository.save(seat);
    }

    /**
     * # Method설명 : 결제 성공 후 대기열 토큰 만료 처리 (존재할 때만)
     * # MethodName : expireQueueToken
     **/
    private void expireQueueToken(Long userId, Long scheduleId) {
        Optional<String> tokenIdOpt = queueTokenRepository.findTokenIdByUserIdAndScheduleId(userId, scheduleId);
        tokenIdOpt.ifPresent(queueTokenRepository::expiresQueueToken);
    }

}
