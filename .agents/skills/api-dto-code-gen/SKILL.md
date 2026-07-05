---
name: "api-dto-code-gen"
description: "Given an example JSON API response, scaffold Response DTO (all nullable, kotlinx.serialization) + VO (defaulted) + toVO() conversion for this project's data layer, and the Retrofit ApiService / DataSource / Repository / Hilt Module that match the existing intro / common-media patterns. Trigger when the user pastes or attaches a sample JSON response and asks to \"make DTOs\", \"scaffold this API\", \"generate from this response\", or to wire up a new endpoint."
---

# Sample JSON → DTO/VO/Repository code generation

This skill is invoked together with **a sample API response JSON**. It generates the data-layer code that consumes that JSON, following this project's multi-module structure and base classes (`BaseRemoteDataSource`, `BaseUseCase`, `HttpResponseException`, `HttpErrorType`).

## 0. Inputs — must be confirmed before generating

The same API generates different code depending on which module it goes into (package paths, Hilt module placement, gradle deps all change). Confirm the following before starting.

### 0.1 Required — if missing, ask once and wait

1. **Sample success response JSON** (one record). File attachment or pasted body — both fine.
2. **Endpoint + HTTP method** — e.g. `GET v2/search/image`, `POST v1/feed/list`. Path/query params too.
   - The Retrofit baseUrl comes from `BuildConfig.API_BASE_URL` (default `https://dapi.kakao.com/`, set in `common/data/build.gradle.kts` and read by `common/data/.../di/NetworkModule.kt`). Write only the path after baseUrl, e.g. `@GET("v2/search/image")`.
   - **Auth is injected centrally** by an OkHttp interceptor in `NetworkModule.kt` (`Authorization: KakaoAK ${BuildConfig.API_KEY}`). Do **not** add `@Headers` for auth on ApiService methods — only declare endpoint-specific headers if a particular call genuinely needs one.
3. **Target placement** — one of:
   - `common` — shared resource used by multiple features (like the existing `mediaSearch`). Goes under `common/data/<sub>/`, `common/domain/<sub>/`, `common/entity/<sub>/` as a sub-domain folder (§2).
   - `<feature>` — one of `intro` / `search` / `favorite` / `fullScreenMedia` / `main`, or a new feature name. Distributed across that feature's 4 modules.

### 0.2 Optional — infer if missing

- Domain/sub folder name (e.g. `feed`, `article`, `media`). If ambiguous, take it from the last meaningful segment of the endpoint path, but confirm once.
- Request param types / headers (reuse if already present).

### 0.3 Pre-flight checklist

- [ ] JSON in hand?
- [ ] Endpoint, HTTP method known? (auth header is added centrally — no per-method auth header needed)
- [ ] Target module decided (`common` vs `<feature>`)?
- [ ] (If feature) does the module already exist? If not, point the user to `make-new-feature-module` first.

If any answer is missing, **ask the user once with the missing items grouped together** and wait. Do not guess — getting placement wrong cascades into wrong package paths and gradle deps, expensive to undo.

> Quick prompt example: *"Is the endpoint `GET v2/search/image`? And should this code live in shared `common`, or under a specific feature like `search`?"*

## 1. Project state — fixed conventions

Root package: `com.jongchan.androidarchi`. Multi-module + feature/common split.

Serialization and networking are **already decided** (`gradle/libs.versions.toml` + `common/data`):

- Serialization: **kotlinx.serialization** (`libs.kotlinx.serialization.json`, plugin `libs.plugins.kotlinx.serialization`)
- Network: Retrofit 3.x + `retrofit2-kotlinx-serialization-converter` + OkHttp + logging interceptor
- DI: Hilt

Do not swap libraries (Moshi/Gson/Ktor).

### 1.1 Already-provided shared infrastructure — do not redefine

`common/data/.../di/NetworkModule.kt` exposes the following as Hilt `@Provides @Singleton`:

- `Json` — `ignoreUnknownKeys = true`, `explicitNulls = false`, `coerceInputValues = true`
- `OkHttpClient` — an auth `Interceptor` adding `Authorization: KakaoAK ${BuildConfig.API_KEY}` + `HttpLoggingInterceptor(Level.BODY)` + 30s timeouts
- `Retrofit` — `baseUrl = BuildConfig.API_BASE_URL` (default `https://dapi.kakao.com/`), `Json.asConverterFactory("application/json")`

Auth is therefore applied to **every** request centrally — generated ApiService methods must not carry their own auth `@Headers`.

