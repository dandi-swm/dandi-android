package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.settings.entity.MyProfileVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 마이페이지에 보일 내 프로필을 읽는다.
 * 읽지 못하면 안내하고 [Result.failure]를 돌려준다. 다시 시도는 [onRetry]로 화면에 맡긴다.
 * 회원이 없으면(404) 다시 시도해도 소용없으므로 다시 로그인하라고 알리고, 확인을 누르면 [onSignOut]으로 로그아웃을 화면에 맡긴다.
 */
class GetMyProfileUseCase @Inject constructor(
    private val repository: SettingsRepository,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(onRetry: () -> Unit, onSignOut: () -> Unit): Result<MyProfileVO> = try {
        Result.success(repository.getMyProfile())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpResponseException) {
        handleHttpError<MyProfileErrorType>(
            e,
            onDomainError = { error ->
                if (error == MyProfileErrorType.USER_NOT_FOUND) {
                    messageHelper.showOneButtonDialog(
                        descText = resourceHelper.getString(error.errorMsg),
                        cantIgnore = true,
                        buttonText = resourceHelper.getString(StringResource.SIGN_IN_AGAIN),
                        onClickButton = onSignOut,
                    )
                } else {
                    messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = resourceHelper.getString(error.errorMsg))
                }
            },
            onUnknownError = { showLoadFailed(onRetry) },
        )
        Result.failure(e)
    } catch (e: Exception) {
        // 네트워크 끊김 등. 던지면 화면 코루틴이 앱을 죽인다.
        showLoadFailed(onRetry)
        Result.failure(e)
    }

    private fun showLoadFailed(onRetry: () -> Unit) {
        messageHelper.showSnackBar(
            iconType = IconType.ERROR,
            messageText = resourceHelper.getString(StringResource.SETTINGS_PROFILE_LOAD_FAILED),
            callToActionText = resourceHelper.getString(StringResource.RETRY),
            onClickCTA = onRetry,
        )
    }
}
