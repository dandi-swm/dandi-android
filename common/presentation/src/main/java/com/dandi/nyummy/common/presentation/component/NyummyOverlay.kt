package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import com.dandi.nyummy.common.presentation.ui.theme.designSystemDropShadow

/**
 * Canonical Edit dialog from Figma node 956:522.
 *
 * This composable owns only the dialog surface. Place it above [NyummyModalScrim] in the host;
 * no platform dim, back-dismiss, or outside-click policy is introduced here.
 */
@Composable
fun NyummyEditDialog(
    title: String,
    modifier: Modifier = Modifier,
    fieldLabel: String,
    fieldValue: String,
    onFieldValueChange: (String) -> Unit,
    timeLabel: String,
    errorMessage: String? = null,
    cancelLabel: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    NyummyDialogContainer(
        title = title,
        modifier = modifier,
        width = EditDialogWidth,
        height = EditDialogHeight,
        testTag = EditDialogTag,
        hasShadow = true,
    ) {
        DandiText(
            text = title,
            modifier = Modifier
                .figmaFrame(x = 20, y = 21, width = 220, height = 28)
                .semantics { heading() },
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularL,
        )

        BasicTextField(
            value = fieldValue,
            onValueChange = onFieldValueChange,
            modifier = Modifier
                .figmaFrame(x = 20, y = 64, width = 294, height = 58)
                .background(colors.bgInputDefault, RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16))
                .border(
                    BorderStroke(OverlayBorderWidth, colors.borderInputDefault),
                    RoundedCornerShape(DesignSystemThemeImpl.designSystemRadius.radius16),
                )
                .semantics { contentDescription = fieldLabel },
            textStyle = DesignSystemThemeImpl.typeScale.textStrongL.copy(
                color = colors.contentDefaultLevel0,
            ),
            singleLine = true,
            cursorBrush = SolidColor(colors.contentAccentSage),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize()) {
                    DandiText(
                        text = fieldLabel,
                        modifier = Modifier.figmaFrame(x = 13, y = 6, width = 100, height = 18),
                        color = colors.contentDefaultLevel2,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                    Box(
                        modifier = Modifier.figmaFrame(x = 13, y = 24, width = 260, height = 24),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        innerTextField()
                    }
                }
            },
        )

        Surface(
            modifier = Modifier.figmaFrame(x = 20, y = 134, width = 294, height = 40),
            shape = DesignSystemThemeImpl.designSystemShape.inputDefault,
            color = colors.bgWarningSoft,
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                DandiText(
                    text = timeLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = DesignSystemThemeImpl.designSystemSpacing.space12),
                    color = colors.contentWarning,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
            }
        }

        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .figmaFrame(x = 20, y = 186, width = 294, height = 46)
                    .semantics(mergeDescendants = true) {},
                shape = RoundedCornerShape(ErrorPanelRadius),
                color = colors.bgDangerSoft,
                border = BorderStroke(OverlayBorderWidth, colors.borderDangerDefault),
            ) {
                Box {
                    DandiText(
                        text = ErrorMarker,
                        modifier = Modifier.figmaFrame(x = 11, y = 11, width = 22, height = 22),
                        color = colors.contentIconDanger,
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                        textAlign = TextAlign.Center,
                    )
                    DandiText(
                        text = errorMessage,
                        modifier = Modifier.figmaFrame(x = 37, y = 14, width = 248, height = 18),
                        color = colors.contentError,
                        style = DesignSystemThemeImpl.typeScale.labelStrongS,
                    )
                }
            }
        }

        OverlayActionButton(
            label = cancelLabel,
            modifier = Modifier.figmaFrame(x = 20, y = 242, width = 136, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            style = OverlayActionStyle.Secondary,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = DialogCancelTag,
            onClick = onCancel,
        )
        OverlayActionButton(
            label = confirmLabel,
            modifier = Modifier.figmaFrame(x = 166, y = 242, width = 148, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            style = OverlayActionStyle.Primary,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = DialogConfirmTag,
            onClick = onConfirm,
        )
    }
}

