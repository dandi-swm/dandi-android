---
name: "make-new-feature-module-ko"
description: "Korean-language variant of `make-new-feature-module`. Scaffold a brand-new feature as a 4-module set (entity / domain / data / presentation) mirroring the existing :intro / :search modules \u2014 gradle files, packages, AndroidManifest, base classes (VO, ErrorType in domain, Repository, UseCase, Page navigation object, DataSource, ApiService, RepositoryImpl, DataModule, DTO, MVI Intent + UIState, ViewModel, Page composable), the settings.gradle.kts include block, the :app dependency block, and an AppRouteRegistry entry. Use ONLY when the user explicitly asks for Korean output or asks to run `make-new-feature-module-ko`; use this skill for that request. Default for this task is the English `make-new-feature-module` skill (lower token cost)."
---

# 새 Feature 모듈 스캐폴딩 (intro / search 모듈 기준)

이 프로젝트는 feature 단위로 4개 Gradle 모듈(`entity` / `domain` / `data` / `presentation`)을 묶어 운영합니다. 본 스킬은 기존 `:intro`, `:search`, `:favorite`, `:fullScreenMedia` 모듈의 컨벤션을 그대로 따라 새 feature 4-모듈 세트를 만들어냅니다.

> 참고: 단일 Composable 추가는 `compose-component` 스킬, 기존 feature 안에 새 API 만 끼우는 작업은 `api-dto-code-gen-ko` 스킬. 본 스킬은 **새 Gradle 모듈 4개를 추가** 해 feature 자체를 신설하는 경우입니다. 헷갈리면 한 번 확인.

> 네비게이션 계약: 라우트/백스택/딥링크와 관련된 모든 것은 **`navigation-conventions`** 스킬을 따릅니다. Page 객체(§2.8)와 `AppRouteRegistry` 항목(§5)이 그 불변식이 사는 지점이니, 이 둘을 건드리기 전에 `navigation-conventions` 를 먼저 읽으세요.

## 0. 입력 — 진행 전 반드시 확보

### 0.1 필수 — 없으면 한 번 묻고 답을 기다림

- **`{NewFeature}` 이름** — 한 단어 또는 짧은 합성어. 예: `splash`, `search`, `feedDetail`, `profile`.
  - 빈 값/공백/특수문자/숫자 시작은 거부.

### 0.2 선택 — 비어 있으면 진행 시 한 번 더 확인

- **원격 API 사용 여부** — 데이터 소스가 있으면 ApiService/DTO/RepositoryImpl/DataSource/DataModule/Repository 풀세트 생성 (intro 패턴). 없어도 `data` 모듈은 그대로 생성되고 완전히 설정됩니다(`com.android.library` + hilt + manifest) — Kotlin 소스만 최소한으로 둡니다. UseCase 가 `:common:data` 의 기존 Repository 를 주입받아 쓰는 형태(`search`/`favorite`/`fullScreenMedia` 패턴). feature 별 의존성은 다릅니다: `favorite/data` 는 `kotlinx.serialization` 를 유지하지만(로컬 KV DTO) retrofit 은 없고, `search/data` 는 retrofit 을 두고 `:common:data` 를 소비합니다.
- **하단 탭 여부** (`isBottomTab`) — `search` / `favorite` 처럼 BottomTab 으로 노출할지. 기본은 `false`.
- **Navigation Args 가 있는지** — `FullScreenMediaPage.Args` 처럼 typed 인자가 필요한지. 기본은 인자 없음(intro/search/favorite 처럼 `object FeaturePage : Page` 만).
- **계층(중첩) 라우트인지** — `/articleList/articlePage/{articleId}` 처럼 path 파라미터를 가진 다중 세그먼트 경로(부모 리스트 아래의 상세 화면 등). 기본은 아니오(단일 세그먼트 path). 예이면: **부모 라우트**(기존 또는 형제 등록 경로)와 **path 파라미터 이름**을 정한다. `PATH` 는 `{param}` 템플릿이 되고, registry 의 `syntheticStack` 이 부모 체인을 깐다. §2.8(중첩 Page) + §5(중첩 라우트) 참고. typed Args 를 포함한다.

### 0.3 이름 정규화 — 두 형태로 분리

- `featureLower` (lowerCamelCase): Gradle 모듈 / 디렉토리 / 패키지 / namespace. 예: `feedDetail`.
- `FeatureUpper` (PascalCase): 클래스 prefix. 예: `FeedDetail`.

입력이 어떤 케이스로 오든 둘 다 도출:
- `splash` → `splash` / `Splash`
- `FeedDetail` → `feedDetail` / `FeedDetail`
- `feed_detail` → `feedDetail` / `FeedDetail` (snake → camel)

### 0.4 충돌 검사 — 진행 전 반드시

이미 있으면 **사용자에게 보고 + 중단** (덮어쓰기 금지):
- 루트의 `<featureLower>/` 디렉토리
- `settings.gradle.kts` 에 이미 `include(":<featureLower>:")` 라인이 있는지
- `app/build.gradle.kts` 의존성 블록에 이미 `:<featureLower>:` 라인이 있는지

