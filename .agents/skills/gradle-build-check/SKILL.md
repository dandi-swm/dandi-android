---
name: "gradle-build-check"
description: "Verify the Android project builds and passes lint after code changes. Trigger before reporting a coding task complete, after editing Kotlin/Gradle files, or when the user asks to \"build\", \"lint\", \"compile check\", or \"make sure it works\"."
---

# 빌드 & 린트 점검

코드 변경 후 "끝났다"고 보고하기 전에 반드시 거쳐야 할 검증 절차입니다.

## 1. 빠른 검증 (권장 기본)

컴파일만 빠르게 확인:
```bash
./gradlew :app:compileDebugKotlin
```

대부분의 타입 에러·import 누락·미사용 코드는 여기서 잡힙니다. 1~2초 변경에 30초 빌드는 과합니다.

## 2. 전체 검증 (PR 직전, 의존성 변경 시)

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

- `assembleDebug`: APK 빌드까지 — 리소스·Manifest·R 클래스 검증.
- `lintDebug`: Android Lint — deprecated API, 접근성, 성능 경고 검출.

## 3. 결과 처리

### 성공 시
한 줄 보고: "빌드/린트 통과". 출력 그대로 붙여넣기 금지.

### 실패 시
1. 에러 메시지 첫 번째만 집중 (이후 에러는 cascade인 경우가 많음).
2. 파일 경로·라인 번호를 [path:line](path:line) 형식으로 보고.
3. 첫 번째 에러만으로 수정 방향이 명확하지 않거나 로그가 길면 `build-fixer` subagent에 원래 실행한 Gradle 명령과 핵심 에러를 전달해 수정하게 한다.
4. 수정 후 메인 에이전트가 다시 같은 명령으로 재검증한다. 통과할 때까지 반복.

### 린트 경고
- Lint 리포트: `app/build/reports/lint-results-debug.html`
- **에러(error)**는 반드시 수정. **경고(warning)**는 사용자에게 알리고 판단 위임.

## 4. 흔한 함정

- `./gradlew build`는 모든 variant + 테스트까지 돌려 매우 느립니다. 위 명령들로 대체하세요.
- 첫 빌드/Gradle 데몬 시작은 오래 걸립니다. `run_in_background: true`로 띄우세요.
- "Unresolved reference"는 보통 (a) import 누락 (b) `gradle/libs.versions.toml`에 의존성 미등록. 후자라면 카탈로그 먼저 손보고 Gradle sync.
- Compose 관련 에러는 Kotlin 버전과 Compose Compiler 버전 호환을 확인. 이 프로젝트는 `kotlin-compose` 플러그인이 자동 매칭하므로 보통 문제없음.

## 5. 정리 (clean이 필요한 경우)

평소엔 불필요합니다. 다음 경우에만:
- "이상한" 에러가 안 사라질 때
- 의존성 버전 변경 후
- 캐시 손상 의심 시

```bash
./gradlew clean
```

clean 후엔 빌드가 처음부터 다시 돌아 1~2분 걸릴 수 있음을 인지하세요.
