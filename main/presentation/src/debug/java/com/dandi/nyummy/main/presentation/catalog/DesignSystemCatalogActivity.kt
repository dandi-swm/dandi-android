package com.dandi.nyummy.main.presentation.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.common.presentation.R as CommonR
import com.dandi.nyummy.common.presentation.component.DandiText
import com.dandi.nyummy.common.presentation.component.NyummyAnalysisBanner
import com.dandi.nyummy.common.presentation.component.NyummyAnalysisBannerState
import com.dandi.nyummy.common.presentation.component.NyummyAnalysisState
import com.dandi.nyummy.common.presentation.component.NyummyBadge
import com.dandi.nyummy.common.presentation.component.NyummyBadgeTone
import com.dandi.nyummy.common.presentation.component.NyummyBottomNavigation
import com.dandi.nyummy.common.presentation.component.NyummyBottomNavigationStyle
import com.dandi.nyummy.common.presentation.component.NyummyButton
import com.dandi.nyummy.common.presentation.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyCalendarDay
import com.dandi.nyummy.common.presentation.component.NyummyCalendarHeaderAction
import com.dandi.nyummy.common.presentation.component.NyummyCalendarHeaderDirection
import com.dandi.nyummy.common.presentation.component.NyummyCalendarNutritionStatus
import com.dandi.nyummy.common.presentation.component.NyummyCard
import com.dandi.nyummy.common.presentation.component.NyummyChip
import com.dandi.nyummy.common.presentation.component.NyummyCollectionDetailBottomSheet
import com.dandi.nyummy.common.presentation.component.NyummyConfirmBottomSheet
import com.dandi.nyummy.common.presentation.component.NyummyDailyMacroSummary
import com.dandi.nyummy.common.presentation.component.NyummyDailyNutritionData
import com.dandi.nyummy.common.presentation.component.NyummyDailyNutritionState
import com.dandi.nyummy.common.presentation.component.NyummyDailyNutritionSummary
import com.dandi.nyummy.common.presentation.component.NyummyDestructiveDialog
import com.dandi.nyummy.common.presentation.component.NyummyEditDialog
import com.dandi.nyummy.common.presentation.component.NyummyFloatingTodayMeals
import com.dandi.nyummy.common.presentation.component.NyummyIconButton
import com.dandi.nyummy.common.presentation.component.NyummyIconButtonStyle
import com.dandi.nyummy.common.presentation.component.NyummyDualLinearProgress
import com.dandi.nyummy.common.presentation.component.NyummyLinearProgress
import com.dandi.nyummy.common.presentation.component.NyummyListRow
import com.dandi.nyummy.common.presentation.component.NyummyLoading
import com.dandi.nyummy.common.presentation.component.NyummyLoadingSize
import com.dandi.nyummy.common.presentation.component.NyummyMealDetailCard
import com.dandi.nyummy.common.presentation.component.NyummyMealDetailData
import com.dandi.nyummy.common.presentation.component.NyummyMealRow
import com.dandi.nyummy.common.presentation.component.NyummyMealRowData
import com.dandi.nyummy.common.presentation.component.NyummyMealSummaryBottomSheet
import com.dandi.nyummy.common.presentation.component.NyummyModalScrim
import com.dandi.nyummy.common.presentation.component.NyummyNavigationDestination
import com.dandi.nyummy.common.presentation.component.NyummyNoticeDialog
import com.dandi.nyummy.common.presentation.component.NyummyPhotoPicker
import com.dandi.nyummy.common.presentation.component.NyummyPhotoPickerState
import com.dandi.nyummy.common.presentation.component.NyummySheetMacroSummary
import com.dandi.nyummy.common.presentation.component.NyummySnackbar
import com.dandi.nyummy.common.presentation.component.NyummyStateSurface
import com.dandi.nyummy.common.presentation.component.NyummyStateSurfaceType
import com.dandi.nyummy.common.presentation.component.NyummyTextField
import com.dandi.nyummy.common.presentation.component.NyummyTopAppBar
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl

