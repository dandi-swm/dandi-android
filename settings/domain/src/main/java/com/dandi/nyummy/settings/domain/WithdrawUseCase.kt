package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.tti.TTIHelper
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject

/**
 * 회원 탈퇴하고 첫 화면으로 돌아간다. 되돌릴 수 없다.
 *
 * 탈퇴되면 이 기기의 로그인 토큰, 푸시 토큰, 첫 사용자 튜토리얼 진행 상태를 지운 뒤 첫 화면으로 간다. 로그아웃과 같은 순서로, 다시 가입하거나
 * 로그인할 때의 토큰 등록이 지우기보다 먼저 실행되지 않게 한다.
 * 실패하면 회원은 그대로이므로 오류 스낵바만 띄우고 실패를 돌려준다. 로그인 만료(401)와 5xx는 공통 안내가 맡는다.
 */
class WithdrawUseCase @Inject constructor(
    private val repository: SettingsRepository,
    private val pushTokenHelper: PushTokenHelper,
    private val tutorialHelper: TutorialHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    suspend operator fun invoke(): Result<Unit> {
        try {
            repository.withdraw()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpResponseException) {
            handleHttpError<SettingsErrorType>(
                e,
                onDomainError = { type -> showError(type.errorMsg) },
                onUnknownError = { showError(WITHDRAW_FAILED_MESSAGE) },
            )
            return Result.failure(e)
        } catch (e: Exception) {
            // 네트워크 끊김 등 HTTP 응답 전 실패.
            showError(WITHDRAW_FAILED_MESSAGE)
            return Result.failure(e)
        }
        pushTokenHelper.deleteToken()
        try {
            tutorialHelper.clear()
        } catch (_: IOException) {
        }
        messageHelper.showSnackBar(iconType = IconType.SUCCESS, messageText = WITHDRAWN_MESSAGE)
        navigationHelper.navigateToInitial()
        return Result.success(Unit)
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }

    companion object {
        const val WITHDRAWN_MESSAGE = "탈퇴했어요. 그동안 함께해 줘서 고마워요. 또 봐요!"
        const val WITHDRAW_FAILED_MESSAGE = "탈퇴하지 못했어요. 잠시 후 다시 시도해 주세요."
    }
}
