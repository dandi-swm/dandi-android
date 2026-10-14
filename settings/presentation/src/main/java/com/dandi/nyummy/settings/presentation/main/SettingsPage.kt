package com.dandi.nyummy.settings.presentation.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.dandi.nyummy.common.domain.helper.AppPermission
import com.dandi.nyummy.common.entity.meal.Meal
import com.dandi.nyummy.common.entity.meal.MealTimesVO
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRow
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyListRowTrailing
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySectionCaption
import com.dandi.nyummy.common.presentation.designsystem.component.NyummySheetTitle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTextButtonTone
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyTopBar
import com.dandi.nyummy.common.presentation.designsystem.component.nyummyMealTimeText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import com.dandi.nyummy.common.presentation.permission.rememberPermissionRequester
import com.dandi.nyummy.legal.entity.LegalDocument
import com.dandi.nyummy.settings.presentation.R
import com.dandi.nyummy.common.presentation.R as CommonR

/**
 * 설정. 알림(앱 알림, 서비스 알림, 마케팅 알림, 식사 시간), 약관, 로그아웃.
 * "앱 알림"은 기기에서 이 앱의 알림을 허용했는지다. 켜면 시스템 알림 권한 팝업을 띄우고, 끄면 기기 알림 설정을 연다.
 * 서비스, 마케팅 알림은 서버에 저장된 값을 보여 주되, 앱 알림이 꺼져 있으면 꺼진 것으로 보이고 바꿀 수 없다.
 */
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 팝업 결과보다 실제 허용 여부를 본다. Android 12 이하는 팝업 없이 바로 끝나고, 기기 설정에서 끈 알림은 권한과 따로 꺼진다.
    val notificationPermission = rememberPermissionRequester {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.onIntent(SettingsIntent.DeviceNotificationRequestFinished(isDeviceNotificationEnabled = enabled))
    }
    LaunchedEffect(uiState.isDeviceNotificationRequested) {
        if (uiState.isDeviceNotificationRequested) notificationPermission.request(listOf(AppPermission.NOTIFICATION))
    }

    // 식사 시간을 고치거나 기기 설정에서 알림을 켜고 돌아오면 바로 보여야 하므로 보일 때마다 다시 읽는다.
    LifecycleResumeEffect(Unit) {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.onIntent(SettingsIntent.ScreenResumed(isDeviceNotificationEnabled = enabled))
        onPauseOrDispose { }
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
            NotificationSection(uiState, onIntent, onOpenDeviceSettings)
            LegalSection(onIntent)
            Spacer(Modifier.height(NyummyTheme.spacing.s24))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NyummyTextButton(
                    text = stringResource(R.string.settings_logout),
                    onClick = { onIntent(SettingsIntent.ClickLogout) },
                    tone = NyummyTextButtonTone.Danger,
                )
                NyummyTextButton(
                    text = stringResource(R.string.settings_withdraw),
                    onClick = { onIntent(SettingsIntent.ClickWithdraw) },
                    tone = NyummyTextButtonTone.Neutral,
                    size = NyummyTextButtonSize.S,
                )
            }

        }
    }

    if (uiState.isDeviceSettingsDialogVisible) {
        NyummyDialog(
            title = stringResource(R.string.settings_device_notification_off_title),
            body = stringResource(R.string.settings_device_notification_off_body),
            confirmText = stringResource(R.string.settings_open_device_settings),
            onConfirm = {
                onIntent(SettingsIntent.ConfirmDeviceSettings)
                onOpenDeviceSettings()
            },
            onDismissRequest = { onIntent(SettingsIntent.DismissDeviceSettingsDialog) },
        )
    }

    uiState.notificationDetail?.let { kind ->
        NotificationDetailSheet(
            kind = kind, onDismiss = { onIntent(SettingsIntent.DismissNotificationDetail) })
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

    if (uiState.isWithdrawDialogVisible) {
        NyummyDialog(
            title = stringResource(R.string.settings_withdraw_dialog_title),
            body = stringResource(R.string.settings_withdraw_dialog_body),
            confirmText = stringResource(R.string.settings_withdraw_dialog_confirm),
            onConfirm = { onIntent(SettingsIntent.ConfirmWithdraw) },
            onDismissRequest = { onIntent(SettingsIntent.DismissWithdrawDialog) },
            type = NyummyDialogType.Destructive,
            confirmEnabled = !uiState.isWithdrawing,
            dismissEnabled = !uiState.isWithdrawing,
        )
    }
}

