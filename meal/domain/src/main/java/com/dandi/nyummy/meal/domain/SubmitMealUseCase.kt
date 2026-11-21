package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.DomainFailure
import com.dandi.nyummy.common.domain.error.toDomainFailure
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.domain.session.SessionExpiryHandler
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 촬영한 식사 사진을 제출한다.
 *
 * 촬영본 준비(검증·상한 초과 시 재압축) → presigned URL 발급 → 스토리지 업로드 →
 * 식사 생성(음식 인식)을 순서대로 수행하며, 어느 단계에서 실패해도 실패 안내 후
 * [Result.failure] 를 돌려준다. 식사 생성 응답이 오면 기록이 접수된 것이고(음식이 아니라고 판정돼도 접수다),
 * 응답의 인식 결과가 다음 화면을 정한다. 기록하지 못한 경우는 에러 코드로 와서 [MealErrorType] 으로 안내한다.
 * 접수되면 그 순간을 마지막 기록으로 기기 홈 화면 위젯에 넘긴다(튜토리얼 식사는 [SubmitTutorialMealUseCase]라 넘기지 않는다).
 */
class SubmitMealUseCase @Inject constructor(
    private val repository: MealRecordRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val widgetHelper: WidgetHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
    sessionExpiryHandler: SessionExpiryHandler? = null,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper, sessionExpiryHandler) {

    suspend operator fun invoke(photoPath: String, source: MealPhotoSource): Result<CreatedMealVO> = try {
        repository.prepareUploadImage(photoPath, source)
        val uploadTarget = repository.issueImageUploadUrl(photoPath)
        repository.uploadImage(uploadTarget, photoPath)
        val created = repository.createMeal(uploadTarget.imageKey)
        widgetHelper.onMealRecorded()
        Result.success(created)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 공통 안내(로그인 만료, 5xx)로 끝나는 실패까지 모든 실패의 사유를 남겨야 해서 request 대신 분류를 직접 쓴다.
        val failure = e.toDomainFailure(MealErrorType.entries)
        logBlocked(failure.analyticsReason)
        notifyFailure(
            failure,
            onBusinessError = { showError(it.errorMsg) },
            // 촬영본 검증, 압축 실패는 그 사유를, 그 밖(등록하지 않은 응답 오류, 네트워크 끊김)은 기본 안내를 한다.
            onUnknownError = { cause -> showError((cause as? MealPhotoInvalidException)?.reason ?: StringResource.MEAL_SUBMIT_FAILED) },
        )
        Result.failure(e)
    }

    /** 제출이 막힌 사유를 사용 분석에 남긴다. 기록이 막힌 사유별 비율을 잰다. */
    private fun logBlocked(reason: String) {
        analyticsHelper.logEvent(AnalyticsEvent.MealRecordBlocked(stage = "submit", reason = reason))
    }

    private fun showError(message: StringResource) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(message))
    }
}

/** 분석에 남기는 막힌 사유. 응답 오류는 서버 code별로, code가 없으면 server_error다. */
private val DomainFailure<MealErrorType>.analyticsReason: String
    get() = when (this) {
        is DomainFailure.Business -> type.analyticsReason
        is DomainFailure.Common, is DomainFailure.UnknownHttp -> "server_error"
        is DomainFailure.Unexpected -> if (cause is MealPhotoInvalidException) "invalid_photo" else "network_or_unknown"
        is DomainFailure.Network -> "network_or_unknown"
    }

/** 분석에 남기는 막힌 사유. */
private val MealErrorType.analyticsReason: String
    get() = when (this) {
        MealErrorType.STALE_IMAGE -> "stale_image"
        MealErrorType.DUPLICATE_IMAGE_KEY -> "duplicate_image"
        MealErrorType.DAILY_COUNT_EXCEEDED -> "daily_limit"
        MealErrorType.UNSUPPORTED_CONTENT_TYPE -> "unsupported_type"
        MealErrorType.FILE_SIZE_EXCEEDED -> "file_too_large"
        MealErrorType.TUTORIAL_ALREADY_COMPLETED -> "tutorial_already_completed"
        MealErrorType.MEAL_FOODS_NOT_UPDATABLE -> "foods_not_updatable"
        MealErrorType.MEAL_NOT_FOUND -> "meal_not_found"
    }

/** 제출 전 촬영본 검증·압축 실패. [reason] 은 사용자에게 보여 줄 안내 문구다. */
class MealPhotoInvalidException(val reason: StringResource) : IllegalStateException(reason.name)
