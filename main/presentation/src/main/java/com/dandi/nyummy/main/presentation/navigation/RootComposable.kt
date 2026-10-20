package com.dandi.nyummy.main.presentation.navigation

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomNav
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyMainTabs
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySnackbarHost
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.helper.LocalMessageHelper
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.common.presentation.room.LocalRoomCardSize
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.home.presentation.homeRoomCardSize
import kotlinx.coroutines.flow.Flow

/**
 * 앱 뼈대(Scaffold, 하단 탭, 공통 다이얼로그와 스낵바, 화면 전환).
 *
 * @param onScreenShown 맨 위 화면의 경로가 바뀔 때마다 그 경로로 부른다. 사용 분석의 화면 조회 기록에 쓴다.
 */
@Composable
fun RootComposable(
    modifier: Modifier = Modifier,
    startStack: List<NavKey>,
    onScreenShown: (path: String) -> Unit = {},
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NyummyTheme.colors.bg.canvas,
        snackbarHost = {
            // Scaffold는 스낵바를 하단 탭 위나 contentWindowInsets의 하단 인셋만큼 올린다. 전체 화면 라우트는 인셋을 0으로
            // 두고 하단 탭도 없어, 직접 올리지 않으면 스낵바가 시스템 내비게이션 바(3버튼) 뒤에 깔린다.
            NyummySnackbarHost(
                hostState = snackBarHostState,
                modifier = if (drawsBehindSystemBars) Modifier.navigationBarsPadding() else Modifier,
            )
        },
        // 전체 화면 라우트는 시스템 바 뒤까지 그리고 인셋을 스스로 처리한다. 나머지는 Scaffold가 민다.
        contentWindowInsets = if (drawsBehindSystemBars) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (currentRoute?.isBottomTab == true && currentTabIndex >= 0) {
                // 내비가 시스템 내비게이션 바 영역까지 흰 바탕을 이어 그린다.
                NyummyBottomNav(
                    items = NyummyMainTabs,
                    selectedIndex = currentTabIndex,
                    onSelect = { index -> navigationHelper.navigateTo(mainTabPages[index]) },
                )
            }
        }
    ) { innerPadding ->
        // 마이룸은 홈 고양이방 카드와 같은 크기로 방을 보여 준다. 딥링크로 홈을 거치지 않고 들어와도 같도록 창 크기로 늘 셈해 둔다.
        CompositionLocalProvider(LocalRoomCardSize provides homeRoomCardSizeInWindow()) {
            AppNavHost(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/**
 * 홈이 받는 자리로 셈한 고양이방 카드 크기. 홈은 Scaffold 안에서 시스템 바와 하단 내비(내비게이션 바 영역까지)를 뺀
 * 자리를 받는다. 지금 어느 화면이든 창 크기로 세므로 홈을 한 번도 그리지 않았어도 같은 값이다.
 */
@Composable
private fun homeRoomCardSizeInWindow(): DpSize {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val window = LocalWindowInfo.current.containerSize
    val systemBars = ScaffoldDefaults.contentWindowInsets
    val navigationBars = WindowInsets.navigationBars
    val width = with(density) {
        (window.width - systemBars.getLeft(density, layoutDirection) - systemBars.getRight(density, layoutDirection)).toDp()
    }
    val height = with(density) {
        (window.height - systemBars.getTop(density) - navigationBars.getBottom(density)).toDp()
    } - NyummyTheme.size.bottomNav
    return homeRoomCardSize(width, height)
}

/** 하단 내비 탭 순서대로 이동할 화면. [NyummyMainTabs]의 순서(홈, 기록)와 같다. */
internal val mainTabPages: List<Page> = listOf(HomePage, HistoryPage)

/** [path] 화면이 몇 번째 탭인지. 탭 화면이 아니면 -1이다. */
internal fun mainTabIndexOf(path: String?): Int =
    mainTabPages.indexOfFirst { it.toRoute().path == path }

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
