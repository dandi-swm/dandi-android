package com.dandi.nyummy.settings.presentation.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNotice
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionCaption
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.component.nyummyMealTimeText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.settings.entity.LegalDocument
import com.dandi.nyummy.settings.presentation.R

/**
 * 설정. 소리(배경음), 알림(전체, 식사 기록, 식사 시간, 중요 공지), 약관, 로그아웃.
 * 기기에서 알림을 꺼 두었으면 알림 토글을 막고 기기 설정으로 가는 안내를 띄운다.
 */
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 식사 시간을 고치거나 기기 설정에서 알림을 켜고 돌아오면 바로 보여야 하므로 보일 때마다 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = enabled))
        onPauseOrDispose { }
    }

    LaunchedEffect(uiState.pendingLegalDocument) {
        val document = uiState.pendingLegalDocument ?: return@LaunchedEffect
        val opened = openUrl(context, document.url)
        viewModel.onIntent(SettingsIntent.LegalLinkHandled(document, opened))
    }

    SettingsScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onOpenDeviceSettings = { openNotificationSettings(context) },
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUIState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenDeviceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas),
    ) {
        NyummyTopBar(
            title = stringResource(R.string.settings_title),
            onBackClick = { onIntent(SettingsIntent.ClickBack) },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = ContentMaxWidth)
                .padding(horizontal = NyummyTheme.spacing.gutter)
                .padding(bottom = NyummyTheme.spacing.s24),
        ) {
            SoundSection(uiState, onIntent)
            NotificationSection(uiState, onIntent, onOpenDeviceSettings)
            LegalSection(onIntent)
            Spacer(Modifier.height(NyummyTheme.spacing.s24))
            NyummyTextButton(
                text = stringResource(R.string.settings_logout),
                onClick = { onIntent(SettingsIntent.ClickLogout) },
                tone = NyummyTextButtonTone.Danger,
            )
        }
    }

    if (uiState.isLogoutDialogVisible) {
        NyummyDialog(
            title = stringResource(R.string.settings_logout_dialog_title),
            body = stringResource(R.string.settings_logout_dialog_body),
            confirmText = stringResource(R.string.settings_logout_dialog_confirm),
            onConfirm = { onIntent(SettingsIntent.ConfirmLogout) },
            onDismissRequest = { onIntent(SettingsIntent.DismissLogoutDialog) },
            confirmEnabled = !uiState.isLoggingOut,
            dismissEnabled = !uiState.isLoggingOut,
        )
    }
}

@Composable
private fun SoundSection(uiState: SettingsUIState, onIntent: (SettingsIntent) -> Unit) {
    NyummySectionCaption(title = stringResource(R.string.settings_section_sound))
    NyummyListRow(
        title = stringResource(R.string.settings_bgm),
        subtitle = stringResource(R.string.settings_bgm_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = uiState.isBgmEnabled,
            onCheckedChange = { onIntent(SettingsIntent.ToggleBgm(it)) },
        ),
    )
}

/**
 * 알림 묶음. 전체 알림을 끄면 식사 기록, 중요 공지 토글은 값을 그대로 둔 채 막는다.
 * 기기 알림이 꺼져 있으면 세 토글을 모두 꺼진 모습으로 막는다.
 */
@Composable
private fun NotificationSection(
    uiState: SettingsUIState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenDeviceSettings: () -> Unit,
) {
    val device = uiState.isDeviceNotificationEnabled
    val notification = uiState.notification
    val subEnabled = device && notification.isAllEnabled
    NyummySectionCaption(title = stringResource(R.string.settings_section_notification))
    if (!device) {
        NyummyInlineNotice(
            title = stringResource(R.string.settings_device_notification_off_title),
            body = stringResource(R.string.settings_device_notification_off_body),
        )
        NyummyTextButton(
            text = stringResource(R.string.settings_open_device_settings),
            onClick = onOpenDeviceSettings,
            size = NyummyTextButtonSize.S,
            trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
        )
    }
    NyummyListRow(
        title = stringResource(R.string.settings_notification_all),
        trailing = NyummyListRowTrailing.Switch(
            checked = device && notification.isAllEnabled,
            onCheckedChange = { onIntent(SettingsIntent.ToggleAllNotification(it)) },
        ),
        enabled = device,
    )
    NyummyListRow(
        title = stringResource(R.string.settings_notification_meal),
        subtitle = stringResource(R.string.settings_notification_meal_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = device && notification.isMealReminderEnabled,
            onCheckedChange = { onIntent(SettingsIntent.ToggleMealReminder(it)) },
        ),
        enabled = subEnabled,
    )
    NyummyListRow(
        title = stringResource(R.string.settings_meal_time),
        subtitle = mealTimesSummary(uiState.mealTimes),
        onClick = { onIntent(SettingsIntent.ClickMealTime) },
    )
    NyummyListRow(
        title = stringResource(R.string.settings_notification_notice),
        subtitle = stringResource(R.string.settings_notification_notice_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = device && notification.isNoticeEnabled,
            onCheckedChange = { onIntent(SettingsIntent.ToggleNotice(it)) },
        ),
        enabled = subEnabled,
    )
}

@Composable
private fun LegalSection(onIntent: (SettingsIntent) -> Unit) {
    NyummySectionCaption(title = stringResource(R.string.settings_section_legal))
    NyummyListRow(
        title = stringResource(R.string.settings_terms),
        onClick = { onIntent(SettingsIntent.ClickLegal(LegalDocument.TERMS)) },
    )
    NyummyListRow(
        title = stringResource(R.string.settings_privacy),
        onClick = { onIntent(SettingsIntent.ClickLegal(LegalDocument.PRIVACY)) },
    )
}

/** "오전 8시, 오후 12시, 오후 6시". 안 먹는 끼니는 뺀다. */
@Composable
private fun mealTimesSummary(mealTimes: MealTimesVO): String {
    val times = Meal.entries.map { mealTimes[it] }.filterNot { it.isSkipped }
    if (times.isEmpty()) return stringResource(R.string.settings_meal_time_none)
    return times.map { nyummyMealTimeText(it) }.joinToString(", ")
}

/** 이 앱의 기기 알림 설정 화면을 연다. Android 8 미만은 앱 정보 화면으로 간다. */
private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    runCatching { context.startActivity(intent) }
}

/** 브라우저로 연다. 주소가 없거나 열 앱이 없으면 false. */
private fun openUrl(context: Context, url: String): Boolean {
    if (url.isBlank()) return false
    return runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
}

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    NyummyTheme {
        SettingsScreen(uiState = SettingsUIState.empty, onIntent = {}, onOpenDeviceSettings = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenDeviceOffPreview() {
    NyummyTheme {
        SettingsScreen(
            uiState = SettingsUIState(isDeviceNotificationEnabled = false),
            onIntent = {},
            onOpenDeviceSettings = {},
        )
    }
}
