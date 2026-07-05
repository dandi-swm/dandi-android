package com.swm.dandi.tti

/**
 * TTI 측정 결과를 외부 텔레메트리(예: Datadog RUM)로 흘려보내기 위한 추상화.
 * :tti 모듈을 플랫폼/SDK 비의존으로 유지하기 위한 Seam.
 */
interface TTIReporter {
    fun startView(key: String, name: String, attributes: Map<String, Any?> = emptyMap())
    fun stopView(key: String, attributes: Map<String, Any?> = emptyMap())
}

object NoOpTTIReporter : TTIReporter {
    override fun startView(key: String, name: String, attributes: Map<String, Any?>) = Unit
    override fun stopView(key: String, attributes: Map<String, Any?>) = Unit
}
