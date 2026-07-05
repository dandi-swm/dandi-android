---
name: "make-new-feature-module"
description: "Scaffold a brand-new feature as a 4-module set (entity / domain / data / presentation) mirroring the existing :intro / :search modules \u2014 gradle files, packages, AndroidManifest, base classes (VO, ErrorType in domain, Repository, UseCase, Page navigation object, DataSource, ApiService, RepositoryImpl, DataModule, DTO, MVI Intent + UIState, ViewModel, Page composable), the settings.gradle.kts include block, the :app dependency block, and an AppRouteRegistry entry. Trigger when the user asks to \"make a new feature module\", names a new feature (e.g. \"splash\", \"search\") and wants the whole module skeleton, or asks to run `make_new_feature_module` / `make-new-feature-module`; use this skill for that request."
---

# New feature module scaffolding (intro / search pattern)

This project ships features as 4 Gradle modules (`entity` / `domain` / `data` / `presentation`). This skill creates a new 4-module set for a new feature, following the conventions of the existing `:intro`, `:search`, `:favorite`, `:fullScreenMedia` modules.

> Related: `compose-component` adds a single Composable; `api-dto-code-gen` wires a new API into an existing feature. **This skill creates 4 brand-new Gradle modules** for a brand-new feature. Confirm with the user if ambiguous.

> Navigation contract: this skill follows the **`navigation-conventions`** skill for everything route/back-stack/deep-link related. The Page object (§2.8) and the `AppRouteRegistry` entry (§5) are where those invariants live — read `navigation-conventions` before changing them.

## 0. Inputs — confirm before starting

### 0.1 Required — ask once if missing

- **`{NewFeature}` name** — one word or short compound. Examples: `splash`, `search`, `feedDetail`, `profile`.
  - Reject empty / whitespace / special chars / leading digit.

### 0.2 Optional — confirm once if missing

- **Remote API used?** — yes → full set (ApiService/DTO/RepositoryImpl/DataSource/DataModule/Repository, intro pattern). No → the `data` module still exists and stays fully configured (`com.android.library` + hilt + manifest); only the Kotlin source is minimal — the UseCase consumes an existing Repository from `:common:data` (the `search`/`favorite`/`fullScreenMedia` pattern). Per-feature deps vary: `favorite/data` keeps `kotlinx.serialization` (local KV DTO) but no retrofit; `search/data` still lists retrofit and consumes `:common:data`.
- **Bottom tab?** (`isBottomTab`) — like `search` / `favorite`. Default `false`.
- **Navigation Args needed?** — typed args like `FullScreenMediaPage.Args`. Default: no args (just `object FeaturePage : Page` like intro/search/favorite).
- **Nested / hierarchical route?** — a multi-segment path with a path parameter, e.g. `/articleList/articlePage/{articleId}` (a detail under a parent list). Default: no (single-segment path). If yes: pick the **parent route** (an existing or sibling registered path) and the **path-param name(s)**; the `PATH` becomes a `{param}` template and the registry `syntheticStack` lays the parent chain. See §2.8 (nested Page) + §5 (nested route). Implies typed Args.

### 0.3 Name normalization — derive both forms

- `featureLower` (lowerCamelCase): Gradle module / directory / package / namespace. e.g. `feedDetail`.
- `FeatureUpper` (PascalCase): class prefix. e.g. `FeedDetail`.

Whatever case the input arrives in, derive both:
- `splash` → `splash` / `Splash`
- `FeedDetail` → `feedDetail` / `FeedDetail`
- `feed_detail` → `feedDetail` / `FeedDetail` (snake → camel)

### 0.4 Conflict check — required

If any of these already exist, **report and stop** (no overwrite):
- `<featureLower>/` directory at root
- `include(":<featureLower>:")` line in `settings.gradle.kts`
- `:<featureLower>:` line in `app/build.gradle.kts` dependencies block

## 1. Directory layout — 4 modules

