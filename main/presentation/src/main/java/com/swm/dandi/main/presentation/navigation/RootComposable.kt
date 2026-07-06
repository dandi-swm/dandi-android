package com.swm.dandi.main.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.swm.dandi.common.domain.message.MessageEffect
import com.swm.dandi.common.presentation.component.ArchiText
import com.swm.dandi.common.presentation.helper.LocalMessageHelper
import com.swm.dandi.common.presentation.helper.LocalNavigationHelper
import com.swm.dandi.common.presentation.ui.theme.DesignSystemTheme
import com.swm.dandi.common.presentation.ui.theme.DesignSystemThemeImpl
import com.swm.dandi.sprite.domain.SpritePage
import kotlinx.coroutines.flow.Flow

@Composable
fun RootComposable(
    modifier: Modifier = Modifier,
    // TODO: 실제 앱 시작 route가 생기면 SpritePage 대신 그 route를 기본 시작 스택으로 교체한다.
    // startStack: List<NavKey> = emptyList(),
    startStack: List<NavKey> = listOf(GenericNavKey(SpritePage.PATH)),
) {
    val snackBarHostState = remember { SnackbarHostState() }
    var oneButtonDialogEffect by remember {
        mutableStateOf<MessageEffect.ShowOneButtonDialog?>(null)
    }

    DesignSystemTheme {
        val backStack = rememberNavBackStack(*startStack.toTypedArray())
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
                        ArchiText(
                            text = titleText,
                            style = DesignSystemThemeImpl.typeScale.titleStrongL,
                            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                            maxLines = Int.MAX_VALUE,
                        )
                    }
                },
                text = {
                    ArchiText(
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
                        ArchiText(
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
        ) { innerPadding ->
            AppNavHost(
                backStack = backStack,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

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
