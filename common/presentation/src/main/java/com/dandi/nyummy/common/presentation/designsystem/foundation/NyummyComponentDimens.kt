package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.ui.unit.dp

/**
 * 컴포넌트 디자인에 직접 정해진 치수 중 공통 치수 토큰(spacing, radius, size)에 없는 값.
 *
 * 토큰(NyummyTheme)은 디자인 토큰과 1:1로 유지하고, 컴포넌트 내부에서만 쓰는 고정 치수는 여기에 모은다.
 * 화면 코드에서는 쓰지 않는다.
 */
internal object NyummyComponentDimens {
    /** Chip 높이. */
    val CompactControlHeight = 36.dp

    /** Button S(작은 버튼) 높이. */
    val ButtonSmallHeight = 32.dp

    /** 입력칸이 입력 중이거나 오류일 때 테두리 두께. */
    val FieldActiveBorderWidth = 1.5.dp

    /** Text Field 입력칸 높이. */
    val TextFieldHeight = 52.dp

    /** Text Area 입력칸 높이(고정, 내용이 많으면 안에서 스크롤). */
    val TextAreaHeight = 140.dp

    /** Text Area 위아래 안쪽 여백. */
    val TextAreaVerticalPadding = 14.dp

    /** 인증 코드 셀 크기. */
    val CodeCellWidth = 48.dp
    val CodeCellHeight = 56.dp

    /** Wheel Picker 전체 높이와 행 높이(50 × 3). */
    val WheelPickerHeight = 150.dp
    val WheelPickerRowHeight = 50.dp

    /** Wheel Picker 선택 밴드 높이와 좌우 들여쓰기. */
    val WheelPickerBandHeight = 44.dp
    val WheelPickerBandInset = 6.dp

    /** 작은 Wheel Picker(폼 안 생년월일·키·몸무게): 높이 105, 행 35 × 3, 밴드 31. */
    val WheelPickerCompactHeight = 105.dp
    val WheelPickerCompactRowHeight = 35.dp
    val WheelPickerCompactBandHeight = 31.dp
    val WheelPickerCompactBandInset = 9.dp
    val WheelPickerCompactBandRadius = 10.dp

    /** Radio 크기와 안 점. */
    val SelectionControlSize = 24.dp
    val RadioDotSize = 12.dp

    /** 식사 시각 줄의 오른쪽 시각 칸 너비. */
    val MealTimeChipWidth = 150.dp

    /** Checkbox 크기, 모서리, 안 체크 아이콘. */
    val CheckboxSize = 22.dp
    val CheckboxRadius = 6.dp
    val CheckboxCheckSize = 16.dp

    /** Switch 트랙과 썸. */
    val SwitchWidth = 48.dp
    val SwitchHeight = 28.dp
    val SwitchThumbSize = 22.dp
    val SwitchThumbInset = 3.dp

    /** List Row 최소 높이와 앞 아이콘 칸. */
    val ListRowMinHeight = 56.dp
    val ListRowLeadingSize = 40.dp

    /** 안 읽은 소식을 알리는 빨간 점. 홈 HUD 버튼의 점과 같은 크기다. */
    val UnreadDotSize = 9.dp

    /** 스와이프 삭제 버튼 폭, 위아래 안쪽 여백, 행이 밀려 멈추는 거리. */
    val SwipeActionWidth = 64.dp
    val SwipeActionVerticalInset = 8.dp
    val SwipeRevealDistance = 80.dp

    /** Reward Row 앞 그림 칸. */
    val RewardRowLeadingSize = 48.dp

    /** Reward Row 앞 칸 안 그림. 코인, 아이템은 36, 보상 상자는 칸을 채우는 48. */
    val RewardRowImageSize = 36.dp
    val RewardRowLargeImageSize = 48.dp

    /** HUD Pill 높이와 아이콘, 숫자 사이 간격. */
    val HudPillHeight = 40.dp
    val HudPillEndPadding = 14.dp

    /** Badge 세로 여백. label/s 줄 높이(14)와 합쳐 높이 20이 된다. */
    val BadgeVerticalPadding = 3.dp

