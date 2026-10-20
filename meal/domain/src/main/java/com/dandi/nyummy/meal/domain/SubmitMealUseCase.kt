package com.dandi.nyummy.meal.domain

import com.dandi.nyummy.common.domain.analytics.AnalyticsEvent
import com.dandi.nyummy.common.domain.analytics.AnalyticsHelper
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.meal.entity.CreatedMealVO
import com.dandi.nyummy.meal.entity.MealPhotoSource
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject

/**
 * 촬영한 식사 사진을 제출한다.
 *
 * 촬영본 준비(검증·상한 초과 시 재압축) → presigned URL 발급 → 스토리지 업로드 →
 * 식사 생성(분석 시작)을 순서대로 수행하며, 어느 단계에서 실패해도 실패 안내 후
 * [Result.failure] 를 돌려준다.
 */
class SubmitMealUseCase @Inject constructor(
    private val repository: MealRecordRepository,
    private val analyticsHelper: AnalyticsHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(photoPath: String, source: MealPhotoSource): Result<CreatedMealVO> = try {
        repository.prepareUploadImage(photoPath, source)
        val uploadTarget = repository.issueImageUploadUrl(photoPath)
        repository.uploadImage(uploadTarget, photoPath)
        Result.success(repository.createMeal(uploadTarget.imageKey))
    } catch (e: MealPhotoInvalidException) {
        logBlocked("invalid_photo")
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = e.message)
        Result.failure(e)
    } catch (e: HttpResponseException) {
        var reason = "server_error"
        handleHttpError<MealErrorType>(
            e,
            onDomainError = {
                reason = it.analyticsReason
                showError(it.errorMsg)
            },
            onUnknownError = { showError(SUBMIT_ERROR_MESSAGE) },
        )
        logBlocked(reason)
        Result.failure(e)
    } catch (e: java.util.concurrent.CancellationException) {
        throw e
    } catch (e: Exception) {
        logBlocked("network_or_unknown")
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = SUBMIT_ERROR_MESSAGE)
        Result.failure(e)
    }

    /** 제출이 막힌 사유를 사용 분석에 남긴다. 기록이 막힌 사유별 비율을 잰다. */
    private fun logBlocked(reason: String) {
        analyticsHelper.logEvent(AnalyticsEvent.MealRecordBlocked(stage = "submit", reason = reason))
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    private companion object {
        const val SUBMIT_ERROR_MESSAGE = "식사를 기록하지 못했어요. 다시 시도해주세요"
    }
}

/** 분석에 남기는 막힌 사유. */
private val MealErrorType.analyticsReason: String
    get() = when (this) {
        MealErrorType.STALE_IMAGE -> "stale_image"
        MealErrorType.DUPLICATE_IMAGE_KEY -> "duplicate_image"
        MealErrorType.DAILY_COUNT_EXCEEDED -> "daily_limit"
        MealErrorType.UNSUPPORTED_CONTENT_TYPE -> "unsupported_type"
        MealErrorType.FILE_SIZE_EXCEEDED -> "file_too_large"
    }

/** 제출 전 촬영본 검증·압축 실패. [message] 는 사용자에게 그대로 노출할 안내 문구다. */
class MealPhotoInvalidException(override val message: String) : IllegalStateException(message)