/** Canonical destructive retry dialog from Figma node 956:533. */
@Composable
fun NyummyDestructiveDialog(
    title: String,
    modifier: Modifier = Modifier,
    body: String,
    targetLabel: String,
    helper: String,
    cancelLabel: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    NyummyDialogContainer(
        title = title,
        modifier = modifier,
        width = DestructiveDialogWidth,
        height = DestructiveDialogHeight,
        testTag = DestructiveDialogTag,
        hasShadow = true,
    ) {
        DandiText(
            text = title,
            modifier = Modifier
                .figmaFrame(x = 20, y = 22, width = 294, height = 28)
                .semantics { heading() },
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularL,
        )
        DandiText(
            text = body,
            modifier = Modifier.figmaFrame(x = 20, y = 64, width = 294, height = 36),
            color = colors.contentDefaultLevel1,
            maxLines = 2,
            overflow = TextOverflow.Clip,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Surface(
            modifier = Modifier.figmaFrame(x = 20, y = 124, width = 294, height = 42),
            shape = DesignSystemThemeImpl.designSystemShape.inputDefault,
            color = colors.bgDangerSoft,
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                DandiText(
                    text = targetLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = DesignSystemThemeImpl.designSystemSpacing.space12),
                    color = colors.contentError,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
            }
        }
        DandiText(
            text = helper,
            modifier = Modifier.figmaFrame(x = 20, y = 178, width = 294, height = 18),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        OverlayActionButton(
            label = cancelLabel,
            modifier = Modifier.figmaFrame(x = 20, y = 223, width = 136, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            style = OverlayActionStyle.Secondary,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = DialogCancelTag,
            onClick = onCancel,
        )
        OverlayActionButton(
            label = confirmLabel,
            modifier = Modifier.figmaFrame(x = 166, y = 223, width = 148, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            style = OverlayActionStyle.Danger,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = DialogConfirmTag,
            onClick = onConfirm,
        )
    }
}

/** Canonical notice dialog from Figma node 956:562. */
@Composable
fun NyummyNoticeDialog(
    title: String,
    modifier: Modifier = Modifier,
    typeLabel: String,
    selectedPage: Int,
    pageCount: Int,
    periodLabel: String,
    bodyTitle: String,
    body: String,
    footer: String,
    artworkDescription: String,
    artwork: @Composable BoxScope.() -> Unit,
    benefitsContent: @Composable RowScope.() -> Unit,
    primaryLabel: String,
    nextLabel: String,
    closeLabel: String,
    onPrimary: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    NyummyDialogContainer(
        title = title,
        modifier = modifier,
        width = NoticeDialogWidth,
        height = NoticeDialogHeight,
        testTag = NoticeDialogTag,
        hasShadow = false,
        border = BorderStroke(OverlayBorderWidth, colors.borderCardSubtle),
    ) {
        Surface(
            modifier = Modifier.figmaFrame(x = 19, y = 19, width = 100, height = 28),
            shape = RoundedCornerShape(NoticeTypeRadius),
            color = colors.bgBrandSoft,
        ) {
            Box(contentAlignment = Alignment.Center) {
                DandiText(
                    text = typeLabel,
                    color = colors.contentAccent,
                    style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                    textAlign = TextAlign.Center,
                )
            }
        }
        DandiText(
            text = pagePositionLabel(selectedPage = selectedPage, pageCount = pageCount),
            modifier = Modifier.figmaFrame(x = 193, y = 24, width = 56, height = 18),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
            textAlign = TextAlign.Center,
        )
        DandiText(
            text = title,
            modifier = Modifier
                .figmaFrame(x = 19, y = 68, width = 302, height = 32)
                .semantics { heading() },
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        DandiText(
            text = periodLabel,
            modifier = Modifier.figmaFrame(x = 19, y = 117, width = 302, height = 18),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )

        Surface(
            modifier = Modifier.figmaFrame(x = 19, y = 147, width = 302, height = 286),
            shape = DesignSystemThemeImpl.designSystemShape.sheetDefault,
            color = colors.bgWarningSoft,
            border = BorderStroke(OverlayBorderWidth, colors.borderDefaultLevel1),
        ) {
            Box {
                DandiText(
                    text = bodyTitle,
                    modifier = Modifier.figmaFrame(x = 15, y = 13, width = 170, height = 56),
                    color = colors.contentDefaultLevel0,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    style = DesignSystemThemeImpl.typeScale.displayRegularL,
                )
                DandiText(
                    text = body,
                    modifier = Modifier.figmaFrame(x = 15, y = 81, width = 174, height = 36),
                    color = colors.contentDefaultLevel0,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                )
                Box(
                    modifier = Modifier
                        .figmaFrame(x = 171, y = 7, width = 120, height = 132)
                        .semantics(mergeDescendants = true) {
                            contentDescription = artworkDescription
                        },
                    content = artwork,
                )
                Surface(
                    modifier = Modifier.figmaFrame(x = 13, y = 147, width = 274, height = 72),
                    shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
                    color = colors.bgSurfaceSubtle,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        content = benefitsContent,
                    )
                }
                DandiText(
                    text = footer,
                    modifier = Modifier.figmaFrame(x = 15, y = 233, width = 270, height = 36),
                    color = colors.contentDefaultLevel1,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    style = DesignSystemThemeImpl.typeScale.textRegularS,
                )
            }
        }

        NoticePager(
            selectedPage = selectedPage,
            pageCount = pageCount,
            modifier = Modifier.figmaFrame(x = 125, y = 447, width = 90, height = 16),
        )
        OverlayActionButton(
            label = primaryLabel,
            modifier = Modifier.figmaFrame(x = 19, y = 481, width = 302, height = 52),
            visualHeight = LargeActionHeight,
            shape = RoundedCornerShape(LargeActionRadius),
            style = OverlayActionStyle.Primary,
            textStyle = DesignSystemThemeImpl.typeScale.textStrongL,
            testTag = NoticePrimaryTag,
            onClick = onPrimary,
        )
        OverlayActionButton(
            label = nextLabel,
            modifier = Modifier.figmaFrame(x = 95, y = 541, width = 150, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
            style = OverlayActionStyle.Ghost,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = NoticeNextTag,
            onClick = onNext,
        )
        OverlayActionButton(
            label = closeLabel,
            modifier = Modifier.figmaFrame(x = 261, y = 9, width = 60, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            style = OverlayActionStyle.OutlinedSecondary,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = NoticeCloseTag,
            onClick = onClose,
        )
    }
}

/** Canonical draw-confirmation sheet from Figma node 956:582. */
@Composable
fun NyummyConfirmBottomSheet(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String,
    subtitle: String,
    productArtDescription: String,
    productArt: @Composable BoxScope.() -> Unit,
    balanceLabel: String,
    balanceValue: String,
    balanceHelper: String,
    cancelLabel: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    NyummySheetContainer(
        title = title,
        modifier = modifier,
        width = StandardSheetWidth,
        height = ConfirmSheetHeight,
        testTag = ConfirmSheetTag,
    ) {
        SheetHandle(modifier = Modifier.figmaFrame(x = 171, y = 10, width = 48, height = 4))
        Box(
            modifier = Modifier
                .figmaFrame(x = 24, y = 34, width = 92, height = 76)
                .semantics(mergeDescendants = true) {
                    contentDescription = productArtDescription
                },
            content = productArt,
        )
        DandiText(
            text = eyebrow,
            modifier = Modifier.figmaFrame(x = 136, y = 33, width = 220, height = 16),
            color = colors.contentActionSecondary,
            style = DesignSystemThemeImpl.typeScale.labelStrongXS,
        )
        DandiText(
            text = title,
            modifier = Modifier
                .figmaFrame(x = 136, y = 55, width = 220, height = 26)
                .semantics { heading() },
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularM,
        )
        DandiText(
            text = subtitle,
            modifier = Modifier.figmaFrame(x = 136, y = 87, width = 220, height = 16),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.labelRegularXS,
        )
        Surface(
            modifier = Modifier.figmaFrame(x = 24, y = 126, width = 342, height = 64),
            shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
            color = colors.bgSurfaceCardSubtle,
            border = BorderStroke(OverlayBorderWidth, colors.borderCardSubtle),
        ) {
            Box {
                DandiText(
                    text = balanceLabel,
                    modifier = Modifier.figmaFrame(x = 15, y = 10, width = 110, height = 16),
                    color = colors.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.labelRegularXS,
                )
                DandiText(
                    text = balanceValue,
                    modifier = Modifier.figmaFrame(x = 135, y = 7, width = 188, height = 22),
                    color = colors.contentActionSecondary,
                    textAlign = TextAlign.End,
                    style = DesignSystemThemeImpl.typeScale.textStrongM,
                )
                DandiText(
                    text = balanceHelper,
                    modifier = Modifier.figmaFrame(x = 15, y = 35, width = 308, height = 16),
                    color = colors.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.labelRegularXS,
                )
            }
        }
        OverlayActionButton(
            label = cancelLabel,
            modifier = Modifier.figmaFrame(x = 24, y = 214, width = 112, height = 52),
            visualHeight = LargeActionHeight,
            shape = RoundedCornerShape(ConfirmActionRadius),
            style = OverlayActionStyle.OutlinedNeutral,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = SheetCancelTag,
            onClick = onCancel,
        )
        OverlayActionButton(
            label = confirmLabel,
            modifier = Modifier.figmaFrame(x = 148, y = 214, width = 218, height = 52),
            visualHeight = LargeActionHeight,
            shape = RoundedCornerShape(ConfirmActionRadius),
            style = OverlayActionStyle.Reward,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = SheetConfirmTag,
            onClick = onConfirm,
        )
    }
}

@Immutable
data class NyummySheetMacroSummary(
    val label: String,
    val amount: String,
    val percent: String,
)

/** Canonical daily meal summary sheet from Figma node 956:626. */
@Composable
fun NyummyMealSummaryBottomSheet(
    title: String,
    modifier: Modifier = Modifier,
    completionLabel: String,
    summary: String,
    progressFraction: Float,
    carbohydrate: NyummySheetMacroSummary,
    protein: NyummySheetMacroSummary,
    fat: NyummySheetMacroSummary,
    sectionTitle: String,
    remainingLabel: String,
    addMealLabel: String,
    mealsContent: @Composable ColumnScope.() -> Unit,
    onAddMeal: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val normalizedProgress = progressFraction.coerceIn(0f, 1f)
    val scrollState = rememberScrollState()

    NyummySheetContainer(
        title = title,
        modifier = modifier,
        width = MealSummarySheetWidth,
        height = MealSummarySheetHeight,
        testTag = MealSummarySheetTag,
    ) {
        SheetHandle(modifier = Modifier.figmaFrame(x = 160, y = 10, width = 50, height = 4))
        DandiText(
            text = title,
            modifier = Modifier
                .figmaFrame(x = 20, y = 30, width = 235, height = 28)
                .semantics { heading() },
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularL,
        )
        DandiText(
            text = completionLabel,
            modifier = Modifier.figmaFrame(x = 282, y = 32, width = 68, height = 26),
            color = colors.contentNutritionLabel,
            textAlign = TextAlign.End,
            style = DesignSystemThemeImpl.typeScale.textStrongXL,
        )
        DandiText(
            text = summary,
            modifier = Modifier.figmaFrame(x = 20, y = 61, width = 250, height = 18),
            color = colors.contentDefaultLevel1,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        Box(
            modifier = Modifier
                .figmaFrame(x = 20, y = 84, width = 330, height = 10)
                .clip(DesignSystemThemeImpl.designSystemShape.progress)
                .background(colors.bgProgressTrack)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(normalizedProgress, 0f..1f)
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalizedProgress)
                    .height(MealProgressHeight)
                    .background(colors.dataProgressDefault),
            )
        }
        MacroSummaryCard(
            summary = carbohydrate,
            modifier = Modifier.figmaFrame(x = 20, y = 112, width = 100, height = 68),
        )
        MacroSummaryCard(
            summary = protein,
            modifier = Modifier.figmaFrame(x = 135, y = 112, width = 100, height = 68),
        )
        MacroSummaryCard(
            summary = fat,
            modifier = Modifier.figmaFrame(x = 250, y = 112, width = 100, height = 68),
        )
        DandiText(
            text = sectionTitle,
            modifier = Modifier
                .figmaFrame(x = 20, y = 194, width = 150, height = 24)
                .semantics { heading() },
            color = colors.contentNutritionLabel,
            style = DesignSystemThemeImpl.typeScale.textStrongL,
        )
        Column(
            modifier = Modifier
                .figmaFrame(x = 20, y = 226, width = 330, height = 226)
                .clipToBounds()
                .verticalScroll(scrollState),
        ) {
            mealsContent()
            Spacer(Modifier.height(DesignSystemThemeImpl.designSystemSpacing.space12))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GoalRemainderHeight),
                shape = RoundedCornerShape(ConfirmActionRadius),
                color = colors.bgSurfaceCardSubtle,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(GoalRemainderLeadingSpace))
                    Box(
                        modifier = Modifier
                            .size(GoalDotSize)
                            .clip(DesignSystemThemeImpl.designSystemShape.pill)
                            .background(colors.dataProgressDefault),
                    )
                    Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
                    DandiText(
                        text = remainingLabel,
                        color = colors.contentDefaultLevel1,
                        style = DesignSystemThemeImpl.typeScale.textStrongM,
                    )
                }
            }
        }
        OverlayActionButton(
            label = addMealLabel,
            modifier = Modifier.figmaFrame(x = 20, y = 476, width = 330, height = 52),
            visualHeight = LargeActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.pill,
            style = OverlayActionStyle.Secondary,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongXS,
            testTag = AddMealTag,
            onClick = onAddMeal,
        )
    }
}

