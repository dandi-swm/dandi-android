package com.dandi.nyummy.main.presentation.navigation

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomNav
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomNavActionRise
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMainTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMealRecordAction
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySnackbarHost
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.domain.message.IconType
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.helper.BottomBarVisibility
import com.dandi.nyummy.common.presentation.helper.LocalBottomBarVisibility
import com.dandi.nyummy.common.presentation.helper.LocalMessageHelper
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.meal.domain.MealRecordPage
import com.dandi.nyummy.room.domain.MyRoomPage
import com.dandi.nyummy.shop.domain.ShopPage
import kotlinx.coroutines.flow.Flow

/**
 * 앱 뼈대(Scaffold, 하단 탭, 공통 다이얼로그와 스낵바, 화면 전환).
 *
 * @param onScreenShown 맨 위 화면의 경로가 바뀔 때마다 그 경로로 부른다. 사용 분석의 화면 조회 기록에 쓴다.
 * @param mainTabFlags 기능 플래그로 켜고 끄는 하단 탭. 원격 구성은 인트로에서 받아 오므로 탭을 누를 때마다 읽는다.
 */
@Composable
fun RootComposable(
    modifier: Modifier = Modifier,
    startStack: List<NavKey>,
    onScreenShown: (path: String) -> Unit = {},
    mainTabFlags: () -> MainTabFlags = { MainTabFlags() },
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var oneButtonDialogEffect by remember {
        mutableStateOf<MessageEffect.ShowOneButtonDialog?>(null)
    }
    var twoButtonDialogEffect by remember {
        mutableStateOf<MessageEffect.ShowTwoButtonDialog?>(null)
    }

    val backStack = rememberNavBackStack(*startStack.toTypedArray())
    val navigationHelper = LocalNavigationHelper.current
    val currentKey = backStack.lastOrNull() as? GenericNavKey
    val currentRoute = currentKey?.let { appRouteByPath[it.path] }
    SystemBarIconsEffect(lightIcons = currentRoute?.usesLightSystemBarIcons == true)
    val currentTabIndex = mainTabIndexOf(currentKey?.path)
    val latestOnScreenShown by rememberUpdatedState(onScreenShown)
    // 경로는 라우트 패턴이라 식사 id 같은 값이 들어가지 않는다.
    LaunchedEffect(currentKey?.path) {
        currentKey?.path?.let(latestOnScreenShown)
    }

    val messageHelper = LocalMessageHelper.current

    val onShowOneButtonDialog = remember<(MessageEffect.ShowOneButtonDialog) -> Unit> {
        { oneButtonDialogEffect = it }
    }
    val onShowTwoButtonDialog = remember<(MessageEffect.ShowTwoButtonDialog) -> Unit> {
        { twoButtonDialogEffect = it }
    }
    MessageEffect(
        messageEffectFlow = messageHelper.effect,
        snackBarHostState = snackBarHostState,
        onShowOneButtonDialog = onShowOneButtonDialog,
        onShowTwoButtonDialog = onShowTwoButtonDialog,
    )

    // 서버와 UseCase가 띄우는 공통 다이얼로그. 제목이 없으면 본문을 제목 자리에 둔다.
    oneButtonDialogEffect?.let { dialog ->
        val close = { oneButtonDialogEffect = null }
        NyummyDialog(
            title = dialog.titleText ?: dialog.descText,
            body = dialog.titleText?.let { dialog.descText },
            confirmText = dialog.buttonText,
            onConfirm = {
                dialog.onClickButton?.invoke()
                close()
            },
            onDismissRequest = close,
            type = NyummyDialogType.Alert,
            dismissible = !dialog.cantIgnore,
        )
    }

    twoButtonDialogEffect?.let { dialog ->
        val close = { twoButtonDialogEffect = null }
        NyummyDialog(
            title = dialog.titleText ?: dialog.descText,
            body = dialog.titleText?.let { dialog.descText },
            confirmText = dialog.rightButtonText,
            onConfirm = {
                dialog.onClickRightButton?.invoke()
                close()
            },
            dismissText = dialog.leftButtonText,
            onDismissClick = {
                dialog.onClickLeftButton?.invoke()
                close()
            },
            onDismissRequest = close,
            dismissible = !dialog.cantIgnore,
        )
    }

    val drawsBehindSystemBars = currentRoute?.drawsBehindSystemBars == true
    // 홈 눈 버튼처럼 화면이 잠시 하단 내비를 숨길 수 있다.
    val bottomBarVisibility = remember { BottomBarVisibility() }
    val showsBottomBar = currentRoute?.isBottomTab == true && currentTabIndex >= 0 && !bottomBarVisibility.isHidden
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NyummyTheme.colors.bg.canvas,
        snackbarHost = {
            // Scaffold는 스낵바를 하단 탭 위나 contentWindowInsets의 하단 인셋만큼 올린다. 하단 탭이 있으면 가운데 버튼이
            // 솟은 만큼 더 올린다. 전체 화면 라우트는 인셋을 0으로 두고 하단 탭도 없어, 직접 올리지 않으면 스낵바가
            // 시스템 내비게이션 바(3버튼) 뒤에 깔린다.
            NyummySnackbarHost(
                hostState = snackBarHostState,
                modifier = when {
                    showsBottomBar -> Modifier.padding(bottom = NyummyBottomNavActionRise)
                    drawsBehindSystemBars -> Modifier.navigationBarsPadding()
                    else -> Modifier
                },
            )
        },
        // 전체 화면 라우트는 시스템 바 뒤까지 그리고 인셋을 스스로 처리한다. 나머지는 Scaffold가 민다.
        contentWindowInsets = if (drawsBehindSystemBars) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (showsBottomBar) {
                // 내비가 시스템 내비게이션 바 영역까지 흰 바탕을 이어 그린다.
                NyummyBottomNav(
                    items = NyummyMainTabs,
                    selectedIndex = currentTabIndex,
                    onSelect = { index ->
                        val page = mainTabPages[index]
                        if (isMainTabReady(page, mainTabFlags())) {
                            navigationHelper.navigateTo(page)
                        } else {
                            messageHelper.showSnackBar(iconType = IconType.WARNING, messageRes = CommonR.string.nyummy_not_ready)
                        }
                    },
                    centerAction = NyummyMealRecordAction,
                    centerActionIndex = MealRecordActionIndex,
                    onCenterActionClick = { navigationHelper.navigateTo(MealRecordPage) },
                )
            }
        }
    ) { innerPadding ->
        CompositionLocalProvider(LocalBottomBarVisibility provides bottomBarVisibility) {
            AppNavHost(
                backStack = backStack,
                modifier = if (currentRoute?.drawsBehindBottomBar == true) Modifier else Modifier.padding(innerPadding),
            )
        }
    }
}

