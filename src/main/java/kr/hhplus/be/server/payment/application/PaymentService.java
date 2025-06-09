package kr.hhplus.be.server.payment.application;


import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.Seat;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.concert.repository.SeatRepository;
import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import kr.hhplus.be.server.payment.domain.model.Payment;
import kr.hhplus.be.server.payment.domain.repository.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.model.SeatReservation;
import kr.hhplus.be.server.reservation.domain.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import kr.hhplus.be.server.user.repository.UserBalanceRepository;
import kr.hhplus.be.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class PaymentService {   // 좌석 예약 서비스

    private final PaymentRepository paymentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final SeatRepository seatRepository;

    public PaymentService(PaymentRepository paymentRepository, SeatReservationRepository seatReservationRepository, UserRepository userRepository, UserBalanceRepository userBalanceRepository, SeatRepository seatRepository) {
        this.paymentRepository = paymentRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.userRepository = userRepository;
        this.userBalanceRepository = userBalanceRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Payment payment(Long userId, Long reservationId) {

        // 예약ID 검증
        SeatReservation seatReservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "예약 정보를 찾을 수 없습니다."));

        // 임시 예약 상태가 아님 (이미 예약 완료, 취소, 만료 등)
        if (ReservationStatus.TEMP_RESERVED != seatReservation.getStatus())
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "결제할 수 없는 예약 정보입니다.");


        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다."));

        // 사용자 현재 잔액 조회
        long currentBalance = userBalanceRepository
                .findTopByUser_UserIdOrderByBalanceHistoryIdDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);


        // 콘서트 좌석 조회 (예약 내역의 좌석id로)
        Seat seat = seatRepository.findById(seatReservation.getSeatId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "좌석 정보를 찾을 수 없습니다."));
        long amount = seat.getPrice();   // 콘서트 가격


        // 잔액 사용 내역 생성
        UserBalance userBalance = UserBalance.use(user, amount, currentBalance);
        userBalanceRepository.save(userBalance);


        // 결제 도메인 모델 생성 (POJO)
        Payment payment = Payment.create(
                user.getUserId(),
                seatReservation.getReservationId(),
                amount,
                PaymentStatus.SUCCESS
        );
        Payment result = paymentRepository.save(payment);

        // 예약 내역 - 예약 확정 변경
        seatReservation.confirmReservation();
        seatReservationRepository.save(seatReservation);

        // 좌석 - 예약 확정 변경
        seat.setStatus(SeatStatus.CONFIRMED);
        seatRepository.save(seat);

        return result;
    }


}