Each `<feature>:data` module gets the `Retrofit` instance via `implementation(project(":common:data"))`. **A feature/data module never builds its own OkHttp/Retrofit/Json.** Do not create a separate `JsonConfig.kt`.

`BaseRemoteDataSource` (in `common/data`) provides `checkResponse(response: Response<T>): T` and a mapping-lambda overload, throwing `HttpResponseException` on non-2xx — DataSources just call it.

### 1.2 Common error-handling contract

- `common/domain/error/HttpError.kt` — `interface HttpErrorType { val type; val errorMsg; val isHandledOnDomain }`, `class HttpResponseException(status, rawCode, errorRequestUrl, msg, cause)`, `enum HttpResponseStatus`
- `common/domain/error/ErrorParser.kt` — `inline fun <reified ErrorType> HttpResponseException.handlingErrorOnUseCase(): ErrorType?` (matches enum by `cause.message`), `fun HttpResponseException.isCommonErrorHandling(): Boolean` (true for 401/404/5xx)
- `common/domain/BaseUseCase` — `executeCommonErrorHanding(e)` (401/404/other dialogs)

New ErrorTypes always live in `<feature>/domain/<Feature>ErrorType.kt` — **the domain module, not entity.** (e.g. `search/domain/SearchErrorType.kt`, `intro/domain/IntroErrorType.kt`)

## 2. Package structure — where each file goes

### 2.1 Layer-to-module mapping

| Layer | Gradle module | Package (feature) | Package (common) |
|---|---|---|---|
| Composable / ViewModel / Intent / UIState | `:<feature>:presentation` | `com.jongchan.androidarchi.<feature>.presentation` | `com.jongchan.androidarchi.common.presentation` |
| Repository interface / UseCase / Page (Navigation) / ErrorType | `:<feature>:domain` | `com.jongchan.androidarchi.<feature>.domain` | `com.jongchan.androidarchi.common.domain` |
| DTO / DataSource / RepositoryImpl / Hilt DataModule / ApiService | `:<feature>:data` | `com.jongchan.androidarchi.<feature>.data` | `com.jongchan.androidarchi.common.data` |
| VO / Constants / pure models | `:<feature>:entity` | `com.jongchan.androidarchi.<feature>.entity` | `com.jongchan.androidarchi.common.entity` |

`<feature>` is lowerCamelCase (`intro`, `search`, `favorite`, `fullScreenMedia`...). Class prefix is PascalCase (`Intro`, `Search`, `Favorite`, `FullScreenMedia`).

### 2.2 New API inside a feature

```
<feature>/data/src/main/java/com/jongchan/androidarchi/<feature>/data/
    <Feature>ApiService.kt              # Retrofit interface
    <Feature>DataSource.kt              # extends BaseRemoteDataSource(), uses checkResponse(...)
    <Feature>RepositoryImpl.kt          # implements domain Repository, dataSource.toVO() mapping
    <Feature>DataModule.kt              # @Module @InstallIn(SingletonComponent::class) — ApiService/DataSource/Repository @Provides @Singleton
    dto/
        <Feature>DTO.kt                 # @Serializable + toVO() (same file or class method)

<feature>/domain/src/main/java/com/jongchan/androidarchi/<feature>/domain/
    <Feature>Repository.kt              # interface returning VO
    <Feature>UseCase.kt                 # extends BaseUseCase, @Inject constructor
    <Feature>ErrorType.kt               # enum implementing HttpErrorType (domain-specific errors only)

<feature>/entity/src/main/java/com/jongchan/androidarchi/<feature>/entity/
    <Feature>VO.kt                      # non-nullable + companion object empty (+ optional isEmpty)
```

Reference — actual `intro` layout:

- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/dto/IntroDTO.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/dto/IntroDTO.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroApiService.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroApiService.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataSource.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataSource.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroRepositoryImpl.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroRepositoryImpl.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataModule.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataModule.kt)
- [intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroRepository.kt](intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroRepository.kt)
- [intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroErrorType.kt](intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroErrorType.kt)
- [intro/entity/src/main/java/com/jongchan/androidarchi/intro/entity/IntroVO.kt](intro/entity/src/main/java/com/jongchan/androidarchi/intro/entity/IntroVO.kt)

### 2.3 New API as a shared resource (e.g. mediaSearch — used by multiple features)

