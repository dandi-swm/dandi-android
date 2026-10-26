# 데이터 레이어

## 왜

- **DTO 전 필드 nullable + VO 기본값** 규칙은 서버 스키마 변동/필드 누락에 대한 방어선을 `toVO()` 한 곳에 모은다 — 파싱 크래시가 구조적으로 불가능해진다.
- Repository 인터페이스를 domain에 두면 presentation/domain은 네트워크 구현(Retrofit)을 전혀 모른다.

## 구성 요소와 골든 예제 (history = 표준형)

```
[domain]  {Feature}Repository (인터페이스)        history/domain/HistoryRepository.kt
[data]    {Feature}ApiService (Retrofit)          history/data/HistoryApiService.kt
[data]    {Feature}DataSource : BaseRemoteDataSource   history/data/HistoryDataSource.kt
[data]    dto/{Xxx}DTO (@Serializable, all-nullable, toVO())   history/data/dto/MealDTO.kt 등
[data]    {Feature}RepositoryImpl                 history/data/HistoryRepositoryImpl.kt
[data]    {Feature}DataModule (Hilt 바인딩)        history/data/HistoryDataModule.kt
[entity]  {Xxx}VO (비-nullable, 기본값, companion empty)   history/entity/*VO.kt
```

API 계약의 기준은 dev 서버 Swagger(`https://ddanddi.nyummy.co.kr/swagger-ui/index.html`)다. 새 API는 Swagger의 응답 예시로 `api-dto-code-gen` 스킬을 돌려 만든다.

핵심 패턴:

```kotlin
// DTO — 전 필드 nullable + 기본값 null, 변환은 toVO()에서만
@Serializable
data class MealStatusDTO(val id: Long? = null, val status: String? = null) {
    fun toVO(): MealAnalysisStatus = status.toMealAnalysisStatus()
}

// DataSource — checkResponse가 실패를 HttpResponseException으로 통일
class HistoryDataSource(private val apiService: HistoryApiService) : BaseRemoteDataSource() {
    suspend fun getMeal(mealId: Long): MealDTO = checkResponse(apiService.getMeal(mealId))
}

// Hilt DataModule — @InstallIn(SingletonComponent::class), 3종 @Provides
@Module @InstallIn(SingletonComponent::class)
object HistoryDataModule {
    @Provides @Singleton fun provideHistoryApiService(retrofit: Retrofit): HistoryApiService = retrofit.create(HistoryApiService::class.java)
    @Provides @Singleton fun provideHistoryDataSource(apiService: HistoryApiService): HistoryDataSource = HistoryDataSource(apiService)
    @Provides @Singleton fun provideHistoryRepository(dataSource: HistoryDataSource): HistoryRepository = HistoryRepositoryImpl(dataSource)
}
```

## 서버 시간 → 기기 타임존

- 서버와는 UTC 순간(ISO + 오프셋)으로 주고받는다. 서버 시간 문자열을 화면용 값으로 바꾸는 일은 **data 레이어 `toVO()`에서만** 하고, VO에는 변환이 끝난 값만 담는다.
- 파싱은 [DeviceTime.kt](../../common/entity/src/main/java/com/dandi/nyummy/common/entity/time/DeviceTime.kt)의 `parseIsoOrNull()`/`parseIsoToEpochMillisOrNull()`로만 한다(오프셋이 없으면 UTC로 본다). 예: [HistoryDateParsers.kt](../../history/data/src/main/java/com/dandi/nyummy/history/data/util/HistoryDateParsers.kt)의 `toDisplayTime()`/`toDeviceHistoryDate()`.
- 서버는 요청 헤더 `X-Timezone-Id`의 타임존으로 하루를 끊는다(아래 공통 헤더).

## NetworkModule (공용, 단일 생성처)

[NetworkModule.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/di/NetworkModule.kt) — OkHttp/Retrofit/Json을 한 곳에서 생성.

- Retrofit은 두 벌이다.
  - **인증 Retrofit**(qualifier 없음): `Authorization: Bearer` 헤더를 붙이고, 401이면 `TokenAuthenticator`가 조용히 토큰을 재발급해 다시 보낸다. refresh 토큰까지 만료됐을 때만 401이 위로 올라간다(처리는 error-handling.md).
  - **`@NoAuthApi` Retrofit**: 인증 헤더도 Authenticator도 없다. `AuthApiService` 전체(로그인·회원가입·토큰 재발급 등, [AuthDataModule.kt](../../auth/data/src/main/java/com/dandi/nyummy/auth/data/AuthDataModule.kt))와, 우리 서버가 아닌 S3 업로드(`ImageStorageApiService`, `@Url`로 presigned URL에 직접 PUT)에 쓴다. refresh 호출이 Authenticator를 타지 않아 401 재귀가 구조적으로 생기지 않는다.
