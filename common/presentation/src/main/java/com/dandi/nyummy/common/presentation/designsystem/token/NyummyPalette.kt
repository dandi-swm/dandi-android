package com.dandi.nyummy.common.presentation.designsystem.token

import androidx.compose.ui.graphics.Color

/**
 * 디자인 시스템의 기본 색 팔레트. 피그마 `Nyummy v2 / 1 Primitive` 컬렉션과 1:1로 맞춘다.
 *
 * 화면과 컴포넌트는 이 값을 직접 쓰지 않고 [com.dandi.nyummy.common.presentation.designsystem.theme.NyummyColors]
 * 역할 색만 참조한다. 팔레트 값이 바뀌면 이 파일만 갱신한다.
 */
internal object NyummyPalette {
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    /** 모달 딤(검정 40%). */
    val Scrim = Color(0x66000000)

    /** 눌림 피드백(검정 8%). 표면 위에 덮는다. */
    val PressedOverlay = Color(0x14000000)

    val Gray50 = Color(0xFFF7F7F7)
    val Gray100 = Color(0xFFF0F0F0)
    val Gray150 = Color(0xFFEDEDED)
    val Gray200 = Color(0xFFE4E4E4)
    val Gray300 = Color(0xFFC0C0C0)
    val Gray400 = Color(0xFFA5A5A5)
    val Gray500 = Color(0xFF939393)
    val Gray700 = Color(0xFF686868)
    val Gray900 = Color(0xFF262626)

    val Evergreen50 = Color(0xFFF4FAF6)
    val Evergreen100 = Color(0xFFECF5F0)
    val Evergreen200 = Color(0xFFD2E7DA)
    val Evergreen300 = Color(0xFFACCFBA)
    val Evergreen400 = Color(0xFF7FB394)
    val Evergreen500 = Color(0xFF5C9A74)

    /** 브랜드 앵커. 다른 값으로 스냅하지 않는다. */
    val Evergreen600 = Color(0xFF43855D)
    val Evergreen700 = Color(0xFF376D4C)
    val Evergreen800 = Color(0xFF2B553B)
    val Evergreen900 = Color(0xFF1F3E2B)

    /** 냐미 말투와 코인 숫자에 쓰는 짙은 초록-검정. */
    val EvergreenInk = Color(0xFF243329)

    val Sage400 = Color(0xFFAFB8AF)

    val Red50 = Color(0xFFFDEBED)
    val Red500 = Color(0xFFEA3348)
    val Red600 = Color(0xFFC92639)

    val Orange50 = Color(0xFFFCF2E8)
    val Orange500 = Color(0xFFDC7E19)

    val Gold300 = Color(0xFFFBC15D)

    val SceneRewardStage = Color(0xFF173221)
    val SceneRoomFloor = Color(0xFFC57D33)

    /** 컷신 바탕. 종이(크림), 새벽(라벤더), 밤(남색). */
    val SceneCutscenePaper = Color(0xFFF6ECDC)
    val SceneCutsceneDawn = Color(0xFFE4E1F2)
    val SceneCutsceneNight = Color(0xFF222B57)

    // 외부 브랜드 색. 피그마 v2에는 카카오 노랑·심볼만 있고, 나머지는 소셜 로그인 버튼 가이드를 따르려고 남긴다.
    val ExternalNaverGreen = Color(0xFF03C75A)
    val ExternalGoogleBlue = Color(0xFF4285F4)
    val ExternalGoogleGreen = Color(0xFF34A853)
    val ExternalGoogleYellow = Color(0xFFFBBC05)
    val ExternalGoogleRed = Color(0xFFEA4335)
    val ExternalKakaoYellow = Color(0xFFFEE500)
    val ExternalKakaoSymbol = Color(0xFF000000)
    val ExternalKakaoLabel = Color(0xD9000000)
}
