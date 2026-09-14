package com.dandi.nyummy.main.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.dandi.nyummy.common.domain.message.MessageEffect
import com.dandi.nyummy.common.domain.navigation.Page
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyBottomNavigation
import com.dandi.nyummy.common.presentation.component.NyummyNavigationDestination
import com.dandi.nyummy.common.presentation.helper.LocalMessageHelper
import com.dandi.nyummy.common.presentation.helper.LocalNavigationHelper
import com.dandi.nyummy.collection.domain.CollectionPage
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.history.domain.HistoryPage
import com.dandi.nyummy.home.domain.HomePage
import com.dandi.nyummy.meal.domain.MealRecordPage
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
        val currentTab = bottomNavTabs.firstOrNull { tab ->
            tab.page.toRoute().path == currentKey?.path
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

        oneButtonDialogEffect?.let { dialog ->
            AlertDialog(
                onDismissRequest = {
                    if (!dialog.cantIgnore) oneButtonDialogEffect = null
                },
                title = dialog.titleText?.let { titleText ->
                    {
                        DandiText(
                            text = titleText,
                            style = DesignSystemThemeImpl.typeScale.titleStrongL,
                            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                            maxLines = Int.MAX_VALUE,
                        )
                    }
                },
                text = {
                    DandiText(
                        text = dialog.descText,
                        style = DesignSystemThemeImpl.typeScale.textRegularL,
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                        maxLines = Int.MAX_VALUE,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog.onClickButton?.invoke()
                            oneButtonDialogEffect = null
                        }
                    ) {
                        DandiText(
                            text = dialog.buttonText,
                            style = DesignSystemThemeImpl.typeScale.textStrongL,
                            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
                        )
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = !dialog.cantIgnore,
                    dismissOnClickOutside = !dialog.cantIgnore,
                ),
            )
        }

        twoButtonDialogEffect?.let { dialog ->
            AlertDialog(
                onDismissRequest = {
                    if (!dialog.cantIgnore) twoButtonDialogEffect = null
                },
                title = dialog.titleText?.let { titleText ->
                    {
                        DandiText(
                            text = titleText,
                            style = DesignSystemThemeImpl.typeScale.titleStrongL,
                            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                            maxLines = Int.MAX_VALUE,
                        )
                    }
                },
                text = {
                    DandiText(
                        text = dialog.descText,
                        style = DesignSystemThemeImpl.typeScale.textRegularL,
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                        maxLines = Int.MAX_VALUE,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dialog.onClickRightButton?.invoke()
                            twoButtonDialogEffect = null
                        }
                    ) {
                        DandiText(
                            text = dialog.rightButtonText,
                            style = DesignSystemThemeImpl.typeScale.textStrongL,
                            color = DesignSystemThemeImpl.designSystemColor.contentAccent,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            dialog.onClickLeftButton?.invoke()
                            twoButtonDialogEffect = null
                        }
                    ) {
                        DandiText(
                            text = dialog.leftButtonText,
                            style = DesignSystemThemeImpl.typeScale.textStrongL,
                            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                        )
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = !dialog.cantIgnore,
                    dismissOnClickOutside = !dialog.cantIgnore,
                ),
            )
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            // 탭 화면들이 칠하는 배경(bgSurfaceIvory)과 동일하게 맞춰, 플로팅 바텀 네비 주변이
            // 사각형 띠처럼 달라 보이지 않게 한다.
            containerColor = DesignSystemThemeImpl.designSystemColor.bgSurfaceIvory,
            snackbarHost = { SnackbarHost(snackBarHostState) },
            bottomBar = {
                if (currentRoute?.isBottomTab == true && currentTab != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = DesignSystemThemeImpl.designSystemSpacing.space8),
                        contentAlignment = Alignment.Center,
                    ) {
                        NyummyBottomNavigation(
                            selectedDestination = currentTab.destination,
                            onCameraClick = { navigationHelper.navigateTo(MealRecordPage) },
                            onDestinationSelected = { destination ->
                                bottomNavTabs.firstOrNull { it.destination == destination }
                                    ?.page
                                    ?.let { navigationHelper.navigateTo(it) }
                            },
                        )
                    }
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

private data class BottomNavTab(
    val destination: NyummyNavigationDestination,
    val page: Page,
)

/** 하단 내비게이션 탭 ↔ 화면 매핑. Page object 는 전역 싱글턴이므로 top-level 상수로 둔다. */
private val bottomNavTabs = listOf(
    BottomNavTab(NyummyNavigationDestination.Home, HomePage),
    BottomNavTab(NyummyNavigationDestination.History, HistoryPage),
    BottomNavTab(NyummyNavigationDestination.Collection, CollectionPage),
    BottomNavTab(NyummyNavigationDestination.Shop, ShopPage),
)

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

                is MessageEffect.ShowSnackBarError -> snackBarHostState.showSnackbar(effect.message)
                is MessageEffect.ShowOneButtonDialog -> onShowOneButtonDialog(effect)
                is MessageEffect.ShowTwoButtonDialog -> onShowTwoButtonDialog(effect)
            }
        }
    }
}
