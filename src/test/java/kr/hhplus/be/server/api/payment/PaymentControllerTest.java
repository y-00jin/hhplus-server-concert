package kr.hhplus.be.server.api.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.payment.api.PaymentController;
import kr.hhplus.be.server.payment.application.PaymentService;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.dto.request.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PaymentService paymentService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void 결제_API_정상호출() throws Exception {
        // given
        Long userId = 1L;
        Long reservationId = 100L;
        long amount = 30000L;
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment(10L, userId, reservationId, amount, PaymentStatus.SUCCESS, now, now);

        PaymentRequest request = new PaymentRequest(userId, reservationId);

        when(paymentService.payment(userId, reservationId)).thenReturn(payment);

        // when & then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(10L))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.createdAt").exists());
    }
}