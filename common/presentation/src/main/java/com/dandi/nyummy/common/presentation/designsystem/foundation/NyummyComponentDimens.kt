package com.dandi.nyummy.common.presentation.designsystem.foundation

import androidx.compose.ui.unit.dp

/**
 * Figma 컴포넌트에 직접 적힌 치수 중 `Nyummy / 3 Dimension` 변수로 묶이지 않은 값.
 *
 * 토큰(NyummyTheme)은 Figma 변수와 1:1로 유지하고, 컴포넌트 내부에서만 쓰는 고정 치수는 여기에 모은다.
 * 화면 코드에서는 쓰지 않는다.
 */
internal object NyummyComponentDimens {
    /** Button S, Chip 높이. */
    val CompactControlHeight = 36.dp

    /** Button S, Chip 좌우 여백. */
    val CompactControlHorizontalPadding = 14.dp

    /** Text Field, Text Area의 라벨과 입력칸 사이. */
    val FieldLabelGap = 6.dp

    /** Text Field 입력칸 높이. */
    val TextFieldHeight = 52.dp

    /** Text Field 아이콘과 글자 사이. */
    val TextFieldIconGap = 10.dp

    /** Text Area 입력칸 높이(고정, 내용이 많으면 안에서 스크롤). */
    val TextAreaHeight = 180.dp

    /** Text Area 위쪽 안쪽 여백. */
    val TextAreaTopPadding = 14.dp

    /** 인증 코드 셀 크기. */
    val CodeCellWidth = 48.dp
    val CodeCellHeight = 56.dp

    /** Wheel Picker 전체 높이와 행 높이(50 × 3). */
    val WheelPickerHeight = 150.dp
    val WheelPickerRowHeight = 50.dp

    /** Wheel Picker 선택 밴드 높이와 좌우 들여쓰기. */
    val WheelPickerBandHeight = 46.dp
    val WheelPickerBandInset = 6.dp

    /** Checkbox, Radio 크기와 Checkbox 안 체크 아이콘, Radio 안 점. */
    val SelectionControlSize = 24.dp
    val CheckboxCheckSize = 16.dp
    val RadioDotSize = 12.dp

    /** Switch 트랙과 썸. */
    val SwitchWidth = 52.dp
    val SwitchHeight = 32.dp
    val SwitchThumbSize = 26.dp
    val SwitchThumbInset = 3.dp

    /** List Row 최소 높이와 앞 아이콘 칸. */
    val ListRowMinHeight = 56.dp
    val ListRowLeadingSize = 40.dp

    /** HUD Pill 높이와 아이콘, 숫자 사이 간격. */
    val HudPillHeight = 40.dp
    val HudPillGap = 6.dp
    val HudPillEndPadding = 14.dp

    /** Badge 세로 여백. */
    val BadgeVerticalPadding = 2.dp

    /** Linear Progress 두께. */
    val LinearProgressHeight = 10.dp

    /** Skeleton 한 줄 높이. */
    val SkeletonLineHeight = 14.dp

    /** Top Bar 높이. */
    val TopBarHeight = 56.dp

    /** Bottom Nav 탭 칸, 선택 상자, 아이콘, 위아래 여백. */
    val BottomNavTabWidth = 70.dp
    val BottomNavTabHeight = 58.dp
    val BottomNavSelectedWidth = 60.dp
    val BottomNavSelectedHeight = 56.dp
    val BottomNavIconSize = 28.dp
    val BottomNavTopPadding = 6.dp

    /** Tab Item 아래 여백과 밑줄 두께. */
    val TabItemBottomPadding = 10.dp
    val TabIndicatorHeight = 2.dp

    /** Step Indicator 막대 두께와 간격. */
    val StepBarHeight = 6.dp
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
}
