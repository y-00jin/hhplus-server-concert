package kr.hhplus.be.server.payment.api;

import kr.hhplus.be.server.payment.application.PaymentService;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.dto.request.PaymentRequest;
import kr.hhplus.be.server.payment.dto.response.PaymentResponse;
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
