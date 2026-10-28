package com.dandi.nyummy.cat.data.line

import android.content.Context
import com.dandi.nyummy.cat.data.R
import com.dandi.nyummy.cat.domain.CatLineRepository
import com.dandi.nyummy.cat.entity.CatLineVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 앱에 든 대사 은행(`res/raw/nyummy_lines.json`)을 한 번 읽어 둔다. */
class CatLineRepositoryImpl(
    private val context: Context,
    private val json: Json,
) : CatLineRepository {

    private var cached: List<CatLineVO>? = null

    override suspend fun getLines(): List<CatLineVO> = cached ?: withContext(Dispatchers.IO) {
        val raw = context.resources.openRawResource(R.raw.nyummy_lines).bufferedReader().use { it.readText() }
        json.decodeFromString<CatLinesDTO>(raw).toVO()
    }.also { cached = it }
}

/** @property lines 대사 목록 */
@Serializable
internal data class CatLinesDTO(
    val version: Int? = null,
    val lines: List<CatLineDTO>? = null,
) {
    fun toVO(): List<CatLineVO> = lines.orEmpty().mapNotNull { it.toVO() }
}

/**
 * @property text 대사
 * @property tags 쓸 상황
 */
@Serializable
internal data class CatLineDTO(
    val text: String? = null,
    val tags: List<String>? = null,
) {
    fun toVO(): CatLineVO? {
        val t = text?.takeIf { it.isNotBlank() } ?: return null
        val tagSet = tags.orEmpty().filter { it.isNotBlank() }.toSet()
        if (tagSet.isEmpty()) return null
        return CatLineVO(text = t, tags = tagSet)
    }
}