/** Debug-only, interactive inventory for comparing Android with Figma rc.3. */
class DesignSystemCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DesignSystemTheme {
                DesignSystemCatalog()
            }
        }
    }
}

@Composable
private fun DesignSystemCatalog() {
    val colors = DesignSystemThemeImpl.designSystemColor
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgDefaultLevel0)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = DesignSystemThemeImpl.designSystemSpacing.space24),
        verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space32),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
        ) {
            DandiText(
                text = "Nyummy Design System",
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularXL,
            )
            DandiText(
                text = "LIVE Android Design System · v1.0.0-rc.3 · page 984:37241",
                color = colors.contentDefaultLevel1,
                style = DesignSystemThemeImpl.typeScale.textRegularM,
            )
        }

        CommonCatalogSection()
        OverlayAndStateCatalogSection()
        NavigationCatalogSection()
        MealAndHistoryCatalogSection()
        ReferenceIntegrationSection()
    }
}

@Composable
private fun CommonCatalogSection() {
    CatalogSection(
        title = "Common",
        source = "Button 990:1052 · IconButton 990:1079 · Input 992:765 · TopAppBar 993:681 · Snackbar 993:690 · Card 993:695 · ListRow 993:699 · Chip 993:728 · Badge 993:739 · Progress 993:753 · Loading 993:762 · ModalScrim 993:764",
    ) {
        CatalogGroup("Button · 990:1052") {
            WideFixture { CatalogButtonMatrix(fullWidth = false) }
            WideFixture { CatalogButtonMatrix(fullWidth = true) }
        }

        CatalogGroup("IconButton · 990:1079") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyIconButtonStyle.entries.forEach { style ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                            FixtureLabel(style.name)
                            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                                NyummyIconButton(
                                    contentDescription = "$style 기본",
                                    style = style,
                                    onClick = {},
                                ) { CatalogGlyph("＋") }
                                NyummyIconButton(
                                    contentDescription = "$style 비활성",
                                    style = style,
                                    enabled = false,
                                    onClick = {},
                                ) { CatalogGlyph("＋") }
                            }
                        }
                    }
                }
            }
        }

        CatalogGroup("Input · 992:765") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    listOf(true, false).forEach { singleLine ->
                        CatalogInputState.entries.forEach { state ->
                            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                                FixtureLabel("${if (singleLine) "SingleLine" else "Multiline"} · ${state.label}")
                                NyummyTextField(
                                    value = state.value,
                                    onValueChange = {},
                                    modifier = Modifier.width(360.dp),
                                    placeholder = "음식 이름을 입력해 주세요",
                                    label = "음식 이름",
                                    helperText = state.helper,
                                    isError = state == CatalogInputState.Error,
                                    enabled = state != CatalogInputState.Disabled,
                                    readOnly = state == CatalogInputState.ReadOnly,
                                    singleLine = singleLine,
                                )
                            }
                        }
                    }
                }
            }
        }

        CatalogGroup("TopAppBar · 993:681") {
            WideFixture {
                NyummyTopAppBar(
                    title = "오늘의 냐미",
                    modifier = Modifier.width(360.dp),
                    navigationIcon = { CatalogGlyph("‹") },
                    actions = {
                        NyummyIconButton(
                            contentDescription = "추가",
                            onClick = {},
                        ) { CatalogGlyph("＋") }
                    },
                )
            }
        }

        CatalogGroup("Snackbar · 993:690") {
            WideFixture {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                    NyummySnackbar(
                        message = "식사 기록을 저장했어요.",
                        modifier = Modifier.width(400.dp),
                    )
                    NyummySnackbar(
                        message = "분석에 실패했어요. 사진과 텍스트 기록은 보존했어요.",
                        modifier = Modifier.width(400.dp),
                        actionLabel = "다시 분석",
                        onAction = {},
                    )
                }
            }
        }

        CatalogGroup("Card · 993:695 / ListRow · 993:699") {
            WideFixture {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyCard(
                        modifier = Modifier.width(360.dp),
                        title = "하루 기록",
                        body = "Figma canonical surface와 semantic token을 그대로 사용하는 공통 카드입니다.",
                    )
                    NyummyListRow(
                        modifier = Modifier.width(400.dp),
                        title = "닭가슴살 포케",
                        supportingText = "오늘 · 12:24 · 524 kcal",
                        leading = {
                            FoodPixelAsset(
                                resourceId = CommonR.drawable.nyummy_food_salad,
                                contentDescription = "샐러드 픽셀 아이콘",
                            )
                        },
                        trailing = { CatalogGlyph("›") },
                        onClick = {},
                    )
                }
            }
        }

        CatalogGroup("Chip · 993:728 / Badge · 993:739") {
            WideFixture {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                        NyummyChip(label = "기본", selected = false, onClick = {})
                        NyummyChip(
                            label = "선택됨",
                            selected = true,
                            leadingIcon = {
                                FoodPixelAsset(
                                    resourceId = CommonR.drawable.nyummy_food_rice,
                                    contentDescription = "밥 픽셀 아이콘",
                                )
                            },
                            onClick = {},
                        )
                        NyummyChip(label = "비활성", selected = false, enabled = false, onClick = {})
                        NyummyChip(label = "선택 비활성", selected = true, enabled = false, onClick = {})
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                        NyummyBadgeTone.entries.forEach { tone ->
                            NyummyBadge(label = tone.name, tone = tone)
                        }
                    }
                }
            }
        }

        CatalogGroup("LinearProgress · 993:753 / Loading · 993:762") {
            WideFixture {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { progress ->
                            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                                FixtureLabel("${(progress * 100).toInt()}%")
                                NyummyLinearProgress(progress = progress, modifier = Modifier.width(320.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                        listOf(0.70f to 0.22f, 0.83f to 0.37f, 1f to 0.50f).forEach { (daily, meal) ->
                            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                                FixtureLabel("dual ${(daily * 100).toInt()}% · ${(meal * 100).toInt()}%")
                                NyummyDualLinearProgress(
                                    primaryProgress = daily,
                                    secondaryProgress = meal,
                                    modifier = Modifier.width(320.dp),
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                        NyummyLoadingSize.entries.forEach { size ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
                            ) {
                                FixtureLabel(size.name)
                                NyummyLoading(size = size)
                            }
                        }
                    }
                }
            }
        }

        CatalogGroup("ModalScrim · 993:764") {
            WideFixture {
                Box(
                    modifier = Modifier
                        .size(width = 360.dp, height = 200.dp)
                        .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel1),
                    contentAlignment = Alignment.Center,
                ) {
                    DandiText(
                        text = "이전 화면 fixture",
                        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
                        style = DesignSystemThemeImpl.typeScale.displayRegularL,
                    )
                    NyummyModalScrim(
                        modifier = Modifier.fillMaxSize(),
                        onDismissRequest = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogButtonMatrix(fullWidth: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
        FixtureLabel(if (fullWidth) "Width · Full (320dp reference)" else "Width · Hug")
        NyummyButtonStyle.entries.forEach { style ->
            NyummyButtonSize.entries.forEach { size ->
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                    CatalogButtonState.entries.forEach { state ->
                        NyummyButton(
                            label = "${style.name} ${state.label}",
                            modifier = if (fullWidth) Modifier.width(320.dp) else Modifier,
                            style = style,
                            size = size,
                            enabled = state != CatalogButtonState.Disabled,
                            loading = state == CatalogButtonState.Loading,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayAndStateCatalogSection() {
    var editedName by remember { mutableStateOf("닭가슴살 포케") }

    CatalogSection(
        title = "Overlay / State",
        source = "Dialog root 956:563 (Edit 956:522 · Destructive 956:533 · Notice 956:562) · Sheet root 956:645 (Confirm 956:582 · MealSummary 956:626 · CollectionDetail 956:644) · State root 475:62",
    ) {
        CatalogGroup("Dialog · 956:563") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                    NyummyEditDialog(
                        title = "기록 수정",
                        fieldLabel = "음식 이름",
                        fieldValue = editedName,
                        onFieldValueChange = { editedName = it },
                        timeLabel = "촬영 시각은 수정할 수 없어요",
                        errorMessage = "사진과 다른 음식인지 확인해 주세요",
                        cancelLabel = "취소",
                        confirmLabel = "저장",
                        onCancel = {},
                        onConfirm = {},
                    )
                    NyummyDestructiveDialog(
                        title = "다시 분석할까요?",
                        body = "기존 분석 결과는 새 결과로 대체돼요.",
                        targetLabel = "닭가슴살 포케",
                        helper = "사진과 텍스트 기록은 보존돼요",
                        cancelLabel = "취소",
                        confirmLabel = "다시 분석",
                        onCancel = {},
                        onConfirm = {},
                    )
                    NyummyNoticeDialog(
                        title = "7월 냐미 챌린지",
                        typeLabel = "EVENT",
                        selectedPage = 1,
                        pageCount = 3,
                        periodLabel = "2026.07.01 – 2026.07.31",
                        bodyTitle = "매일 한 끼를\n기록해 보세요",
                        body = "기록을 이어 갈수록\n보상이 쌓여요.",
                        footer = "이벤트 아트는 화면에서 서버 또는 제품 asset으로 주입합니다.",
                        artworkDescription = "이벤트 아트 runtime slot",
                        artwork = { RuntimeSlotFixture("runtime slot\nevent artwork") },
                        benefitsContent = {
                            DandiText(
                                text = "runtime slot · event benefits",
                                modifier = Modifier.fillMaxWidth(),
                                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
                                style = DesignSystemThemeImpl.typeScale.labelStrongS,
                                textAlign = TextAlign.Center,
                            )
                        },
                        primaryLabel = "참여하기",
                        nextLabel = "다음",
                        closeLabel = "닫기",
                        onPrimary = {},
                        onNext = {},
                        onClose = {},
                    )
                }
            }
        }

        CatalogGroup("Bottom Sheet · 956:645") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                    NyummyConfirmBottomSheet(
                        title = "냐미 상자 열기",
                        eyebrow = "COLLECTION",
                        subtitle = "중복 보상은 코인으로 전환돼요",
                        productArtDescription = "제품 아트 runtime slot",
                        productArt = { RuntimeSlotFixture("runtime slot\nproduct art") },
                        balanceLabel = "현재 보유",
                        balanceValue = "1,280 coin",
                        balanceHelper = "상자 1개를 사용합니다",
                        cancelLabel = "취소",
                        confirmLabel = "열기",
                        onCancel = {},
                        onConfirm = {},
                    )
                    CatalogMealSummarySheet()
                    NyummyCollectionDetailBottomSheet(
                        title = "토마토 파스타",
                        eyebrow = "FOOD COLLECTION",
                        artworkDescription = "파스타 픽셀 아이콘",
                        artwork = {
                            FoodPixelAsset(
                                resourceId = CommonR.drawable.nyummy_food_pasta,
                                contentDescription = "파스타 픽셀 아이콘",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(DesignSystemThemeImpl.designSystemSpacing.space8),
                            )
                        },
                        acquisitionLabel = "획득일",
                        acquisitionValue = "2026.07.22",
                        fragmentsLabel = "보유 조각",
                        fragmentsValue = "4 / 5",
                        infoHelper = "식사 기록에서 만난 음식이에요",
                        duplicateHelper = "중복 획득 시 조각으로 전환돼요",
                        rewardLabel = "보상 받기",
                        closeLabel = "닫기",
                        onReward = {},
                        onClose = {},
                    )
                }
            }
        }

        CatalogGroup("State Surface 14 · 475:62") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyStateSurfaceType.entries.forEach { type ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                            FixtureLabel("${type.name} · ${stateNodeId(type)}")
                            NyummyStateSurface(
                                type = type,
                                onAction = {},
                                onSecondaryAction = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationCatalogSection() {
    CatalogSection(
        title = "Navigation",
        source = "Bottom Navigation root 634:11768 · Item 619:44573 · icons Home 619:44557 / History 619:44559 / Quest 619:44562 / Collection 619:44565 / Shop 619:44570 · Floating Today Meals 884:554",
    ) {
        NyummyBottomNavigationStyle.entries.forEach { style ->
            CatalogGroup("BottomNavigation ${style.name} · 634:11768") {
                WideFixture {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16)) {
                        NyummyNavigationDestination.entries.forEach { selected ->
                            Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                                FixtureLabel("Selected · ${selected.label}")
                                Box(modifier = Modifier.width(390.dp), contentAlignment = Alignment.Center) {
                                    NyummyBottomNavigation(
                                        selectedDestination = selected,
                                        style = style,
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }

        CatalogGroup("Floating Today Meals · 884:554 / Salad 125:44") {
            WideFixture {
                NyummyFloatingTodayMeals(
                    label = "오늘 현황",
                    leadingIcon = {
                        FoodPixelAsset(
                            resourceId = CommonR.drawable.nyummy_food_salad,
                            contentDescription = "샐러드 픽셀 아이콘",
                        )
                    },
                    onClick = {},
                )
            }
        }
    }
}

@Composable
private fun MealAndHistoryCatalogSection() {
    val meal = NyummyMealRowData(
        orderLabel = "첫 번째 기록",
        name = "닭가슴살 포케",
        recordedMeta = "오늘 · 12:24",
        calories = "524 kcal",
    )
    val dailyNutrition = catalogDailyNutritionData()

    CatalogSection(
        title = "Meal / History",
        source = "CalendarHeader 800:13515 · CalendarDay 844:12774 · MealRow 800:13506 · AnalysisBanner 1042:11 · DailyNutrition 1043:64 · MealDetail 481:87 · MealNutrition 1044:66 · PhotoPicker 1046:588 · pixel assets Salad 125:44 / Pasta 125:42 / Rice 125:50",
    ) {
        CatalogGroup("Calendar Header · 800:13515") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                    NyummyCalendarHeaderDirection.entries.forEach { direction ->
                        NyummyCalendarHeaderAction(direction = direction, onClick = {})
                    }
                }
            }
        }

        CatalogGroup("Calendar Day 24 · 844:12774") {
            WideFixture {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    listOf(false, true).forEach { selected ->
                        (0..2).forEach { iconCount ->
                            Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                                FixtureLabel("${if (selected) "Selected" else "Default"} · icons $iconCount")
                                NyummyCalendarNutritionStatus.entries.forEachIndexed { index, status ->
                                    NyummyCalendarDay(
                                        day = (19 + index).toString(),
                                        selected = selected,
                                        nutritionStatus = status,
                                        firstFoodIcon = if (iconCount >= 1) {
                                            {
                                                FoodPixelAsset(
                                                    resourceId = CommonR.drawable.nyummy_food_salad,
                                                    contentDescription = "샐러드 픽셀 아이콘",
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        secondFoodIcon = if (iconCount == 2) {
                                            {
                                                FoodPixelAsset(
                                                    resourceId = CommonR.drawable.nyummy_food_rice,
                                                    contentDescription = "밥 픽셀 아이콘",
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        CatalogGroup("Meal Row 4 · 800:13506") {
            WideFixture {
                Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                    NyummyAnalysisState.entries.forEach { state ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space4)) {
                            FixtureLabel(state.name)
                            NyummyMealRow(
                                data = meal,
                                state = state,
                                foodIcon = {
                                    FoodPixelAsset(
                                        resourceId = CommonR.drawable.nyummy_food_salad,
                                        contentDescription = "샐러드 픽셀 아이콘",
                                    )
                                },
                                onClick = {},
                                onRetry = {},
                            )
                        }
                    }
                }
            }
        }

        CatalogGroup("Analysis Banner · 1042:11") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyAnalysisBannerState.entries.forEach { state ->
                        NyummyAnalysisBanner(
                            title = if (state == NyummyAnalysisBannerState.Failed) "분석하지 못했어요" else "다시 분석하는 중",
                            message = if (state == NyummyAnalysisBannerState.Failed) {
                                "사진과 텍스트는 그대로 보존했어요"
                            } else {
                                "잠시만 기다려 주세요"
                            },
                            state = state,
                            onRetry = {},
                            guideCharacter = { RuntimeSlotFixture("runtime slot\ncat guide") },
                        )
                    }
                }
            }
        }

        CatalogGroup("Daily Nutrition 5 · 1043:64") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyDailyNutritionState.entries.forEach { state ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                            FixtureLabel(state.name)
                            NyummyDailyNutritionSummary(data = dailyNutrition, state = state)
                        }
                    }
                }
            }
        }

        CatalogGroup("Meal Detail 4 · 481:87") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyAnalysisState.entries.forEach { state ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                            FixtureLabel(state.name)
                            NyummyMealDetailCard(
                                data = NyummyMealDetailData(
                                    orderAndTime = "첫 번째 기록 · 12:24",
                                    name = "닭가슴살 포케",
                                    calories = "524",
                                    capturedAt = "2026.07.22 12:24",
                                ),
                                state = state,
                                photo = { RuntimeSlotFixture("runtime slot\nmeal photo") },
                                foodIcon = {
                                    FoodPixelAsset(
                                        resourceId = CommonR.drawable.nyummy_food_salad,
                                        contentDescription = "샐러드 픽셀 아이콘",
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        CatalogGroup("Photo Picker 5 · 1046:588") {
            WideFixture {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space12)) {
                    NyummyPhotoPickerState.entries.forEach { state ->
                        Column(verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8)) {
                            FixtureLabel(state.name)
                            NyummyPhotoPicker(
                                state = state,
                                photo = { RuntimeSlotFixture("runtime slot\nmeal photo") },
                                onClick = {},
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceIntegrationSection() {
    var selectedDestination by remember { mutableStateOf(NyummyNavigationDestination.Home) }

    CatalogSection(
        title = "390dp Reference Integration",
        source = "Home sheet 956:626 + Bottom Navigation 634:11768 · canonical rule: MealSummary is laid out above navigation and never covers it",
    ) {
        WideFixture {
            Box(
                modifier = Modifier
                    .size(width = 390.dp, height = 800.dp)
                    .background(DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0),
            ) {
                NyummyTopAppBar(
                    title = "오늘의 냐미",
                    modifier = Modifier.align(Alignment.TopCenter),
                    actions = { CatalogGlyph("＋") },
                )
                DandiText(
                    text = "runtime slot · home content",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 96.dp),
                    color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
                    style = DesignSystemThemeImpl.typeScale.labelStrongS,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = DesignSystemThemeImpl.designSystemSize.bottomNavigationFloating),
                ) {
                    CatalogMealSummarySheet()
                }
                NyummyBottomNavigation(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    selectedDestination = selectedDestination,
                ) { selectedDestination = it }
            }
        }
    }
}

@Composable
private fun CatalogMealSummarySheet(modifier: Modifier = Modifier) {
    NyummyMealSummaryBottomSheet(
        title = "7월 22일 식사",
        modifier = modifier,
        completionLabel = "71%",
        summary = "4개 기록 · 1,420 / 2,000 kcal",
        progressFraction = 0.71f,
        carbohydrate = NyummySheetMacroSummary("탄수화물", "168 / 240g", "70%"),
        protein = NyummySheetMacroSummary("단백질", "62 / 75g", "83%"),
        fat = NyummySheetMacroSummary("지방", "38 / 60g", "63%"),
        sectionTitle = "오늘의 식사",
        remainingLabel = "오늘 목표까지 580 kcal 남았어요",
        addMealLabel = "식사 추가하기",
        mealsContent = {
            NyummyMealRow(
                data = NyummyMealRowData(
                    orderLabel = "첫 번째 기록",
                    name = "닭가슴살 포케",
                    recordedMeta = "12:24",
                    calories = "524 kcal",
                ),
                foodIcon = {
                    FoodPixelAsset(
                        resourceId = CommonR.drawable.nyummy_food_salad,
                        contentDescription = "샐러드 픽셀 아이콘",
                    )
                },
                onClick = {},
            )
        },
        onAddMeal = {},
    )
}

@Composable
private fun CatalogSection(
    title: String,
    source: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = DesignSystemThemeImpl.designSystemColor
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space16),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space4),
        ) {
            DandiText(
                text = title,
                color = colors.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.displayRegularL,
            )
            DandiText(
                text = source,
                color = colors.contentDefaultLevel2,
                style = DesignSystemThemeImpl.typeScale.textRegularS,
            )
        }
        content()
    }
}

@Composable
private fun CatalogGroup(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSystemThemeImpl.designSystemSpacing.space8),
    ) {
        DandiText(
            text = label,
            modifier = Modifier.padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space20),
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel1,
            style = DesignSystemThemeImpl.typeScale.textStrongM,
        )
        content()
    }
}

@Composable
private fun WideFixture(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = DesignSystemThemeImpl.designSystemSpacing.space20),
        content = { content() },
    )
}

@Composable
private fun FixtureLabel(label: String) {
    DandiText(
        text = label,
        color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
        style = DesignSystemThemeImpl.typeScale.labelStrongS,
    )
}

@Composable
private fun CatalogGlyph(glyph: String) {
    DandiText(
        text = glyph,
        color = DesignSystemThemeImpl.designSystemColor.contentIconLevel0,
        style = DesignSystemThemeImpl.typeScale.displayRegularM,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RuntimeSlotFixture(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                DesignSystemThemeImpl.designSystemColor.bgSurfaceSubtle,
                DesignSystemThemeImpl.designSystemShape.cardDefault,
            ),
        contentAlignment = Alignment.Center,
    ) {
        DandiText(
            text = label,
            color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel2,
            style = DesignSystemThemeImpl.typeScale.labelRegularXS,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FoodPixelAsset(
    @DrawableRes resourceId: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(resourceId),
        contentDescription = contentDescription,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

private fun catalogDailyNutritionData() = NyummyDailyNutritionData(
    currentCalories = "1,420",
    targetCalories = "2,000",
    calorieRatioLabel = "71%",
    carbohydrate = NyummyDailyMacroSummary("탄수화물", "68%", 0.68f),
    protein = NyummyDailyMacroSummary("단백질", "82%", 0.82f),
    fat = NyummyDailyMacroSummary("지방", "54%", 0.54f),
)

private fun stateNodeId(type: NyummyStateSurfaceType): String = when (type) {
    NyummyStateSurfaceType.Empty -> "475:6"
    NyummyStateSurfaceType.Loading -> "475:14"
    NyummyStateSurfaceType.Offline -> "475:22"
    NyummyStateSurfaceType.AnalysisFailed -> "475:30"
    NyummyStateSurfaceType.PermissionDenied -> "475:38"
    NyummyStateSurfaceType.Ended -> "475:46"
    NyummyStateSurfaceType.Destructive -> "475:54"
    NyummyStateSurfaceType.Partial -> "519:6032"
    NyummyStateSurfaceType.Retrying -> "519:6041"
    NyummyStateSurfaceType.RewardPending -> "519:6087"
    NyummyStateSurfaceType.RewardCompleted -> "519:6096"
    NyummyStateSurfaceType.AlreadyClaimed -> "519:6105"
    NyummyStateSurfaceType.ReconcileFailed -> "519:6114"
    NyummyStateSurfaceType.RateLimited -> "519:6123"
}

private enum class CatalogButtonState(val label: String) {
    Default("Default"),
    Disabled("Disabled"),
    Loading("Loading"),
}

private enum class CatalogInputState(
    val label: String,
    val value: String,
    val helper: String,
) {
    Empty("Empty", "", "입력 전 상태"),
    Focused("Focused · tap field", "", "탭해서 실제 focus 상태 확인"),
    Filled("Filled", "닭가슴살 포케", "입력 완료"),
    Error("Error", "닭가슴살 포케", "사진과 다른 음식인지 확인해 주세요"),
    Disabled("Disabled", "닭가슴살 포케", "현재 수정할 수 없어요"),
    ReadOnly("ReadOnly", "닭가슴살 포케", "읽기 전용"),
}
