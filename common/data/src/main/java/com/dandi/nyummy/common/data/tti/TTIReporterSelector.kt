package com.dandi.nyummy.common.data.tti

import com.dandi.nyummy.tti.NoOpTTIReporter
import com.dandi.nyummy.tti.TTIReporter

/**
 * 주입할 [TTIReporter] 를 고른다.
 *
 * - release 가 아니면 [NoOpTTIReporter]. 개발, 벤치마크 측정이 실사용자 지표에 섞이지 않게 한다(로그는 TTILogger 가 남긴다).
 * - release 면 [createRemote] 로 만든 외부 전송 reporter. 만들다가 예외가 나면(Firebase 초기화 실패 등)
 *   [NoOpTTIReporter] 로 떨어진다. 관측 도구 장애로 앱 시작이나 화면 생성이 죽으면 안 되기 때문이다.
 */
fun selectTTIReporter(isReleaseBuild: Boolean, createRemote: () -> TTIReporter): TTIReporter {
    if (!isReleaseBuild) return NoOpTTIReporter
    return runCatching(createRemote).getOrDefault(NoOpTTIReporter)
}
