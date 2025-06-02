
# 유저 대기열 토큰 발급

- 서비스를 이용할 토큰을 발급받는 API를 작성합니다.
- 토큰은 유저의 UUID 와 해당 유저의 대기열을 관리할 수 있는 정보 ( 대기 순서 or 잔여 시간 등 ) 를 포함합니다.
- 이후 모든 API 는 위 토큰을 이용해 대기열 검증을 통과해야 이용 가능합니다.

> 기본적으로 폴링으로 본인의 대기열을 확인한다고 가정하며, 다른 방안 또한 고려해보고 구현해 볼 수 있습니다.


```mermaid
sequenceDiagram

	autonumber
	actor 사용자 as 사용자
	participant TokenAPI as 대기열 토큰 API
	participant QueueService as 대기열 토큰 서비스
	
	사용자 ->>+ TokenAPI: 대기열 토큰 발급 요청
	TokenAPI ->>+ QueueService: 대기열 토큰 발급 요청
	QueueService ->> QueueService: 기존 유효 토큰 조회
	
	alt 기존 유효 토큰 존재
		QueueService -->> TokenAPI: 기존 대기열 토큰(ID) 반환
	else 기존 유효 토큰 없음
		QueueService ->> QueueService: UUID 기반 토큰 생성
		QueueService ->> QueueService: 대기열 DB에 등록 (순번, 시간 등은 내부 관리)
		QueueService -->> TokenAPI: 신규 대기열 토큰(ID) 반환
	end
	
	TokenAPI -->>- 사용자: 대기열 토큰 반환
	
	loop 일정 시간 간격으로 폴링
		사용자 ->> QueueService: 대기열 상태 조회 (토큰 포함)
		QueueService ->> Redis: 현재 순서 및 토큰 위치 조회
		Redis -->> QueueService: 순번 및 예상 대기 시간
		QueueService -->> 사용자: 대기 순서 반환
	end
```


# 예약 가능 날짜 조회

- 예약가능한 날짜를 조회하는 API 
- 예약 가능한 날짜 목록을 조회할 수 있습니다.

```mermaid
sequenceDiagram

	autonumber
	actor 사용자 as 사용자
	participant ScheduleAPI as 콘서트 일정 API
	participant QueueService as 대기열 서비스
	participant Redis as Redis
	participant ScheduleService as 콘서트 일정 서비스

	
	사용자 ->>+ ScheduleAPI: 예약 가능 날짜 목록 조회 요청
	ScheduleAPI ->>+ QueueService: 대기열 토큰 상태 및 순번 검증
	QueueService ->>+ Redis: 현재 입장 가능 순번 조회
	Redis -->>- QueueService: 현재 순번 반환

	alt 토큰 상태 : 만료/취소/예약완료
		QueueService -->> ScheduleAPI: 검증 실패
		ScheduleAPI -->> 사용자: 대기열 토큰 오류 응답
	else 토큰 상태 : 대기
		alt 순번 도달 전
			QueueService -->> ScheduleAPI: 순번 대기중
			ScheduleAPI -->> 사용자: 순번 대기중
		else 입장 가능
			QueueService -->> ScheduleAPI: 검증 성공
			ScheduleAPI ->>+ ScheduleService: 예약 가능 날짜 목록 조회
			ScheduleService -->>- ScheduleAPI: 예약 가능 날짜 목록 반환
			ScheduleAPI -->> 사용자: 예약 가능 날짜 목록 반환
		end
	end

```

# 예약 가능 좌석 조회

- 해당 날짜의 좌석을 조회하는 API 작성
- 날짜 정보를 입력받아 예약가능한 좌석정보를 조회할 수 있습니다.

> 좌석 정보는 1 ~ 50 까지의 좌석번호로 관리됩니다.