Under `<featureLower>/`. Class prefix uses `FeatureUpper`. Default case (remote API used):

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
│       ├── <FeatureUpper>ErrorType.kt       # ⚠ in domain, NOT entity
│       └── <FeatureUpper>Page.kt            # NavRoute / Page definition
├── data/
│   ├── build.gradle.kts                     # android-library + kotlinx.serialization (when DTO present)
│   └── src/main/
│       ├── AndroidManifest.xml              # empty placeholder
│       └── java/com/jongchan/androidarchi/<featureLower>/data/
│           ├── <FeatureUpper>ApiService.kt
│           ├── <FeatureUpper>DataSource.kt
│           ├── <FeatureUpper>RepositoryImpl.kt
│           ├── <FeatureUpper>DataModule.kt
│           └── dto/<FeatureUpper>DTO.kt
└── presentation/
    ├── build.gradle.kts                     # android-library + compose
    └── src/main/
        ├── AndroidManifest.xml              # empty placeholder
        └── java/com/jongchan/androidarchi/<featureLower>/presentation/
            ├── <FeatureUpper>Intent.kt        # MVI Intent (sealed interface : MviIntent)
            ├── <FeatureUpper>ReducerEvent.kt  # MVI ReducerEvent (sealed interface : ReducerEvent) — stateful screens
            ├── <FeatureUpper>ViewModel.kt
            └── <FeatureUpper>Page.kt          # contains data class <FeatureUpper>UIState
```

> Stateful screens (search / favorite / fullScreenMedia) are MVI: state changes go through `dispatch(ReducerEvent) → reduce()`, never `uiState.update {}` directly (AGENTS.md rule 3). They ship a `<FeatureUpper>ReducerEvent.kt`. `intro` is the minimal non-MVI exception (plain `ViewModel`, empty Composable stub) — omit the ReducerEvent file for that simplest-screen case only.

> AndroidManifest is an empty `<manifest>` placeholder. Only data/presentation need it (entity/domain are kotlin-jvm modules).

> No-remote-API case (like `search`, `favorite`, `fullScreenMedia` — no own Api): the `data` module is still created and fully configured (`com.android.library` + hilt + manifest) — only its Kotlin source is minimal. Omit `<FeatureUpper>ApiService.kt`, `<FeatureUpper>DataSource.kt`, `<FeatureUpper>RepositoryImpl.kt`, `<FeatureUpper>DataModule.kt`, `dto/`. Also omit `domain/<FeatureUpper>Repository.kt` if there's no own domain — the UseCase injects an existing Repository (e.g. `MediaSearchRepository`, `FavoriteRepository`) from `:common:domain`.

## 2. File templates

Replace `<featureLower>` / `<FeatureUpper>` with the values from §0.3. Other text is verbatim.

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

> This is the representative domain template (`kotlin.jvm` + `coroutines.core`, no `ksp`) — it matches `search/domain` / `favorite/domain`. `intro/domain` is a special case: it adds the `ksp` plugin and an `implementation(project(":search:domain"))` cross-feature dep, and drops `coroutines.core`. So clone search/favorite for the default; only mirror intro when you need ksp / a cross-feature Page nav.
>
> If a UseCase calls another feature's `Page` object (e.g. `GetIntroUseCase` navigates to `SearchPage`), add that `<other>:domain` as `implementation` (as `intro/domain` does for `:search:domain`).

### 2.3 `data/build.gradle.kts` (with remote API)

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

> No-remote-API case: drop `alias(libs.plugins.kotlinx.serialization)`, `libs.retrofit`, `libs.kotlinx.serialization.json`.

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

> The own `:<featureLower>:entity` dep is needed when the screen references the feature's VO type directly (the `intro/presentation` pattern). Drop it if the feature has no own entity and reuses `:common` VOs (the `search/presentation` pattern).
>
> If this screen navigates to another feature and constructs that feature's Args directly (like `SearchPage` building `FullScreenMediaPage.Args` — `search/presentation` adds `:fullScreenMedia:domain` for exactly this), add that `<other>:domain` as `implementation`.

### 2.5 `data/src/main/AndroidManifest.xml` and `presentation/src/main/AndroidManifest.xml`

Both identical:

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

> Once the actual domain is known, replace via `api-dto-code-gen`. `placeholder` is intentionally a temporary field — don't fill in guesses before the domain is decided.

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
        errorMsg = "An unknown error occurred.",
        isHandledOnDomain = true,
    ),
}
```

> See `api-dto-code-gen` §10 for the `type` key convention and per-enum matching behavior.

### 2.8 `domain/.../<FeatureUpper>Page.kt` (Navigation definition)

Default — args-less page (intro / search / favorite pattern):

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.navigation.NavRoute
import com.jongchan.androidarchi.common.domain.navigation.Page

