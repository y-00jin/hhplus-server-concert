package kr.hhplus.be.server.application.payment;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.Seat;
import kr.hhplus.be.server.domain.concert.SeatRepository;
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

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PaymentService {   // 좌석 예약 서비스

    private final PaymentRepository paymentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final SeatRepository seatRepository;
    private final QueueTokenRepository queueTokenRepository;

    /**
     * # Method설명 : 예약한 좌석 결제
     * # MethodName : payment
     **/
    @Transactional
    public Payment payment(Long userId, Long reservationId) {

        // 1. 검증 단계
        User user = validateUser(userId);   // 사용자
        SeatReservation seatReservation = validateSeatReservation(reservationId, userId);   // 예약 정보
        Seat seat = validateSeat(seatReservation.getSeatId());  // 콘서트 좌석 (예약 내역의 좌석id로)

        // 2. 비즈니스 처리
        useBalance(user, seat, userId); // 잔액 차감
        Payment payment = savePayment(user, seatReservation, seat); // 결제 생성
        confirmReservation(seatReservation); // 예약 확정
        confirmSeat(seat); // 좌석 확정

        // 3. 후처리
        expireQueueToken(userId, seat.getScheduleId()); // 토큰 만료
        return payment;
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
        SeatReservation seatReservation = seatReservationRepository.findByIdForUpdate(reservationId)
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
        // 결제 시도 시 비관적 락으로 좌석 row 락 획득
        return seatRepository.findBySeatIdForUpdate(seatId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "좌석 정보("+seatId+")를 찾을 수 없습니다."));
    }

    /**
     * # Method설명 : 잔액 사용 내역 생성
     * # MethodName : useBalance
     **/
    private void useBalance(User user, Seat seat, Long userId) {

        // 현재 잔액 조회 (lock 획득)
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByBalanceHistoryIdDescForUpdate(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        long amount = seat.getPrice();  // 콘서트 좌석 금액
        UserBalance userBalance = UserBalance.use(user.getUserId(), amount, currentBalance);
        userBalanceRepository.save(userBalance);    // 잔액 사용 내역 생성
    }

    /**
     * # Method설명 : 결제 내역 생성
     * # MethodName : savePayment
     **/
    private Payment savePayment(User user, SeatReservation seatReservation, Seat seat) {
        Payment payment = Payment.create(
                user.getUserId(),
                seatReservation.getReservationId(),
                seat.getPrice(),    // 콘서트 좌석 금액
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
