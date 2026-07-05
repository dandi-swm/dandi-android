---
name: "api-dto-code-gen-ko"
description: "Korean-language variant of `api-dto-code-gen`. Given an example JSON API response, scaffold Response DTO (all nullable, kotlinx.serialization) + VO (defaulted) + toVO() conversion for this project's data layer, and the Retrofit ApiService / DataSource / Repository / Hilt Module that match the existing intro / common-media patterns. Use ONLY when the user explicitly asks for Korean output or invokes the `api-dto-code-gen-ko` skill. Default for this task is the English `api-dto-code-gen` skill (lower token cost)."
---

# 예시 응답 JSON → DTO/VO/Repository 코드 생성

이 스킬은 **예시 API 응답 JSON**과 함께 호출됩니다. 그 JSON을 받는 데이터 레이어 코드를 이 프로젝트의 멀티 모듈 구조와 base 클래스(`BaseRemoteDataSource`, `BaseUseCase`, `HttpResponseException`, `HttpErrorType`)에 맞춰 생성합니다.

## 0. 입력 파악 — 코드 생성 전 반드시 확보해야 하는 것

같은 API라도 어느 모듈에 들어가느냐에 따라 패키지 경로·Hilt 모듈 위치·gradle 의존성이 달라집니다. 시작 전 아래 3가지가 명확히 결정돼 있어야 합니다.

### 0.1 필수 입력 — 없으면 진행 금지, 한 번 묻고 답이 와야 시작

1. **성공 응답 예시 JSON** (한 건). 파일 첨부 또는 본문 붙여넣기 모두 가능.
2. **엔드포인트 + HTTP 메서드** — 예: `GET v2/search/image`, `POST v1/feed/list`. 경로/쿼리 파라미터도 같이.
   - `Retrofit` baseUrl 은 `BuildConfig.API_BASE_URL` 에서 옵니다 (기본값 `https://dapi.kakao.com/`, `common/data/build.gradle.kts` 에서 설정하고 `common/data/.../di/NetworkModule.kt` 가 읽음). `@GET("v2/search/image")` 처럼 baseUrl 다음의 경로만 적습니다.
   - **인증은 중앙에서 주입됩니다** — `NetworkModule.kt` 의 OkHttp 인터셉터가 모든 요청에 `Authorization: KakaoAK ${BuildConfig.API_KEY}` 를 붙입니다. ApiService 메서드에 인증용 `@Headers` 를 달지 마세요. 특정 엔드포인트만 필요한 헤더가 있을 때만 선언합니다.
3. **타겟 배치** — 다음 중 하나:
   - `common` — 여러 feature가 공유하는 자원 (예: 기존 `mediaSearch`). 이 경우 `common/data/<sub>/`, `common/domain/<sub>/`, `common/entity/<sub>/` 에 sub-도메인 폴더로 들어감 (§2 참고).
   - `<feature>` — `intro` / `search` / `favorite` / `fullScreenMedia` / `main` 중 하나, 또는 새 feature 이름. 이 경우 해당 feature 의 4 모듈에 분배.

### 0.2 선택 입력 — 비어 있으면 추론 OK

- 도메인/sub 폴더 이름 (예: `feed`, `article`, `media`). 모호하면 엔드포인트 경로의 마지막 의미 단위에서 가져오되 한 번 확인.
- Request 파라미터 타입 / 헤더 (이미 있으면 그대로 반영).

### 0.3 시작 전 체크리스트

- [ ] 응답 JSON 이 손에 들어왔는가?
- [ ] 엔드포인트와 HTTP 메서드를 알고 있는가? (인증 헤더는 중앙에서 붙으므로 메서드별 인증 헤더는 불필요)
- [ ] 결과 코드가 들어갈 모듈이 `common` 인가, 아니면 특정 feature 인가?
- [ ] (feature 라면) 그 feature 모듈이 이미 존재하는가? 없으면 `make-new-feature-module-ko` 스킬을 먼저 안내.

