package com.dandi.nyummy.intro.domain

import com.dandi.nyummy.auth.domain.LoginPage
import com.dandi.nyummy.cat.domain.PrefetchCatAnimationsUseCase
import com.dandi.nyummy.common.domain.base.BaseUseCase
import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.domain.helper.DeviceHelper
import com.dandi.nyummy.common.domain.helper.MessageHelper
import com.dandi.nyummy.common.domain.helper.PermissionHelper
import com.dandi.nyummy.common.domain.helper.NavigationHelper
import com.dandi.nyummy.common.domain.helper.ResourceHelper
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.domain.remoteconfig.RemoteConfigHelper
import com.dandi.nyummy.common.entity.remoteconfig.VersionCheckVO
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.domain.PrefetchHomeSummaryUseCase
import com.dandi.nyummy.onboarding.domain.OnboardingPage
import com.dandi.nyummy.tti.TTIHelper
import com.dandi.nyummy.tti.TTIMetaData
import com.dandi.nyummy.tti.TimelineCategory
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class GetIntroUseCase @Inject constructor(
    private val repository: IntroRepository,
    private val remoteConfigHelper: RemoteConfigHelper,
    private val deviceHelper: DeviceHelper,
    private val permissionHelper: PermissionHelper,
    private val prefetchHomeSummary: PrefetchHomeSummaryUseCase,
    private val prefetchCatAnimations: PrefetchCatAnimationsUseCase,
    resourceHelper: ResourceHelper,
    messageHelper: MessageHelper,
    navigationHelper: NavigationHelper,
    ttiHelper: TTIHelper,
) : BaseUseCase(resourceHelper, messageHelper, navigationHelper, ttiHelper) {

    /**
     * 앱 시작 게이트 + 분기.
     *
     * 1. 권한 안내를 아직 노출하지 않았고 미허용 시작 권한이 있으면 [requestPermissions] 로
     *    UI 게이트(접근권한 안내 바텀시트)를 확인까지 suspend 한 뒤 노출 완료를 기록한다.
     *    시스템 권한 팝업은 여기서 띄우지 않는다 — 각 기능(식사 기록 카메라, 온보딩·설정 알림)을 쓸 때 그 화면이 요청한다.
     *    로딩(버전 체크)보다 먼저 두어 "권한부터 받고 로딩" 흐름을 만든다.
     * 2. RemoteConfig 를 sync(fetch/activate)한다 — 앱 전체에서 여기 1회뿐이며,
     *    이후 다른 기능의 flag 조회는 활성화된 스냅샷만 읽는다.
     * 3. 최소 요구 버전을 조회해 현재 앱 versionCode 와 비교한다.
     *    미만이면 닫을 수 없는 강제 업데이트 다이얼로그를 띄우고 홈/로그인 이동을 중단한다.
     * 4. 홈으로 갈 회원(리프레시 토큰이 있고 온보딩을 마친 회원)이면 1~3과 함께 홈 요약과 냐미 애니메이션, 시트를 미리 받는다.
     *    요약은 기다리지 않고, 냐미는 홈으로 넘어가기 전에 [CAT_PREFETCH_TIMEOUT_MS]까지만 기다린다. 다 받지 못한 것은 홈이 이어서 받는다.
     *    온보딩을 마치지 않은 회원은 아직 고양이가 없어 받지 않는다.
     * 5. 이동 직전 [onBeforeNavigate] 로 UI 마무리(스플래시 진행바 100% 채움)를 기다린 뒤,
     *    저장된 리프레시 토큰과 온보딩 상태에 따라 홈/온보딩/로그인을 루트로 이동한다.
     *    실패할 수 있는 작업은 모두 이 훅 이전에 끝나 있어야 한다.
     *
     * 실패는 여기(domain)에서 종결한다 — 닫을 수 없는 재시도 다이얼로그를 띄우고,
     * 재시도 버튼은 [onRetry] 로 흐름을 재실행한다. 인트로는 시작 게이트라
     * 실패를 방치하면 빈 화면에 갇히므로 조용한 실패 반환을 허용하지 않는다.
     *
     * 토큰 유효성은 여기서 검증하지 않는다 — 만료된 토큰은 이후 요청에서
     * Authenticator 의 재발급 실패 → 공통 401 처리(세션 만료)로 걸러진다.
     */
    suspend operator fun invoke(
        onRetry: () -> Unit = {},
        requestPermissions: suspend (List<AppPermission>) -> Unit = {},
        onBeforeNavigate: suspend () -> Unit = {},
    ): Result<VersionCheckVO> = try {
        // 냐미 미리 받기가 실패해도 시작 흐름은 이어지도록 자식의 실패가 번지지 않는 스코프에서 돌린다.
        supervisorScope {
            // 권한 안내, 원격 설정, 버전 확인과 함께 받기 시작해 기다리는 시간을 줄인다.
            val catPrefetch = async {
                if (startDestination() == HomePage) {
                    // 요약은 받기 시작만 하고 기다리지 않는다. 홈이 열리면 그 요청을 이어받는다.
                    prefetchHomeSummary()
                    prefetchCatAnimations()
                }
            }

            val ungranted = ungrantedStartupPermissions()
            if (ungranted.isNotEmpty() && !repository.hasShownPermissionNotice()) {
                // 권한 안내를 기다린 시간은 TTI 구간에서 뺄 수 없어 표시만 남긴다.
                ttiHelper.addTTIMetaData(TTIMetaData.USER_WAIT_INCLUDED, true)
                requestPermissions(ungranted)
                repository.markPermissionNoticeShown()
            }

            // API 구간은 성공했을 때만 닫는다. 구간은 처음 찍은 시작과 끝을 유지하므로, 실패 뒤 재시도로 성공하면
            // 첫 요청부터 재시도 성공까지가 한 구간이 되어 같은 범위를 재는 TTI 값과 어긋나지 않는다.
            ttiHelper.startTTITimeline(TimelineCategory.API_RESPONSE_TIME)
            remoteConfigHelper.sync()
            val version = remoteConfigHelper.getVersionCheck()
            ttiHelper.endTTITimeline(TimelineCategory.API_RESPONSE_TIME)
            // 버전 확인이 끝나 다음 행동이 정해진 시점이 인트로의 TTI다. 실패하면 여기까지 오지 않아 미완료로 남는다.
            ttiHelper.endTTITracking()

            if (isForceUpdateRequired(version)) {
                catPrefetch.cancel()
                showForceUpdateDialog(version)
                return@supervisorScope Result.success(version)
            }

            val destination = startDestination()
            if (destination == HomePage) {
                // 느린 망에서도 인트로에 갇히지 않게 상한까지만 기다린다. 넘기면 시트 받기를 멈추고, 받지 못한 것은 홈이 이어서 받는다.
                withTimeoutOrNull(CAT_PREFETCH_TIMEOUT_MS) { catPrefetch.join() } ?: catPrefetch.cancel()
            }
            onBeforeNavigate()
            navigationHelper.navigateToAsRoot(destination)
            Result.success(version)
        }
    } catch (e: CancellationException) {
        // 화면이 닫혀 취소된 것은 실패가 아니다. 다이얼로그를 띄우지 않고 취소를 이어 간다.
        throw e
    } catch (e: Throwable) {
        // 재시도 다이얼로그는 사용자가 누를 때까지 기다리므로, 재시도로 끝난 측정에는 사용자 대기가 들어간다.
        ttiHelper.addTTIMetaData(TTIMetaData.USER_WAIT_INCLUDED, true)
        showIntroErrorDialog(onRetry)
        Result.failure(e)
    }

    /** 저장된 리프레시 토큰과 온보딩 상태로 정한 시작 목적지. */
    private suspend fun startDestination(): Page = when {
        !repository.hasRefreshToken() -> LoginPage
        repository.isOnboardingIncomplete() -> OnboardingPage
        else -> HomePage
    }

    private fun isForceUpdateRequired(version: VersionCheckVO): Boolean =
        version.minimumVersionCode > 0L && deviceHelper.appVersionCode < version.minimumVersionCode

    private suspend fun ungrantedStartupPermissions(): List<AppPermission> =
        STARTUP_PERMISSIONS.filter { !permissionHelper.isGranted(it) }

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

        /** 앱 시작 시 접근권한 안내에 올리는 권한. 하나라도 미허용이면 안내를 노출한다. */
        private val STARTUP_PERMISSIONS = listOf(AppPermission.CAMERA, AppPermission.NOTIFICATION)

        /** 홈으로 가기 전에 냐미 미리 받기를 기다리는 상한. 방 준비 화면과 같다. */
        internal const val CAT_PREFETCH_TIMEOUT_MS = 3_000L
    }
}
