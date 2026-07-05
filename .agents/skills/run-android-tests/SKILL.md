---
name: "run-android-tests"
description: "Run Android unit tests or instrumented (Compose UI) tests for this project. Trigger when the user asks to \"run tests\", \"test the app\", \"verify\", \"check test pass\", or after editing files under app/src/test or app/src/androidTest. Also use after non-trivial code changes to verify nothing broke."
---

# Android 테스트 실행

이 템플릿의 테스트는 **도메인 레이어 JVM 유닛 테스트뿐**입니다. 계측(androidTest)/Compose UI/Espresso 테스트는 현재 구성되어 있지 않습니다.

## 1. 어떤 테스트인지 판단

| 종류 | 위치 | 명령 | 언제 |
|---|---|---|---|
| **유닛 테스트** (순수 Kotlin/JVM) | `<feature>/domain/src/test/` | `./gradlew test` (루트) 또는 모듈별 `:<feature>:domain:test` | UseCase, RouteMatcher/RoutePattern 등 순수 로직 |

- `domain` 모듈들은 `kotlin.jvm` 모듈이라 테스트 태스크는 `test` 입니다 (`testDebugUnitTest` 는 jvm 모듈에 존재하지 않습니다).
- `app/src/test/` 와 `app/src/androidTest/` 는 **비어 있습니다** (테스트 .kt 없음). `:app:testDebugUnitTest` / `:app:connectedDebugAndroidTest` 로 돌릴 테스트가 없습니다.
- 현재 테스트가 있는 모듈: `intro/domain`, `search/domain`, `common/domain`, `main/domain`, `fullScreenMedia/domain` (UseCase 테스트 + `RouteMatcher`/`RoutePattern` 테스트).
- 계측/UI 테스트는 이 템플릿에 없습니다. UIAutomator 는 오직 `:baselineprofile` (Macrobenchmark) 에서만 쓰입니다 — UI 테스트 스위트가 아닙니다.

판단 기준:
- "테스트 돌려줘" → 루트 `./gradlew test` 로 도메인 유닛 테스트 전체. 빠르고 디바이스 불필요.
- 특정 feature 만 → `:<feature>:domain:test` (예: `:search:domain:test`).

## 2. 실행 절차

```bash
# 전체 유닛 테스트 (모든 도메인 모듈)
./gradlew test

# 특정 모듈만
./gradlew :search:domain:test
./gradlew :main:domain:test     # RouteMatcher / RoutePattern
```

## 3. 결과 보고

- 통과: 통과한 테스트 수만 한 줄로. 출력을 그대로 붙여넣지 마세요.
- 실패: 실패한 테스트 이름 + 짧은 원인 + 해당 파일 경로(라인 번호). 사용자가 바로 수정 위치로 이동할 수 있게.
- 리포트 HTML 경로도 함께 안내 (모듈별): 예) `search/domain/build/reports/tests/test/index.html`

## 4. 흔한 함정
- `:app:testDebugUnitTest` / `:app:connectedDebugAndroidTest` 는 쓰지 마세요 — app 모듈에는 유닛/계측 테스트가 없습니다.
- 첫 빌드는 의존성 다운로드로 오래 걸립니다. `run_in_background: true`로 띄우고 알림을 받으세요.
- 테스트 프레임워크는 JUnit4 + MockK (도메인 모듈의 `testImplementation`). Compose UI Test / Espresso 는 이 템플릿에서 사용하지 않습니다.