위 4개 중 하나라도 비어 있으면 **사용자에게 부족한 항목만 묶어서 한 번 묻고** 답을 기다립니다. 추측해서 진행하지 않습니다 — 잘못 추측하면 패키지 경로와 의존성이 어긋나서 되돌리는 비용이 큽니다.

> 빠르게 묻는 예시: *"이 응답을 받는 엔드포인트가 `GET v2/search/image` 맞나요? 그리고 이 코드는 여러 feature 가 공유하는 `common` 쪽으로 들어가야 하는지, 아니면 특정 feature(예: `search`) 에 묶여야 하는지 알려주세요."*

## 1. 프로젝트 현재 상태 — 그대로 따라야 하는 것

루트 패키지: `com.jongchan.androidarchi`. 멀티 모듈 + feature/common 분리.

직렬화/네트워킹은 다음으로 **이미 확정**되어 있습니다 (`gradle/libs.versions.toml` + `common/data`):

- 직렬화: **kotlinx.serialization** (`libs.kotlinx.serialization.json`, plugin `libs.plugins.kotlinx.serialization`)
- 네트워크: Retrofit 3.x + `retrofit2-kotlinx-serialization-converter` + OkHttp + logging interceptor
- DI: Hilt

다른 라이브러리(Moshi/Gson/Ktor)로 교체하지 않습니다.

### 1.1 이미 갖춰진 공용 인프라 — 재정의 금지

`common/data/.../di/NetworkModule.kt` 에 다음이 Hilt `@Provides @Singleton` 으로 노출되어 있습니다:

- `Json` — `ignoreUnknownKeys = true`, `explicitNulls = false`, `coerceInputValues = true`
- `OkHttpClient` — `Authorization: KakaoAK ${BuildConfig.API_KEY}` 를 붙이는 인증 `Interceptor` + `HttpLoggingInterceptor(Level.BODY)` + 30s 타임아웃
- `Retrofit` — `baseUrl = BuildConfig.API_BASE_URL` (기본값 `https://dapi.kakao.com/`), `Json.asConverterFactory("application/json")`

따라서 인증은 **모든** 요청에 중앙에서 적용됩니다 — 생성하는 ApiService 메서드에 인증용 `@Headers` 를 따로 붙이면 안 됩니다.

각 `<feature>:data` 모듈은 `implementation(project(":common:data"))` 로 이 `Retrofit` 인스턴스를 주입받습니다. feature/data 모듈이 **직접 OkHttp/Retrofit/Json 빌더를 만들지 않습니다.** 별도 `JsonConfig.kt` 같은 파일도 만들지 마세요.

`BaseRemoteDataSource` (in `common/data`) 는 `checkResponse(response: Response<T>): T` 와 매핑 람다 오버로드를 제공하며, 비-2xx 응답을 `HttpResponseException` 으로 throw 합니다 — DataSource 는 이걸 그대로 호출하면 됩니다.

### 1.2 공용 에러 처리 계약

- `common/domain/error/HttpError.kt` — `interface HttpErrorType { val type; val errorMsg; val isHandledOnDomain }`, `class HttpResponseException(status, rawCode, errorRequestUrl, msg, cause)`, `enum HttpResponseStatus`
- `common/domain/error/ErrorParser.kt` — `inline fun <reified ErrorType> HttpResponseException.handlingErrorOnUseCase(): ErrorType?` (cause.message 로 enum 매칭), `fun HttpResponseException.isCommonErrorHandling(): Boolean` (401/404/5xx 판정)
- `common/domain/BaseUseCase` — `executeCommonErrorHanding(e)` (401/404/그 외 다이얼로그)

새 ErrorType 은 항상 `<feature>/domain/<Feature>ErrorType.kt` 에 둡니다 — **entity 모듈이 아니라 domain 모듈입니다.** (예: `search/domain/SearchErrorType.kt`, `intro/domain/IntroErrorType.kt`)

## 2. 패키지 구조 — 어디에 어떤 파일을 두는가

### 2.1 모듈 레이어 매핑