## 1. 만들 디렉토리 구조 — 4 모듈

루트(`<featureLower>/`) 아래 4개 모듈. 클래스 prefix 는 `FeatureUpper` 사용. 각 모듈의 src 트리는 다음과 같습니다 (원격 API 사용하는 default 케이스):

```
<featureLower>/
├── entity/
│   ├── build.gradle.kts                     # kotlin-jvm
│   └── src/main/java/com/jongchan/androidarchi/<featureLower>/entity/
│       └── <FeatureUpper>VO.kt
├── domain/
│   ├── build.gradle.kts                     # kotlin-jvm
│   └── src/main/java/com/jongchan/androidarchi/<featureLower>/domain/
│       ├── <FeatureUpper>Repository.kt
│       ├── <FeatureUpper>UseCase.kt
│       ├── <FeatureUpper>ErrorType.kt       # ⚠ entity 가 아니라 domain
│       └── <FeatureUpper>Page.kt            # NavRoute / Page 정의
├── data/
│   ├── build.gradle.kts                     # android-library + kotlinx.serialization (DTO 가 있을 때)
│   └── src/main/
│       ├── AndroidManifest.xml              # 빈 manifest
│       └── java/com/jongchan/androidarchi/<featureLower>/data/
│           ├── <FeatureUpper>ApiService.kt
│           ├── <FeatureUpper>DataSource.kt
│           ├── <FeatureUpper>RepositoryImpl.kt
│           ├── <FeatureUpper>DataModule.kt
│           └── dto/<FeatureUpper>DTO.kt
└── presentation/
    ├── build.gradle.kts                     # android-library + compose
    └── src/main/
        ├── AndroidManifest.xml              # 빈 manifest
        └── java/com/jongchan/androidarchi/<featureLower>/presentation/
            ├── <FeatureUpper>Intent.kt        # MVI Intent (sealed interface : MviIntent)
            ├── <FeatureUpper>ReducerEvent.kt  # MVI ReducerEvent (sealed interface : ReducerEvent) — stateful 화면
            ├── <FeatureUpper>ViewModel.kt
            └── <FeatureUpper>Page.kt          # 안에 data class <FeatureUpper>UIState 정의
```

> stateful 화면(search / favorite / fullScreenMedia)은 MVI: 상태 변이는 `dispatch(ReducerEvent) → reduce()` 를 거치며, 절대 `uiState.update {}` 를 직접 호출하지 않는다(AGENTS.md 규칙 3). 이들은 `<FeatureUpper>ReducerEvent.kt` 를 함께 둔다. `intro` 는 최소 non-MVI 예외(plain `ViewModel`, 빈 Composable stub) — 이 가장 단순한 화면 케이스에서만 ReducerEvent 파일을 생략한다.

> AndroidManifest 는 빈 `<manifest>` 태그만 있는 placeholder. data/presentation 에만 둠 (entity/domain 은 kotlin-jvm 모듈이라 manifest 불필요).

> 원격 API 가 없는 케이스(`search`, `favorite`, `fullScreenMedia` 처럼 자체 Api 가 없을 때): `data` 모듈은 여전히 생성되고 완전히 설정됩니다(`com.android.library` + hilt + manifest) — Kotlin 소스만 최소한으로 둡니다. `<FeatureUpper>ApiService.kt` / `<FeatureUpper>DataSource.kt` / `<FeatureUpper>RepositoryImpl.kt` / `<FeatureUpper>DataModule.kt` / `dto/` 를 생략. `domain/<FeatureUpper>Repository.kt` 도 자체 도메인이 없으면 생략하고 `:common:domain` 의 기존 Repository (예: `MediaSearchRepository`, `FavoriteRepository`) 를 UseCase 에서 주입받게 합니다.

## 2. 파일 템플릿

아래 템플릿에서 `<featureLower>` / `<FeatureUpper>` 를 §0.3 의 도출 값으로 치환합니다. 그 외 텍스트는 그대로 사용.

### 2.1 `entity/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    api(project(":common:entity"))
}
```

### 2.2 `domain/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    api(project(":common:domain"))
    api(project(":<featureLower>:entity"))
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

> 이것이 대표 domain 템플릿(`kotlin.jvm` + `coroutines.core`, `ksp` 없음) — `search/domain` / `favorite/domain` 와 일치한다. `intro/domain` 은 특수 케이스: `ksp` 플러그인과 `implementation(project(":search:domain"))` cross-feature 의존을 추가하고 `coroutines.core` 는 뺀다. 기본은 search/favorite 를 복제하고, ksp / cross-feature Page 네비게이션이 필요할 때만 intro 를 따른다.
>
> 다른 feature 의 `Page` 객체를 호출하는 UseCase 가 있으면 (예: `GetIntroUseCase` 가 `SearchPage` 로 navigate) 해당 `<other>:domain` 도 implementation 으로 추가합니다 (`intro/domain` 이 `:search:domain` 에 대해 하는 것처럼).

