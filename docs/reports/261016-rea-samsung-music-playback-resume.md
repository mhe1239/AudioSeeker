# 작업 보고서: REA 기반 재생 위치 복원 보강

- 작성일: 2026-10-16
- 상태: 완료
- 관련 요구사항: [`REQUIREMENTS.md`](../../REQUIREMENTS.md), [`docs/02-specs.md`](../02-specs.md)

## 변경 내용

- REA로 연결된 Galaxy S23+에서 Samsung Music `com.sec.android.app.music` 16.2.45.1 APK를 읽기 전용으로 확보하고 정적 분석했다. APK SHA-256은 `020557d92af42c151d5c06eff992d6f36e1d9d5e6e261c26c069acca485ba2cc`다.
- `PlaybackService`가 재생 위치 저장·삭제를 유일하게 담당한다. 재생 중 5초마다 저장하고, 탐색·트랙 전환·작업 제거·서비스 종료·일시정지 시에도 즉시 저장한다. 재생 종료 직전 1초 이내의 위치는 제거한다.
- 서비스의 재생 항목 전환 시 마지막 재생 곡 ID도 저장해, 화면이 없는 백그라운드 재생에서도 ViewModel의 복원 키와 일치하도록 했다.
- `PlayerViewModel`과 `MainActivity`는 위치를 저장하지 않고, 서비스의 `MediaController` 위치를 화면에 반영한다. ViewModel은 서비스가 저장한 값만 새 재생 큐 시작 위치로 읽는다.

## 검증

- `./gradlew.bat :app:assembleDebug` 성공.
- 실제 기기 설치·백그라운드 재생·프로세스 종료 후 복원은 이번 작업에서 실행하지 않았다.

## 판단과 제약

- 관찰: Samsung Music `PlayerService`는 `onCreate`에서 미디어 세션을 연결한다(REA 증거 `ev_67e6f38ff4b00a8abbfb1bcc32aa83d8c930d719dcbbf7bb6198390ad6723427`). 서비스의 재생 큐 경로는 `long` 위치 값을 플레이어 소스까지 전달한다(REA 증거 `ev_d7338bd8770bba795fe7f2b23d6f9bc8006015e176637c27eef34f17e6eb0c03`, `ev_f5ab1a49082ab493427b41ec08bff68a8f0e88ecb2def01ec1fabcb2ce95f9b2`).
- 추론: 화면 수명과 무관한 서비스 측 위치 저장이 AudioSeeker의 기존 ViewModel 저장을 보완한다.
- 미확인: Samsung Music의 난독화된 내부 저장소와 실제 실행 시 복원 조건은 정적 분석만으로 확정할 수 없다. AudioSeeker도 실제 기기에서 서비스 제거·재시작 경로를 확인해야 한다.

## 후속 작업

- [재생 서비스 위치 저장 실제 기기 검증](../todo/261016-playback-service-resume-device-validation.md)