| 레이어 | Gradle 모듈 | 패키지 (feature) | 패키지 (common) |
|---|---|---|---|
| Composable / ViewModel / Intent / UIState | `:<feature>:presentation` | `com.jongchan.androidarchi.<feature>.presentation` | `com.jongchan.androidarchi.common.presentation` |
| Repository 인터페이스 / UseCase / Page (Navigation) / ErrorType | `:<feature>:domain` | `com.jongchan.androidarchi.<feature>.domain` | `com.jongchan.androidarchi.common.domain` |
| DTO / DataSource / RepositoryImpl / Hilt DataModule / ApiService | `:<feature>:data` | `com.jongchan.androidarchi.<feature>.data` | `com.jongchan.androidarchi.common.data` |
| VO / Constants / Pure 모델 | `:<feature>:entity` | `com.jongchan.androidarchi.<feature>.entity` | `com.jongchan.androidarchi.common.entity` |

`<feature>` 는 lowerCamelCase (`intro`, `search`, `favorite`, `fullScreenMedia` ...). 클래스 prefix 는 PascalCase (`Intro`, `Search`, `Favorite`, `FullScreenMedia`).

### 2.2 신규 API — feature 안에 들어가는 경우

```
<feature>/data/src/main/java/com/jongchan/androidarchi/<feature>/data/
    <Feature>ApiService.kt              # Retrofit 인터페이스
    <Feature>DataSource.kt              # BaseRemoteDataSource() 상속, checkResponse(...) 사용
    <Feature>RepositoryImpl.kt          # domain 의 Repository 구현, dataSource.toVO() 매핑
    <Feature>DataModule.kt              # @Module @InstallIn(SingletonComponent::class) — ApiService/DataSource/Repository @Provides @Singleton
    dto/
        <Feature>DTO.kt                 # @Serializable + toVO() (같은 파일 또는 클래스 안쪽 메서드)

<feature>/domain/src/main/java/com/jongchan/androidarchi/<feature>/domain/
    <Feature>Repository.kt              # 인터페이스, VO 반환
    <Feature>UseCase.kt                 # BaseUseCase 상속, @Inject constructor
    <Feature>ErrorType.kt               # HttpErrorType 구현 enum (도메인-특화 에러만)

<feature>/entity/src/main/java/com/jongchan/androidarchi/<feature>/entity/
    <Feature>VO.kt                      # non-nullable + companion object empty (+ optional isEmpty)
```

참고 — 실제 `intro` 배치:

- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/dto/IntroDTO.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/dto/IntroDTO.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroApiService.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroApiService.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataSource.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataSource.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroRepositoryImpl.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroRepositoryImpl.kt)
- [intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataModule.kt](intro/data/src/main/java/com/jongchan/androidarchi/intro/data/IntroDataModule.kt)
- [intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroRepository.kt](intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroRepository.kt)
- [intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroErrorType.kt](intro/domain/src/main/java/com/jongchan/androidarchi/intro/domain/IntroErrorType.kt)
- [intro/entity/src/main/java/com/jongchan/androidarchi/intro/entity/IntroVO.kt](intro/entity/src/main/java/com/jongchan/androidarchi/intro/entity/IntroVO.kt)

### 2.3 신규 API 가 공통 자원일 때 (예: mediaSearch 처럼 여러 feature 가 공유)

```
common/data/src/main/java/com/jongchan/androidarchi/common/data/<sub>/
    <Sub>ApiService.kt
    <Sub>DataSource.kt
    <Sub>RepositoryImpl.kt
    <Sub>DataModule.kt
    dto/<Sub>DTO.kt                     # @Serializable + 파일 하단 toVO() 확장 함수

common/domain/src/main/java/com/jongchan/androidarchi/common/domain/<sub>/
    <Sub>Repository.kt

common/entity/src/main/java/com/jongchan/androidarchi/common/entity/<sub>/
    <Sub>VO.kt
```