/** Canonical food-icon collection detail sheet from Figma node 956:644. */
@Composable
fun NyummyCollectionDetailBottomSheet(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String,
    artworkDescription: String,
    artwork: @Composable BoxScope.() -> Unit,
    acquisitionLabel: String,
    acquisitionValue: String,
    fragmentsLabel: String,
    fragmentsValue: String,
    infoHelper: String,
    duplicateHelper: String,
    rewardLabel: String,
    closeLabel: String,
    onReward: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    NyummySheetContainer(
        title = title,
        modifier = modifier,
        width = StandardSheetWidth,
        height = CollectionSheetHeight,
        testTag = CollectionSheetTag,
        border = BorderStroke(OverlayBorderWidth, colors.borderCardSubtle),
    ) {
        SheetHandle(modifier = Modifier.figmaFrame(x = 170, y = 9, width = 48, height = 4))
        DandiText(
            text = eyebrow,
            modifier = Modifier.figmaFrame(x = 23, y = 30, width = 250, height = 18),
            color = colors.contentActionSecondary,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        DandiText(
            text = title,
            modifier = Modifier
                .figmaFrame(x = 23, y = 54, width = 220, height = 32)
                .semantics { heading() },
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.displayRegularXL,
        )
        Surface(
            modifier = Modifier
                .figmaFrame(x = 269, y = 33, width = 72, height = 72)
                .semantics(mergeDescendants = true) {
                    contentDescription = artworkDescription
                },
            shape = RoundedCornerShape(LargeActionRadius),
            color = colors.bgWarningSoft,
        ) {
            Box(Modifier.fillMaxSize(), content = artwork)
        }
        Surface(
            modifier = Modifier.figmaFrame(x = 23, y = 113, width = 342, height = 132),
            shape = RoundedCornerShape(LargeActionRadius),
            color = colors.bgSurfaceCardSubtle,
            border = BorderStroke(OverlayBorderWidth, colors.borderCardSubtle),
        ) {
            Box {
                InfoLabel(
                    label = acquisitionLabel,
                    modifier = Modifier.figmaFrame(x = 15, y = 14, width = 110, height = 16),
                )
                InfoValue(
                    value = acquisitionValue,
                    modifier = Modifier.figmaFrame(x = 131, y = 11, width = 194, height = 22),
                )
                InfoLabel(
                    label = fragmentsLabel,
                    modifier = Modifier.figmaFrame(x = 15, y = 52, width = 110, height = 16),
                )
                InfoValue(
                    value = fragmentsValue,
                    modifier = Modifier.figmaFrame(x = 131, y = 49, width = 194, height = 22),
                )
                DandiText(
                    text = infoHelper,
                    modifier = Modifier.figmaFrame(x = 15, y = 94, width = 310, height = 32),
                    color = colors.contentDefaultLevel1,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                    style = DesignSystemThemeImpl.typeScale.labelRegularXS,
                )
            }
        }
        DandiText(
            text = duplicateHelper,
            modifier = Modifier.figmaFrame(x = 23, y = 265, width = 342, height = 18),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
        OverlayActionButton(
            label = rewardLabel,
            modifier = Modifier.figmaFrame(x = 23, y = 305, width = 342, height = 54),
            visualHeight = CollectionRewardHeight,
            shape = RoundedCornerShape(LargeActionRadius),
            style = OverlayActionStyle.Reward,
            textStyle = DesignSystemThemeImpl.typeScale.textStrongM,
            testTag = CollectionRewardTag,
            onClick = onReward,
        )
        OverlayActionButton(
            label = closeLabel,
            modifier = Modifier.figmaFrame(x = 23, y = 364, width = 342, height = 48),
            visualHeight = CompactActionHeight,
            shape = DesignSystemThemeImpl.designSystemShape.inputDefault,
            style = OverlayActionStyle.Ghost,
            textStyle = DesignSystemThemeImpl.typeScale.labelStrongS,
            testTag = SheetCloseTag,
            onClick = onClose,
        )
    }
}

@Composable
private fun NyummyDialogContainer(
    title: String,
    modifier: Modifier,
    width: Dp,
    height: Dp,
    testTag: String,
    hasShadow: Boolean,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = DesignSystemThemeImpl.designSystemShape.dialogDefault
    val shadowModifier = if (hasShadow) {
        Modifier.designSystemDropShadow(
            shape = shape,
            shadow = DesignSystemThemeImpl.designSystemElevation.dialogStandard,
        )
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .requiredSize(width = width, height = height)
            .then(shadowModifier)
            .testTag(testTag)
            .semantics {
                paneTitle = title
            },
        shape = shape,
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        contentColor = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        border = border,
    ) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun NyummySheetContainer(
    title: String,
    modifier: Modifier,
    width: Dp,
    height: Dp,
    testTag: String,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = DesignSystemThemeImpl.designSystemShape.sheetDefault
    Surface(
        modifier = modifier
            .requiredSize(width = width, height = height)
            .designSystemDropShadow(
                shape = shape,
                shadow = DesignSystemThemeImpl.designSystemElevation.sheetStandard,
            )
            .testTag(testTag)
            .semantics {
                paneTitle = title
            },
        shape = shape,
        color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1,
        contentColor = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        border = border,
    ) {
        Box(Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun SheetHandle(modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(DesignSystemThemeImpl.designSystemShape.handle)
            .background(DesignSystemThemeImpl.designSystemColor.bgSheetHandle),
    )
}

private enum class OverlayActionStyle {
    Primary,
    Secondary,
    Danger,
    Reward,
    OutlinedSecondary,
    OutlinedNeutral,
    Ghost,
}

@Immutable
private data class OverlayActionColors(
    val container: Color,
    val content: Color,
    val border: Color? = null,
)

@Composable
private fun OverlayActionButton(
    label: String,
    modifier: Modifier,
    visualHeight: Dp,
    shape: Shape,
    style: OverlayActionStyle,
    textStyle: TextStyle,
    testTag: String,
    onClick: () -> Unit,
) {
    val colors = overlayActionColors(style)
    Box(
        modifier = modifier
            .testTag(testTag)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualHeight),
            shape = shape,
            color = colors.container,
            contentColor = colors.content,
            border = colors.border?.let { BorderStroke(OverlayBorderWidth, it) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                DandiText(
                    text = label,
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.content,
                    textAlign = TextAlign.Center,
                    style = textStyle,
                )
            }
        }
    }
}

@Composable
private fun overlayActionColors(style: OverlayActionStyle): OverlayActionColors {
    val colors = DesignSystemThemeImpl.designSystemColor
    return when (style) {
        OverlayActionStyle.Primary -> OverlayActionColors(
            container = colors.bgActionPrimaryDefault,
            content = colors.contentActionPrimary,
        )
        OverlayActionStyle.Secondary -> OverlayActionColors(
            container = colors.bgActionSecondaryDefault,
            content = colors.contentActionSecondary,
        )
        OverlayActionStyle.Danger -> OverlayActionColors(
            container = colors.bgActionDangerDefault,
            content = colors.contentActionDanger,
            border = colors.borderDangerDefault,
        )
        OverlayActionStyle.Reward -> OverlayActionColors(
            container = colors.bgActionRewardDefault,
            content = colors.contentActionReward,
        )
        OverlayActionStyle.OutlinedSecondary -> OverlayActionColors(
            container = colors.bgActionSecondaryDefault,
            content = colors.contentActionSecondary,
            border = colors.borderActionSecondary,
        )
        OverlayActionStyle.OutlinedNeutral -> OverlayActionColors(
            container = colors.bgDefaultLevel1,
            content = colors.contentActionSecondary,
            border = colors.borderCardSubtle,
        )
        OverlayActionStyle.Ghost -> OverlayActionColors(
            container = colors.bgDefaultLevel0.copy(alpha = 0f),
            content = colors.contentActionSecondary,
        )
    }
}

@Composable
private fun NoticePager(
    selectedPage: Int,
    pageCount: Int,
    modifier: Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    val safeCount = pageCount.coerceAtLeast(1)
    val safeSelectedPage = selectedPage.coerceIn(0, safeCount - 1)
    Row(
        modifier = modifier.semantics {
            stateDescription = pagePositionLabel(safeSelectedPage, safeCount)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val leadingSpace = ((90 - (safeCount * 8) - 8 - ((safeCount - 1) * 8)) / 2)
            .coerceAtLeast(0)
        Spacer(Modifier.width(leadingSpace.dp))
        repeat(safeCount) { index ->
            if (index > 0) Spacer(Modifier.width(DesignSystemThemeImpl.designSystemSpacing.space8))
            Box(
                modifier = Modifier
                    .size(
                        width = if (index == safeSelectedPage) ActivePagerWidth else InactivePagerWidth,
                        height = PagerHeight,
                    )
                    .clip(DesignSystemThemeImpl.designSystemShape.pill)
                    .background(
                        if (index == safeSelectedPage) {
                            colors.dataProgressDefault
                        } else {
                            colors.dataEvaluationUnrecorded
                        },
                    ),
            )
        }
    }
}

@Composable
private fun MacroSummaryCard(
    summary: NyummySheetMacroSummary,
    modifier: Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ConfirmActionRadius),
        color = colors.bgSurfaceCardSubtle,
    ) {
        Box {
            DandiText(
                text = summary.label,
                modifier = Modifier.figmaFrame(x = 10, y = 10, width = 80, height = 16),
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            )
            DandiText(
                text = summary.amount,
                modifier = Modifier.figmaFrame(x = 10, y = 31, width = 80, height = 16),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongXS,
            )
            DandiText(
                text = summary.percent,
                modifier = Modifier.figmaFrame(x = 10, y = 48, width = 80, height = 18),
                color = colors.contentNutritionLabel,
                style = DesignSystemThemeImpl.typeScale.labelStrongS,
            )
        }
    }
}

@Composable
private fun InfoLabel(
    label: String,
    modifier: Modifier,
) {
    DandiText(
        text = label,
        modifier = modifier,
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
        style = DesignSystemThemeImpl.typeScale.labelStrongXS,
    )
}

@Composable
private fun InfoValue(
    value: String,
    modifier: Modifier,
) {
    DandiText(
        text = value,
        modifier = modifier,
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
        style = DesignSystemThemeImpl.typeScale.textStrongM,
    )
}

private fun Modifier.figmaFrame(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
): Modifier = offset(x = x.dp, y = y.dp).requiredSize(width = width.dp, height = height.dp)

private fun pagePositionLabel(selectedPage: Int, pageCount: Int): String {
    val safeCount = pageCount.coerceAtLeast(1)
    return "${selectedPage.coerceIn(0, safeCount - 1) + 1} / $safeCount"
}

@Composable
private fun PreviewFrame(
    alignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    DesignSystemTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0),
            contentAlignment = alignment,
            content = content,
        )
    }
}