    /** Info Chip 세로 여백. body/s 줄 높이(17)와 합쳐 높이 24에 가깝게 맞춘다. */
    val InfoChipVerticalPadding = 3.dp

    /** Segmented 바깥 모서리. */
    val SegmentedContainerRadius = 14.dp

    /** Segmented 고른 칸 모서리. */
    val SegmentedOptionRadius = 10.dp

    /** Skeleton 한 줄 높이. */
    val SkeletonLineHeight = 14.dp

    /** Top Bar 높이. */
    val TopBarHeight = 56.dp

    /** Bottom Nav 가운데 동작 버튼(바 안의 초록 버튼). */
    val BottomNavActionSize = 54.dp

    /** Tab Item 아래 여백과 밑줄 두께. */
    val TabItemBottomPadding = 10.dp
    val TabIndicatorHeight = 2.dp

    /** Step Indicator 막대 두께와 간격. */
    val StepBarHeight = 4.dp
    val StepBarGap = 6.dp

    /** Voice Bubble 꼬리 크기(밑변 20 × 높이 12)와 몸통 모서리에서 꼬리까지 거리. */
    val BubbleTailBase = 20.dp
    val BubbleTailHeight = 12.dp
    val BubbleTailOffset = 24.dp
    val BubbleTailOffsetVertical = 14.dp

    /** Coach Card 말풍선 모서리, 안쪽 여백, 매달린 냐미 크기. */
    val CoachCardRadius = 20.dp
    val CoachCardTopPadding = 26.dp
    val CoachCardBottomPadding = 10.dp
    val CoachCardHorizontalPadding = 18.dp
    val CoachHangWidth = 96.dp
    val CoachHangHeight = 66.dp

    /** Voice Toast 안쪽 여백. */
    val VoiceToastPadding = 6.dp

    /** 카드, 대화창, 버튼, 스낵바의 최대 너비(태블릿에서는 가운데 정렬). */
    val ContentMaxWidth = 480.dp

    /** Dialog 최대 너비와 화면 좌우 여백. */
    val DialogMaxWidth = 320.dp
    /** Dialog 위쪽 안쪽 여백. */
    val DialogTopPadding = 28.dp
    val DialogScreenMargin = 32.dp

    /** Star Rating 별 하나 크기와 별 사이 간격. */
    val StarRatingStarSize = 36.dp
    val StarRatingGap = 6.dp

    /** Bottom Sheet 손잡이. */
    val SheetHandleWidth = 36.dp
    val SheetHandleHeight = 4.dp

    /** Confirm Sheet 냐미 크기와 바닥 타원. */
    val ConfirmSheetPoseSize = 128.dp
    val ConfirmSheetGroundWidth = 80.dp
    val ConfirmSheetGroundHeight = 10.dp

    /** Snackbar 높이와 행동 버튼 칸. */
    val SnackbarHeight = 56.dp
    val SnackbarActionMinWidth = 60.dp
    val SnackbarActionHeight = 40.dp

    /** State Surface 로딩 막대. */
    val StateLoadingBarWidth = 200.dp
    val StateLoadingBarHeight = 8.dp

    /** 화면 안 안내 카드(NyummyInlineNotice)의 좌우 여백과 아이콘 크기. */
    val InlineNoticeHorizontalPadding = 14.dp
    val InlineNoticeIconSize = 18.dp

    /** 처리 중 아이콘(NyummySpinner) 크기. */
    val SpinnerSize = 36.dp

    /** 영양소 일러스트 아이콘(NyummyNutrientStat) 크기. */
    val NutrientIconSize = 24.dp

    /** 식사 행(NyummyMealRow) 왼쪽 사진, 냐미 자리 크기. */
    val MealRowLeadingSize = 40.dp

    /** 꾸민 글자(NyummyRichText) 안 코인 아이콘 크기. */
    val RichTextCoinSize = 18.dp

    /** 상품 태그(NyummyTag Item) 좌우 여백. 위아래 여백은 spacing.s2. */
    val TagItemHorizontalPadding = 6.dp

    /** 배너 배지(NyummyTag Banner) 최소 높이와 여백. */
    val TagBannerMinHeight = 24.dp
    val TagBannerHorizontalPadding = 10.dp
    val TagBannerVerticalPadding = 3.dp
}