/** 기능 플래그로 켜고 끄는 하단 탭. 꺼져 있어도 탭은 보이고, 누르면 준비 중이라고 알린다. */
data class MainTabFlags(
    val isMyRoomEnabled: Boolean = false,
    val isShopEnabled: Boolean = false,
)

/**
 * 하단 내비 탭 순서대로 이동할 화면. [NyummyMainTabs]의 순서(홈, 마이룸, 기록, 상점)와 같다.
 * 마이룸 탭은 하단 내비 없는 마이룸 화면으로 가는 지름길이라 탭 루트가 아니다(뒤로 가면 보던 탭으로 돌아온다).
 */
internal val mainTabPages: List<Page> = listOf(HomePage, MyRoomPage, HistoryPage, ShopPage)

/** 가운데 식사 기록 버튼 앞에 올 탭 수. 탭 네 개 가운데(마이룸과 기록 사이)에 둔다. */
internal const val MealRecordActionIndex = 2

/** [path] 화면이 [mainTabPages]에서 몇 번째인지. 탭 화면이 아니면 -1이다. */
internal fun mainTabIndexOf(path: String?): Int =
    mainTabPages.indexOfFirst { it.toRoute().path == path }

/** [page] 탭으로 갈 수 있는지. 기능 플래그가 꺼진 탭은 아직 준비 중이다. 홈과 기록은 늘 갈 수 있다. */
internal fun isMainTabReady(page: Page, flags: MainTabFlags): Boolean = when (page) {
    MyRoomPage -> flags.isMyRoomEnabled
    ShopPage -> flags.isShopEnabled
    else -> true
}

@Composable
private fun MessageEffect(
    messageEffectFlow: Flow<MessageEffect>,
    snackBarHostState: SnackbarHostState,
    onShowOneButtonDialog: (MessageEffect.ShowOneButtonDialog) -> Unit,
    onShowTwoButtonDialog: (MessageEffect.ShowTwoButtonDialog) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext

    LaunchedEffect(Unit) {
        messageEffectFlow.collect { effect ->
            when (effect) {
                is MessageEffect.ShowToastMsg -> Toast.makeText(
                    appContext,
                    effect.message,
                    Toast.LENGTH_LONG
                ).show()

                is MessageEffect.ShowSnackBarError -> {
                    // 버튼이 있으면 Material 기본값이 계속 띄워 두기라, 짧게(약 4초) 띄우도록 명시한다.
                    val result = snackBarHostState.showSnackbar(
                        message = effect.message,
                        actionLabel = effect.actionLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) effect.onAction?.invoke()
                }
                is MessageEffect.ShowOneButtonDialog -> onShowOneButtonDialog(effect)
                is MessageEffect.ShowTwoButtonDialog -> onShowTwoButtonDialog(effect)
            }
        }
    }
}

/**
 * 현재 화면에 맞춰 시스템 바 아이콘 색을 바꾼다. 앱 바탕은 밝아 기본은 어두운 아이콘이고,
 * 어두운 배경을 시스템 바 뒤까지 그리는 화면에서만 밝은 아이콘을 쓴다.
 */
@Composable
private fun SystemBarIconsEffect(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = LocalActivity.current?.window ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }
}