@Composable
private fun PreviewArtwork(modifier: Modifier = Modifier) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = DesignSystemThemeImpl.designSystemShape.cardDefault,
            color = colors.bgBrandSoft,
            border = BorderStroke(OverlayBorderWidth, colors.borderBrandDefault),
        ) {
            Box(contentAlignment = Alignment.Center) {
                DandiText(
                    text = "ART",
                    color = colors.contentAccent,
                    style = DesignSystemThemeImpl.typeScale.labelStrongXS,
                )
            }
        }
    }
}

@Composable
private fun RowScope.PreviewNoticeBenefits() {
    val colors = DesignSystemThemeImpl.designSystemColor
    PreviewNoticeBenefit(
        title = "음식 도감팩",
        body = "픽셀 아이콘 수집",
        modifier = Modifier.weight(1f),
    )
    Box(
        modifier = Modifier
            .width(OverlayBorderWidth)
            .height(42.dp)
            .background(colors.borderDefaultLevel1),
    )
    PreviewNoticeBenefit(
        title = "고양이 도감팩",
        body = "캐릭터 도감 수집",
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun PreviewNoticeBenefit(
    title: String,
    body: String,
    modifier: Modifier,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Box(modifier = modifier.fillMaxSize()) {
        DandiText(
            text = title,
            modifier = Modifier.figmaFrame(x = 12, y = 12, width = 115, height = 18),
            color = colors.contentDefaultLevel0,
            style = DesignSystemThemeImpl.typeScale.labelStrongS,
        )
        DandiText(
            text = body,
            modifier = Modifier.figmaFrame(x = 12, y = 39, width = 115, height = 18),
            color = colors.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.textRegularS,
        )
    }
}

@Composable
private fun PreviewMealRow() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        shape = RoundedCornerShape(LargeActionRadius),
        color = colors.bgDefaultLevel1,
        border = BorderStroke(OverlayBorderWidth, colors.borderMealRow),
    ) {
        Box {
            PreviewArtwork(Modifier.figmaFrame(x = 11, y = 15, width = 50, height = 50))
            DandiText(
                text = "닭가슴살 포케",
                modifier = Modifier.figmaFrame(x = 69, y = 14, width = 155, height = 24),
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
            DandiText(
                text = "12:36 · 540 kcal",
                modifier = Modifier.figmaFrame(x = 69, y = 42, width = 180, height = 16),
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            )
        }
    }
}

@Preview(name = "Dialog · Edit", showBackground = true, widthDp = 390, heightDp = 360)
@Composable
private fun NyummyEditDialogPreview() {
    PreviewFrame {
        NyummyEditDialog(
            title = "음식 이름 수정",
            fieldLabel = "음식 이름",
            fieldValue = "치킨 샐러드",
            onFieldValueChange = {},
            timeLabel = "촬영 시각 · 08:10",
            errorMessage = "저장하지 못했어요. 입력한 이름은 그대로예요.",
            cancelLabel = "취소",
            confirmLabel = "다시 저장",
            onCancel = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "Dialog · Destructive", showBackground = true, widthDp = 390, heightDp = 340)
@Composable
private fun NyummyDestructiveDialogPreview() {
    PreviewFrame {
        NyummyDestructiveDialog(
            title = "삭제하지 못했어요",
            body = "네트워크 문제로 기록을 삭제하지 못했어요.\n치킨 샐러드 기록은 그대로 남아 있어요.",
            targetLabel = "치킨 샐러드 · 08:10",
            helper = "연결을 확인한 뒤 같은 기록만 다시 요청해요.",
            cancelLabel = "취소",
            confirmLabel = "다시 삭제",
            onCancel = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "Dialog · Notice", showBackground = true, widthDp = 390, heightDp = 650)
@Composable
private fun NyummyNoticeDialogPreview() {
    PreviewFrame {
        NyummyNoticeDialog(
            title = "여름 도감팩 페스티벌",
            typeLabel = "시즌 이벤트",
            selectedPage = 0,
            pageCount = 2,
            periodLabel = "7월 18일 — 7월 31일",
            bodyTitle = "새 음식과 친구가\n찾아왔어요!",
            body = "음식 픽셀 아이콘과 새 고양이를\n기간 한정 도감팩에서 만나보세요.",
            footer = "확률은 팩을 열기 전에 확인할 수 있어요.",
            artworkDescription = "시즌 이벤트 음식과 고양이",
            artwork = { PreviewArtwork() },
            benefitsContent = { PreviewNoticeBenefits() },
            primaryLabel = "상점에서 도감팩 보기",
            nextLabel = "다음 소식 보기",
            closeLabel = "닫기",
            onPrimary = {},
            onNext = {},
            onClose = {},
        )
    }
}

@Preview(name = "Bottom Sheet · Confirm", showBackground = true, widthDp = 390, heightDp = 330)
@Composable
private fun NyummyConfirmBottomSheetPreview() {
    PreviewFrame(alignment = Alignment.BottomCenter) {
        NyummyConfirmBottomSheet(
            title = "여름 고양이 도감팩",
            eyebrow = "고양이 뽑기 · 1회",
            subtitle = "도감팩 1개",
            productArtDescription = "여름 고양이 도감팩",
            productArt = { PreviewArtwork() },
            balanceLabel = "고양이 도감팩",
            balanceValue = "2개 → 1개",
            balanceHelper = "대신 코인 100으로도 뽑을 수 있어요.",
            cancelLabel = "취소",
            confirmLabel = "1회 뽑기",
            onCancel = {},
            onConfirm = {},
        )
    }
}

@Preview(name = "Bottom Sheet · Meal Summary", showBackground = true, widthDp = 390, heightDp = 580)
@Composable
private fun NyummyMealSummaryBottomSheetPreview() {
    PreviewFrame(alignment = Alignment.BottomCenter) {
        NyummyMealSummaryBottomSheet(
            title = "7월 12일 식사",
            completionLabel = "75%",
            summary = "1개 기록 · 1,350 / 1,800 kcal",
            progressFraction = 0.75f,
            carbohydrate = NyummySheetMacroSummary("탄수화물", "185 / 240g", "77%"),
            protein = NyummySheetMacroSummary("단백질", "42 / 55g", "76%"),
            fat = NyummySheetMacroSummary("지방", "31 / 50g", "62%"),
            sectionTitle = "오늘의 식사",
            remainingLabel = "오늘 목표까지 450 kcal 남았어요",
            addMealLabel = "식사 추가하기",
            mealsContent = { PreviewMealRow() },
            onAddMeal = {},
        )
    }
}

@Preview(name = "Bottom Sheet · Collection Detail", showBackground = true, widthDp = 390, heightDp = 470)
@Composable
private fun NyummyCollectionDetailBottomSheetPreview() {
    PreviewFrame(alignment = Alignment.BottomCenter) {
        NyummyCollectionDetailBottomSheet(
            title = "샐러드",
            eyebrow = "음식 아이콘 · 보유 중",
            artworkDescription = "샐러드 픽셀 아이콘",
            artwork = { PreviewArtwork() },
            acquisitionLabel = "획득 경로",
            acquisitionValue = "상점 음식 도감팩",
            fragmentsLabel = "중복 조각",
            fragmentsValue = "7 / 10",
            infoHelper = "카드를 누르면 획득 경로와 수집 상태를 확인할 수 있어요.",
            duplicateHelper = "같은 아이콘이 다시 나오면 조각 1개가 쌓여요.",
            rewardLabel = "상점에서 음식 도감팩 열기",
            closeLabel = "닫기",
            onReward = {},
            onClose = {},
        )
    }
}

private const val ErrorMarker = "!"
private const val EditDialogTag = "nyummy_dialog_edit"
private const val DestructiveDialogTag = "nyummy_dialog_destructive"
private const val NoticeDialogTag = "nyummy_dialog_notice"
private const val DialogCancelTag = "nyummy_dialog_cancel"
private const val DialogConfirmTag = "nyummy_dialog_confirm"
private const val NoticePrimaryTag = "nyummy_notice_primary"
private const val NoticeNextTag = "nyummy_notice_next"
private const val NoticeCloseTag = "nyummy_notice_close"
private const val ConfirmSheetTag = "nyummy_sheet_confirm"
private const val MealSummarySheetTag = "nyummy_sheet_meal_summary"
private const val CollectionSheetTag = "nyummy_sheet_collection_detail"
private const val SheetCancelTag = "nyummy_sheet_cancel"
private const val SheetConfirmTag = "nyummy_sheet_confirm_action"
private const val SheetCloseTag = "nyummy_sheet_close"
private const val AddMealTag = "nyummy_sheet_add_meal"
private const val CollectionRewardTag = "nyummy_sheet_collection_reward"

private val EditDialogWidth = 334.dp
private val EditDialogHeight = 308.dp
private val DestructiveDialogWidth = 334.dp
private val DestructiveDialogHeight = 286.dp
private val NoticeDialogWidth = 342.dp
private val NoticeDialogHeight = 610.dp
private val StandardSheetWidth = 390.dp
private val ConfirmSheetHeight = 290.dp
private val MealSummarySheetWidth = 370.dp
private val MealSummarySheetHeight = 546.dp
private val CollectionSheetHeight = 430.dp
private val OverlayBorderWidth = 1.dp
private val ErrorPanelRadius = 14.dp
private val NoticeTypeRadius = 14.dp
private val CompactActionHeight = 44.dp
private val LargeActionHeight = 52.dp
private val CollectionRewardHeight = 54.dp
private val LargeActionRadius = 18.dp
private val ConfirmActionRadius = 14.dp
private val MealProgressHeight = 10.dp
private val GoalRemainderHeight = 48.dp
private val GoalRemainderLeadingSpace = 14.dp
private val GoalDotSize = 8.dp
private val ActivePagerWidth = 16.dp
private val InactivePagerWidth = 8.dp
private val PagerHeight = 8.dp
