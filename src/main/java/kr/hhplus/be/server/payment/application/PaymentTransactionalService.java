package kr.hhplus.be.server.payment.application;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.application.event.ConcertSoldoutEventPublisher;
import kr.hhplus.be.server.concert.domain.event.ConcertSoldoutEvent;
import kr.hhplus.be.server.concert.domain.seat.Seat;
import kr.hhplus.be.server.concert.domain.seat.SeatRepository;
import kr.hhplus.be.server.payment.application.event.PaymentEventPublisher;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentRepository;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.domain.event.PaymentSuccessEvent;
import kr.hhplus.be.server.queue.domain.QueueTokenRepository;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.user.UserRepository;
import kr.hhplus.be.server.user.domain.userBalance.UserBalance;
import kr.hhplus.be.server.user.domain.userBalance.UserBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionalService {

    private final PaymentRepository paymentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final SeatRepository seatRepository;
    private final QueueTokenRepository queueTokenRepository;

    private final ConcertSoldoutEventPublisher soldoutEventPublisher;
    private final PaymentEventPublisher paymentEventPublisher;

    /**
     * # Method설명 : 트랜잭션 결제 처리
     * # MethodName : doPaymentTransactional
     **/
    @Transactional
    public Payment doPaymentTransactional(Long userId, Long reservationId, Long seatId) {
        // 1. 검증 및 데이터 조회
        SeatReservation seatReservation = validateSeatReservation(reservationId, userId);
        Seat seat = validateSeat(seatId);
        int amount = seat.getPrice();

        // 2. 비즈니스 처리
        useBalance(userId, amount);     // 잔액 차감
        Payment payment = savePayment(userId, seatReservation.getReservationId(), amount); // 결제 생성
        confirmReservation(seatReservation); // 예약 확정
        confirmSeat(seat);                  // 좌석 확정

        // 3. 매진 체크 & 랭킹 등록
        soldoutEventPublisher.publish(new ConcertSoldoutEvent(seat.getScheduleId()));

        // 결제 성공 이벤트 발행
        paymentEventPublisher.publish(new PaymentSuccessEvent(
                payment.getPaymentId(),
                userId,
                reservationId,
                amount,
                payment.getCreatedAt()
        ));

        // 4. 후처리
        expireQueueToken(userId, seat.getScheduleId());  // 토큰 만료
        return payment;
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