/**
 * 알림 묶음. 맨 위 "앱 알림"(기기 알림 허용), 그 아래 서비스 알림과 마케팅 알림 토글(각각 "자세히 보기"), 식사 시간.
 * 앱 알림은 앱이 끌 수 없어 끄려고 하면 기기 알림 설정을 연다.
 * 서비스, 마케팅 토글은 서버 값을 보여 주되 앱 알림이 꺼져 있거나 설정 값을 읽기 전에는 막는다.
 */
@Composable
private fun NotificationSection(
    uiState: SettingsUIState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenDeviceSettings: () -> Unit,
) {
    val togglesEnabled = uiState.isLoaded && uiState.isDeviceNotificationEnabled
    NyummySectionCaption(title = stringResource(R.string.settings_section_notification))
    NyummyListRow(
        title = stringResource(R.string.settings_notification_app),
        subtitle = stringResource(R.string.settings_notification_app_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = uiState.isDeviceNotificationEnabled,
            onCheckedChange = { enabled ->
                if (enabled) onIntent(SettingsIntent.EnableAppNotification) else onOpenDeviceSettings()
            },
        ),
    )
    NyummyListRow(
        title = stringResource(R.string.settings_notification_service),
        subtitle = stringResource(R.string.settings_notification_service_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = uiState.isServiceOn,
            onCheckedChange = { onIntent(SettingsIntent.ToggleServicePush(it)) },
        ),
        enabled = togglesEnabled,
    )
    NotificationDetailButton { onIntent(SettingsIntent.ClickNotificationDetail(NotificationKind.SERVICE)) }
    NyummyListRow(
        title = stringResource(R.string.settings_notification_marketing),
        subtitle = stringResource(R.string.settings_notification_marketing_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = uiState.isMarketingOn,
            onCheckedChange = { onIntent(SettingsIntent.ToggleMarketingPush(it)) },
        ),
        enabled = togglesEnabled,
    )
    NotificationDetailButton { onIntent(SettingsIntent.ClickNotificationDetail(NotificationKind.MARKETING)) }
    NyummyListRow(
        title = stringResource(R.string.settings_meal_time),
        subtitle = uiState.mealTimes?.let { mealTimesSummary(it) },
        onClick = { onIntent(SettingsIntent.ClickMealTime) },
        enabled = uiState.isLoaded,
    )
}

/** 알림 묶음 토글 아래의 "자세히 보기". 기기 알림이 꺼져 있거나 읽기 전이어도 볼 수 있다. */
@Composable
private fun NotificationDetailButton(onClick: () -> Unit) {
    NyummyTextButton(
        text = stringResource(R.string.settings_notification_detail),
        onClick = onClick,
        tone = NyummyTextButtonTone.Neutral,
        size = NyummyTextButtonSize.S,
        trailingIcon = CommonR.drawable.nyummy_ic_chevron_right,
    )
}

/** 알림 묶음에 들어 있는 알림 하나와 언제 오는지. */
private data class IncludedPush(
    @StringRes val name: Int,
    @StringRes val description: Int,
)

/**
 * "자세히 보기" 시트에 밝히는 내용. 스토어 정책과 광고성 정보 전송 규정에 맞춰
 * 보내는 곳, 알림 목적, 알림 내용, 받지 않는(동의를 철회하는) 방법을 같은 틀로 보여 준다.
 */
private data class NotificationDisclosure(
    @StringRes val title: Int,
    @StringRes val purpose: Int,
    val contents: List<IncludedPush>,
    @StringRes val optOutLabel: Int,
    @StringRes val optOut: Int,
)