참고 — 실제 `mediaSearch` 배치 (data 의 sub-폴더는 `mediaSearch`; entity/domain 의 VO 는 `media` 아래에 있음):
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchApiService.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchApiService.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/dto/MediaSearchDTO.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/dto/MediaSearchDTO.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataSource.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataSource.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchRepositoryImpl.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchRepositoryImpl.kt)
- [common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataModule.kt](common/data/src/main/java/com/jongchan/androidarchi/common/data/mediaSearch/MediaSearchDataModule.kt)
- [common/domain/src/main/java/com/jongchan/androidarchi/common/domain/media/MediaSearchRepository.kt](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/media/MediaSearchRepository.kt)
- [common/entity/src/main/java/com/jongchan/androidarchi/common/entity/media/MediaVO.kt](common/entity/src/main/java/com/jongchan/androidarchi/common/entity/media/MediaVO.kt) — `MediaItemVO`, `MediaSearchResultVO`, enum `MediaType` 를 정의 (`MediaVO` 라는 이름의 클래스는 없음)

어떤 feature 에 속하는지 모호하면 한 번 묻고 결정합니다.

### 2.4 기존 도메인에 파일이 이미 있다면

새 파일을 만들지 말고 **기존 파일에 메서드/프로퍼티/Provides 만 추가**합니다. 예: `IntroApiService.kt` 에 새 엔드포인트 메서드 추가, `IntroDataModule.kt` 에 새 `@Provides` 추가, 기존 DTO 파일에 nested DTO data class 추가.

### 2.5 `<feature>:data` 에 `@Serializable` DTO 를 처음 도입하는 경우 — gradle 수정 필요

`<feature>/data/build.gradle.kts` 에 다음 두 가지가 있어야 `@Serializable` 클래스의 serializer 가 컴파일 시 생성됩니다:

1. `alias(libs.plugins.kotlinx.serialization)` — plugins 블록에 추가
2. `implementation(libs.kotlinx.serialization.json)` — dependencies 블록 (Network 섹션) 에 추가

참고 — `intro/data/build.gradle.kts` 에는 둘 다 있고, `search/data/build.gradle.kts` 에는 둘 다 없습니다 (search 는 자체 DTO 가 없고 `:common:data` 의 `MediaSearchRepository` 만 사용).

`@Serializable` DTO 에는 `@Keep` 을 붙이지 않습니다 — 직렬화 컴파일러 플러그인이 R8 keep 룰을 자동 생성합니다.

## 3. JSON → DTO 자료형 매핑

Response DTO 의 **모든 프로퍼티는 nullable + 기본값 `null`**. 서버가 필드를 누락하거나 새 필드가 추가되어도 디코딩이 깨지지 않게.

| JSON 값 | DTO 타입 | 비고 |
|---|---|---|
| `"text"` | `String? = null` | |
| `"2024-05-03T10:00:00Z"` (ISO 8601) | `String? = null` | VO 변환 시 `Instant`/`kotlinx.datetime` 등으로. minSdk 24 환경에서 `java.time.*` 는 desugaring 필요 |
| `"123.45"` (소수 문자열, 금액) | `String? = null` | VO 변환 시 `BigDecimal`. 정밀도가 필요한 금액은 `Double` 대신 권장 |
| 정수 `42` | `Int? = null` | `id`/`timestamp`/`count`/`epochMs` 거나 큰 값이면 `Long? = null` |
| 소수 `3.14` | `Double? = null` | 금액·정밀도 의미면 `String? = null` 후 VO 단계에서 `BigDecimal` |
| `true`/`false` | `Boolean? = null` | |
| `null` | 필드명에서 추론. 모르면 `String? = null` 로 두고 한 번 확인 |
| `[ {...} ]` (객체 배열) | `List<XDto>? = null` + nested DTO | |
| `[ "a","b" ]` (원시 배열) | `List<String>? = null` 등 | |
| `{ ... }` (객체) | nested DTO | |

## 4. DTO 작성 규칙 (kotlinx.serialization)

