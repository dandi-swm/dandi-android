package com.dandi.nyummy.common.data.di

import com.dandi.nyummy.common.data.BuildConfig
import com.dandi.nyummy.common.data.token.TokenAuthenticator
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.data.token.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 인증 헤더·재발급이 붙지 않는 공개 API 바인딩 qualifier
 * (login/signup/email-verification/refresh).
 *
 * 비인증 엔드포인트를 클라이언트 수준에서 분리하면:
 * - refresh 호출이 [TokenAuthenticator] 를 타지 않아 401 재귀·데드락이 구조적으로 불가능하다.
 * - 로그인 401(자격 증명 오류)이 토큰 재발급·세션 폐기로 오인될 경로가 사라진다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** 공개 클라이언트 — 로깅·타임아웃만. */
    @Provides
    @Singleton
    @NoAuthApi
    fun provideNoAuthOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** 인증 클라이언트(무한정 기본) — 토큰 헤더 부착 + 401 silent re-auth. 커넥션 풀은 공유. */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @NoAuthApi noAuthClient: OkHttpClient,
        tokenProvider: TokenProvider,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient {
        val authHeaderInterceptor = Interceptor { chain ->
            val accessToken = tokenProvider.accessToken?.takeIf { it.isNotBlank() }
                ?: return@Interceptor chain.proceed(chain.request())
            chain.proceed(
                chain.request().newBuilder()
                    .header(
                        TokenAuthenticator.HEADER_AUTHORIZATION,
                        "${TokenAuthenticator.BEARER_PREFIX}$accessToken",
                    )
                    .build(),
            )
        }
        return noAuthClient.newBuilder()
            // 로깅 인터셉터보다 앞에 둬야 Authorization 헤더가 로그에 남는다.
            .apply { interceptors().add(0, authHeaderInterceptor) }
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    @NoAuthApi
    fun provideNoAuthRetrofit(
        @NoAuthApi okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = buildRetrofit(okHttpClient, json)

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        buildRetrofit(okHttpClient, json)

    // TokenProvider 바인딩은 LocalStorageModule, TokenRefresher 바인딩은 auth/data(AuthDataModule) 담당.
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenProvider: TokenProvider,
        tokenRefresher: TokenRefresher,
    ): TokenAuthenticator = TokenAuthenticator(tokenProvider, tokenRefresher)

    private fun buildRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(okHttpClient)
            .build()
    }
}
