package com.dandi.nyummy.main.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.home.domain.HomePage
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

    DesignSystemTheme {
        val backStack = rememberNavBackStack(*startStack.toTypedArray())
        val navigationHelper = LocalNavigationHelper.current
        // TODO: 매 recomposition마다 재생성됨. 각 탭 화면 구현 후 remember/top-level 상수로 호이스팅.
        // TODO: Home 외 탭은 화면 미구현으로 page=null(무동작). 화면 구현 후 각 Page 연결.
        val tabs = listOf(
            BottomNavTab(NyummyNavigationDestination.Home, HomePage),
            BottomNavTab(NyummyNavigationDestination.History, null),
            BottomNavTab(NyummyNavigationDestination.Quest, null),
            BottomNavTab(NyummyNavigationDestination.Collection, null),
            BottomNavTab(NyummyNavigationDestination.Shop, null)
        )
        val currentKey = backStack.lastOrNull() as? GenericNavKey
        val currentRoute = currentKey?.let { appRouteByPath[it.path] }
        val currentTab = tabs.firstOrNull { tab ->
            tab.page != null && tab.page.toRoute().path == currentKey?.path
        }

        val messageHelper = LocalMessageHelper.current

        val onShowOneButtonDialog = remember<(MessageEffect.ShowOneButtonDialog) -> Unit> {
            { oneButtonDialogEffect = it }
        }
        MessageEffect(
            messageEffectFlow = messageHelper.effect,
            snackBarHostState = snackBarHostState,
            onShowOneButtonDialog = onShowOneButtonDialog,
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

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1),
            snackbarHost = { SnackbarHost(snackBarHostState) },
            bottomBar = {
                if (currentRoute?.isBottomTab == true && currentTab != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        NyummyBottomNavigation(
                            selectedDestination = currentTab.destination,
                            onDestinationSelected = { destination ->
                                tabs.firstOrNull { it.destination == destination }
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
    // FIXME: Home 외 탭 화면 미구현으로 임시 nullable. 모든 탭 화면 구현 후 반드시 non-nullable(Page)로 되돌릴 것.
    val page: Page?,
)

@Composable
private fun MessageEffect(
    messageEffectFlow: Flow<MessageEffect>,
    snackBarHostState: SnackbarHostState,
    onShowOneButtonDialog: (MessageEffect.ShowOneButtonDialog) -> Unit,
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
            }
        }
    }
}