```mermaid
sequenceDiagram
	autonumber
	actor 사용자 as 사용자
	participant ScheduleAPI as 콘서트 일정 API
	participant QueueService as 대기열 서비스
	participant Redis as Redis
	participant ScheduleService as 콘서트 일정 서비스
	participant SeatService as 콘서트 좌석 서비스
	
	사용자 ->>+ ScheduleAPI: 특정 날짜의 예약 가능 좌석 조회 요청
	ScheduleAPI ->>+ QueueService: 대기열 토큰 상태 및 순번 검증
	QueueService ->>+ Redis: 현재 입장 가능 순번 조회
	Redis -->>- QueueService: 현재 순번 반환
	
	alt 토큰 상태 : 만료/취소/예약완료
		QueueService -->> ScheduleAPI: 검증 실패
		ScheduleAPI -->> 사용자: 대기열 토큰 오류 응답
	else 토큰 상태 : 대기
		alt 순번 도달 전
			QueueService -->> ScheduleAPI: 순번 대기중
			ScheduleAPI -->> 사용자: 순번 대기중
		else 입장 가능
			QueueService -->> ScheduleAPI: 검증 성공
			ScheduleAPI ->>+ ScheduleService: 특정 날짜의 콘서트 일정 조회
			
			opt 콘서트 일정 없음
				ScheduleService -->> ScheduleAPI: 콘서트 일정 조회 실패
				ScheduleAPI -->> 사용자: 잘못된 요청 오류 응답
			end
			
			ScheduleService -->>- ScheduleAPI: 콘서트 일정 정보 반환
			
			ScheduleAPI ->>+ SeatService: 예약 가능 좌석 조회
			SeatService -->>- ScheduleAPI: 예약 가능 좌석 목록 반환
			ScheduleAPI -->>- 사용자: 예약 가능 좌석 목록 반환
		end
	end
```



# 잔액 충전

- 결제에 사용될 금액을 API 를 통해 충전하는 API 를 작성합니다.
- 사용자 식별자 및 충전할 금액을 받아 잔액을 충전합니다.


```mermaid
sequenceDiagram

	autonumber
	actor 사용자 as 사용자
	participant BalanceHistoryAPI as 잔액 내역 API
	participant UserService as 사용자 서비스
	participant BalanceHistoryService as 잔액 내역 서비스
	
	사용자 ->>+ BalanceHistoryAPI: 잔액 충전 요청 (userId, 충전 금액)
	BalanceHistoryAPI ->>+ UserService: 사용자 정보 검증
	opt 사용자 정보 없음
		UserService -->> BalanceHistoryAPI: 검증 실패
		BalanceHistoryAPI -->> 사용자: 사용자 인증 오류 응답
	end
	
	UserService -->>- BalanceHistoryAPI: 검증 성공
	BalanceHistoryAPI ->>+ BalanceHistoryService: 사용자 잔액 충전 요청
	BalanceHistoryService -->>- BalanceHistoryAPI: 잔액 충전 내역 반환
	BalanceHistoryAPI -->>- 사용자: 잔액 충전 내역 반환
	
```



# 잔액 조회

- 사용자 식별자를 통해 해당 사용자의 잔액을 조회합니다.

```mermaid
sequenceDiagram
	autonumber
	actor 사용자 as 사용자
	participant BalanceHistoryAPI as 잔액 내역 API
	participant UserService as 사용자 서비스
	participant BalanceHistoryService as 잔액 내역 서비스
	
	사용자 ->>+ BalanceHistoryAPI: 잔액 조회 요청 (userId)
	BalanceHistoryAPI ->>+ UserService: 사용자 정보 검증
	
	opt 사용자 정보 없음
		UserService -->> BalanceHistoryAPI: 검증 실패
		BalanceHistoryAPI -->> 사용자: 사용자 인증 오류 응답
	end
	UserService -->>- BalanceHistoryAPI: 검증 성공
	
	BalanceHistoryAPI ->>+ BalanceHistoryService: 사용자 잔액 내역 조회 요청
	BalanceHistoryService ->> BalanceHistoryService: 마지막 잔액 내역 검색
	BalanceHistoryService -->>- BalanceHistoryAPI: 마지막 잔액 내역 반환
	BalanceHistoryAPI -->>- 사용자: 현재 잔액 반환(current_balance)
```



# 좌석 예약 요청 API

- 날짜와 좌석 정보를 입력받아 좌석을 예약 처리하는 API 를 작성합니다.
- 좌석 예약과 동시에 해당 좌석은 그 유저에게 약 5분간 임시 배정됩니다. ( 시간은 정책에 따라 자율적으로 정의합니다. )
- 만약 배정 시간 내에 결제가 완료되지 않는다면 좌석에 대한 임시 배정은 해제되어야 하며 다른 사용자는 예약할 수 없어야 한다.


