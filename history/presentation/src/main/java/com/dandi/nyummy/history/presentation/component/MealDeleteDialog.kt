package com.dandi.nyummy.history.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialog
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyDialogType
import com.dandi.nyummy.history.presentation.R

/**
 * 식사 기록 삭제 확인(빨간 주 버튼). 상세 페이지와 기록 화면의 분석 실패 줄이 함께 쓴다.
 * 지우는 동안에는 버튼과 닫기를 막아 결과가 엇갈리지 않게 한다.
 */
@Composable
internal fun MealDeleteDialog(
    mealName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    NyummyDialog(
        type = NyummyDialogType.Destructive,
        title = if (mealName.isBlank()) {
            stringResource(R.string.history_delete_dialog_title_unnamed)
        } else {
            stringResource(R.string.history_delete_dialog_title, mealName)
        },
        body = stringResource(R.string.history_delete_dialog_body),
        confirmText = stringResource(R.string.history_delete_dialog_confirm),
        onConfirm = onConfirm,
        onDismissRequest = onCancel,
        confirmEnabled = !isDeleting,
        dismissEnabled = !isDeleting,
        dismissible = !isDeleting,
    )
}
