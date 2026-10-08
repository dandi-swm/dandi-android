package com.dandi.nyummy.common.presentation.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.designsystem.foundation.NyummyComponentDimens
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme

enum class NyummyDialogType {
    /** 보조 "닫기" + 주 버튼. */
    Confirm,

    /** 주 버튼 하나가 꽉 찬 너비. */
    Alert,

    /** 보조 "닫기" + 빨간 주 버튼. 되돌릴 수 없는 행동에만 쓴다. */
    Destructive,
}

/**
 * 다이얼로그. 아이콘, 색 배지, 캐릭터 없이 제목, 본문, 버튼만 둔다. 왼쪽 정렬, 그림자 없음, 딤 40%.
 * radius l, 여백 20(위 24), 최대 너비 320(좌우 여백 32).
 *
 * 버튼 라벨은 동사로 쓰고, 닫는 버튼은 "닫기"로 쓴다. 되돌릴 수 있는 행동은 다이얼로그 대신 되돌리기 스낵바를 쓴다.
 * [content]에는 이름 수정 같은 입력칸을 넣는다(본문 아래).
 *
 * [onDismissRequest]는 바깥 탭과 뒤로 가기에, [onDismissClick]은 보조 버튼에 쓰인다(기본은 같은 동작).
 * 닫을 수 없는 다이얼로그(강제 업데이트 등)는 [dismissible]을 false로 둔다.
 * 요청을 보내는 중처럼 보조 버튼을 잠시 막아야 하면 [dismissEnabled]를 false로 둔다.
 */
@Composable
fun NyummyDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    type: NyummyDialogType = NyummyDialogType.Confirm,
    body: String? = null,
    dismissText: String = stringResource(R.string.nyummy_dialog_dismiss),
    onDismissClick: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    NyummyDialogWindow(onDismissRequest = onDismissRequest, dismissible = dismissible) {
        NyummyDialogCard(
            title = title,
            confirmText = confirmText,
            onConfirm = onConfirm,
            onDismiss = onDismissClick,
            modifier = modifier,
            type = type,
            body = body,
            dismissText = dismissText,
            confirmEnabled = confirmEnabled,
            dismissEnabled = dismissEnabled,
            content = content,
        )
    }
}

/** 다이얼로그 카드 본체. 창 없이 그려 프리뷰와 테스트에서 쓴다. */
@Composable
internal fun NyummyDialogCard(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    type: NyummyDialogType = NyummyDialogType.Confirm,
    body: String? = null,
    dismissText: String = stringResource(R.string.nyummy_dialog_dismiss),
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .widthIn(max = NyummyComponentDimens.DialogMaxWidth)
            .fillMaxWidth()
            .background(NyummyTheme.colors.bg.surface, RoundedCornerShape(NyummyTheme.radius.l))
            .semantics { paneTitle = title }
            .padding(
                start = NyummyTheme.spacing.s20,
                end = NyummyTheme.spacing.s20,
                top = NyummyTheme.spacing.s24,
                bottom = NyummyTheme.spacing.s20,
            ),
    ) {
        // 긴 본문이나 큰 글꼴에서도 버튼 행이 밀려나지 않게, 제목과 본문과 슬롯만 스크롤한다.
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            NyummyText(text = title, style = NyummyTheme.typography.titleL)
            if (body != null) {
                NyummyText(
                    text = body,
                    style = NyummyTheme.typography.bodyL,
                    color = NyummyTheme.colors.content.secondary,
                    modifier = Modifier.padding(top = NyummyTheme.spacing.s8),
                )
            }
            if (content != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = NyummyTheme.spacing.s8),
                    content = content,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = NyummyTheme.spacing.s20),
            horizontalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s8),
        ) {
            if (type != NyummyDialogType.Alert) {
                NyummyButton(
                    text = dismissText,
                    onClick = onDismiss,
                    style = NyummyButtonStyle.Secondary,
                    size = NyummyButtonSize.M,
                    enabled = dismissEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
            NyummyButton(
                text = confirmText,
                onClick = onConfirm,
                style = if (type == NyummyDialogType.Destructive) NyummyButtonStyle.Danger else NyummyButtonStyle.Primary,
                size = NyummyButtonSize.M,
                enabled = confirmEnabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 플랫폼 기본 딤 대신 bg/scrim(40%)을 직접 깐 전체 화면 다이얼로그 창.
 * 바깥을 누르거나 뒤로 가기를 누르면 [onDismissRequest]가 호출된다.
 */
@Composable
internal fun NyummyDialogWindow(
    onDismissRequest: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dismiss = if (dismissible) onDismissRequest else ({})
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NyummyTheme.colors.bg.scrim)
                // 바깥 탭으로 닫는다. clickable은 다이얼로그 전체 semantics를 병합하므로 포인터 입력만 받는다.
                // 접근성 사용자는 뒤로 가기로 닫는다.
                .pointerInput(dismiss) { detectTapGestures { dismiss() } }
                // 딤은 전체 화면에 깔고, 카드는 시스템 바와 키보드를 뺀 영역 안에 둔다.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = NyummyComponentDimens.DialogScreenMargin, vertical = NyummyTheme.spacing.s24),
            contentAlignment = Alignment.Center,
        ) {
            // 카드 안쪽을 눌러도 바깥 클릭으로 닫히지 않게 탭을 소비한다.
            // clickable을 쓰면 카드 semantics가 버튼으로 병합되어 paneTitle과 충돌하므로 포인터 입력만 받는다.
            Box(modifier = Modifier.pointerInput(Unit) { detectTapGestures { } }) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF9E9E9E, widthDp = 390)
@Composable
private fun NyummyDialogPreview() {
    NyummyTheme {
        Column(
            modifier = Modifier.padding(NyummyTheme.spacing.s32),
            verticalArrangement = Arrangement.spacedBy(NyummyTheme.spacing.s16),
        ) {
            NyummyDialogCard(title = "기록을 그만둘까요?", body = "지금 나가면 찍은 사진이 사라져요.", confirmText = "나가기", onConfirm = {}, onDismiss = {})
            NyummyDialogCard(title = "전송하지 못했어요", body = "네트워크를 확인해 주세요.", confirmText = "다시 시도", onConfirm = {}, onDismiss = {}, type = NyummyDialogType.Alert)
            NyummyDialogCard(title = "기록을 삭제할까요?", body = "삭제한 기록은 되돌릴 수 없어요.", confirmText = "삭제하기", onConfirm = {}, onDismiss = {}, type = NyummyDialogType.Destructive)
        }
    }
}
