package com.dandi.nyummy.settings.domain

import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.error.HttpResponseException
import com.dandi.nyummy.common.domain.helper.CompanionMemoryHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.PushTokenHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.helper.StringResource
import com.dandi.nyummy.common.domain.helper.TutorialHelper
import com.dandi.nyummy.common.domain.helper.WidgetHelper
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * 회원 탈퇴하고 첫 화면으로 돌아간다. 되돌릴 수 없다.
 *
 * 탈퇴되면 이 기기의 로그인 토큰과 푸시 토큰, 냐미가 기억한 값(마지막 방문, 체형), 첫 사용자 튜토리얼 진행 상태,
 * 기기 홈 화면 위젯에 남긴 기록을 지운 뒤 첫 화면으로 간다. 로그아웃과 같은 순서로, 다시 가입하거나
 * 로그인할 때의 토큰 등록이 지우기보다 먼저 실행되지 않게 한다.
 * 실패하면 회원은 그대로이므로 오류 스낵바만 띄우고 실패를 돌려준다. 로그인 만료(401)와 5xx는 공통 안내가 맡는다.
 */
class WithdrawUseCase @Inject constructor(
    private val repository: SettingsRepository,
    private val pushTokenHelper: PushTokenHelper,
    private val companionMemory: CompanionMemoryHelper,
    private val tutorialHelper: TutorialHelper,
    private val widgetHelper: WidgetHelper,
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
                onDomainError = { type -> showError(resourceHelper.getString(type.errorMsg)) },
                onUnknownError = { showError(resourceHelper.getString(StringResource.SETTINGS_WITHDRAW_FAILED)) },
            )
            return Result.failure(e)
        } catch (e: Exception) {
            // 네트워크 끊김 등 HTTP 응답 전 실패.
            showError(resourceHelper.getString(StringResource.SETTINGS_WITHDRAW_FAILED))
            return Result.failure(e)
        }
        pushTokenHelper.deleteToken()
        companionMemory.clear()
        tutorialHelper.clear()
        widgetHelper.clear()
        messageHelper.showSnackBar(iconType = IconType.SUCCESS, messageText = resourceHelper.getString(StringResource.SETTINGS_WITHDRAWN))
        navigationHelper.navigateToInitial()
        return Result.success(Unit)
    }

    private fun showError(message: String) {
        messageHelper.showSnackBar(iconType = IconType.ERROR, messageText = message)
    }
}
