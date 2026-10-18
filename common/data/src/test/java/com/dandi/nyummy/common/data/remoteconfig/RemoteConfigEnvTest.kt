package com.dandi.nyummy.common.data.remoteconfig

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteConfigEnvTest {

    @Test
    fun `release 빌드는 접미사 없는 운영 키를 읽는다`() {
        val env = RemoteConfigEnv.of(isReleaseBuild = true)

        assertEquals(RemoteConfigEnv.RELEASE, env)
        assertEquals("android_minimum_version_code", env.keyOf("android_minimum_version_code"))
    }

    @Test
    fun `release가 아닌 빌드(debug, benchmark)는 _debug 키를 읽는다`() {
        val env = RemoteConfigEnv.of(isReleaseBuild = false)

        assertEquals(RemoteConfigEnv.DEBUG, env)
        assertEquals("android_mail_item_tab_enabled_debug", env.keyOf("android_mail_item_tab_enabled"))
    }
}
