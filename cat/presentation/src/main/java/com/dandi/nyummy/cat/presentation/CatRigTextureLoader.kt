package com.dandi.nyummy.cat.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dandi.nyummy.cat.entity.rig.AttachmentArtVO
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 리그 고양이의 그림을 읽는다. 파츠는 앱 에셋(`cats/rig/parts/...`), 아이템은 에셋 경로이거나
 * `sample:`로 시작하면 코드로 그린 검증용 그림([SampleAttachmentArt])이다.
 *
 * 마지막으로 읽은 고양이 하나만 기억해 홈과 마이룸을 오갈 때 다시 디코딩하지 않는다(체형 1종 약 2.5MB).
 */
object CatRigTextureLoader {

    private var cachedKey: String? = null
    private var cached: CatRigTextures? = null

    suspend fun load(context: Context, character: CatRigCharacterVO, colors: SampleArtColors): CatRigTextures =
        withContext(Dispatchers.IO) {
            val key = cacheKey(character)
            synchronized(this@CatRigTextureLoader) { if (key == cachedKey) cached?.let { return@withContext it } }
            val bitmaps = HashMap<String, Bitmap>()
            val rig = character.bodyRig
            for (layer in rig.layers) {
                decode(context, rig.assetDir + layer.file)?.let { bitmaps["part:${layer.id}"] = it }
            }
            for (item in character.equipped.values) {
                listOfNotNull(item.art, item.backArt).forEach { art ->
                    art(context, art, item.width, item.height, colors, item.lineColor)?.let { bitmaps[art.key] = it }
                }
            }
            CatRigTextures(bitmaps).also {
                synchronized(this@CatRigTextureLoader) {
                    cachedKey = key
                    cached = it
                }
            }
        }

    private fun cacheKey(character: CatRigCharacterVO): String =
        character.bodyRig.assetDir + "|" + character.equipped.values.sortedBy { it.id }.joinToString(",") { it.id }

    private fun art(context: Context, art: AttachmentArtVO, width: Float, height: Float, colors: SampleArtColors, lineColor: Int?): Bitmap? =
        if (art.key.startsWith(SampleAttachmentArt.PREFIX)) {
            SampleAttachmentArt.draw(art.key, width, height, colors, lineColor ?: colors.ink)
        } else {
            decode(context, art.key)
        }

    private fun decode(context: Context, path: String): Bitmap? = runCatching {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
        }?.also { it.setHasMipMap(true) }
    }.getOrNull()
}
