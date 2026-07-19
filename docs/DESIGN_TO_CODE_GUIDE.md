# 디자인 스펙 → 코드 구현 가이드 (Claude Code)

피그마 디자인 스펙(링크) 또는 디자인 스펙 이미지/PDF를 Claude Code의 Context로 넣고,
이 프로젝트의 아키텍처(4-레이어 멀티모듈 + MVI + 디자인 토큰)에 맞는 화면/기능을 생성하는 방법입니다.

```
[디자인 입력]                      [스킬 파이프라인]                      [산출물]
Figma 링크 (MCP) ──┐
디자인 스펙 이미지 ──┼──▶ /design-token-sync ──▶ /design-to-feature ──▶ 4-모듈 feature
디자인 스펙 PDF  ──┘      (테마 토큰 갱신)        (화면/기능 생성)         + 라우트 등록
                                                      │                  + 빌드 통과
                                                      └─ gradle-build-check / run-android-tests 자동 검증
```

---

## 1. 사전 준비

### 1.1 실행 위치

반드시 **Dandi 프로젝트 루트**(또는 이 템플릿에서 파생한 프로젝트 루트)에서 `claude` 를 실행합니다.
`.agents/skills/` 의 스킬과 README 의 아키텍처 규칙이 자동으로 Context에 잡힙니다.

### 1.2 디자인 입력 3가지 방법

| 방법 | 준비 | 품질 |
|---|---|---|
| **① Figma MCP (권장)** | 아래 1.3 연결 필요 | 레이아웃 수치·Variables(토큰)·컴포넌트 구조를 **구조화된 데이터로** 수신. 가장 정확 |
| **② 디자인 스펙 이미지** | png/jpg 파일을 프롬프트에 드래그하거나 경로 제공 | 시각 분석 기반. 색상/타이포는 근사치 — 토큰 매핑 리포트 확인 필수 |
| **③ 디자인 스펙 PDF** | PDF 경로 제공 (페이지 지정 권장) | ②와 동일 + 텍스트로 적힌 수치 스펙은 정확히 추출됨 |

### 1.3 Figma MCP 연결 (방법 ①)

**A. Remote 서버 (권장 — 기능이 가장 넓음):**

```bash
claude mcp add --transport http figma https://mcp.figma.com/mcp
```

이후 Claude Code 안에서 `/mcp` 로 Figma 계정 인증(OAuth)을 완료합니다.

> **CLI가 설치되어 있지 않은 경우(데스크톱 앱만 사용):** 위 명령은 불필요합니다 — 이 저장소 루트의 `.mcp.json` 이 같은 역할을 하며 이미 포함되어 있습니다. 프로젝트 폴더에서 **새 세션을 시작**하면 figma 서버 사용 승인을 묻고, 첫 사용 시 인증(OAuth) 안내가 뜹니다. CLI 설치는 `npm install -g @anthropic-ai/claude-code`.

**B. Figma 데스크톱 앱 내장 서버:** Figma 데스크톱 앱 → Preferences → *Enable Dev Mode MCP Server* 활성화 후:

```bash
claude mcp add --transport http figma-desktop http://127.0.0.1:3845/mcp
```

**C. Figma의 Claude Code 플러그인:** Figma Community 의 "Figma MCP in Claude Code" 플러그인을 설치하면 MCP 설정과 디자인 워크플로우용 스킬이 함께 들어옵니다.

