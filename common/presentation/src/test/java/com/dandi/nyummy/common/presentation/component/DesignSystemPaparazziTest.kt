package com.dandi.nyummy.common.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import com.android.resources.Density
import com.dandi.nyummy.common.presentation.R
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemTheme
import com.dandi.nyummy.common.presentation.ui.theme.DesignSystemThemeImpl
import org.junit.Rule
import org.junit.Test

/** PR과 회귀 검증에서 사용하는 냐미 공통 컴포넌트 골든 이미지 모음. */
class DesignSystemPaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(softButtons = false),
        renderingMode = RenderingMode.SHRINK,
    )

    @Test
    fun nyummyButton() = snapshot(referenceWidthDp = 340) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NyummyButton(label = "식사 기록하기", onClick = {})
            NyummyButton(
                label = "다시 시도",
                style = NyummyButtonStyle.Secondary,
                onClick = {},
            )
            NyummyButton(
                label = "삭제하기",
                style = NyummyButtonStyle.Danger,
                onClick = {},
            )
            NyummyButton(
                label = "도감팩 열기",
                style = NyummyButtonStyle.Reward,
                size = NyummyButtonSize.Large,
                onClick = {},
            )
        }
    }

    @Test
    fun nyummyIconButton() = snapshot(referenceWidthDp = 140) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NyummyIconButton(contentDescription = "추가", onClick = {}) { FixtureIcon() }
            NyummyIconButton(
                contentDescription = "추가",
                style = NyummyIconButtonStyle.Filled,
                onClick = {},
            ) { FixtureIcon() }
        }
    }

    @Test
    fun nyummyTextField() = snapshot(edgeToEdge = true, referenceWidthDp = 360) {
        NyummyTextField(
            value = "닭가슴살 포케",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = "음식 이름",
            helperText = "사진과 함께 음식 이름을 입력해 주세요",
        )
    }

    @Test
    fun nyummyTopAppBar() = snapshot(edgeToEdge = true, referenceWidthDp = 360) {
        NyummyTopAppBar(
            title = "식사 기록",
            navigationIcon = { FixtureIcon() },
            actions = { FixtureIcon() },
        )
    }

    @Test
    fun nyummySnackbar() = snapshot(edgeToEdge = true, referenceWidthDp = 400) {
        NyummySnackbar(
            message = "저장하지 못했어요. 다시 시도해 주세요.",
            modifier = Modifier.fillMaxWidth(),
            actionLabel = "다시 시도",
            onAction = {},
        )
    }

    @Test
    fun nyummyCard() = snapshot(edgeToEdge = true, referenceWidthDp = 360) {
        NyummyCard(
            modifier = Modifier.fillMaxWidth(),
            title = "하루 영양 현황",
            body = "목표와 현재 섭취량을 한눈에 확인할 수 있어요.",
        )
    }

    @Test
    fun nyummyListRow() = snapshot(edgeToEdge = true, referenceWidthDp = 400) {
        NyummyListRow(
            title = "닭가슴살 포케",
            supportingText = "오후 12:36 · 540 kcal",
            leading = { FixtureFoodIcon() },
            trailing = { FixtureIcon() },
        )
    }

    @Test
    fun nyummyChip() = snapshot(referenceWidthDp = 180) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NyummyChip(label = "전체", selected = false, onClick = {})
            NyummyChip(label = "선택됨", selected = true, onClick = {})
        }
    }

    @Test
    fun nyummyBadge() = snapshot(referenceWidthDp = 200) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NyummyBadge(label = "기본")
            NyummyBadge(label = "완료", tone = NyummyBadgeTone.Positive)
            NyummyBadge(label = "주의", tone = NyummyBadgeTone.Warning)
            NyummyBadge(label = "오류", tone = NyummyBadgeTone.Error)
        }
    }

    @Test
    fun nyummyLinearProgress() = snapshot(edgeToEdge = true, referenceWidthDp = 320) {
        NyummyLinearProgress(progress = 0.71f, modifier = Modifier.fillMaxWidth())
    }

    @Test
    fun nyummyLoading() = snapshot(referenceWidthDp = 112) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NyummyLoading(size = NyummyLoadingSize.Small)
            NyummyLoading(size = NyummyLoadingSize.Medium)
            NyummyLoading(size = NyummyLoadingSize.Large)
        }
    }

    @Test
    fun nyummyModalScrim() = snapshot(edgeToEdge = true, referenceWidthDp = 360) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(800.dp)
                .background(DesignSystemThemeImpl.designSystemColor.bgBrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            DandiText(
                text = "가려지는 화면",
                color = DesignSystemThemeImpl.designSystemColor.contentDefaultLevel0,
                style = DesignSystemThemeImpl.typeScale.textStrongL,
            )
            NyummyModalScrim(Modifier.fillMaxSize())
        }
    }

    @Test
    fun nyummyStateSurface() = snapshot(referenceWidthDp = 210) {
        NyummyStateSurface(type = NyummyStateSurfaceType.Empty, onAction = {})
    }

    @Test
    fun nyummyEditDialog() = snapshot(edgeToEdge = true, referenceWidthDp = 334) {
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

    @Test
    fun nyummyDestructiveDialog() = snapshot(edgeToEdge = true, referenceWidthDp = 334) {
        NyummyDestructiveDialog(
            title = "삭제하지 못했어요",
            body = "네트워크 문제로 기록을 삭제하지 못했어요.\n기록은 그대로 남아 있어요.",
            targetLabel = "치킨 샐러드 · 08:10",
            helper = "연결을 확인한 뒤 다시 요청해요.",
            cancelLabel = "취소",
            confirmLabel = "다시 삭제",
            onCancel = {},
            onConfirm = {},
        )
    }

    @Test
    fun nyummyNoticeDialog() = snapshot(edgeToEdge = true, referenceWidthDp = 342) {
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
            artwork = { FixtureArtwork() },
            benefitsContent = {
                NyummyBadge(label = "한정")
                NyummyBadge(label = "신규", tone = NyummyBadgeTone.Positive)
            },
            primaryLabel = "상점에서 도감팩 보기",
            nextLabel = "다음 소식 보기",
            closeLabel = "닫기",
            onPrimary = {},
            onNext = {},
            onClose = {},
        )
    }

    @Test
    fun nyummyConfirmBottomSheet() = snapshot(edgeToEdge = true, referenceWidthDp = 390) {
        NyummyConfirmBottomSheet(
            title = "여름 고양이 도감팩",
            eyebrow = "고양이 뽑기 · 1회",
            subtitle = "도감팩 1개",
            productArtDescription = "여름 고양이 도감팩",
            productArt = { FixtureArtwork() },
            balanceLabel = "고양이 도감팩",
            balanceValue = "2개 → 1개",
            balanceHelper = "대신 코인 100으로도 뽑을 수 있어요.",
            cancelLabel = "취소",
            confirmLabel = "1회 뽑기",
            onCancel = {},
            onConfirm = {},
        )
    }

    @Test
    fun nyummyMealSummaryBottomSheet() = snapshot(edgeToEdge = true, referenceWidthDp = 370) {
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
            mealsContent = {
                NyummyListRow(
                    title = "닭가슴살 포케",
                    supportingText = "12:36 · 540 kcal",
                    leading = { FixtureFoodIcon() },
                )
            },
            onAddMeal = {},
        )
    }

    @Test
    fun nyummyCollectionDetailBottomSheet() = snapshot(edgeToEdge = true, referenceWidthDp = 390) {
        NyummyCollectionDetailBottomSheet(
            title = "샐러드",
            eyebrow = "음식 아이콘 · 보유 중",
            artworkDescription = "샐러드 픽셀 아이콘",
            artwork = { FixtureArtwork() },
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

    @Test
    fun nyummyBottomNavigation() = snapshot(edgeToEdge = true, referenceWidthDp = 390) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            NyummyBottomNavigation(
                selectedDestination = NyummyNavigationDestination.History,
                onDestinationSelected = {},
            )
        }
    }

    @Test
    fun nyummyFloatingTodayMeals() = snapshot(referenceWidthDp = 142) {
        NyummyFloatingTodayMeals(label = "오늘 현황", onClick = {})
    }

    @Test
    fun nyummyCalendarDay() = snapshot(referenceWidthDp = 70) {
        NyummyCalendarDay(
            day = "22",
            selected = true,
            nutritionStatus = NyummyCalendarNutritionStatus.Positive,
            firstFoodIcon = { FixtureFoodIcon() },
            onClick = {},
        )
    }

    @Test
    fun nyummyCalendarHeaderAction() = snapshot(referenceWidthDp = 128) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NyummyCalendarHeaderAction(NyummyCalendarHeaderDirection.Previous, onClick = {})
            NyummyCalendarHeaderAction(NyummyCalendarHeaderDirection.Next, onClick = {})
        }
    }

    @Test
    fun nyummyMealRow() = snapshot(referenceWidthDp = 370) {
        NyummyMealRow(
            data = mealRowData,
            state = NyummyAnalysisState.Completed,
            onClick = {},
        )
    }

    @Test
    fun nyummyAnalysisBanner() = snapshot(referenceWidthDp = 370) {
        NyummyAnalysisBanner(
            title = "세 번째 끼니 분석이 멈췄어",
            message = "기록은 그대로야. 다시 해볼까?",
            state = NyummyAnalysisBannerState.Failed,
        )
    }

    @Test
    fun nyummyDailyNutritionSummary() = snapshot(referenceWidthDp = 370) {
        NyummyDailyNutritionSummary(data = dailyNutritionData)
    }

    @Test
    fun nyummyMealDetailCard() = snapshot(edgeToEdge = true, referenceWidthDp = 656) {
        NyummyMealDetailCard(
            data = NyummyMealDetailData(
                orderAndTime = "첫 끼 · 12:36",
                name = "닭가슴살 포케",
                calories = "540",
                capturedAt = "2026.07.12 12:36",
            ),
        )
    }

    @Test
    fun nyummyMealNutritionIndicator() = snapshot(referenceWidthDp = 322) {
        NyummyMealNutritionIndicator(data = mealNutritionData) { FixtureFoodIcon() }
    }

    @Test
    fun nyummyPhotoPicker() = snapshot(referenceWidthDp = 370) {
        NyummyPhotoPicker(state = NyummyPhotoPickerState.Empty, onClick = {})
    }

    private fun snapshot(
        edgeToEdge: Boolean = false,
        referenceWidthDp: Int,
        content: @Composable () -> Unit,
    ) {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_5.copy(
                screenWidth = referenceWidthDp,
                xdpi = BaselineDpi,
                ydpi = BaselineDpi,
                density = Density.MEDIUM,
                locale = KoreanLocale,
                softButtons = false,
            ),
            renderingMode = RenderingMode.SHRINK,
        )
        paparazzi.snapshot {
            DesignSystemTheme {
                Surface(color = DesignSystemThemeImpl.designSystemColor.bgDefaultLevel0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (edgeToEdge) Modifier else Modifier.padding(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        content()
                    }
                }
            }
        }
    }

    @Composable
    private fun FixtureIcon() {
        Icon(
            painter = painterResource(R.drawable.arrow_back_24px),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }

    @Composable
    private fun FixtureFoodIcon() {
        Image(
            bitmap = androidx.compose.ui.graphics.ImageBitmap.imageResource(R.drawable.nyummy_food_salad),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            filterQuality = FilterQuality.None,
        )
    }

    @Composable
    private fun FixtureArtwork() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DesignSystemThemeImpl.designSystemColor.bgBrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = androidx.compose.ui.graphics.ImageBitmap.imageResource(R.drawable.nyummy_food_salad),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                filterQuality = FilterQuality.None,
            )
        }
    }

    private companion object {
        const val BaselineDpi = 160
        const val KoreanLocale = "ko-rKR"

        val mealRowData = NyummyMealRowData(
            orderLabel = "첫 끼",
            name = "치킨 샐러드",
            recordedMeta = "08:10 · 사진 기록",
            calories = "412 kcal",
        )

        val dailyNutritionData = NyummyDailyNutritionData(
            currentCalories = "1,420",
            targetCalories = "2,000",
            calorieRatioLabel = "71%",
            carbohydrate = NyummyDailyMacroSummary("탄수화물", "70%", 0.70f),
            protein = NyummyDailyMacroSummary("단백질", "83%", 0.83f),
            fat = NyummyDailyMacroSummary("지방", "63%", 0.63f),
        )

        val mealNutritionData = NyummyMealNutritionData(
            carbohydrate = NyummyNutrientProgress("탄수화물", "168", "240", "42", 0.70f, 0.18f),
            protein = NyummyNutrientProgress("단백질", "62", "75", "28", 0.83f, 0.37f),
            fat = NyummyNutrientProgress("지방", "38", "60", "12", 0.63f, 0.20f),
        )
    }
}
