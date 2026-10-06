package com.dandi.nyummy.main.presentation.navigation

import androidx.compose.runtime.Composable

/**
 * 앱 내 한 페이지의 호스트(main/presentation) 측 메타데이터.
 *
 * - 본 객체는 그 path 가 어떤 Composable 로 렌더되며 어떤 백스택 특성을 가지는지를
 *   호스트 측에서 단일 위치로 모은다. 새 페이지 추가 시 본 파일의 [appRoutes] 에만 한 줄 추가하면 된다.
 */
data class AppRoute(
    val path: String,
    val isBottomTab: Boolean = false,
    /**
     * true면 Scaffold가 시스템 바 인셋만큼 화면을 밀지 않는다. 배경을 상태 표시줄 뒤까지 그리는
     * 전체 화면(인트로, 로그인 등)에 쓰고, 이때 화면이 직접 인셋을 처리한다.
     */
    val drawsBehindSystemBars: Boolean = false,
    /**
     * true면 이 화면에 있는 동안 상태 바와 내비게이션 바 아이콘을 밝은 색으로 그린다.
     * 어두운 배경을 시스템 바 뒤까지 그리는 화면(온보딩의 밤 골목 등)에 쓴다. 다른 화면으로 나가면 어두운 아이콘으로 돌아온다.
     */
    val usesLightSystemBarIcons: Boolean = false,
    /**
     * deep-link 진입 시 구성할 시작 백스택. 일반 페이지는 자기 자신만 푸시된다.
     */
    val syntheticStack: (args: Map<String, String>) -> List<GenericNavKey> = { args ->
        listOf(GenericNavKey(path, args))
    },
    /**
     * 페이지 본체 렌더러. args 를 받아 Composable 을 호출한다.
     */
    val render: @Composable (args: Map<String, String>) -> Unit,
)
