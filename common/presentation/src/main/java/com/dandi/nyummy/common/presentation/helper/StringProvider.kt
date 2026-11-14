package com.dandi.nyummy.common.presentation.helper

import androidx.annotation.StringRes

/**
 * ViewModel이 Compose 밖에서 문자열 리소스를 기기 언어로 읽을 때 쓴다(스낵바·다이얼로그 문구, 버튼 글자).
 * Context를 ViewModel에 넣지 않으려고 둔다. 단위 테스트는 람다로 대신한다(예: `StringProvider { id, _ -> "string:$id" }`).
 */
fun interface StringProvider {
    fun get(@StringRes id: Int, vararg formatArgs: Any): String
}
