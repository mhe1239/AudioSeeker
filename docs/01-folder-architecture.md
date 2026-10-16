# 폴더 아키텍처

이 문서는 AudioSeeker 저장소의 폴더 역할과 변경 위치를 정의한다. 새 기능은 먼저 이 구조에 맞는 위치를 정하고, 구조 자체가 바뀌면 이 문서를 같은 변경에서 갱신한다.

```text
AudioSeeker/
├── AGENTS.md                         # 에이전트·협업 작업 규약
├── CLAUDE.md                         # AGENTS.md를 가리키는 보조 진입점
├── REQUIREMENTS.md                   # 확정된 제품 요구사항
├── design.md                         # 화면·테마 디자인 기준
├── assets/
│   └── branding/                     # README·배포용 브랜딩 아이콘 변형본
├── app/
│   ├── build.gradle.kts              # 앱 모듈 의존성·Android 설정
│   └── src/main/
│       ├── AndroidManifest.xml        # 권한, 서비스, 런처·바로가기 선언
│       ├── java/com/creator/audioseeker/
│       │   ├── AudioTrack.kt         # 오디오 도메인 모델
│       │   ├── MainActivity.kt        # 앱 진입점
│       │   ├── PlayerViewModel.kt     # 재생·라이브러리 단일 상태 흐름
│       │   ├── PlaybackService.kt     # Media3 재생·미디어 세션 서비스
│       │   └── ui/                    # Compose 화면, 컴포넌트, 테마
│       └── res/                       # 문자열, 아이콘, 단축키 XML, 스타일
├── gradle/
│   └── libs.versions.toml             # Gradle 의존성 버전 카탈로그
└── docs/
    ├── 01-folder-architecture.md      # 이 문서
    ├── 02-specs.md                    # 기술 사양과 구현 규칙
    ├── 03-product-plan.md             # 제품 범위와 진행 계획
    ├── RECONSTRUCTION.md              # 이 브랜치의 복원·검증 이력
    ├── reports/                       # 완료 작업과 판단 기록
    └── todo/                          # 미완료 후속 작업
```

## 배치 원칙

- 기능의 UI는 `ui/`, 재생과 라이브러리 상태는 `PlayerViewModel`, 백그라운드 재생과 미디어 알림은 `PlaybackService`에 둔다.
- Android 리소스는 `res/`에 두고, 라이브러리 버전은 모듈 파일에 하드코딩하지 않고 `gradle/libs.versions.toml`에서 관리한다.
- 제품 요구사항은 `REQUIREMENTS.md`, 디자인 결정은 `design.md`, 지속적으로 참조할 개발 규칙은 `docs/`에 둔다.
- `docs/reports/`는 이미 끝난 작업의 맥락을, `docs/todo/`는 아직 끝나지 않은 작업의 실행 단위를 관리한다.

## 문서 진입 순서

새 작업자는 `AGENTS.md`를 읽은 뒤 이 문서, `02-specs.md`, `03-product-plan.md`, `todo/00-todo-list.md` 순서로 확인한다. 과거 복원 과정의 근거가 필요한 경우에만 `RECONSTRUCTION.md`를 추가로 읽는다.
