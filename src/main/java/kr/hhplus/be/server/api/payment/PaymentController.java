package kr.hhplus.be.server.api.payment;

import kr.hhplus.be.server.api.payment.dto.PaymentRequest;
import kr.hhplus.be.server.api.payment.dto.PaymentResponse;
import kr.hhplus.be.server.application.payment.PaymentService;
import kr.hhplus.be.server.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest req) {
        Payment payment = paymentService.payment(req.getUserId(), req.getReservationId());

        PaymentResponse response = PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .userId(payment.getUserId())
                .reservationId(payment.getReservationId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

}