```mermaid

sequenceDiagram
	autonumber
	actor 사용자 as 사용자
	participant ReservationAPI as 예약 API
	participant QueueService as 대기열 서비스
	participant Redis as Redis
	participant ReservationService as 예약 서비스
	participant ScheduleService as 일정 서비스
	participant SeatService as 좌석 서비스
	
	사용자 ->>+ ReservationAPI: 좌석 예약 요청
	ReservationAPI ->>+ QueueService: 대기열 토큰 상태 및 순번 검증
	QueueService ->>+ Redis: 현재 입장 가능 순번 조회
	Redis -->>- QueueService: 현재 순번 반환
	
	alt 토큰 상태 : 만료/취소/예약완료
		QueueService -->> ReservationAPI: 검증 실패
		ReservationAPI -->> 사용자: 대기열 토큰 오류 응답
	else 토큰 상태 : 대기
		alt 순번 도달 전
			QueueService -->> ReservationAPI: 순번 대기중
			ReservationAPI -->> 사용자: 순번 대기중 응답
		else 입장 가능
			QueueService -->> ReservationAPI: 검증 성공
			
			ReservationAPI ->>+ ReservationService: 좌석 예약 처리 요청
			ReservationService ->>+ ScheduleService: 콘서트 일정 확인
			ScheduleService -->>- ReservationService: 일정 정보 반환
			
			opt 콘서트 일정 없음
				ReservationService -->> ReservationAPI : 잘못된 요청 오류 응답
				ReservationAPI -->> 사용자 : 잘못된 요청 오류 응답
			end
			
			ReservationService ->>+ SeatService: 좌석 상태 확인
			SeatService -->>- ReservationService: 예약 가능 여부 응답
			
			opt 예약 불가능
				ReservationService -->> ReservationAPI: 좌석 예약 불가 응답
				ReservationAPI --> 사용자: 좌석 예약 불가 응답
			end
			
			ReservationService ->> ReservationService: 좌석 예약 내역 생성
			ReservationService ->> SeatService: 좌석 상태 임시 예약으로 변경
			SeatService -->> ReservationService: 변경된 좌석 정보 반환
			ReservationService -->>- ReservationAPI: 예약 결과 반환
			ReservationAPI -->>- 사용자: 예약 결과 반환
		end
	end

```



# 결제 API

- 결제 처리하고 결제 내역을 생성하는 API 를 작성합니다.
- 결제가 완료되면 해당 좌석의 소유권을 유저에게 배정하고 대기열 토큰을 만료시킵니다.


```mermaid

sequenceDiagram

	autonumber
	actor 사용자 as 사용자
	participant PaymentAPI as 결제 API
	participant QueueService as 대기열 서비스
	participant Redis as Redis
	participant PaymentService as 결제 서비스
	participant ReservationService as 예약 서비스
	participant BalanceHistoryService as 잔액 내역 서비스
	participant SeatService as 좌석 서비스

	사용자 ->>+ PaymentAPI: 예약 좌석 결제 요청
	PaymentAPI ->>+ QueueService: 대기열 토큰 상태 및 순번 검증 
	QueueService ->>+ Redis: 현재 입장 가능 순번 조회
	Redis -->>- QueueService: 현재 순번 반환

	alt 토큰 상태 : 만료/취소/예약완료
		QueueService -->> PaymentAPI: 검증 실패
		PaymentAPI -->> 사용자: 대기열 토큰 오류 응답
	else 토큰 상태 : 대기
		alt 순번 도달 전
			QueueService -->> PaymentAPI: 순번 대기중
			PaymentAPI -->> 사용자: 순번 대기중 응답
		else 입장 가능
			QueueService -->>- PaymentAPI: 검증 성공 
			PaymentAPI ->>+ PaymentService: 결제 요청
		
			PaymentService ->>+ ReservationService: 예약 조회
			ReservationService -->>- PaymentService:예약 정보 반환
			
			opt 예약 정보 없음
				PaymentService -->> PaymentAPI : 잘못된 요청 오류 응답
				PaymentAPI -->> 사용자 : 잘못된 요청 오류 응답
			end
			
			PaymentService ->>+ BalanceHistoryService : 잔액 내역 생성 (콘서트 가격만큼 사용)
			BalanceHistoryService -->>- PaymentService : 잔액 내역 반환
			PaymentService ->> PaymentService : 결제 처리 (결제 내역 생성)
			PaymentService ->>+ReservationService : 예약 정보 변경 요청(예약 상태 : 예약 확정, 예약 만료 시각 : 제거)
			ReservationService -->>-PaymentService : 변경된 예약 정보 반환
		
			PaymentService ->>+ SeatService : 좌석 정보 변경 요청 (좌석 상태 : 예약 확정)
			SeatService -->>- PaymentService : 변경된 좌석 정보 반환 
			
			PaymentService -->>- PaymentAPI : 결제 내역 반환
			
			PaymentAPI ->>+ QueueService: 대기열 토큰 예약 완료 처리
			QueueService -->>- PaymentAPI : 대기열 토큰 예약 완료 처리 결과
			PaymentAPI -->>- 사용자 : 결제 내역 반환
		end
	end
```