object <FeatureUpper>Page : Page {
    const val PATH = "/<featureLower>"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
```

> The start destination is the exception: `IntroPage.PATH = ""` (empty string), not `/intro`. Use `""` only for the app's start route; every other args-less page uses `"/<featureLower>"`.

Page with typed Args (FullScreenMediaPage pattern — `Args : Page` is the actual Page that callers use):

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

**Nested / hierarchical page** with a path parameter (articleList → articlePage pattern — `/parent/<featureLower>/{paramName}`). This is the typed-Args form above with one twist: **`PATH` is a multi-segment `{param}` template**, and the param is carried in `args`, never spliced into the path string.

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.common.domain.navigation.NavRoute
import com.jongchan.androidarchi.common.domain.navigation.Page

object <FeatureUpper>Page {
    // ★ PATH is the TEMPLATE itself. Concrete values are NEVER substituted here — they go in args.
    const val PATH = "/<parentSegment>/<featureLower>/{<paramName>}"

    // ★ This key MUST equal the {<paramName>} in PATH, character-for-character.
    private const val KEY_<PARAM_UPPER> = "<paramName>"

    data class Args(
        val <paramName>: String = "",
    ) : Page {
        override fun toRoute(): NavRoute =
            NavRoute(PATH, mapOf(KEY_<PARAM_UPPER> to <paramName>))   // path stays the template

        companion object {
            fun from(args: Map<String, String>): Args = Args(
                <paramName> = args[KEY_<PARAM_UPPER>].orEmpty(),
            )
        }
    }
}
```

> **Three-way name match (silent-failure trap):** the template segment `{<paramName>}`, the constant `KEY_<PARAM_UPPER> = "<paramName>"`, and the `Args.from` lookup key must be the same string. Mismatch → the deep-link value is dropped with no error. See `navigation-conventions` Golden rules 2–3 in `.agents/skills/navigation-conventions/SKILL.md`.
>
> Why `PATH` holds `{<paramName>}` literally: that template string is the back-stack key identity (`GenericNavKey.path`), the O(1) render-dispatch key (`appRouteByPath[path]`), and the deep-link URL template — all one value. The concrete `123` lives only in `args`, so serialization/process-death restore stays safe and `RoutePattern` can extract it. A route whose `PATH` contains `{` is auto-added to `appRoutePatterns` — deep links match it for free.

### 2.9 `domain/.../<FeatureUpper>Repository.kt` (with remote API only)

```kotlin
package com.jongchan.androidarchi.<featureLower>.domain

import com.jongchan.androidarchi.<featureLower>.entity.<FeatureUpper>VO

interface <FeatureUpper>Repository {
    suspend fun get<FeatureUpper>(): <FeatureUpper>VO
}
```

### 2.10 `domain/.../<FeatureUpper>UseCase.kt`

> Naming: there is no single `<Feature>UseCase.get<Feature>()` convention. `intro` names its class `GetIntroUseCase` with `operator fun invoke()`; `search` names its class `SearchUseCase`, also with `operator fun invoke()`. The default scaffold below uses `<FeatureUpper>UseCase` (the `search` form). Use the `Get<FeatureUpper>UseCase` form only when the class is a single-action getter like `GetIntroUseCase`.

With remote API (`Result<XVO>` + `HttpResponseException` branching, like `GetIntroUseCase`):

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
                // TODO: domain-specific handling
            }
        }
    }
}
```

> See `api-dto-code-gen` §10 for variant patterns: Flow return / shared favorite UseCase injection / no-favorite case.

### 2.11 `data/.../<FeatureUpper>ApiService.kt`

```kotlin
package com.jongchan.androidarchi.<featureLower>.data

import com.jongchan.androidarchi.<featureLower>.data.dto.<FeatureUpper>DTO
import retrofit2.Response
import retrofit2.http.GET

interface <FeatureUpper>ApiService {
    @GET("/<featureLower>") // TODO: replace with actual endpoint
    suspend fun get<FeatureUpper>(): Response<<FeatureUpper>DTO>
}
```

> No per-endpoint auth header. Authorization is added centrally in `common/data/.../di/NetworkModule.kt` (`Authorization: KakaoAK ${BuildConfig.API_KEY}`), so the ApiService is just `@GET("/<endpoint>")` like `IntroApiService`.

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

> See `api-dto-code-gen` §4 for DTO authoring (all-nullable + default, `@SerialName` mapping, nested DTO, `toVO()` placement variants).

### 2.16 `presentation/.../<FeatureUpper>Intent.kt` (MVI Intent)

```kotlin
package com.jongchan.androidarchi.<featureLower>.presentation

import com.jongchan.androidarchi.common.presentation.mvi.MviIntent

