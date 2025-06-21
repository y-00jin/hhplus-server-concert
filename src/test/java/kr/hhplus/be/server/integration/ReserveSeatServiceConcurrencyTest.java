package kr.hhplus.be.server.integration;

import kr.hhplus.be.server.application.reservation.ReserveSeatService;
import kr.hhplus.be.server.common.exception.ApiException;
import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.domain.concert.*;
import kr.hhplus.be.server.domain.queue.QueueTokenRepository;
import kr.hhplus.be.server.domain.reservation.SeatReservationRepository;
import kr.hhplus.be.server.domain.user.User;
import kr.hhplus.be.server.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
public class ReserveSeatServiceConcurrencyTest {

    @Autowired
    ReserveSeatService reserveSeatService;

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

        //

        LocalDate concertDate = LocalDate.of(2025, 6, 23);
        int seatNumber = 1;
        // 콘서트 일정 및 좌석 등록 (좌석은 임시 예약 상태로)
        Long scheduleId = scheduleRepository.save(new ConcertSchedule(null, concertDate, now, now)).getScheduleId();
        Long seatId = seatRepository.save(new Seat(null, scheduleId, seatNumber, 30000, SeatStatus.TEMP_RESERVED , now, now)).getSeatId();

        int threadCount = 2; // 동시 예약 시도
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Throwable> exceptions = new ArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            final Long userId = (i == 0) ? userId1 : userId2;
            executor.submit(() -> {
                try {
                    reserveSeatService.reserveSeat(userId, concertDate, seatNumber);
                } catch (Throwable e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 두 스레드가 모두 종료될 때까지 대기

        // then
        // 성공한 예약은 1건
        assertThat(exceptions.size()).isEqualTo(1); // 실패 수
        Throwable ex = exceptions.get(0);
        assertThat(ex).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
