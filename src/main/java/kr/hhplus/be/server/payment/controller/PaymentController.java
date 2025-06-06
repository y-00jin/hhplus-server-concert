package kr.hhplus.be.server.payment.controller;


import kr.hhplus.be.server.payment.dto.PaymentResponse;
import kr.hhplus.be.server.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * # Method설명 : 예약된 좌석 결제
     * # MethodName : payForReservedSeat
     **/
    @PostMapping
    public ResponseEntity<PaymentResponse> payForReservedSeat(@RequestParam Long userId, @RequestParam Long reservationId) {
        PaymentResponse response = paymentService.payment(userId, reservationId);
        return ResponseEntity.ok(response);
    }
}