private val NotificationKind.disclosure: NotificationDisclosure
    get() = when (this) {
        NotificationKind.SERVICE -> NotificationDisclosure(
            title = R.string.settings_notification_service,
            purpose = R.string.settings_push_service_purpose,
            contents = listOf(
                IncludedPush(
                    R.string.settings_push_meal_time, R.string.settings_push_meal_time_desc
                ),
                IncludedPush(R.string.settings_push_analysis, R.string.settings_push_analysis_desc),
            ),
            optOutLabel = R.string.settings_push_service_opt_out_label,
            optOut = R.string.settings_push_service_opt_out,
        )

        NotificationKind.MARKETING -> NotificationDisclosure(
            title = R.string.settings_notification_marketing,
            purpose = R.string.settings_push_marketing_purpose,
            contents = listOf(
                IncludedPush(
                    R.string.settings_push_retention, R.string.settings_push_retention_desc
                ),
            ),
            optOutLabel = R.string.settings_push_marketing_opt_out_label,
            optOut = R.string.settings_push_marketing_opt_out,
        )
    }

/** "자세히 보기" 시트. 알림 묶음이 무엇을 위해 어떤 알림을 보내는지, 받지 않으려면 어떻게 하는지 밝힌다. */
@Composable
private fun NotificationDetailSheet(kind: NotificationKind, onDismiss: () -> Unit) {
    val disclosure = kind.disclosure
    NyummyBottomSheet(onDismissRequest = onDismiss) {
        NyummySheetTitle(text = stringResource(disclosure.title))
        Column {
            DisclosureRow(label = R.string.settings_push_detail_sender_label) {
                DisclosureValue(text = stringResource(R.string.settings_push_detail_sender))
            }
            NyummyDivider()
            DisclosureRow(label = R.string.settings_push_detail_purpose_label) {
                DisclosureValue(text = stringResource(disclosure.purpose))
            }
            NyummyDivider()
            DisclosureRow(label = R.string.settings_push_detail_contents_label) {
                Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s12)) {
                    disclosure.contents.forEach { push ->
                        Column(verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s2)) {
                            NyummyText(
                                text = stringResource(push.name),
                                style = NyummyTheme.typography.labelMStrong
                            )
                            NyummyText(
                                text = stringResource(push.description),
                                style = NyummyTheme.typography.bodyS,
                                color = NyummyTheme.colors.content.secondary,
                            )
                        }
                    }
                }
            }
            NyummyDivider()
            DisclosureRow(label = disclosure.optOutLabel) {
                DisclosureValue(text = stringResource(disclosure.optOut))
            }
        }
        NyummyButton(
            text = stringResource(R.string.settings_push_detail_confirm),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 시트의 항목 한 줄. 좁은 화면에서도 값이 잘리지 않게 이름을 위, 값을 아래에 둔다. */
@Composable
private fun DisclosureRow(@StringRes label: Int, value: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NyummyTheme.spacing.s12),
        verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s4),
    ) {
        NyummyText(
            text = stringResource(label),
            style = NyummyTheme.typography.labelM,
            color = NyummyTheme.colors.content.secondary,
        )
        value()
    }
}

@Composable
private fun DisclosureValue(text: String) {
    NyummyText(text = text, style = NyummyTheme.typography.bodyM)
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
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
            Settings.EXTRA_APP_PACKAGE, context.packageName
        )
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    }
    runCatching { context.startActivity(intent) }
}

private val ContentMaxWidth = 480.dp

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenPreview() {
    NyummyTheme {
        SettingsScreen(
            uiState = SettingsUIState(isLoaded = true),
            onIntent = {},
            onOpenDeviceSettings = {})
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsScreenDeviceOffPreview() {
    NyummyTheme {
        SettingsScreen(
            uiState = SettingsUIState(isLoaded = true, isDeviceNotificationEnabled = false),
            onIntent = {},
            onOpenDeviceSettings = {},
        )
    }
}