- 공통 헤더 [CommonHeaderInterceptor.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/network/CommonHeaderInterceptor.kt): 앱 버전, 기기 모델, OS 버전, 언어, 지역(SIM → 네트워크 국가 → 언어 설정 순), `X-Timezone-Id`(`DeviceTime.zone().id`). **우리 API 호스트로 가는 요청에만** 붙인다(S3 presigned URL에 헤더가 붙으면 서명이 깨진다).
- base URL은 `local.properties`(`API_BASE_URL_DEV`/`API_BASE_URL_PROD` — debug/release별, 없으면 환경변수) → BuildConfig 주입. 주입 지점은 [common/data/build.gradle.kts](../../common/data/build.gradle.kts)의 `API-CONFIG-INJECTION-POINT` 마커 2곳(API 키, base URL). `API_KEY`는 BuildConfig에 들어가지만 지금 읽는 코드는 없다.
- Json 정책: `ignoreUnknownKeys / explicitNulls=false / coerceInputValues`

## 이미지

- 업로드: presigned URL 발급(`ImageUploadApiService`, 인증) → S3에 직접 PUT(`ImageStorageApiService`, `@NoAuthApi`). 두 단계를 [ImageUploadDataSource.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/image/ImageUploadDataSource.kt)가 묶는다.
- 로딩: Coil 의존성은 `common:presentation`에서만 `api`로 노출하고, ImageLoader 설정은 [NyummyImageLoaderFactory.kt](../../common/presentation/src/main/java/com/dandi/nyummy/common/presentation/image/NyummyImageLoaderFactory.kt) 한 곳에서 한다(presigned URL 캐시 키 정규화 포함). feature 모듈에 coil 의존을 따로 선언하지 않는다.

## 변형 패턴

| 상황 | 패턴 | 골든 예제 |
|---|---|---|
| 여러 feature가 같은 데이터를 씀 | presentation 없는 공유 도메인 모듈을 두고, 쓰는 쪽이 그 domain을 의존 | `cat` (home, room이 `:cat:domain` 사용) |
| 병렬 API 호출 머지 | UseCase에서 `coroutineScope { async/async → await }` 후 머지 | [GetMonthWithDayMealsUseCase.kt](../../history/domain/src/main/java/com/dandi/nyummy/history/domain/GetMonthWithDayMealsUseCase.kt) |
| 로컬 저장소 (KV + 변경 구독) | DataStore Preferences + [BaseLocalDataSource](../../common/data/src/main/java/com/dandi/nyummy/common/data/BaseLocalDataSource.kt) | [TokenProviderImpl.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/token/TokenProviderImpl.kt), [AppPreferenceProviderImpl.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/preference/AppPreferenceProviderImpl.kt) |

로컬 저장소 핵심: `BaseLocalDataSource`를 상속해 `observe(key): Flow<T?>` / `read` / `write` / `remove` / `editAtomically { }`를 쓴다. 디스크 읽기 실패(`IOException`)는 빈 값으로 복구하고 그 밖의 예외와 쓰기 실패는 호출자에게 전파한다. 쓰기에 Mutex를 두지 않는다 — `DataStore.edit`가 직렬화·원자성을 보장한다. 파일은 용도별로 나눈다: 인증 토큰 `@AuthTokenDataStore`("auth_tokens"), 앱 환경설정 `@AppPreferenceDataStore`("app_prefs") — [LocalStorageModule.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/di/LocalStorageModule.kt).

## 규칙 요약

1. DTO는 data 모듈 밖으로 나가지 않는다 — domain/presentation은 VO만 본다.
2. 서버 필드는 camelCase라 DTO 프로퍼티 이름을 그대로 맞춘다. `@SerialName`은 Kotlin 이름과 서버 이름이 달라야 할 때만 쓴다.
3. DataSource는 `checkResponse` 경유 필수 (직접 `response.body()!!` 금지).
4. 페이지네이션은 cursor 방식이다 — VO에 `nextCursor`(마지막 묶음이면 null)를 담는다. 화면 쪽 패턴은 mvi.md의 페이징 절.
