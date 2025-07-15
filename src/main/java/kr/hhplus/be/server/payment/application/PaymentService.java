package kr.hhplus.be.server.payment.application;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.lock.DistributedLockRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.reservation.domain.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.user.User;
import kr.hhplus.be.server.user.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PaymentService {   // 좌석 예약 서비스

    private final PaymentTransactionalService paymentTransactionalService;

    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
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
        return distributedLockRepository.withMultiLock(lockKeys, () -> paymentTransactionalService.doPaymentTransactional(userId, reservationId, seatId),
                LOCK_TIMEOUT_MILLIS, MAX_RETRY, SLEEP_MILLIS);
    }

    /**
     * # Method설명 : 사용자 userId 검증
     * # MethodName : validateUser
     **/
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID("+ userId +")의 사용자를 찾을 수 없습니다."));
    }

}
