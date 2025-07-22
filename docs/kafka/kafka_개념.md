# Kafka 개요

Kafka는 대용량 데이터를 실시간으로 처리하기 위해 만들어진 분산 메시지 스트리밍 플랫폼입니다.  
초기에는 LinkedIn에서 로그 처리 시스템으로 개발되었으며 현재는 다양한 분야에서 실시간 데이터 처리의 표준으로 자리잡고 있습니다.

> Kafka는 쉽게 말해 **실시간 데이터 파이프라인** 이라고 볼 수 있습니다.
>
> 한쪽에서 메시지를 보내면(**Producer**),  
> **Kafka**가 이를 저장하고,  
> 다른 쪽에서 가져가서 처리(**Consumer**)할 수 있게 한다.
  

# 구성 요소

![kafka구성요소.png](/docs/imgs/kafka구성요소.png)



## 프로듀서 (Producer)
- Kafka에 데이터를 보내는 역할
- 메시지를 생성하고, 특정 Topic으로 전송함


## 브로커 (Broker)
- 메시지를 받아서 디스크에 저장하고, 컨슈머가 요청하면 메시지를 전달하는 저장소 역할
- 하나의 Kafka 시스템(클러스터)은 여러 개의 Broker로 구성될 수 있음
- Bootstrap Servers : 클라이언트가 카프카 클러스터에 처음 연결할 때 사용하는 진입점이 되는 브로커

## 컨슈머 (Consumer)
- 카프카 브로커에 적재된 메시지를 읽어오는 서비스
- 여러 컨슈머를 조합한 **Consumer Group**으로 묶으면 병렬 처리 가능

## 토픽 (Topic)
- 메시지를 분류하는 논리적 단위 
- Producer는 특정 Topic으로 메시지를 전송하고, Consumer는 구독한 Topic에서 메시지를 소비함

## 파티션 (Partition)
- 실제 메시지가 저장되는 단위로, 내부적으로는 순서가 보장된 로그 구조로 되어 있음
- 토픽은 여러개의 파티션으로 구성되어 있음
- 메시지의 Key를 해시한 값을 기반으로 어떤 파티션에 들어갈지 결정됨

## 컨슈머 그룹 (Consumer Group)
- 여러 Consumer가 협업하여 하나의 토픽을 병렬로 읽는 구조 
- 동일 그룹 내에서는 하나의 Partition을 하나의 Consumer만 소비함 
- 서로 다른 그룹은 동일 메시지를 각각 독립적으로 소비 가능


# Kafka 주요 특징
Kafka는 아래와 같은 특징을 바탕으로 대용량 처리 환경에서 강력한 성능과 안정성을 제공합니다.

## 1. 고처리량 (High Throughput)  

배치 전송, Zero-Copy, 비동기 처리 기술 등을 통해 초당 수백만 건의 메시지를 처리할 수 있는 고성능 시스템입니다.

- Batch 처리 : 메시지를 묶음으로 처리해 속도 향상
- Zero Copy : 메모리 복사 없이 OS 레벨에서 전송
- 비동기 처리 : Producer/Consumer 모두 비동기 방식으로 처리하기 때문에 병목 없이 빠르게 동작

## 2. 확장성 (Scalability)

시스템을 멈추지 않고도 서버(Broker)나 파티션(Partition)을 추가하여 성능을 확장할 수 있습니다.

- Partition 단위로 데이터 분산 처리 
  - 하나의 Topic을 여러 Partition으로 나눠 여러 서버(Broker)에서 동시에 병렬 처리 가능  
- Broker 수평 확장
  - Kafka Broker를 더 추가하면 Partition이 분산되어 자연스럽게 처리 성능 증가  
- Consumer Group을 늘려 병렬 처리 가능
  - Consumer가 늘어나도 파티션 수만큼 병렬로 메시지를 읽을 수 있어 처리량 증가  


## 3. 고가용성 (High Availability)

장애가 발생해도 서비스가 중단되지 않도록 설계된 고가용성 시스템입니다.

- Replication (복제)
  - 하나의 Partition은 여러 Broker에 복제본(replica)을 만들어 둬서 하나의 Broker가 죽어도 다른 복제본이 동작
- Leader-Follower 구조
  - Partition 마다 Leader와 Follower가 존재하고,  
    Leader가 죽으면 Follower 중 하나가 자동으로 승격된다.  
- Kafka Controller
  - Kafka Controller가 클러스터 상태를 모니터링하고 복구 조정  


# Kafka CLI TEST

## 컨테이너 실행

- Kafka 클러스터 실행
```bash
docker-compose -f docker-compose.kafka.yml up -d
```

- bash 접속
```bash
docker exec -it broker1 bash
```

## 토픽 목록 확인

`kafka-topics --bootstrap-server <브로커주소:포트> --list`

```bash
kafka-topics --bootstrap-server broker1:29092 --list
```

