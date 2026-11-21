package com.dandi.nyummy.cat.data.rig

import android.content.Context

/**
 * debug 빌드에서 리그를 켜 보는 기기 설정. `CatRigDebugActivity`(app debug)만 쓴다.
 * release 빌드에는 쓰는 화면이 없어 값이 비어 있고, 그러면 원격 구성 값만 따른다.
 */
class CatRigDebugStore(context: Context) {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 리그 켜기 덮어쓰기. null이면 원격 구성 값을 따른다. */
    var enabledOverride: Boolean?
        get() = if (prefs.contains(KEY_ENABLED)) prefs.getBoolean(KEY_ENABLED, false) else null
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY_ENABLED) else putBoolean(KEY_ENABLED, value) }.apply()
        }

    var skin: String?
        get() = prefs.getString(KEY_SKIN, null)
        set(value) = prefs.edit().putString(KEY_SKIN, value).apply()

    var equipped: List<String>
        get() = prefs.getString(KEY_EQUIPPED, null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        set(value) = prefs.edit().putString(KEY_EQUIPPED, value.joinToString(",")).apply()

    private companion object {
        const val FILE = "cat_rig_debug"
        const val KEY_ENABLED = "enabled"
        const val KEY_SKIN = "skin"
        const val KEY_EQUIPPED = "equipped"
    }
}