- 클래스 어노테이션: `@Serializable` (필수). `@Keep` 불필요.
- 필드명 매핑: 서버 키와 Kotlin 프로퍼티명이 다르면 `@SerialName("server_field")`. 같으면 생략.
- 모든 프로퍼티는 nullable + 기본값 `null` — kotlinx.serialization 은 default 가 있는 nullable 필드만 missing key 를 허용하므로 `String? = null` 형태로 항상 default 명시.
- `Json` 인스턴스는 이미 `NetworkModule` 에 있음 → 새로 만들지 마세요.
- `toVO()` 위치는 두 패턴 중 하나:
  - **클래스 메서드** (단일 DTO + 단일 VO): IntroDTO 패턴.
  - **파일 하단 확장 함수** (DTO/VO 가 nested 로 여러 개): MediaSearchDTO 패턴 — nested DTO 마다 `XDto.toVO()` 확장 함수를 같은 파일 하단에 모음.

DTO 예시 — 단일 (IntroDTO 스타일):

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

DTO 예시 — 중첩 (실제 MediaSearchDTO 스타일):

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

다형성(`type` 필드로 분기)이 필요하면 `@Serializable sealed interface` + `@JsonClassDiscriminator` 를 사용하고 의도를 한 번 확인.

## 5. VO 작성 규칙

- 위치: `<feature>/entity/.../<feature>/entity/` 또는 `common/entity/.../<sub>/`
- DTO 와 같은 모양이지만 **모든 타입을 non-nullable + 의미 있는 기본값** 으로 해소
- VO 는 **흔히**(항상은 아님) `companion object { val empty }` 를 두고, 가끔 `val isEmpty: Boolean get() = this == empty` 도 둠 (예: `IntroVO`, `MediaItemVO`). 둘 다 없는 VO 도 있음 (`FavoriteItemVO`, `MediaSearchResultVO`) — placeholder 가 필요한 호출부가 있을 때만 `empty` 를 추가.
- VO 가 `NavRoute.args` 로 **실제로 직렬화될 때만** `@Serializable` 을 붙임. 실제 예시는 `MediaItemVO`. 보편 규칙이 아니며 — `IntroVO`, `FavoriteItemVO` 는 `@Serializable` 이 아닌 일반 data class 임.

기본값 표:

| DTO 타입 | VO 타입 | 기본값 |
|---|---|---|
| `Int? = null` | `Int` | `0` |
| `Long? = null` | `Long` | `0L` |
| `Double? = null` | `Double` | `0.0` |
| `Boolean? = null` | `Boolean` | `false` |
| `String? = null` (일반 텍스트) | `String` | `""` (`.orEmpty()`) |
| `String? = null` (라벨/식별자 의미) | `String` | `UNKNOWN` (from `common/entity/EntityConstants.UNKNOWN = "UNKNOWN"`) |
| `String? = null` (ISO 8601) | `Instant` 등 | `Instant.DISTANT_PAST` (`runCatching { Instant.parse(it) }.getOrNull() ?: Instant.DISTANT_PAST`) |
| `String? = null` (금액) | `BigDecimal` | `runCatching { BigDecimal(it) }.getOrDefault(BigDecimal.ZERO)` |
| `Int? = null` (enum raw value) | `XEnum` | `XEnum.UNKNOWN` (e.g. `MediaType.fromRawValue(value)`) |
| `List<XDto>? = null` | `List<XVO>` | `emptyList()` (각 항목 `it.toVO()`) |
| nested `XDto?` | `XVO` | `XVO.empty` |

VO 예시:

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

## 6. ApiService — Retrofit 인터페이스

