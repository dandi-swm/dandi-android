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