```
common/data/src/main/java/com/jongchan/androidarchi/common/data/<sub>/
    <Sub>ApiService.kt
    <Sub>DataSource.kt
    <Sub>RepositoryImpl.kt
    <Sub>DataModule.kt
    dto/<Sub>DTO.kt                     # @Serializable + bottom-of-file toVO() extension functions

common/domain/src/main/java/com/jongchan/androidarchi/common/domain/<sub>/
    <Sub>Repository.kt

common/entity/src/main/java/com/jongchan/androidarchi/common/entity/<sub>/
    <Sub>VO.kt
```

Reference — actual `mediaSearch` layout (the sub-folder is `mediaSearch`; entity/domain VOs live under `media`):
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchApiService.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchApiService.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/dto/MediaSearchDTO.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/dto/MediaSearchDTO.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataSource.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataSource.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchRepositoryImpl.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchRepositoryImpl.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataModule.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataModule.kt)
- [common/domain/src/main/java/com/jongchan/androidarchi/common/domain/media/MediaSearchRepository.kt](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/media/MediaSearchRepository.kt)
- [common/entity/src/main/java/com/jongchan/androidarchi/common/entity/media/MediaVO.kt](common/entity/src/main/java/com/jongchan/androidarchi/common/entity/media/MediaVO.kt) — defines `MediaItemVO`, `MediaSearchResultVO`, enum `MediaType` (there is no class literally named `MediaVO`)

If feature ownership is ambiguous, ask once.

### 2.4 If files in this domain already exist

Don't create new files — **add methods/properties/Provides to existing ones.** E.g. add a new endpoint method to `IntroApiService.kt`, a new `@Provides` to `IntroDataModule.kt`, a nested DTO data class to the existing DTO file.

### 2.5 First time `<feature>:data` introduces a `@Serializable` DTO — gradle update needed

Two items must be in `<feature>/data/build.gradle.kts` for the serialization compiler plugin to generate serializers for `@Serializable` classes:

1. `alias(libs.plugins.kotlinx.serialization)` — in the `plugins` block
2. `implementation(libs.kotlinx.serialization.json)` — in the `dependencies` block (Network section)