sealed interface <FeatureUpper>Intent : MviIntent {
    data object Load : <FeatureUpper>Intent
    // TODO: add screen actions (e.g. data class ClickItem(val id: Long) : <FeatureUpper>Intent)
}
```

### 2.16b `presentation/.../<FeatureUpper>ReducerEvent.kt` (MVI ReducerEvent — stateful screens)

Internal events fed into `reduce()`. View never dispatches these directly — the ViewModel translates Intents / coroutine results into ReducerEvents (search / favorite / fullScreenMedia pattern).

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

> Omit this file only for the minimal non-MVI case (intro: plain `ViewModel`, empty stub).

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

Stateful (MVI) — extends `MviViewModel`, routes every state change through `dispatch(ReducerEvent) → reduce()` (AGENTS.md rule 3; the `search` / `favorite` / `fullScreenMedia` pattern). Never call `uiState.update {}` directly.

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

    // The single place state mutates — pure function of (state, event).
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
                    // TODO: presentation-side error handling
                }
            }
        }
    }
}
```

> `MviViewModel<I, S, E>` (in `common/presentation/.../mvi/MviViewModel.kt`) exposes the single `uiState: StateFlow<S>`, `onIntent` as the only entry point, `currentState`, and `dispatch(event)` which applies `reduce(current, event)`. `uiState`'s backing field uses Kotlin 2.x explicit-backing-field syntax (the `-Xexplicit-backing-fields` arg, enabled in every module).
>
> **Minimal non-MVI case (intro only):** the simplest screen is a plain `ViewModel` with a single `StateFlow` and an empty Composable stub — no `<FeatureUpper>ReducerEvent.kt`, no `reduce()`. Use that only for a trivial start screen like intro; any screen with real state must be MVI as above.

## 3. `settings.gradle.kts` update

After the existing feature-group block (e.g. `:fullScreenMedia:*`), insert a blank line then the new block. Order follows the existing convention (presentation → domain → data → entity).

```kotlin
include(":<featureLower>:presentation")
include(":<featureLower>:domain")
include(":<featureLower>:data")
include(":<featureLower>:entity")
```

> Never touch existing lines.

## 4. `app/build.gradle.kts` update

After the existing feature dependency block (`:fullScreenMedia:*` group) in the `dependencies { ... }` block, add 4 lines:

```kotlin
implementation(project(":<featureLower>:presentation"))
implementation(project(":<featureLower>:domain"))
implementation(project(":<featureLower>:data"))
implementation(project(":<featureLower>:entity"))
```

> The Hilt graph needs the app module to depend on the new modules to see their `@Module`s. Skipping this leaves `*DataModule` `@Provides` unregistered.

## 4.5 `main/presentation/build.gradle.kts` update — REQUIRED

`AppRouteRegistry.kt` lives in `:main:presentation`, so it must depend on the new feature to import its Page/ViewModel. Add 2 lines (verified by smoke test — skipping this fails compilation of `:main:presentation`):

```kotlin
// after the existing feature presentation deps
implementation(project(":<featureLower>:presentation"))
// after the existing feature domain deps
implementation(project(":<featureLower>:domain"))
```

## 5. `AppRouteRegistry.kt` update — register the route

Add an entry to `appRoutes: List<AppRoute>` in [main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppRouteRegistry.kt](main/presentation/src/main/java/com/jongchan/androidarchi/main/presentation/navigation/AppRouteRegistry.kt).

Default — args-less, non-bottom-tab page:

```kotlin
import com.jongchan.androidarchi.<featureLower>.domain.<FeatureUpper>Page
import com.jongchan.androidarchi.<featureLower>.presentation.<FeatureUpper>Page
import com.jongchan.androidarchi.<featureLower>.presentation.<FeatureUpper>ViewModel

// inside appRoutes
AppRoute(
    path = <FeatureUpper>Page.PATH,
    render = { <FeatureUpper>Page(viewModel = hiltViewModel<<FeatureUpper>ViewModel>()) },
),
```

Bottom-tab — like `search` / `favorite`:

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

Typed Args — like `FullScreenMediaPage.Args` (`render` decodes via `Args.from(args)` with fallback):

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
        // To pass typed args into the ViewModel, use AssistedInject + Factory pattern (see FullScreenMediaViewModel)
        <FeatureUpper>Page(viewModel = hiltViewModel<<FeatureUpper>ViewModel>())
    },
),
```

Nested / `{param}` route — like `articleList → articlePage`. The `syntheticStack` lays the **full parent chain** for cold start; deep links match the template automatically (no extra registration). Cold = `[Home, Parent, X(args)]`; warm deep-link = bring-to-front (handled by `handleDeepLink`, don't re-implement here).

```kotlin
import com.jongchan.androidarchi.<parentFeature>.domain.<ParentUpper>Page   // parent route (existing or sibling)