| 옵션                                 | 의미                            |
| ---------------------------------- | ----------------------------- |
| `kafka-topics`                     | Kafka 토픽을 제어하는 CLI 명령어        |
| `--bootstrap-server broker1:29092` | Kafka 클러스터에 접속할 **진입 브로커 주소** |
| `--list`                           | 클러스터 내에 존재하는 모든 토픽 이름을 조회     |


## 토픽 생성

`kafka-topics --bootstrap-server <브로커주소:포트> --create --topic <토픽명> ...`

```bash
kafka-topics \
  --bootstrap-server broker1:29092 \
  --create \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 1 
```

| 옵션                               | 설명                                                           |
| -------------------------------- | ------------------------------------------------------------ |
| `--bootstrap-server <host:port>` | Kafka 클러스터에 접속할 broker 주소                                    |
| `--create`                       | 새 토픽을 생성하는 명령어                                               |
| `--topic <토픽명>`                  | 생성할 토픽의 이름                                                   |
| `--partitions <숫자>`              | 이 토픽이 가질 파티션 수. 기본값은 broker 설정에 따름                           |
| `--replication-factor <숫자>`      | 각 파티션을 몇 개의 broker에 복제할 것인지<br>(데이터 내구성 확보용). 일반적으로 브로커 수 이하 |


## 메시지 발행

`kafka-console-producer --bootstrap-server <브로커주소:포트> --topic <토픽명> ...`

```bash
kafka-console-producer \
  --bootstrap-server broker1:29092 \
  --topic test-topic \
  --property "parse.key=true" \
  --property "key.separator=:"  

```

| 옵션                               | 설명                                       |
| -------------------------------- | ---------------------------------------- |
| `--property "parse.key=true"`    | 메시지를 `key:value` 형태로 분리                  |
| `--property "key.separator=구분자"` | key와 value 사이 구분자 지정 (기본 `\t`)           |
| `--producer-property a=b`        | KafkaProducer 설정 커스텀 (ex. acks, retries) |
| `--compression-codec`            | 전송 메시지 압축 (gzip, snappy 등)               |
| `--request-required-acks`        | 메시지 전송 후 몇 개의 브로커 응답까지 기다릴지              |
| `--batch-size`                   | 배치로 전송할 최대 메시지 수 (성능 조정용)                |
| `--linger-ms`                    | 배치 대기 시간 설정 (지연 허용 시 처리량 증가)             |

## 메시지 소비

`kafka-console-consumer --bootstrap-server <브로커주소:포트> --topic <토픽명> ...`



```bash
kafka-console-consumer \
  --bootstrap-server broker1:29092 \
  --topic test-topic \
  --group test-group \
  --from-beginning
  --property "print.key=true" \
  --property "print.partition=true" \
  --property "print.timestamp=true" \
  --property "key.separator=:

```


**Consumer Group 관련 옵션**

| 옵션                                               | 설명                                                                                                                            |
| ------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- |
| `--group <group-id>`                             | Consumer Group ID 지정<br>해당 group 기준으로 offset 저장 및 관리함  <br>⇒ 이후 재시작 시 **이어 읽기 가능 (커밋된 offset부터)**                             |
| `--consumer-property enable.auto.commit=false`   | 자동 커밋 비활성화<br>수동 offset 커밋 실습 시 사용                                                                                            |
| `--consumer-property auto.offset.reset=earliest` | offset이 없는 경우 어디서부터 읽을지 설정<br>- - earliest : 처음부터 읽기<br>- - latest : 가장 마지막 offset부터 새로 들어오는 메시지만<br>- - none : offset 없으면 에러 |
| `--from-beginning`                               | offset이 없으면 처음부터, 있으면 저장된 offset부터                                                                   |


**출력 내용 제어용 옵션**

| 옵션                                | 설명                    |
| --------------------------------- | --------------------- |
| `--property print.key=true`       | 메시지 **Key** 출력.       |
| `--property key.separator=:`      | key와 value 사이 구분자 지정. |
| `--property print.timestamp=true` | 각 레코드의 timestamp 출력.  |
| `--property print.partition=true` | 메시지가 어느 파티션에서 왔는지 출력. |
| `--property print.headers=true`   | 헤더 출력.                |


**특정 파티션 / 오프셋 제어**
- group 지정 없이 단발성 읽기 시 제어

| 옵션                | 설명                                          |
| ----------------- | ------------------------------------------- |
| `--partition <n>` | 지정한 파티션만 읽기.                                |
| `--offset <n>`    | 해당 파티션에서 특정 오프셋부터 읽기. `--partition`과 함께 사용. |


**종료 조건 제어**

| 옵션                   | 설명                   |
| -------------------- | -------------------- |
| `--max-messages <N>` | N개 읽고 자동 종료.         |
| `--timeout-ms <ms>`  | 지정 시간 동안 메시지 없으면 종료. |