### 2.3 `data/build.gradle.kts` (원격 API 사용 케이스)

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jongchan.androidarchi.<featureLower>.data"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

dependencies {
    implementation(project(":<featureLower>:domain"))
    implementation(project(":<featureLower>:entity"))
    implementation(project(":common:data"))
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // Network
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

> 원격 API 가 없는 케이스는 `alias(libs.plugins.kotlinx.serialization)` plugin, `libs.retrofit` / `libs.kotlinx.serialization.json` dependencies 를 제거.

### 2.4 `presentation/build.gradle.kts`

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.jongchan.androidarchi.<featureLower>.presentation"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_stability.conf"))
    if (providers.gradleProperty("composecompiler.reports").orNull == "true") {
        val outDir = rootProject.layout.buildDirectory.dir(
            "compose_reports/${project.path.replace(":", "_").trim('_')}"
        )
        reportsDestination.set(outDir)
        metricsDestination.set(outDir)
    }
}

dependencies {
    implementation(project(":<featureLower>:domain"))
    implementation(project(":<featureLower>:entity"))
    implementation(project(":common:presentation"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

> 자체 `:<featureLower>:entity` 의존은 화면이 feature 의 VO 타입을 직접 참조할 때 필요(`intro/presentation` 패턴). 자체 entity 가 없고 `:common` VO 를 재사용하면 빼도 된다(`search/presentation` 패턴).
>
> 다른 feature 화면으로 이동하면서 그쪽 Args 를 직접 구성한다면(`SearchPage` 의 `FullScreenMediaPage.Args` 처럼 — `search/presentation` 이 바로 이 때문에 `:fullScreenMedia:domain` 을 추가한다) 해당 `<other>:domain` 도 implementation 추가.

### 2.5 `data/src/main/AndroidManifest.xml` 와 `presentation/src/main/AndroidManifest.xml`

둘 다 동일:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

</manifest>
```

### 2.6 `entity/.../<FeatureUpper>VO.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.entity

data class <FeatureUpper>VO(
    val placeholder: String,
) {
    companion object {
        val empty: <FeatureUpper>VO = <FeatureUpper>VO("")
    }
}
```

> 실제 도메인이 잡히면 `api-dto-code-gen-ko` 스킬로 교체. `placeholder` 는 의도적으로 임시 필드 — 도메인 확정 전에는 추측해서 채우지 않습니다.

### 2.7 `domain/.../<FeatureUpper>ErrorType.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.error.HttpErrorType

enum class <FeatureUpper>ErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    UNKNOWN(
        type = "api.<featureLower>.unknown",
        errorMsg = "알수없는 에러가 발생했습니다.",
        isHandledOnDomain = true,
    ),
}
```

> `type` 키 컨벤션과 enum-별 매핑 동작은 `api-dto-code-gen-ko` §10 의 ErrorType 섹션 참고.

### 2.8 `domain/.../<FeatureUpper>Page.kt` (Navigation 정의)

기본 — args 없는 페이지 (intro / search / favorite 패턴):

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.navigation.NavRoute
import com.jongchan.androidarchi.common.domain.navigation.Page

object <FeatureUpper>Page : Page {
    const val PATH = "/<featureLower>"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
```

> 시작 화면(start destination)만 예외: `IntroPage.PATH = ""` (빈 문자열), `/intro` 가 아니다. `""` 는 앱의 시작 라우트에만 쓰고, 나머지 args 없는 페이지는 `"/<featureLower>"` 를 쓴다.

typed Args 가 있는 페이지 (FullScreenMediaPage 패턴 — `Args : Page` 가 호출자가 사용하는 진짜 Page):

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.navigation.NavRoute
import com.jongchan.androidarchi.common.domain.navigation.NavRouteJson
import com.jongchan.androidarchi.common.domain.navigation.Page

object <FeatureUpper>Page {
    const val PATH = "/<featureLower>"

    private const val KEY_ID = "id"

    data class Args(
        val id: String = "",
    ) : Page {
        override fun toRoute(): NavRoute {
            val argsMap = buildMap {
                if (id.isNotEmpty()) put(KEY_ID, id)
            }
            return NavRoute(PATH, argsMap)
        }

        companion object {
            fun from(args: Map<String, String>): Args = Args(
                id = args[KEY_ID].orEmpty(),
            )
        }
    }
}
```

**계층(중첩) Page** — path 파라미터를 가진 형태(articleList → articlePage 패턴, `/parent/<featureLower>/{paramName}`). 위 typed-Args 형태에 한 가지가 더해진다: **`PATH` 가 다중 세그먼트 `{param}` 템플릿**이고, 파라미터는 path 문자열에 끼우지 않고 `args` 로만 나른다.

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.navigation.NavRoute
import com.jongchan.androidarchi.common.domain.navigation.Page

object <FeatureUpper>Page {
    // ★ PATH 는 "템플릿" 그 자체. 콘크리트 값은 절대 여기에 끼우지 않는다 — args 로만 전달.
    const val PATH = "/<parentSegment>/<featureLower>/{<paramName>}"

    // ★ 이 키는 PATH 의 {<paramName>} 과 "철자까지" 동일해야 한다.
    private const val KEY_<PARAM_UPPER> = "<paramName>"

    data class Args(
        val <paramName>: String = "",
    ) : Page {
        override fun toRoute(): NavRoute =
            NavRoute(PATH, mapOf(KEY_<PARAM_UPPER> to <paramName>))   // path 는 템플릿 그대로

        companion object {
            fun from(args: Map<String, String>): Args = Args(
                <paramName> = args[KEY_<PARAM_UPPER>].orEmpty(),
            )
        }
    }
}
```

> **3중 이름 일치(조용한 실패 함정):** 템플릿 구간 `{<paramName>}`, 상수 `KEY_<PARAM_UPPER> = "<paramName>"`, `Args.from` 조회 키가 모두 같은 문자열이어야 한다. 어긋나면 딥링크 값이 **오류 없이 유실**된다. `.agents/skills/navigation-conventions/SKILL.md`의 `navigation-conventions` Golden rules 2–3 참고.
>
> `PATH` 가 `{<paramName>}` 를 그대로 담는 이유: 그 템플릿 문자열이 백스택 키 식별자(`GenericNavKey.path`), O(1) 렌더 dispatch 키(`appRouteByPath[path]`), 딥링크 URL 템플릿 — 셋 모두 하나의 값이다. 콘크리트 `123` 은 `args` 에만 들어가므로 직렬화/프로세스 사망 복원이 안전하고 `RoutePattern` 이 값을 추출할 수 있다. `PATH` 에 `{` 가 들어간 라우트는 `appRoutePatterns` 에 자동 등록되어 딥링크가 공짜로 매칭된다.

### 2.9 `domain/.../<FeatureUpper>Repository.kt` (원격 API 가 있을 때만)

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.<featureLower>.entity.<FeatureUpper>VO

interface <FeatureUpper>Repository {
    suspend fun get<FeatureUpper>(): <FeatureUpper>VO
}
```

### 2.10 `domain/.../<FeatureUpper>UseCase.kt`

> 네이밍: `<Feature>UseCase.get<Feature>()` 라는 단일 컨벤션은 없다. `intro` 는 클래스명이 `GetIntroUseCase` 이고 `operator fun invoke()`, `search` 는 클래스명이 `SearchUseCase` 이고 역시 `operator fun invoke()`. 아래 기본 스캐폴드는 `<FeatureUpper>UseCase`(`search` 형태)를 쓴다. `GetIntroUseCase` 처럼 단일 getter 액션 클래스일 때만 `Get<FeatureUpper>UseCase` 형태를 쓴다.

원격 API 가 있는 케이스 (`Result<XVO>` 반환 + `HttpResponseException` 분기, `GetIntroUseCase` 처럼):

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.base.BaseUseCase
import com.jongchan.androidarchi.common.domain.error.HttpResponseException
import com.jongchan.androidarchi.common.domain.error.handlingErrorOnUseCase
import com.jongchan.androidarchi.common.domain.error.isCommonErrorHandling
import com.jongchan.androidarchi.common.domain.helper.MessageHelper
import com.jongchan.androidarchi.common.domain.helper.NavigationHelper
import com.jongchan.androidarchi.common.domain.helper.ResourceHelper
import com.jongchan.androidarchi.tti.TTIHelper
import com.jongchan.androidarchi.<featureLower>.entity.<FeatureUpper>VO
import javax.inject.Inject

// BaseUseCase 는 4개 헬퍼(resourceHelper, messageHelper, navigationHelper, ttiHelper)를 받는다.
// 생성자 파라미터를 그대로 super 로 넘긴다 (override val 아님). 실제 GetIntroUseCase 참고.
class <FeatureUpper>UseCase @Inject constructor(
    private val repository: <FeatureUpper>Repository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend fun get<FeatureUpper>(): Result<<FeatureUpper>VO> = try {
        Result.success(repository.get<FeatureUpper>())
    } catch (e: HttpResponseException) {
        handle<FeatureUpper>Error(e)
        Result.failure(e)
    }

    private fun handle<FeatureUpper>Error(e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
            return
        }
        val errorType = e.handlingErrorOnUseCase<<FeatureUpper>ErrorType>() ?: return
        when (errorType) {
            <FeatureUpper>ErrorType.UNKNOWN -> {
                // TODO: 도메인-특화 처리
            }
        }
    }
}
```

> `Flow` 반환 / 공용 favorite UseCase 주입 / Favorite 미사용 등 변형 패턴은 `api-dto-code-gen-ko` §10 참고.

### 2.11 `data/.../<FeatureUpper>ApiService.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.data

import com.jongchan.androidarchi.<featureLower>.data.dto.<FeatureUpper>DTO
import retrofit2.Response
import retrofit2.http.GET

interface <FeatureUpper>ApiService {
    @GET("/<featureLower>") // TODO: 실제 엔드포인트로 교체
    suspend fun get<FeatureUpper>(): Response<<FeatureUpper>DTO>
}
```

> 엔드포인트별 인증 헤더는 없다. 인증은 `common/data/.../di/NetworkModule.kt` 에서 중앙으로 추가된다(`Authorization: KakaoAK ${BuildConfig.API_KEY}`). 따라서 ApiService 는 `IntroApiService` 처럼 `@GET("/<endpoint>")` 만 둔다.

### 2.12 `data/.../<FeatureUpper>DataSource.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.data

import com.jongchan.androidarchi.common.data.BaseRemoteDataSource
import com.jongchan.androidarchi.<featureLower>.data.dto.<FeatureUpper>DTO

class <FeatureUpper>DataSource(
    private val apiService: <FeatureUpper>ApiService,
) : BaseRemoteDataSource() {
    suspend fun get<FeatureUpper>(): <FeatureUpper>DTO =
        checkResponse(apiService.get<FeatureUpper>())
}
```

### 2.13 `data/.../<FeatureUpper>RepositoryImpl.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.data

import com.jongchan.androidarchi.<featureLower>.domain.<FeatureUpper>Repository

class <FeatureUpper>RepositoryImpl(
    val dataSource: <FeatureUpper>DataSource,
) : <FeatureUpper>Repository {
    override suspend fun get<FeatureUpper>() = dataSource.get<FeatureUpper>().toVO()
}
```

### 2.14 `data/.../<FeatureUpper>DataModule.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.data

import com.jongchan.androidarchi.<featureLower>.domain.<FeatureUpper>Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object <FeatureUpper>DataModule {

    @Provides
    @Singleton
    fun provide<FeatureUpper>Repository(dataSource: <FeatureUpper>DataSource): <FeatureUpper>Repository =
        <FeatureUpper>RepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provide<FeatureUpper>DataSource(apiService: <FeatureUpper>ApiService): <FeatureUpper>DataSource =
        <FeatureUpper>DataSource(apiService)

    @Provides
    @Singleton
    fun provide<FeatureUpper>ApiService(retrofit: Retrofit): <FeatureUpper>ApiService =
        retrofit.create(<FeatureUpper>ApiService::class.java)
}
```

### 2.15 `data/.../dto/<FeatureUpper>DTO.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.data.dto

import com.jongchan.androidarchi.common.entity.UNKNOWN
import com.jongchan.androidarchi.<featureLower>.entity.<FeatureUpper>VO
import kotlinx.serialization.Serializable

@Serializable
data class <FeatureUpper>DTO(
    val placeholder: String? = null,
) {
    fun toVO(): <FeatureUpper>VO = <FeatureUpper>VO(
        placeholder = placeholder ?: UNKNOWN,
    )
}
```

> DTO 작성 규칙 (모든 필드 nullable + default, `@SerialName` 매핑, nested DTO, `toVO()` 위치 변형) 은 `api-dto-code-gen-ko` §4 참고.

### 2.16 `presentation/.../<FeatureUpper>Intent.kt` (MVI Intent)

```kotlin
package com.jongchan.androidarchi.<featureLower>.presentation

import com.jongchan.androidarchi.common.presentation.mvi.MviIntent

sealed interface <FeatureUpper>Intent : MviIntent {
    data object Load : <FeatureUpper>Intent
    // TODO: 화면 액션을 추가 (예: data class ClickItem(val id: Long) : <FeatureUpper>Intent)
}
```

### 2.16b `presentation/.../<FeatureUpper>ReducerEvent.kt` (MVI ReducerEvent — stateful 화면)

`reduce()` 에 입력되는 내부 이벤트. View 가 직접 dispatch 하지 않고, ViewModel 이 Intent / 코루틴 결과를 ReducerEvent 로 변환한다(search / favorite / fullScreenMedia 패턴).

```kotlin
package com.jongchan.androidarchi.<featureLower>.presentation

import com.jongchan.androidarchi.common.presentation.mvi.ReducerEvent
import com.jongchan.androidarchi.<featureLower>.entity.<FeatureUpper>VO

sealed interface <FeatureUpper>ReducerEvent : ReducerEvent {
    data object LoadStarted : <FeatureUpper>ReducerEvent
    data class Loaded(val vo: <FeatureUpper>VO) : <FeatureUpper>ReducerEvent
    data object LoadFailed : <FeatureUpper>ReducerEvent
}
```

> 이 파일은 최소 non-MVI 케이스(intro: plain `ViewModel`, 빈 stub)에서만 생략한다.

### 2.17 `presentation/.../<FeatureUpper>Page.kt` (UIState + Composable)

```kotlin
package com.jongchan.androidarchi.<featureLower>.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jongchan.androidarchi.common.presentation.mvi.UiState

data class <FeatureUpper>UIState(
    val isLoading: Boolean = true,
    val message: String = "",
) : UiState {
    companion object {
        val empty: <FeatureUpper>UIState = <FeatureUpper>UIState()
    }
}

@Composable
fun <FeatureUpper>Page(viewModel: <FeatureUpper>ViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    <FeatureUpper>PageContent(uiState = uiState)
}

@Composable
private fun <FeatureUpper>PageContent(uiState: <FeatureUpper>UIState) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            else -> Text(
                text = uiState.message.ifEmpty { "<FeatureUpper> Screen" },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
```

### 2.18 `presentation/.../<FeatureUpper>ViewModel.kt`

stateful (MVI) — `MviViewModel` 를 상속하고, 모든 상태 변이를 `dispatch(ReducerEvent) → reduce()` 로만 흘린다(AGENTS.md 규칙 3; `search` / `favorite` / `fullScreenMedia` 패턴). `uiState.update {}` 를 직접 호출하지 않는다.

```kotlin
package com.jongchan.androidarchi.<featureLower>.presentation

import androidx.lifecycle.viewModelScope
import com.jongchan.androidarchi.common.domain.error.HttpResponseException
import com.jongchan.androidarchi.common.domain.error.handlingErrorOnUseCase
import com.jongchan.androidarchi.common.domain.helper.MessageHelper
import com.jongchan.androidarchi.common.domain.helper.NavigationHelper
import com.jongchan.androidarchi.common.presentation.mvi.MviViewModel
import com.jongchan.androidarchi.<featureLower>.domain.<FeatureUpper>ErrorType
import com.jongchan.androidarchi.<featureLower>.domain.<FeatureUpper>UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class <FeatureUpper>ViewModel @Inject constructor(
    private val useCase: <FeatureUpper>UseCase,
    private val messageHelper: MessageHelper,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<<FeatureUpper>Intent, <FeatureUpper>UIState, <FeatureUpper>ReducerEvent>(
    <FeatureUpper>UIState.empty,
) {

    init {
        onIntent(<FeatureUpper>Intent.Load)
    }

    override fun onIntent(intent: <FeatureUpper>Intent) {
        when (intent) {
            <FeatureUpper>Intent.Load -> load()
        }
    }

    // 상태가 변이하는 유일한 곳 — (state, event) 의 순수 함수.
    override fun reduce(
        state: <FeatureUpper>UIState,
        event: <FeatureUpper>ReducerEvent,
    ): <FeatureUpper>UIState = when (event) {
        <FeatureUpper>ReducerEvent.LoadStarted -> state.copy(isLoading = true)
        is <FeatureUpper>ReducerEvent.Loaded -> state.copy(isLoading = false, message = event.vo.placeholder)
        <FeatureUpper>ReducerEvent.LoadFailed -> state.copy(isLoading = false)
    }

    private fun load() {
        dispatch(<FeatureUpper>ReducerEvent.LoadStarted)
        viewModelScope.launch {
            useCase.get<FeatureUpper>()
                .onSuccess { vo -> dispatch(<FeatureUpper>ReducerEvent.Loaded(vo)) }
                .onFailure(::handle<FeatureUpper>PageError)
        }
    }

    private fun handle<FeatureUpper>PageError(throwable: Throwable) {
        dispatch(<FeatureUpper>ReducerEvent.LoadFailed)
        val exception = throwable as? HttpResponseException ?: return
        exception.handlingErrorOnUseCase<<FeatureUpper>ErrorType>()?.let { errorType ->
            when (errorType) {
                <FeatureUpper>ErrorType.UNKNOWN -> {
                    // TODO: presentation 단 에러 처리
                }
            }
        }
    }
}
```

> `MviViewModel<I, S, E>` (`common/presentation/.../mvi/MviViewModel.kt`) 은 단일 `uiState: StateFlow<S>`, 유일한 진입점 `onIntent`, `currentState`, 그리고 `reduce(current, event)` 를 적용하는 `dispatch(event)` 를 제공한다. `uiState` 의 backing field 는 Kotlin 2.x explicit backing field 문법(`-Xexplicit-backing-fields` 옵션, 모든 모듈에 적용됨)을 쓴다.
>
> **최소 non-MVI 케이스(intro 한정):** 가장 단순한 화면은 단일 `StateFlow` 하나를 가진 plain `ViewModel` + 빈 Composable stub — `<FeatureUpper>ReducerEvent.kt` 도 `reduce()` 도 없다. intro 같은 사소한 시작 화면에만 쓰고, 실제 상태가 있는 화면은 위처럼 MVI 로 만든다.

## 3. `settings.gradle.kts` 수정

기존 다른 feature 그룹 (예: `:fullScreenMedia:*`) 블록 **다음** 빈 줄을 두고 새 블록 추가. 순서는 기존 컨벤션 (presentation → domain → data → entity).

```kotlin
include(":<featureLower>:presentation")
include(":<featureLower>:domain")
include(":<featureLower>:data")
include(":<featureLower>:entity")
```

> 기존 줄을 절대 건드리지 않습니다.

## 4. `app/build.gradle.kts` 수정

`dependencies { ... }` 블록의 기존 feature 의존성 블록 (`:fullScreenMedia:*` 묶음) 다음에 새 4개 줄을 추가:

```kotlin
implementation(project(":<featureLower>:presentation"))
implementation(project(":<featureLower>:domain"))
implementation(project(":<featureLower>:data"))
implementation(project(":<featureLower>:entity"))
```

> Hilt graph 가 새 모듈의 `@Module` 들을 보려면 app 이 직접 의존성을 가져야 합니다. 이걸 빼면 `*DataModule` 의 `@Provides` 가 graph 에 등록되지 않습니다.

## 4.5 `main/presentation/build.gradle.kts` update — REQUIRED

`AppRouteRegistry.kt` lives in `:main:presentation`, so it must depend on the new feature to import its Page/ViewModel. Add 2 lines (verified by smoke test — skipping this fails compilation of `:main:presentation`):

```kotlin
// after the existing feature presentation deps
implementation(project(":<featureLower>:presentation"))
// after the existing feature domain deps
implementation(project(":<featureLower>:domain"))
```

## 5. `AppRouteRegistry.kt` 수정 — 라우팅 등록

[main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppRouteRegistry.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppRouteRegistry.kt) 의 `appRoutes: List<AppRoute>` 에 한 entry 추가:

기본 — args 없고 BottomTab 도 아닌 페이지:

```kotlin
import com.jongchan.androidarchi.<featureLower>.domain.<FeatureUpper>Page
import com.jongchan.androidarchi.<featureLower>.presentation.<FeatureUpper>Page
import com.jongchan.androidarchi.<featureLower>.presentation.<FeatureUpper>ViewModel

// appRoutes 안에 추가
AppRoute(
    path = <FeatureUpper>Page.PATH,
    render = { <FeatureUpper>Page(viewModel = hiltViewModel<<FeatureUpper>ViewModel>()) },
),
```

BottomTab 으로 노출하는 경우 — `search` / `favorite` 처럼:

```kotlin
AppRoute(
    path = <FeatureUpper>Page.PATH,
    isBottomTab = true,
    syntheticStack = { args ->
        listOf(
            GenericNavKey(SearchPage.PATH),
            GenericNavKey(<FeatureUpper>Page.PATH, args),
        )
    },
    render = { <FeatureUpper>Page(viewModel = hiltViewModel<<FeatureUpper>ViewModel>()) },
),
```

typed Args 가 있는 경우 — `FullScreenMediaPage.Args` 처럼 (`render` 에서 `Args.from(args)` 로 디코딩 + 실패 시 fallback):

```kotlin
AppRoute(
    path = <FeatureUpper>Page.PATH,
    syntheticStack = { args ->
        listOf(
            GenericNavKey(SearchPage.PATH),
            GenericNavKey(<FeatureUpper>Page.PATH, args),
        )
    },
    render = { rawArgs ->
        val parsedArgs = remember(rawArgs) {
            runCatching { <FeatureUpper>Page.Args.from(rawArgs) }.getOrNull()
        }
        if (parsedArgs == null) {
            LocalNavigationHelper.current.navigateTo(SearchPage)
            return@AppRoute
        }
        // typed args 를 ViewModel 에 넘기려면 AssistedInject + Factory 패턴 (FullScreenMediaViewModel 참고)
        <FeatureUpper>Page(viewModel = hiltViewModel<<FeatureUpper>ViewModel>())
    },
),
```

계층 / `{param}` 라우트 — `articleList → articlePage` 처럼. `syntheticStack` 이 콜드 스타트용 **부모 체인 전체**를 깐다. 딥링크는 템플릿이 자동 매칭된다(추가 등록 불필요). 콜드 = `[Home, Parent, X(args)]`, 웜 딥링크 = bring-to-front(`handleDeepLink` 담당 — 여기서 재구현하지 말 것).

```kotlin
import com.jongchan.androidarchi.<parentFeature>.domain.<ParentUpper>Page   // 부모 라우트(기존 또는 형제)

AppRoute(
    path = <FeatureUpper>Page.PATH,                  // "/<parentSegment>/<featureLower>/{<paramName>}" — 템플릿
    syntheticStack = { args ->
        listOf(
            GenericNavKey(SearchPage.PATH),           // Home(=Search 탭), favorite/fullScreenMedia 와 동일
            GenericNavKey(<ParentUpper>Page.PATH),    // 부모 리스트 화면(이것도 반드시 등록된 라우트)
            GenericNavKey(<FeatureUpper>Page.PATH, args),  // 자기 자신 — path 는 템플릿, 값은 args
        )
    },
    render = { rawArgs ->
        val args = <FeatureUpper>Page.Args.from(rawArgs)   // 관대한 디코딩(기본값), FullScreenMediaPage.Args.from 처럼
        <FeatureUpper>Page(args = args, viewModel = hiltViewModel<<FeatureUpper>ViewModel>())
    },
),
```

> **단일 출처:** 콜드 스타트 백스택은 오직 이 `syntheticStack` 에서만 정의한다. 웜 딥링크 경로(`AppNavHost.kt` 의 `handleDeepLink`)는 대상만 최전면으로 올릴 뿐, 거기서 스택을 재정의하지 않는다.
>
> 부모(`<ParentUpper>Page`)도 등록된 라우트여야 한다. 부모가 같은 feature 안의 **새 형제 화면**이면 두 번째 `Page` 객체 + Composable + registry 항목을 추가한다 — feature 모듈 하나가 여러 화면(리스트 + 상세)을 가질 수 있다.

> `import` 가 같은 파일에 두 개의 `<FeatureUpper>Page` (domain 측 navigation object + presentation 측 Composable) 를 가리키므로, 기존 다른 feature 처럼 두 import 가 모두 들어가야 합니다.

## 6. 실행 순서 (체크리스트)

1. `{NewFeature}` / 옵션(원격 API 사용 여부, BottomTab, typed Args, 계층/중첩 라우트 여부) 입력 확보 → `featureLower` / `FeatureUpper` 도출.
2. 충돌 검사 (§0.4). 있으면 보고하고 중단.
3. 디렉토리 + 파일 생성 (§1, §2). 템플릿의 `<featureLower>` / `<FeatureUpper>` 치환 누락 검토.
4. `settings.gradle.kts` 에 4 줄 추가 (§3).
5. `app/build.gradle.kts` 의존성 블록에 4 줄 추가 (§4) + `main/presentation/build.gradle.kts` 에 2 줄 추가 (§4.5, REQUIRED).
6. `AppRouteRegistry.kt` 에 entry 추가 (§5).
7. 빌드 확인 — `gradle-build-check` 스킬. 새 모듈 4개가 컴파일되고 Hilt 그래프가 깨지지 않는지.
8. `architecture-guardian` subagent로 변경된 Kotlin/Gradle 파일을 리뷰한다. HIGH/MEDIUM 지적은 수정 후 `gradle-build-check`를 다시 실행한다.
9. 사용자에게 보고:
   - 만든 파일 목록 (markdown 링크)
   - 다음 단계 안내:
     - 진짜 API 응답이 잡히면 `api-dto-code-gen-ko` 스킬로 `placeholder` DTO/VO 교체
     - 화면을 진짜 그릴 때는 `compose-component` 스킬로 컴포넌트 추가
     - UseCase 단위 테스트는 `<feature>/domain/src/test/...` 에 작성 (`run-android-tests`)

## 7. 하지 말 것

스캐폴딩 단계에서만 적용되는 항목:

- 기존 모듈(`intro`, `search`, `favorite`, `fullScreenMedia`, `main`, `common`)의 파일은 절대 수정하지 않는다 — `settings.gradle.kts` / `app/build.gradle.kts` / `main/presentation/build.gradle.kts` / `AppRouteRegistry.kt` 의 **추가** 만 허용.
- `<FeatureUpper>Page` (Navigation 정의) 를 `presentation` 에 두지 않는다 — `domain` 에. presentation 의 `<FeatureUpper>Page.kt` 는 Composable.
- (중첩 라우트) path 파라미터 값을 `PATH` 에 끼우지 않는다 — `PATH` 는 `{param}` **템플릿** 그대로, 값은 `args` 로. 그리고 `{paramName}` · `KEY_*` · `Args.from` 키가 어긋나지 않게 한다(값이 조용히 유실됨). `.agents/skills/navigation-conventions/SKILL.md`의 `navigation-conventions` Golden rules 2–3 참고.
- (중첩 라우트) 콜드 백스택을 `syntheticStack` 외의 곳에서 재정의하지 않는다. Nav3 내장 딥링크 파서를 찾지 않는다 — 그런 건 없고 `RoutePattern` + 레지스트리가 처리한다.
- 빈 `<NewFeature>` 입력으로 진행하지 않는다.
- `placeholder` 필드가 의미 없어 보여도 임의 도메인 필드를 추측해서 채우지 않는다 — 후속 `api-dto-code-gen-ko` 단계에서 교체될 placeholder.
- ViewModel 의 상태를 `MutableStateFlow<String>` 같은 단일 primitive 로 두지 않는다 — `data class <Feature>UIState` 형태가 컨벤션.
- stateful 화면에서 `uiState.update {}` 로 상태를 직접 변이하지 않는다 — 모든 변이는 `dispatch(<Feature>ReducerEvent) → reduce()` 를 거친다(AGENTS.md 규칙 3). plain `ViewModel` 형태는 intro 같은 최소 화면에만.

데이터 레이어 컨벤션 (DTO `@Keep` 금지, `Json` / `Retrofit` / `OkHttp` 재정의 금지, `ErrorType` 위치, DTO→VO 매핑 등) 은 `api-dto-code-gen-ko` §13 의 don't 리스트가 canonical — 스캐폴딩으로 만든 placeholder 를 채울 때 따른다.
