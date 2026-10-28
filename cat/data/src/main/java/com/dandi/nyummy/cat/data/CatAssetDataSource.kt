package com.dandi.nyummy.cat.data

import android.content.Context
import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** 앱 에셋 `cats/<체형>/animations.json`을 읽는다. 체형 폴더 이름은 고양이 조회의 체형 값과 같다. */
class CatAssetDataSource(
    private val context: Context,
    private val json: Json,
) {
    /** [weight] 체형의 동작 목록. 그 체형 폴더가 없으면 null이다. */
    suspend fun getAnimations(weight: String): CatAnimationSetDTO? = withContext(Dispatchers.IO) {
        val raw = runCatching {
            context.assets.open("$ROOT/$weight/$FILE").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return@withContext null
        json.decodeFromString<CatAnimationSetDTO>(raw)
    }

    /** [weight] 체형 시트 주소의 앞부분. Coil이 그대로 읽는다. */
    fun baseUrl(weight: String): String = "file:///android_asset/$ROOT/$weight/"

    private companion object {
        const val ROOT = "cats"
        const val FILE = "animations.json"
    }
}
