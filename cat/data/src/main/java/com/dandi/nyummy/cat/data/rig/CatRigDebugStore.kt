package com.dandi.nyummy.cat.data.rig

import android.content.Context

/** 홈, 마이룸 냐미의 스킨과 끼운 아이템. 착용 서버 API가 생기기 전까지는 debug 기기 설정([CatRigDebugStore])이다. */
interface CatRigDebugSettings {
    /** 스킨 id. 정하지 않았으면 null(기본 냐미) */
    val skin: String?

    /** 끼운 아이템 id */
    val equipped: List<String>
}

/**
 * debug 빌드에서 홈, 마이룸 냐미의 스킨과 끼운 아이템을 바꿔 보는 기기 설정. `CatRigDebugActivity`(app debug)만 쓴다.
 * release 빌드에는 쓰는 화면이 없어 값이 비어 있고, 그러면 기본 냐미에 아이템 없이 그린다.
 */
class CatRigDebugStore(context: Context) : CatRigDebugSettings {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var skin: String?
        get() = prefs.getString(KEY_SKIN, null)
        set(value) = prefs.edit().putString(KEY_SKIN, value).apply()

    override var equipped: List<String>
        get() = prefs.getString(KEY_EQUIPPED, null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        set(value) = prefs.edit().putString(KEY_EQUIPPED, value.joinToString(",")).apply()

    private companion object {
        const val FILE = "cat_rig_debug"
        const val KEY_SKIN = "skin"
        const val KEY_EQUIPPED = "equipped"
    }
}