Reference — `intro/data/build.gradle.kts` has both, `search/data/build.gradle.kts` has neither (search has no own DTO, only consumes `:common:data`'s `MediaSearchRepository`).

Do not annotate `@Serializable` DTOs with `@Keep` — the serialization compiler plugin auto-generates R8 keep rules.

## 3. JSON → DTO type mapping

**All DTO properties are nullable with default `null`.** Decoding survives missing fields and new fields.

| JSON value | DTO type | Note |
|---|---|---|
| `"text"` | `String? = null` | |
| `"2024-05-03T10:00:00Z"` (ISO 8601) | `String? = null` | Convert to `Instant`/`kotlinx.datetime` in VO. minSdk 24 needs desugaring for `java.time.*` |
| `"123.45"` (decimal string, money) | `String? = null` | Convert to `BigDecimal` in VO. Prefer over `Double` for precision-sensitive money |
| `42` (integer) | `Int? = null` | Use `Long? = null` for `id`/`timestamp`/`count`/`epochMs` or large values |
| `3.14` (decimal) | `Double? = null` | If money/precision-sensitive, prefer `String? = null` → `BigDecimal` in VO |
| `true`/`false` | `Boolean? = null` | |
| `null` | Infer from field name. If unknown, `String? = null` and confirm once |
| `[ {...} ]` (object array) | `List<XDto>? = null` + nested DTO | |
| `[ "a","b" ]` (primitive array) | `List<String>? = null` etc. | |
| `{ ... }` (object) | nested DTO | |

## 4. DTO authoring (kotlinx.serialization)

- Class annotation: `@Serializable` (required). `@Keep` is unnecessary.
- Field name mapping: if server key differs from Kotlin property name, `@SerialName("server_field")`. Same name → omit.
- Every property nullable with default `null` — kotlinx.serialization only allows missing keys for nullable fields with a default, so always write `String? = null` form.
- The `Json` instance lives in `NetworkModule` — do not create another.
- `toVO()` placement — one of:
  - **Class method** (single DTO + single VO): IntroDTO style.
  - **Bottom-of-file extension functions** (multiple nested DTOs/VOs): MediaSearchDTO style — one `XDto.toVO()` per nested DTO, all collected at the bottom of the file.

DTO example — single (IntroDTO style):

```kotlin
package com.jongchan.androidarchi.intro.data.dto

import com.jongchan.androidarchi.common.entity.UNKNOWN
import com.jongchan.androidarchi.intro.entity.IntroVO
import kotlinx.serialization.Serializable

@Serializable
data class IntroDTO(
    val devTestMsg: String? = null,
    val minAppVersion: String? = null,
    val recommendAppVersion: String? = null,
) {
    fun toVO(): IntroVO = IntroVO(
        devTestMsg = devTestMsg ?: UNKNOWN,
        minAppVersion = minAppVersion ?: UNKNOWN,
        recommendAppVersion = recommendAppVersion ?: UNKNOWN,
    )
}
```

DTO example — nested (real MediaSearchDTO style):

```kotlin
package com.jongchan.androidarchi.common.data.mediaSearch.dto

import com.jongchan.androidarchi.common.entity.UNKNOWN
import com.jongchan.androidarchi.common.entity.media.MediaItemVO
import com.jongchan.androidarchi.common.entity.media.MediaType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchResponse(
    val documents: List<ImageDocumentDTO>? = null,
    val meta: SearchMetaDTO? = null,
)

@Serializable
data class ImageDocumentDTO(
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("display_sitename") val displaySitename: String? = null,
    @SerialName("doc_url") val docUrl: String? = null,
    val datetime: String? = null,
)

@Serializable
data class SearchMetaDTO(
    @SerialName("is_end") val isEnd: Boolean? = null,
)

fun ImageDocumentDTO.toVO(): MediaItemVO = MediaItemVO(
    type = MediaType.IMAGE,
    title = displaySitename ?: UNKNOWN,
    urlKey = imageUrl.orEmpty(),
    thumbnailImageUrl = thumbnailUrl.orEmpty(),
    pageLinkUrl = docUrl.orEmpty(),
    dateTime = datetime.orEmpty(),
)
```

For polymorphism (server discriminates via a `type` field), use `@Serializable sealed interface` + `@JsonClassDiscriminator` and confirm intent once.

## 5. VO authoring

- Location: `<feature>/entity/.../<feature>/entity/` or `common/entity/.../<sub>/`
- Same shape as DTO, but **all types non-nullable with meaningful defaults**
- A VO **commonly** (not always) carries `companion object { val empty }`, sometimes with `val isEmpty: Boolean get() = this == empty` (e.g. `IntroVO`, `MediaItemVO`). Some VOs have neither (`FavoriteItemVO`, `MediaSearchResultVO`) — add `empty` only when a caller needs a placeholder.
- Mark with `@Serializable` **only when** the VO is actually serialized via `NavRoute.args`. `MediaItemVO` is the real example. It is not universal — `IntroVO` and `FavoriteItemVO` are plain (non-`@Serializable`) data classes.

Default-value table:

| DTO type | VO type | Default |
|---|---|---|
| `Int? = null` | `Int` | `0` |
| `Long? = null` | `Long` | `0L` |
| `Double? = null` | `Double` | `0.0` |
| `Boolean? = null` | `Boolean` | `false` |
| `String? = null` (general text) | `String` | `""` (`.orEmpty()`) |
| `String? = null` (label/identifier) | `String` | `UNKNOWN` (from `common/entity/EntityConstants.UNKNOWN = "UNKNOWN"`) |
| `String? = null` (ISO 8601) | `Instant` etc. | `Instant.DISTANT_PAST` (`runCatching { Instant.parse(it) }.getOrNull() ?: Instant.DISTANT_PAST`) |
| `String? = null` (money) | `BigDecimal` | `runCatching { BigDecimal(it) }.getOrDefault(BigDecimal.ZERO)` |
| `Int? = null` (enum raw value) | `XEnum` | `XEnum.UNKNOWN` (e.g. `MediaType.fromRawValue(value)`) |
| `List<XDto>? = null` | `List<XVO>` | `emptyList()` (each item `it.toVO()`) |
| nested `XDto?` | `XVO` | `XVO.empty` |

VO example:

```kotlin
package com.jongchan.androidarchi.intro.entity

import kotlinx.serialization.Serializable

@Serializable
data class IntroVO(
    val devTestMsg: String,
    val minAppVersion: String,
    val recommendAppVersion: String,
) {
    companion object {
        val empty: IntroVO = IntroVO("", "", "")
    }
}
```

## 6. ApiService — Retrofit interface

- One per feature to start. Split when the domain grows.
- Method naming: `{httpMethod}{Resource}` camelCase (`getIntro`, `searchImages`, `postFeedList`).
- **No auth `@Headers`** — auth is added centrally by the OkHttp interceptor in `NetworkModule` (§1.1). Only add `@Headers` for a header a specific endpoint genuinely requires.
- Return type: `Response<XDTO>` (`BaseRemoteDataSource.checkResponse` accepts `Response<T>`).
- Path is what comes after baseUrl. The codebase is **inconsistent** about the leading `/`: `IntroApiService` uses `@GET("/intro")` (leading slash), `MediaSearchApiService` uses `@GET("v2/search/image")` (none). Either resolves correctly against the `https://host/` baseUrl; prefer no leading slash for new code (the `mediaSearch` style) but match a file's existing convention when adding to it.

```kotlin
package com.jongchan.androidarchi.intro.data

import com.jongchan.androidarchi.intro.data.dto.IntroDTO
import retrofit2.Response
import retrofit2.http.GET

interface IntroApiService {
    @GET("/intro")
    suspend fun getIntro(): Response<IntroDTO>
}
```

`@Path`, `@Query`, `@Body` only as needed.

## 7. DataSource

Extend `BaseRemoteDataSource()` and call `checkResponse(...)` — non-2xx is auto-thrown as `HttpResponseException`.

```kotlin
package com.jongchan.androidarchi.intro.data

import com.jongchan.androidarchi.common.data.BaseRemoteDataSource
import com.jongchan.androidarchi.intro.data.dto.IntroDTO

class IntroDataSource(private val introApiService: IntroApiService) : BaseRemoteDataSource() {
    suspend fun getIntro(): IntroDTO {
        return checkResponse(introApiService.getIntro())
    }
}
```

If the response is `Response<Unit>`, return `Unit`.

## 8. Repository — interface (domain) + implementation (data)

**The interface always returns VOs.** DTOs do not leak into the domain layer.

```kotlin
// intro/domain/IntroRepository.kt
package com.jongchan.androidarchi.intro.domain

import com.jongchan.androidarchi.intro.entity.IntroVO

interface IntroRepository {
    suspend fun getIntro(): IntroVO
}
```

```kotlin
// intro/data/IntroRepositoryImpl.kt
package com.jongchan.androidarchi.intro.data

import com.jongchan.androidarchi.intro.domain.IntroRepository

class IntroRepositoryImpl(val dataSource: IntroDataSource) : IntroRepository {
    override suspend fun getIntro() = dataSource.getIntro().toVO()
}
```

## 9. Hilt DataModule

`@Module @InstallIn(SingletonComponent::class) object` with `@Provides @Singleton` for ApiService → DataSource → Repository. The `Retrofit` instance comes from `:common:data`'s NetworkModule.

Provider-method naming convention is `provide{Feature}{Role}` — `IntroDataModule` is the clean example (`provideIntroApiService` / `provideIntroDataSource` / `provideIntroRepository`). Follow it for new modules. (Note: the existing shared `MediaSearchDataModule` does **not** follow it consistently — its methods are `provideKakaoSearchApiService` / `provideKakaoSearchDataSource` / `provideMediaRepository` — so don't copy that module's names verbatim.)

```kotlin
package com.jongchan.androidarchi.intro.data

import com.jongchan.androidarchi.intro.domain.IntroRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntroDataModule {

    @Provides
    @Singleton
    fun provideIntroRepository(dataSource: IntroDataSource): IntroRepository =
        IntroRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideIntroDataSource(apiService: IntroApiService): IntroDataSource =
        IntroDataSource(apiService)

    @Provides
    @Singleton
    fun provideIntroApiService(retrofit: Retrofit): IntroApiService =
        retrofit.create(IntroApiService::class.java)
}
```

## 10. UseCase + ErrorType — out of strict scope but often updated together

Adding a new API usually means touching the UseCase too. Keep the same pattern:

`BaseUseCase`'s constructor takes **four** helpers — `BaseUseCase(resourceHelper: ResourceHelper, messageHelper: MessageHelper, navigationHelper: NavigationHelper, ttiHelper: TTIHelper)`. Real UseCases (`GetIntroUseCase`, `SearchUseCase`) inject all four as **plain** `@Inject constructor` params (not `override val`) and forward them to `super`:

```kotlin
// <feature>/domain/<Feature>UseCase.kt
class IntroUseCase @Inject constructor(
    private val repository: IntroRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend fun getIntro(): Result<IntroVO> = try {
        Result.success(repository.getIntro())
    } catch (e: HttpResponseException) {
        handleIntroError(e)
        Result.failure(e)
    }

    private fun handleIntroError(e: HttpResponseException) {
        if (e.isCommonErrorHandling()) {
            executeCommonErrorHanding(e)
            return
        }
        val errorType = e.handlingErrorOnUseCase<IntroErrorType>() ?: return
        when (errorType) {
            IntroErrorType.REQUIRED_FORCE_UPDATE -> {
                messageHelper.showOneButtonDialog(
                    cantIgnore = true,
                    descText = "...",
                    buttonText = "...",
                    onClickButton = { navigationHelper.navigateToBack() },
                )
            }
        }
    }
}
```

For Flow-based UseCases, apply the same branch on the stream with `.catch { handleRefreshError(it) }` — re-throw if not `HttpResponseException`.

If the screen needs favorite create/delete, do NOT reimplement it — the ViewModel injects the shared favorite UseCases from `:common:domain` (the `search`/`favorite`/`fullScreenMedia` pattern). [common/domain/favorite/](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/favorite/) provides `GetFavoriteItemsUseCase` / `RegisterFavoriteItemUseCase` / `RemoveFavoriteItemUseCase`:

```kotlin
@HiltViewModel
class <Feature>ViewModel @Inject constructor(
    private val <feature>UseCase: <Feature>UseCase,
    private val getFavoriteItemsUseCase: GetFavoriteItemsUseCase,
    private val registerFavoriteItemUseCase: RegisterFavoriteItemUseCase,
    private val removeFavoriteItemUseCase: RemoveFavoriteItemUseCase,
    private val messageHelper: MessageHelper,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<...>(...) { ... }   // see SearchViewModel for the full pattern
```

`<Feature>ErrorType` lives in `<feature>/domain/` as an enum implementing `HttpErrorType`:

```kotlin
package com.jongchan.androidarchi.intro.domain

import com.jongchan.androidarchi.common.domain.error.HttpErrorType

enum class IntroErrorType(
    override val type: String,
    override val errorMsg: String,
    override val isHandledOnDomain: Boolean = true,
) : HttpErrorType {
    REQUIRED_FORCE_UPDATE(
        type = "api.intro.requiredForceUpdate",
        errorMsg = "Force update required.\nUpdate from Play Store.",
        isHandledOnDomain = true,
    ),
}
```

`type` key convention: `api.<feature>.<reason>` — `ErrorParser.handlingErrorOnUseCase` matches on `cause.message == this.type`.

## 11. After the work

1. `./gradlew :app:compileDebugKotlin` to confirm compilation (`gradle-build-check` skill). The serialization compiler plugin generates serializers for each `@Serializable` class at this step.
2. If `<feature>:data` is using `@Serializable` for the first time, double-check the plugin/dep additions from §2.5.
3. If the UseCase was touched, add/update unit tests in `<feature>/domain/src/test/...` (`run-android-tests` skill). Existing patterns: `IntroUseCaseTest`, `SearchUseCaseTest` — `mockk(relaxed = true)` + `runTest` + per-`HttpResponseException`-case branches.
4. Newly-imported `java.time.Instant`, `java.math.BigDecimal` etc. — check minSdk 24 compatibility. `java.time.*` requires `coreLibraryDesugaring` or migration to `kotlinx-datetime`. Notify the user if not yet set up.
5. Round-trip smoke test — when possible, write a one-line unit test `Json.decodeFromString<XxxDTO>(sample).toVO()` against the sample JSON to catch missing field mappings immediately.
6. If the work extends to ViewModel/Composable, continue with `compose-component`.

## 12. Ask once before guessing

Don't auto-infer these — ask once:

- Any of the §0 required inputs (JSON / endpoint+method+headers / target module) missing — **always ask**
- `Int` vs `Long` ambiguity (`id` family is usually `Long`)
- Whether a decimal field is money/precision-sensitive
- Timestamp format (ISO 8601 / epoch seconds / epoch ms / arbitrary)
- Actual type of fields that arrived as `null` or empty array
- Domain/sub folder name (`feed` vs `feedList` vs `article`)
- Whether the VO will be serialized via `NavRoute.args` (whether to add `@Serializable`)
- Polymorphism (`type` discriminator) needed?

## 13. Don'ts

- Don't create `Json` / `Retrofit` / `OkHttpClient` instances inside a feature/data module — inject from `:common:data`.
- Don't annotate DTOs with `@Keep` — `@Serializable` auto-keeps.
- Don't put ErrorType under `entity` — it goes in `domain`.
- Don't expose DTO types to the domain/UseCase — the Repository interface returns VOs.
- If you see `android.provider.MediaStore.UNKNOWN_STRING` in legacy files (like older `IntroDTO.kt`), new code should use `com.jongchan.androidarchi.common.entity.UNKNOWN` instead. Ask before migrating an existing file.
