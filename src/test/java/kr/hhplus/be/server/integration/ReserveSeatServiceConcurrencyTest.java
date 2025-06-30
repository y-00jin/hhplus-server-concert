package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.queue.QueueService;
import kr.hhplus.be.server.application.reservation.ReserveSeatService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.SeatReservation;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
public class ReserveSeatServiceConcurrencyTest {

    @Autowired
    ReserveSeatService reserveSeatService;
    @Autowired
    QueueService queueService;

    @Autowired
    UserRepository userRepository;
    @Autowired
    ConcertScheduleRepository scheduleRepository;
    @Autowired
    SeatRepository seatRepository;
    @Autowired
    QueueTokenRepository queueTokenRepository;
    @Autowired
    SeatReservationRepository seatReservationRepository;

    @AfterEach
    void cleanUp() {
        seatReservationRepository.deleteAllForTest();
        seatRepository.deleteAllForTest();
        scheduleRepository.deleteAllForTest();
        userRepository.deleteAllForTest();
        queueTokenRepository.deleteAllForTest();
    }

    @Test
    void 동시에_두_명이_같은_좌석을_예약_시도하면_한_명만_성공() throws Exception {

        // given
        LocalDateTime now = LocalDateTime.now();

        // 사용자 등록
        Long userId1 = userRepository.save(new User(null, UUID.randomUUID().toString(), "test1@email.com", "pw", "사용자1", now, now)).getUserId();
        Long userId2 = userRepository.save(new User(null, UUID.randomUUID().toString(), "test2@email.com", "pw", "사용자2", now, now)).getUserId();

        LocalDate concertDate = LocalDate.now().plusDays(1);
        int seatNumber = 1;

        // 콘서트 일정 및 좌석 등록
        Long scheduleId = scheduleRepository.save(new ConcertSchedule(null, concertDate, now, now)).getScheduleId();
        Long seatId = seatRepository.save(new Seat(null, scheduleId, seatNumber, 30000, SeatStatus.FREE , now, now)).getSeatId();

        // 토큰 발급
        queueService.issueQueueToken(userId1, scheduleId);
        queueService.issueQueueToken(userId2, scheduleId);

        int threadCount = 5; // 동시 예약 시도
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final Long userId = (i == 0) ? userId1 : userId2;
            executor.submit(() -> {
                try {
                    SeatReservation result = reserveSeatService.reserveSeat(userId, concertDate, seatNumber);
                    log.info("[ 좌석 예약 성공 ] 사용자 : {}, reservationId : {}", result.getUserId(), result.getReservationId());
                } catch (Throwable e) {
                    exceptions.add(e);
                    log.error("[ 좌석 예약 실패 ] 사용자 : {}, {}",userId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 스레드가 모두 종료될 때까지 대기

        // then
        assertThat(exceptions.size()).isEqualTo(threadCount - 1); // 실패 수
        Throwable ex = exceptions.get(0);
        assertThat(ex).isInstanceOf(ApiException.class);
        ErrorCode errorCode = ((ApiException) ex).getErrorCode();
        assertThat(errorCode == ErrorCode.FORBIDDEN || errorCode == ErrorCode.INVALID_INPUT_VALUE).isTrue();
    }
}



