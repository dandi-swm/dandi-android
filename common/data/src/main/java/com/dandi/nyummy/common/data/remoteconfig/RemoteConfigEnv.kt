package com.dandi.nyummy.common.data.remoteconfig

/**
 * 원격 구성 키를 고르는 빌드 환경. 콘솔에서 운영 값과 개발 값을 나눠 둔다.
 *
 * - [RELEASE]: 스토어에 올리는 release 빌드. 접미사 없는 운영 키를 읽는다.
 * - [DEBUG]: 그 밖의 빌드(debug, benchmark, 베이스라인 프로파일 수집용). `_debug` 키를 읽어 측정이나 개발 중에
 *   운영 강제 업데이트와 운영 플래그에 영향을 받지 않는다.
 *
 * 콘솔의 기존 `_qa` 키는 더 읽지 않는다. 이름과 값은 그대로 두었다(docs/remote-config-force-update.md).
 */
enum class RemoteConfigEnv(private val keySuffix: String) {
    RELEASE(""),
    DEBUG("_debug"),
    ;

    fun keyOf(baseKey: String): String = baseKey + keySuffix

    companion object {
        fun of(isReleaseBuild: Boolean): RemoteConfigEnv = if (isReleaseBuild) RELEASE else DEBUG
    }
}
