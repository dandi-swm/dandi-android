package com.dandi.nyummy.auth.data

import com.dandi.nyummy.auth.domain.AuthRepository
import com.dandi.nyummy.auth.domain.SocialSignUpSession
import com.dandi.nyummy.common.data.di.NoAuthApi
import com.dandi.nyummy.common.data.preference.AppPreferenceProvider
import com.dandi.nyummy.common.data.token.TokenProvider
import com.dandi.nyummy.common.data.token.TokenRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AuthDataModule {
    /** 로그인/회원가입/이메일 인증은 전부 비인증 엔드포인트 — 인증 스택 없는 Retrofit 으로 생성한다. */
    @Provides
    @Singleton
    fun provideAuthApiService(@NoAuthApi retrofit: Retrofit): AuthApiService =
        retrofit.create(AuthApiService::class.java)


    @Provides
    @Singleton
    fun provideAuthDataSource(apiService: AuthApiService): AuthDataSource =
        AuthDataSource(apiService)

    @Provides
    @Singleton
    fun provideAuthRepository(
        dataSource: AuthDataSource,
        tokenProvider: TokenProvider,
        appPreferenceProvider: AppPreferenceProvider,
    ): AuthRepository = AuthRepositoryImpl(dataSource, tokenProvider, appPreferenceProvider)

    /** 소셜 로그인 신규 회원의 가입 대기 토큰 — 로그인 화면과 소셜 가입 화면이 같은 인스턴스를 공유해야 한다. */
    @Provides
    @Singleton
    fun provideSocialSignUpSession(): SocialSignUpSession = SocialSignUpSession()

    /** common/data 의 TokenAuthenticator 가 쓰는 재발급 계약 — 구현·바인딩은 auth 소유 (TokenRefresher 참고). */
    @Provides
    @Singleton
    fun provideTokenRefresher(
        apiService: AuthApiService,
        tokenProvider: TokenProvider,
    ): TokenRefresher = TokenRefresherImpl(apiService, tokenProvider)

}
