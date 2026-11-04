package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.settings.entity.MyProfileChange
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 내 프로필에서 [MyProfileChange]가 바꾸는 항목만 서버에 저장한다. 집사 이름은 앞뒤 공백을 빼고 보낸다.
 * 바꿀 항목이 없으면 서버에 보내지 않고 성공으로 본다.
 * 실패하면 안내하고 [Result.failure]를 돌려준다. 다시 시도는 [onRetry]로 화면에 맡긴다.
 * 집사 이름이 비었는지는 화면이 시트 안에서 먼저 막는다.
 */
class UpdateMyProfileUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(change: MyProfileChange, onRetry: () -> Unit): Result<MyProfileChange> {
        val normalized = change.copy(nickname = change.nickname?.trim())
        if (normalized.isEmpty) return Result.success(normalized)
        return try {
            repository.updateMyProfile(normalized)
            Result.success(normalized)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpResponseException) {
            handleHttpError<MyProfileErrorType>(
                e,
                onDomainError = { messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = it.errorMsg) },
                onUnknownError = { showSaveFailed(onRetry) },
            )
            Result.failure(e)
        } catch (e: Exception) {
            // 네트워크 끊김 등. 던지면 화면 코루틴이 앱을 죽인다.
            showSaveFailed(onRetry)
            Result.failure(e)
        }
    }

    private fun showSaveFailed(onRetry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = SAVE_FAILED_MESSAGE,
            callToActionText = RETRY_LABEL,
            onClickCTA = onRetry,
        )
    }

    private companion object {
        const val SAVE_FAILED_MESSAGE = "내 정보를 저장하지 못했어요"
        const val RETRY_LABEL = "다시 시도"
    }
}