- 한 feature 당 한 개부터 시작. 도메인이 늘면 분리.
- 메서드 네이밍: `{httpMethod}{Resource}` 카멜케이스 (`getIntro`, `searchImages`, `postFeedList`).
- **인증용 `@Headers` 는 붙이지 않습니다** — 인증은 `NetworkModule` 의 OkHttp 인터셉터가 중앙에서 처리합니다 (§1.1). 특정 엔드포인트만 진짜 필요한 헤더가 있을 때만 `@Headers` 를 답니다.
- 반환 타입은 `Response<XDTO>` (`BaseRemoteDataSource.checkResponse` 가 `Response<T>` 를 받기 때문).
- baseUrl 다음의 path 만 적습니다. 앞에 `/` 를 붙이는지는 코드베이스에서 **일관적이지 않습니다**: `IntroApiService` 는 `@GET("/intro")` (앞 슬래시 있음), `MediaSearchApiService` 는 `@GET("v2/search/image")` (없음). `https://host/` baseUrl 기준 양쪽 다 정상 동작하며, 새 코드는 슬래시 없는 `mediaSearch` 스타일을 권장하되 기존 파일에 추가할 때는 그 파일의 기존 컨벤션을 따릅니다.

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

`@Path`, `@Query`, `@Body` 는 필요한 것만.

## 7. DataSource

`BaseRemoteDataSource()` 를 상속하고 `checkResponse(...)` 를 호출합니다 — 비-2xx 는 자동으로 `HttpResponseException` 으로 throw 됩니다.

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

`Response<Unit>` 이면 반환 타입을 `Unit` 으로 비웁니다.

## 8. Repository — 인터페이스(domain) + 구현(data)

**인터페이스는 항상 VO 를 반환합니다.** DTO 가 도메인 레이어로 새지 않게.

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

`@Module @InstallIn(SingletonComponent::class) object` 에 ApiService → DataSource → Repository 순으로 `@Provides @Singleton`. `Retrofit` 인스턴스는 `:common:data` NetworkModule 에서 주입받습니다.

provider 메서드 네이밍 컨벤션은 `provide{Feature}{Role}` 입니다 — `IntroDataModule` 이 깔끔한 예시 (`provideIntroApiService` / `provideIntroDataSource` / `provideIntroRepository`). 새 모듈은 이걸 따르세요. (참고: 기존 공용 `MediaSearchDataModule` 은 이 컨벤션을 **일관되게 따르지 않습니다** — 메서드명이 `provideKakaoSearchApiService` / `provideKakaoSearchDataSource` / `provideMediaRepository` 라서, 그 모듈 이름을 그대로 베끼지 마세요.)

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

## 10. UseCase 와 ErrorType — 스킬 범위 밖이지만 함께 갱신할 일이 많음

새 API 를 도입하면서 UseCase 도 같이 손볼 가능성이 큽니다. 같은 패턴을 유지하세요:

`BaseUseCase` 의 생성자는 헬퍼를 **4개** 받습니다 — `BaseUseCase(resourceHelper: ResourceHelper, messageHelper: MessageHelper, navigationHelper: NavigationHelper, ttiHelper: TTIHelper)`. 실제 UseCase(`GetIntroUseCase`, `SearchUseCase`)는 이 4개를 모두 **일반** `@Inject constructor` 파라미터(`override val` 아님)로 받아 `super` 로 그대로 넘깁니다:

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

`Flow` 를 다루는 UseCase 의 경우는 `.catch { handleRefreshError(it) }` 로 스트림 위에서 같은 분기를 합니다 — `HttpResponseException` 이 아니면 그대로 throw.

Favorite 등록/해제가 필요한 화면이면 직접 구현하지 말고, ViewModel 이 `:common:domain` 의 공용 favorite UseCase 들을 주입받습니다 (`search` / `favorite` / `fullScreenMedia` 패턴). [common/domain/favorite/](common/domain/src/main/java/com/jongchan/androidarchi/common/domain/favorite/) 가 `GetFavoriteItemsUseCase` / `RegisterFavoriteItemUseCase` / `RemoveFavoriteItemUseCase` 를 제공:

```kotlin
@HiltViewModel
class <Feature>ViewModel @Inject constructor(
    private val <feature>UseCase: <Feature>UseCase,
    private val getFavoriteItemsUseCase: GetFavoriteItemsUseCase,
    private val registerFavoriteItemUseCase: RegisterFavoriteItemUseCase,
    private val removeFavoriteItemUseCase: RemoveFavoriteItemUseCase,
    private val messageHelper: MessageHelper,
    private val navigationHelper: NavigationHelper,
) : MviViewModel<...>(...) { ... }   // 전체 패턴은 SearchViewModel 참고
```

