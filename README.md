# batch-decoder-demo

Sepolia L1 블록 중 배치 tx(tx.to = batchInBox eoa)를 fetch하고 디코딩하는 데모 서비스. Optimism의 `batch_decoder` 바이너리(Go)를 JAR 리소스로 번들링해서 `ProcessBuilder`로 실행하고, `fetch` → `reassemble` 결과를 도메인 모델로 감싸 REST API로 노출한다.

## 아키텍처

헥사고날(포트&어댑터) 구조.

```
domain/                Channel, Frame — 순수 도메인 모델
application/port/      FetchChannelsUseCase(인바운드), BatchDecoderPort(아웃바운드)
application/service/   FetchChannelsService — UseCase 구현, Port에 위임
adapter/in/            FetchChannelsController — REST 진입점
adapter/out/           ProcessBuilderBatchDecoderAdapter — batch_decoder 바이너리 실행/파싱
config/                DecoderProperties — decoder.* 설정 바인딩
```

## 준비

`src/main/resources/application.yaml`의 `decoder.l1-rpc`, `decoder.l1-beacon`에 본인 Alchemy(또는 다른 Sepolia RPC) API 키를 넣는다.

```yaml
decoder:
  l1-rpc: https://eth-sepolia.g.alchemy.com/v2/YOUR_API_KEY
  l1-beacon: https://eth-sepoliabeacon.g.alchemy.com/v2/YOUR_API_KEY
  inbox: "0x00Ef2E3b7754F2A65f1E897a27A3306d9b52f544"
  sender: "0x1cAAaa58002a7e8B4c6f427ac2c943767b4d6cd7"
```

`inbox`/`sender`는 반드시 따옴표로 감싼다 — 따옴표 없으면 YAML이 `0x...`를 16진수 정수로 파싱해버려서 주소가 깨진다.

## 실행

```powershell
.\gradlew.bat bootRun
```

기본 포트 8080. JPA/DataSource 자동설정은 꺼져있다(엔티티/DB 없음, `BatchDecoderDemoApplication`에서 명시적으로 제외).

## API

```
GET /api/channels?start={fromBlock}&end={toBlockExclusive}
```

지정한 L1 블록 범위를 fetch + reassemble해서 디코딩된 채널 목록을 반환한다.

```powershell
curl "http://localhost:8080/api/channels?start=11378985&end=11379085"
```

## 캐시 디렉터리

- `tx_cache` — fetch 단계 중간 산출물. 요청마다 임시 디렉터리에 생성되고 요청이 끝나면 삭제된다.
- `channel_cache`(프로젝트 루트) — reassemble 결과. 삭제되지 않고 누적된다(결과물 확인용)

## 테스트

`BatchDecoderServiceManualTest`는 실제 L1/beacon 엔드포인트를 호출하는 수동 스모크 테스트다(Spring 컨텍스트 없이 어댑터를 직접 생성). 코드 변경이 없으면 Gradle이 재실행을 스킵하므로 `--rerun-tasks`를 붙인다.

```powershell
.\gradlew.bat test --tests "com.hanati.web3.batch_decoder_demo.BatchDecoderServiceManualTest" --rerun-tasks
```

L1 beacon 엔드포인트가 간헐적으로 연결을 끊는 경우가 있어(관찰됨), 실패하면 재시도해본다. `fetch`는 최대 3회까지 자동 재시도한다.
