package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.concert.domain.enums.SeatStatus;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.enums.PaymentStatus;
import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.reservation.domain.SeatReservation;
import kr.hhplus.be.server.reservation.domain.enums.ReservationStatus;
import kr.hhplus.be.server.reservation.repository.SeatReservationRepository;
import kr.hhplus.be.server.user.domain.User;
import kr.hhplus.be.server.user.domain.UserBalance;
import kr.hhplus.be.server.user.domain.enums.UserBalanceType;
import kr.hhplus.be.server.user.repository.UserBalanceRepository;
import kr.hhplus.be.server.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@AllArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final SeatReservationRepository seatReservationRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentResponse payment(Long userId, Long reservationId) {

        // 예약ID 검증
        SeatReservation seatReservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "예약 정보를 찾을 수 없습니다."));

        // 임시 예약 상태가 아님 (이미 예약 완료, 취소, 만료 등)
        if (ReservationStatus.TEMP_RESERVED != seatReservation.getStatus())
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "결제할 수 없는 예약 정보입니다.");

        // 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "해당 ID의 사용자를 찾을 수 없습니다."));

        // 사용자 잔액 차감
        long currentBalance = userBalanceRepository // 사용자 현재 잔액
                .findTopByUser_UserIdOrderByCreatedAtDesc(userId)
                .map(UserBalance::getCurrentBalance)
                .orElse(0L);

        long price = seatReservation.getSeat().getPrice();  // 콘서트 가격

        // 잔액 부족
        if(currentBalance < price)
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "잔액이 부족합니다.");

        // 사용자 잔액 내역
        UserBalance userBalance = UserBalance.builder()
                .user(user)
                .amount(price)
                .type(UserBalanceType.USE)
                .currentBalance(currentBalance - price)
                .description(price +"원 " + UserBalanceType.USE.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        userBalanceRepository.save(userBalance);    // 잔액 내역 생성


        // 결제 내역 생성
        Payment payment = Payment.builder()
                .user(user)
                .seatReservation(seatReservation)
                .amount(price)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        // 예약 정보 (상태, 만료 시간) 변경
        seatReservation.confirmReservation();

        // 좌석 정보 (상태) 변경
        seatReservation.getSeat().setStatus(SeatStatus.CONFIRMED);

        return payment.toResponse();
    }
}
