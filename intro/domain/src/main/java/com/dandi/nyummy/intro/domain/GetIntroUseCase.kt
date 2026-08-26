package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.intro.entity.VersionCheckVO
import com.dandi.nyummy.tti.TTIHelper
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class GetIntroUseCase @Inject constructor(
    private val repository: IntroRepository,
    private val remoteConfigHelper: RemoteConfigHelper,
    private val deviceHelper: DeviceHelper,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 앱 시작 게이트 + 분기.
     *
     * 1. RemoteConfig 를 sync(fetch/activate)한다 — 앱 전체에서 여기 1회뿐이며,
     *    이후 다른 기능의 flag 조회는 활성화된 스냅샷만 읽는다.
     * 2. 최소 요구 버전을 조회해 현재 앱 versionCode 와 비교한다.
     *    미만이면 닫을 수 없는 강제 업데이트 다이얼로그를 띄우고 홈/로그인 이동을 중단한다.
     * 3. 통과하면 저장된 리프레시 토큰이 있으면 홈, 없으면 로그인을 루트로 이동한다.
     *
     * 실패는 여기(domain)에서 종결한다 — 닫을 수 없는 재시도 다이얼로그를 띄우고,
     * 재시도 버튼은 [onRetry] 로 흐름을 재실행한다. 인트로는 시작 게이트라
     * 실패를 방치하면 빈 화면에 갇히므로 조용한 실패 반환을 허용하지 않는다.
     *
     * 토큰 유효성은 여기서 검증하지 않는다 — 만료된 토큰은 이후 요청에서
     * Authenticator 의 재발급 실패 → 공통 401 처리(세션 만료)로 걸러진다.
     */
    suspend operator fun invoke(onRetry: () -> Unit = {}): Result<VersionCheckVO> = try {
        remoteConfigHelper.sync()
        val version = remoteConfigHelper.getVersionCheck()

        if (isForceUpdateRequired(version)) {
            showForceUpdateDialog(version)
            return Result.success(version)
        }

        if (repository.hasRefreshToken()) {
            navigationHelper.navigateToAsRoot(HomePage)
        } else {
            navigationHelper.navigateToAsRoot(LoginPage)
        }
        Result.success(version)
    } catch (e: CancellationException) {
        // 화면 종료 등으로 취소된 경우 — 에러 다이얼로그를 띄우면 안 되므로 그대로 전파한다.
        throw e
    } catch (e: Throwable) {
        showIntroErrorDialog(onRetry)
        Result.failure(e)
    }

    private fun isForceUpdateRequired(version: VersionCheckVO): Boolean =
        version.minimumVersionCode > 0L && deviceHelper.appVersionCode < version.minimumVersionCode

    private fun showForceUpdateDialog(version: VersionCheckVO) {
        messageHelper.showOneButtonDialog(
            cantIgnore = true,
            descText = version.minimumVersionReleaseNote.ifBlank { DEFAULT_FORCE_UPDATE_MSG },
            buttonText = "스토어로 이동",
            onClickButton = { navigationHelper.navigateToExternalLink(version.latestVersionUpdateLink) },
        )
    }

    private fun showIntroErrorDialog(onRetry: () -> Unit) {
        messageHelper.showOneButtonDialog(
            cantIgnore = true,
            descText = INTRO_ERROR_MSG,
            buttonText = "재시도",
            onClickButton = onRetry,
        )
    }

    companion object {
        private const val DEFAULT_FORCE_UPDATE_MSG =
            "현재 앱이 최소 요구 버전을 만족하지 않습니다.\n최신 버전으로 업데이트 해주세요."
        private const val INTRO_ERROR_MSG =
            "일시적인 오류가 발생했습니다.\n잠시 후 다시 시도해주세요."
    }
}