연결 확인: `claude mcp list` 에 figma 가 ✓ connected 로 보이면 됩니다.
(엔드포인트/절차가 바뀔 수 있으니 안 되면 [Figma 공식 가이드](https://help.figma.com/hc/en-us/articles/39888612464151) 참고)

---

## 2. 표준 워크플로우

> **신규 프로젝트를 디자인 스펙으로 바로 시작하려면**: `/new-project-from-archi MyShop com.example.myshop` 에
> 디자인 스펙(Figma 링크/이미지/PDF)을 함께 주세요 — 템플릿 복제 후 아래 Step 1~2가 이어서 실행됩니다.
> 이미 프로젝트가 있다면 아래 Step 1부터 진행합니다.

> `intro`/`search`/`favorite`/`fullScreenMedia`는 원본 아키텍처를 설명하기 위한 레거시 예시 이름입니다. 현재 저장소의 모듈이나 복제할 소스 경로를 뜻하지 않으며, 실제 모듈은 `settings.gradle.kts`를 기준으로 확인합니다.

### Step 1 — 토큰 동기화: `/design-token-sync`

새 디자인 시스템을 적용할 때 **가장 먼저 1회** 실행합니다. 모든 색/타이포 기본값은
[DesignTokens.kt](../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/ui/token/DesignTokens.kt)
한 파일에 모여 있고, 스킬은 이 파일의 `FIGMA-TOKEN-INJECTION-POINT` 마커 구간만 수정합니다.

```text
/design-token-sync https://www.figma.com/design/abc123/MyApp?node-id=1-2
```

```text
/design-token-sync — 첨부한 디자인 시스템 스펙 이미지 기준으로 컬러/타이포 토큰을 갱신해줘
```

끝나면 **매핑 리포트**(어떤 디자인 값이 어떤 토큰 슬롯에 들어갔는지 + 미매핑 목록)를 반드시 확인하세요.
슬롯이 없는 새로운 색(예: 경고색)은 스킬이 임의로 추가하지 않고 물어봅니다.

> Figma 쪽 Variable 네이밍을 `bg/default/level0`, `content/accent`, `palette/gray/900`,
> Text Style 을 `title/strong/L` 형태로 맞춰두면 매핑이 100% 자동화됩니다. (디자이너 가이드로 공유 권장)

### Step 2 — 화면 구현: `/design-to-feature`

화면(프레임) 단위로 실행합니다. 4-모듈 스캐폴딩 → 토큰 기반 Compose UI → 데이터 레이어 → 라우트 등록 → 빌드 검증까지 한 번에 진행됩니다.

```text
# Figma 링크로
/design-to-feature productList https://www.figma.com/design/abc123/MyApp?node-id=10-345

# 디자인 스펙 이미지로 (이미지를 채팅에 드래그하면서)
/design-to-feature productList — 첨부 이미지가 이 화면의 디자인 스펙이야

# PDF 의 특정 페이지로
/design-to-feature productList — 디자인스펙.pdf 3~4페이지가 이 화면 스펙이야
```

API 명세가 있으면 같이 주세요 (없으면 스텁 데이터로 화면이 뜨게 만들고 `TODO-API-SPEC` 마커를 남깁니다):

```text
/design-to-feature productList <figma링크>
API는 GET /v1/products 이고 응답 예시는 아래 JSON이야: {...}
```

스킬이 구현 전에 **화면 인벤토리**(섹션/상태/인터랙션/데이터/토큰 매핑)와
**베이스 패턴 선택**(intro/search/favorite/fullScreenMedia 중 무엇을 골든 예제로 삼는지)을 먼저 보고합니다 — 여기서 방향이 틀렸으면 바로 잡아주세요.

### Step 3 — 검증

빌드 검증(`gradle-build-check`)은 파이프라인에 포함되어 자동 실행됩니다. 추가 검증:

```text
/run-android-tests                      # 단위 테스트
```

에뮬레이터가 떠 있으면 딥링크로 새 화면에 바로 진입해 눈으로 확인할 수 있습니다:

```bash
./gradlew :app:installDebug
adb shell 'am start -W -a android.intent.action.VIEW -d "https://www.dandi.com/productList" com.dandi.nyummy'
```

스크린샷 비교까지 시키려면:

```text
/verify-screen productList — Figma 프레임(또는 첨부 이미지)과 비교해줘
```

---

## 3. 자주 쓰는 프롬프트 모음

| 상황 | 프롬프트 |
|---|---|
| 토큰만 갱신 | `/design-token-sync <figma링크 또는 이미지>` |
| 화면 1개 구현 | `/design-to-feature <이름> <figma링크 또는 이미지/PDF>` |
| 화면 안에 컴포넌트 1개만 추가 | `/compose-component — 첨부 이미지의 카드 컴포넌트를 만들어줘` |
| API 연결만 | `/api-dto-code-gen` + 예시 JSON 붙여넣기 |
| 빈 모듈 스캐폴딩만 | `/make-new-feature-module <이름>` |
| 빌드 확인 | `/gradle-build-check` |

여러 화면을 연달아 만들 때는 화면마다 `/design-to-feature` 를 반복 실행하면 됩니다 (한 세션에서 진행하면 토큰 매핑 맥락이 유지됩니다).

---

## 4. AI가 따르는 규칙 (요약)

스킬들이 강제하는 핵심 규칙입니다. 결과물 리뷰 시 이 기준으로 보면 됩니다.

1. **색/타이포 하드코딩 금지** — `DesignSystemThemeImpl.designSystemColor.*` / `typeScale.*` 만 사용. raw hex/sp 가 보이면 버그입니다.
2. **의존 방향** — `presentation → domain → entity`, `data → domain → entity`. presentation↔data 직접 의존 금지.
3. **4-모듈 세트** — feature = entity/domain/data/presentation. 골든 예제(intro/search/favorite/fullScreenMedia) 중 하나를 베이스로 복제.
4. **MVI 네이밍** — `{Feature}Page / UIState / Intent / ReducerEvent / ViewModel`, UIState 컬렉션은 ImmutableList/Set.
5. **DTO/VO 분리** — DTO 전 필드 nullable, VO 기본값, 변환은 data 레이어 `toVO()` 에서만.
6. **항상 빌드 통과 상태로 종료** — 실패 시 스킬이 자가 수정 루프를 돕니다.

---

## 5. 트러블슈팅

| 증상 | 조치 |
|---|---|
| Figma MCP 도구가 안 보임 | `claude mcp list` 로 연결 확인 → `/mcp` 재인증. 데스크톱 서버는 Figma 앱이 켜져 있어야 함 |
| 토큰 매핑이 엉뚱함 | Figma Variable 네이밍을 §Step 1 규칙으로 정리하거나, 미매핑 리포트에서 직접 지정: "그 색은 contentAccent로 매핑해" |
| 디자인에 없는 색이 결과물에 있음 | 토큰 기본값(템플릿 값)이 남은 것 — `/design-token-sync` 부터 다시 |
| 빌드 실패 반복 | `/gradle-build-check` 단독 실행으로 에러를 분리 → 해당 에러만 고치게 지시 |
| 이미지 기반 결과의 수치가 어긋남 | 이미지 해상도를 높이거나, 여백/크기 수치를 프롬프트에 텍스트로 명시 (이미지는 근사치임) |
| 에뮬레이터에서 딥링크가 안 열림 (`unable to resolve Intent`) | App Links 도메인 미검증 상태 — `am start` 에 `-n com.dandi.nyummy/.main.presentation.MainActivity` 명시적 컴포넌트 추가 |
| JDK 못 찾음 (`Unable to locate a Java Runtime`) | `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` 후 재시도 |

---

## 6. 함께 쓰는 스킬 / 로드맵

- `/new-project-from-archi`: 이 템플릿을 새 프로젝트로 복제(이름/패키지 치환, 레퍼런스 feature 선택 제거). **디자인 스펙(Figma 링크/이미지/PDF)을 함께 주면 복제 후 토큰 동기화 → 화면 생성까지 이어서 실행** — 사용 가능
- `/verify-screen`: 에뮬레이터 스크린샷 vs 디자인 스펙 시각 비교 — 사용 가능
- Code Connect: Figma 컴포넌트 ↔ 공통 Composable 매핑 등록으로 컴포넌트 재사용 정확도 향상 — 예정 (실제 Figma 디자인 시스템 파일 필요)
- 자세한 설계 배경: [design-system.md](architecture/design-system.md)
