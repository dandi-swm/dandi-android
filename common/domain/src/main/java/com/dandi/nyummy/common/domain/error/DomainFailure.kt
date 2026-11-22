package com.dandi.nyummy.common.domain.error

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * 여러 화면에 공통인 서버 오류(로그인 만료 401, 404, 5xx) 중 이 요청에서 공통 안내로 보낼 범위.
 * 공통 안내는 [com.dandi.nyummy.common.domain.base.BaseUseCase.executeCommonErrorHanding]이 한다.
 * 범위 밖의 공통 오류는 [DomainFailure.UnknownHttp]가 되어 화면의 기본 안내로 간다.
 */
enum class CommonErrorPolicy(
    internal val handlesSessionExpired: Boolean,
    internal val handlesServerErrors: Boolean,
) {
    /** 로그인한 화면의 기본값. 로그인 만료 401, 404, 5xx 모두 공통 안내를 한다. */
    ALL(handlesSessionExpired = true, handlesServerErrors = true),

    /**
     * 로그인 전 화면(이메일 로그인, 가입, 인증 코드, 비밀번호 찾기). 로그인 세션이 없으니 401은 로그인 만료가 아니다.
     * 401은 화면의 기본 안내로 보내고, 404와 5xx만 공통 안내를 한다.
     */
    SIGNED_OUT(handlesSessionExpired = false, handlesServerErrors = true),

    /**
     * 실패를 화면 안에서 조용히 넘기는 요청(화면을 꾸미는 조회, 토글 저장, 기록 취소).
     * 로그인 만료만 공통 안내를 하고, 404와 5xx는 화면의 기본 안내(대개 아무것도 안 함)로 보낸다.
     */
    SESSION_ONLY(handlesSessionExpired = true, handlesServerErrors = false),

    /** 공통 안내를 하지 않는다. 모든 응답 오류를 화면이 상태 코드로 직접 안내한다(소셜 로그인처럼 제공자별 문구가 필요한 곳). */
    NONE(handlesSessionExpired = false, handlesServerErrors = false),
}

/**
 * UseCase에서 난 예외를 처리 순서대로 나눈 결과. [toDomainFailure]가 만든다.
 *
 * 순서: ① 서버 code가 기능의 ErrorType에 있으면 [Business] ② 정책이 맡는 공통 오류면 [Common]
 * ③ 그 밖의 응답 오류는 [UnknownHttp] ④ 응답 전에 끊기면 [Network] ⑤ 나머지는 [Unexpected].
 * 코루틴 취소([CancellationException])는 실패가 아니라서 여기에 오지 않는다.
 */
sealed interface DomainFailure<out E : HttpErrorType> {
    /** 원래 예외. UseCase는 이것을 [Result.failure]로 돌려준다. */
    val cause: Exception

    /** 백엔드가 code로 알려 준, 이 기능이 아는 오류. 공통 오류보다 먼저 본다. */
    data class Business<out E : HttpErrorType>(
        val type: E,
        override val cause: HttpResponseException,
    ) : DomainFailure<E>

    /** 정책([CommonErrorPolicy])이 맡는 공통 오류. 로그인 만료 401, 404, 5xx. */
    data class Common(override val cause: HttpResponseException) : DomainFailure<Nothing>

    /** 응답은 왔지만 등록하지 않은 오류(등록하지 않은 code의 4xx, 정책 밖의 공통 오류). */
    data class UnknownHttp(override val cause: HttpResponseException) : DomainFailure<Nothing>

    /** 응답을 받기 전에 끊겼다(연결 끊김, 시간 초과). */
    data class Network(override val cause: IOException) : DomainFailure<Nothing>

    /** 예상하지 못한 예외(응답 해석 실패, 본문 없는 성공 응답, 잘못된 상태 등). */
    data class Unexpected(override val cause: Exception) : DomainFailure<Nothing>
}

/**
 * 예외를 [DomainFailure]로 나눈다. 화면 안내는 하지 않는 순수 함수다.
 *
 * @param errorTypes 이 기능이 아는 서버 code 목록. 보통 `HistoryErrorType.entries`처럼 넘긴다.
 * @param policy 공통 안내로 보낼 범위.
 * @throws CancellationException 코루틴 취소는 나누지 않고 그대로 다시 던진다. 삼키면 닫힌 화면의 작업이 계속 돈다.
 */
fun <E : HttpErrorType> Exception.toDomainFailure(
    errorTypes: List<E>,
    policy: CommonErrorPolicy = CommonErrorPolicy.ALL,
): DomainFailure<E> {
    if (this is CancellationException) throw this
    return when (this) {
        is HttpResponseException -> {
            val code = serverErrorCode
            val businessType = code?.let { errorTypes.firstOrNull { type -> type.type == it } }
            when {
                businessType != null -> DomainFailure.Business(businessType, this)
                isSessionExpired() -> if (policy.handlesSessionExpired) DomainFailure.Common(this) else DomainFailure.UnknownHttp(this)
                rawCode == 404 || rawCode >= 500 -> if (policy.handlesServerErrors) DomainFailure.Common(this) else DomainFailure.UnknownHttp(this)
                else -> DomainFailure.UnknownHttp(this)
            }
        }
        is IOException -> DomainFailure.Network(this)
        else -> DomainFailure.Unexpected(this)
    }
}

/**
 * UseCase가 돌려준 실패가 공통 안내(로그인 만료, 404, 5xx)로 이미 알려졌는지. UseCase와 같은 [errorTypes]와 [policy]로 나눠 본다.
 * 화면이 실패를 따로 안내할지 고를 때 쓴다. 화면이 `HttpResponseException`을 직접 열어 규칙을 다시 쓰면 UseCase와 어긋난다.
 */
fun <E : HttpErrorType> Throwable.isCommonErrorNotified(
    errorTypes: List<E>,
    policy: CommonErrorPolicy = CommonErrorPolicy.ALL,
): Boolean = this is Exception && this !is CancellationException && toDomainFailure(errorTypes, policy) is DomainFailure.Common

/**
 * [runCatching]과 같지만 코루틴 취소는 다시 던진다. 실패해도 흐름을 이어 가는 작업(기기 저장, 장식용 조회)에 쓴다.
 * suspend 함수를 [runCatching]으로 감싸면 [CancellationException]까지 잡혀, 화면이 닫힌 뒤에도 다음 줄이 실행된다.
 */
suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