AppRoute(
    path = <FeatureUpper>Page.PATH,                  // "/<parentSegment>/<featureLower>/{<paramName>}" — template
    syntheticStack = { args ->
        listOf(
            GenericNavKey(SearchPage.PATH),           // Home (= Search tab), as favorite/fullScreenMedia do
            GenericNavKey(<ParentUpper>Page.PATH),    // parent list screen (must itself be registered)
            GenericNavKey(<FeatureUpper>Page.PATH, args),  // self — path is the template, value lives in args
        )
    },
    render = { rawArgs ->
        val args = <FeatureUpper>Page.Args.from(rawArgs)   // lenient (defaults), like FullScreenMediaPage.Args.from
        <FeatureUpper>Page(args = args, viewModel = hiltViewModel<<FeatureUpper>ViewModel>())
    },
),
```

> **Single source of truth:** the cold-start back stack is defined ONLY in this `syntheticStack`. The warm deep-link path (`handleDeepLink` in `AppNavHost.kt`) brings only the target to front — never redefine the stack there.
>
> The parent (`<ParentUpper>Page`) must be a registered route too. If the parent is a **new sibling screen** in the same feature, add a second `Page` object + Composable + registry entry — one feature module can host multiple screens (list + detail).

> The same file imports two `<FeatureUpper>Page` (the domain navigation object + the presentation Composable). Both imports are required — same as other features.

## 6. Execution checklist

1. Capture `{NewFeature}` and options (remote API? bottom tab? typed Args? nested/hierarchical route?). Derive `featureLower` / `FeatureUpper`.
2. Conflict check (§0.4). If hit, report and stop.
3. Create directories and files (§1, §2). Verify no `<featureLower>` / `<FeatureUpper>` placeholders are left unsubstituted.
4. Add 4 lines to `settings.gradle.kts` (§3).
5. Add 4 lines to `app/build.gradle.kts` deps (§4) + 2 lines to `main/presentation/build.gradle.kts` (§4.5).
6. Add an entry to `AppRouteRegistry.kt` (§5).
7. Build check — `gradle-build-check` skill. Confirm the 4 new modules compile and the Hilt graph isn't broken.
8. Ask the `architecture-guardian` subagent to review changed Kotlin/Gradle files. Fix HIGH/MEDIUM findings, then rerun `gradle-build-check`.
9. Report to user:
   - List of created files (markdown links)
   - Next steps:
     - Once a real API response is decided, replace `placeholder` DTO/VO via `api-dto-code-gen`
     - For the actual screen, use `compose-component` to add components
     - UseCase unit tests go under `<feature>/domain/src/test/...` (`run-android-tests`)

## 7. Don'ts

Scaffolding-step-only items:

- Don't modify existing modules (`intro`, `search`, `favorite`, `fullScreenMedia`, `main`, `common`) — only **add** to `settings.gradle.kts` / `app/build.gradle.kts` / `main/presentation/build.gradle.kts` / `AppRouteRegistry.kt`.
- Don't put `<FeatureUpper>Page` (Navigation definition) in `presentation` — it goes in `domain`. The `<FeatureUpper>Page.kt` under `presentation` is the Composable.
- (Nested route) Don't splice a path-param value into `PATH` — `PATH` stays the `{param}` **template**; the value goes in `args`. And don't let `{paramName}`, `KEY_*`, and the `Args.from` key drift apart (silent value loss). See `navigation-conventions` Golden rules 2–3 in `.agents/skills/navigation-conventions/SKILL.md`.
- (Nested route) Don't redefine a route's cold back stack anywhere but its `syntheticStack`; don't reach for a Nav3 built-in deep-link parser (there is none — it's `RoutePattern` + the registry).
- Don't proceed with empty `<NewFeature>` input.
- Don't fill `placeholder` with guessed domain fields — it's a placeholder for `api-dto-code-gen` to replace later.
- Don't model ViewModel state as a single primitive `MutableStateFlow<String>` — convention is `data class <Feature>UIState`.
- Don't mutate state with `uiState.update {}` in a stateful screen — every change goes through `dispatch(<Feature>ReducerEvent) → reduce()` (AGENTS.md rule 3). The plain-`ViewModel` form is for the intro-style minimal screen only.

Data-layer conventions (DTO `@Keep` vs `@Serializable`, no `Json` / `Retrofit` / `OkHttp` redefinition, ErrorType placement, DTO→VO mapping, etc.) — `api-dto-code-gen` §13 don't list is canonical; follow it when filling in the scaffolded placeholders.
