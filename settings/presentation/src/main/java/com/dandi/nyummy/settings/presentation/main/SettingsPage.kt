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
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyBottomSheet
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDivider
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyInlineNotice
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
import com.dandi.nyummy.settings.entity.LegalDocument
import com.dandi.nyummy.settings.presentation.R

/**
 * 설정. 알림(서비스 알림, 마케팅 알림, 식사 시간), 약관, 로그아웃.
 * 기기에서 알림을 꺼 두었으면 기기 설정으로 가는 안내를 띄운다. 알림 토글은 계정 설정이라 그와 상관없이 바꿀 수 있다.
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

    uiState.notificationDetail?.let { kind ->
        NotificationDetailSheet(kind = kind, onDismiss = { onIntent(SettingsIntent.DismissNotificationDetail) })
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

/**
 * 알림 묶음. 서비스 알림과 마케팅 알림 토글(각각 "자세히 보기"), 그 아래 식사 시간.
 * 토글은 계정 단위 서버 값이라 기기 알림과 상관없이 그대로 보여 주고 바꿀 수 있다(마케팅 동의는 언제든 철회할 수 있어야 한다).
 * 기기 알림이 꺼져 있으면 안내와 기기 설정 버튼만 띄운다. 설정 값을 읽기 전에는 토글을 막는다.
 */
@Composable
private fun NotificationSection(
    uiState: SettingsUIState,
    onIntent: (SettingsIntent) -> Unit,
    onOpenDeviceSettings: () -> Unit,
) {
    val device = uiState.isDeviceNotificationEnabled
    val notification = uiState.notification
    val togglesEnabled = uiState.isLoaded
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
        title = stringResource(R.string.settings_notification_service),
        subtitle = stringResource(R.string.settings_notification_service_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = notification.isServiceEnabled,
            onCheckedChange = { onIntent(SettingsIntent.ToggleServicePush(it)) },
        ),
        enabled = togglesEnabled,
    )
    NotificationDetailButton { onIntent(SettingsIntent.ClickNotificationDetail(NotificationKind.SERVICE)) }
    NyummyListRow(
        title = stringResource(R.string.settings_notification_marketing),
        subtitle = stringResource(R.string.settings_notification_marketing_desc),
        trailing = NyummyListRowTrailing.Switch(
            checked = notification.isMarketingEnabled,
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
                IncludedPush(R.string.settings_push_meal_time, R.string.settings_push_meal_time_desc),
                IncludedPush(R.string.settings_push_analysis, R.string.settings_push_analysis_desc),
            ),
            optOutLabel = R.string.settings_push_service_opt_out_label,
            optOut = R.string.settings_push_service_opt_out,
        )
        NotificationKind.MARKETING -> NotificationDisclosure(
            title = R.string.settings_notification_marketing,
            purpose = R.string.settings_push_marketing_purpose,
            contents = listOf(
                IncludedPush(R.string.settings_push_retention, R.string.settings_push_retention_desc),
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
                            NyummyText(text = stringResource(push.name), style = NyummyTheme.typography.labelMStrong)
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
        SettingsScreen(uiState = SettingsUIState(isLoaded = true), onIntent = {}, onOpenDeviceSettings = {})
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
