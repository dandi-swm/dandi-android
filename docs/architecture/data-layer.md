# 데이터 레이어

## 왜

- **DTO 전 필드 nullable + VO 기본값** 규칙은 서버 스키마 변동/필드 누락에 대한 방어선을 `toVO()` 한 곳에 모은다 — 파싱 크래시가 구조적으로 불가능해진다.
- Repository 인터페이스를 domain에 두면 presentation/domain은 네트워크 구현(Retrofit)을 전혀 모른다.

## 구성 요소와 골든 예제 (intro = 표준형)

> 아래 `Intro`/`Search`/`MediaSearch`/`Favorite` 이름은 레거시 아키텍처 예시입니다. 현재 저장소의 모듈이나 파일 경로를 뜻하지 않습니다.

```
[domain]  {Feature}Repository (인터페이스)        IntroRepository.kt
[data]    {Feature}ApiService (Retrofit)          IntroApiService.kt
[data]    {Feature}DataSource : BaseRemoteDataSource   IntroDataSource.kt
[data]    dto/{Feature}DTO (@Serializable, all-nullable, toVO())   IntroDTO.kt
[data]    {Feature}RepositoryImpl                 IntroRepositoryImpl.kt
[data]    {Feature}DataModule (Hilt 바인딩)        IntroDataModule.kt
[entity]  {Feature}VO (비-nullable, 기본값, companion empty)   IntroVO.kt
```

핵심 패턴:

```kotlin
// DTO — 전 필드 nullable + 기본값 null, 변환은 toVO()에서만
@Serializable
data class IntroDTO(val minAppVersion: String? = null, ...) {
    fun toVO(): IntroVO = IntroVO(minAppVersion = minAppVersion ?: UNKNOWN, ...)
}

// DataSource — checkResponse가 실패를 HttpResponseException으로 통일
class IntroDataSource(private val api: IntroApiService) : BaseRemoteDataSource() {
    suspend fun getIntro(): IntroDTO = checkResponse(api.getIntro())
}

// Hilt DataModule — @InstallIn(SingletonComponent::class), 3종 @Provides
@Module @InstallIn(SingletonComponent::class)
object IntroDataModule {
    @Provides @Singleton fun provideIntroRepository(ds: IntroDataSource): IntroRepository = IntroRepositoryImpl(ds)
    @Provides @Singleton fun provideIntroDataSource(api: IntroApiService): IntroDataSource = IntroDataSource(api)
    @Provides @Singleton fun provideIntroApiService(retrofit: Retrofit): IntroApiService = retrofit.create(IntroApiService::class.java)
}
```

## NetworkModule (공용, 단일 생성처)

[NetworkModule.kt](../../common/data/src/main/java/com/dandi/nyummy/common/data/di/NetworkModule.kt) — OkHttp/Retrofit/Json을 한 곳에서 생성.

- `API-CONFIG-INJECTION-POINT` 마커 2곳: base URL과 인증 헤더 포맷. 값은 `local.properties`(`API_KEY`, `API_BASE_URL`) → BuildConfig 주입 ([common/data/build.gradle.kts](../../common/data/build.gradle.kts))
- Json 정책: `ignoreUnknownKeys / explicitNulls=false / coerceInputValues`
- 새 API 호스트가 추가되면 Retrofit 인스턴스를 `@Named`로 분리 제공

## 변형 패턴

| 상황 | 패턴 | 골든 예제 |
|---|---|---|
| 자체 API 없이 공용 Repository 소비 | data 모듈 비움, UseCase가 `:common:domain` 인터페이스 주입 | 레거시 `search` 예시: `MediaSearchRepository` ← `MediaSearchRepositoryImpl` |
| 병렬 API 호출 머지 | `coroutineScope { async/async → await }` 후 머지/정렬 | `MediaSearchRepositoryImpl.kt` (레거시 예시) |
| 로컬 저장소 (KV + 변경 구독) | SharedPreferences + `callbackFlow` observe + 쓰기 Mutex | `FavoriteKVStorage.kt` (레거시 예시) |

로컬 저장소 핵심: 읽기는 `observeString(key): Flow<String?>`(리스너 등록 + 초기값 emit), 쓰기는 `writeMutex.withLock { read-modify-write }`로 유실 방지. DTO(저장용)↔VO 변환은 data 레이어의 `toVO()`/`toDto()` 확장 함수가 담당하고 Repository가 이를 호출한다.

## 규칙 요약

1. DTO는 data 모듈 밖으로 나가지 않는다 — domain/presentation은 VO만 본다.
2. `@SerialName`으로 snake_case ↔ camelCase 매핑.
3. DataSource는 `checkResponse` 경유 필수 (직접 `response.body()!!` 금지).
4. 페이지네이션 API는 VO에 `isEnd` 류의 종료 신호를 포함시킨다 (MediaSearchResultVO 참고).
