package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.payment.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse payment(Long userId, Long reservationId);

}
