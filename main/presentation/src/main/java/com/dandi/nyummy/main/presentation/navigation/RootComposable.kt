package com.dandi.nyummy.main.presentation.navigation

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.dandi.nyummy.achievement.domain.AchievementPage
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
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.quest.domain.QuestPage
import com.dandi.nyummy.shop.domain.ShopPage
import kotlinx.coroutines.flow.Flow

@Composable
fun RootComposable(
    modifier: Modifier = Modifier,
    startStack: List<NavKey>,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var oneButtonDialogEffect by remember {
        mutableStateOf<MessageEffect.ShowOneButtonDialog?>(null)
    }
    var twoButtonDialogEffect by remember {
        mutableStateOf<MessageEffect.ShowTwoButtonDialog?>(null)
    }

    DesignSystemTheme {
        val backStack = rememberNavBackStack(*startStack.toTypedArray())
        val navigationHelper = LocalNavigationHelper.current
        val currentKey = backStack.lastOrNull() as? GenericNavKey
        val currentRoute = currentKey?.let { appRouteByPath[it.path] }
        SystemBarIconsEffect(lightIcons = currentRoute?.usesLightSystemBarIcons == true)
        val currentTabIndex = mainTabIndexOf(currentKey?.path)

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

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = NyummyTheme.colors.bg.canvas,
            snackbarHost = { NyummySnackbarHost(snackBarHostState) },
            // 전체 화면 라우트는 시스템 바 뒤까지 그리고 인셋을 스스로 처리한다. 나머지는 Scaffold가 민다.
            contentWindowInsets = if (currentRoute?.drawsBehindSystemBars == true) {
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
            AppNavHost(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

/** 하단 내비 탭 순서대로 이동할 화면. [NyummyMainTabs]의 순서(홈, 기록, 퀘스트, 업적, 상점)와 같다. */
internal val mainTabPages: List<Page> = listOf(HomePage, HistoryPage, QuestPage, AchievementPage, ShopPage)

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