`<Feature>ErrorType` 은 `<feature>/domain/` 에 두고 `HttpErrorType` 을 구현하는 enum:

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
        errorMsg = "현재 앱이 최소 요구 버전을 만족하지 않습니다.\nPlay Store에서 최신버전을 업데이트 해주세요.",
        isHandledOnDomain = true,
    ),
}
```

`type` 키는 `api.<feature>.<reason>` 컨벤션 — `ErrorParser.handlingErrorOnUseCase` 가 `cause.message == this.type` 으로 매칭합니다.

## 11. 작업 후 확인

1. `./gradlew :app:compileDebugKotlin` 으로 컴파일 통과 확인 (`gradle-build-check` 스킬). 이 단계에서 직렬화 컴파일러 플러그인이 `@Serializable` 클래스마다 serializer 를 생성합니다.
2. `<feature>:data` 가 `@Serializable` 을 처음 쓰는 경우 §2.5 의 plugin/dep 추가가 들어갔는지 재확인.
3. UseCase 까지 손댔다면 `<feature>/domain/src/test/...` 의 단위 테스트 추가/갱신 (`run-android-tests` 스킬). 기존 `IntroUseCaseTest`, `SearchUseCaseTest` 패턴 — `mockk(relaxed = true)` + `runTest` + `HttpResponseException` 케이스별 분기.
4. 새로 import 한 `java.time.Instant`, `java.math.BigDecimal` 이 minSdk 24 에서 동작하는지 확인 — `java.time.*` 는 `coreLibraryDesugaring` 또는 `kotlinx-datetime` 도입이 필요. 도입 전이면 사용자에게 알립니다.
5. 라운드트립 스모크 테스트 — 가능하면 입력 받은 예시 JSON 으로 `Json.decodeFromString<XxxDTO>(sample).toVO()` 단위 테스트를 한 줄 작성하면 누락된 필드 매핑을 즉시 잡을 수 있습니다.
6. 화면(ViewModel/Composable) 까지 가는 작업이면 `compose-component` 스킬로 이어집니다.

## 12. 추론하지 말고 한 번 묻기

다음은 자동 추론 금지 — 한 번 확인 받습니다:

- §0 의 필수 입력 3종(JSON / 엔드포인트+메서드+헤더 / 타겟 모듈) 중 하나라도 비어 있을 때 — **반드시 묻습니다**
- 정수 필드의 `Int` vs `Long` 모호 (`id` 류는 보통 `Long`)
- 소수 필드가 금액·정밀도 의미인지
- timestamp 포맷 (ISO 8601 / epoch seconds / epoch ms / 임의)
- `null` 또는 빈 배열만 들어온 필드의 실제 타입
- 도메인/sub 폴더 이름 (`feed` vs `feedList` vs `article`)
- VO 가 `NavRoute.args` 로 직렬화될 가능성 (`@Serializable` 부착 여부)
- 다형성(`type` discriminator) 필요 여부

## 13. 하지 말 것

- `Json` / `Retrofit` / `OkHttpClient` 인스턴스를 feature/data 모듈에 새로 만들지 않는다 — `:common:data` 에서 주입.
- DTO 에 `@Keep` 을 붙이지 않는다 — `@Serializable` 이면 자동 keep.
- ErrorType 을 `entity` 에 두지 않는다 — `domain` 에.
- DTO 를 도메인/UseCase 에 노출하지 않는다 — Repository 인터페이스가 VO 를 반환.
- 기존 `IntroDTO.kt` 처럼 `android.provider.MediaStore.UNKNOWN_STRING` 임포트가 보이면 새 코드는 `com.jongchan.androidarchi.common.entity.UNKNOWN` 을 사용. 기존 파일 마이그레이션은 사용자에게 한 번 확인.
