package com.dandi.nyummy.cat.data.rig

import android.content.Context
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 앱 에셋 `cats/rig/`(파츠 묶음, 스킨, 아이템)을 읽는다. 파일이 없거나 형식이 맞지 않으면 null이다.
 *
 * @param read 에셋 경로의 글을 읽는다. 없으면 null. 앱은 [Context.getAssets], 테스트는 모듈의 에셋 폴더를 읽는다.
 */
class CatRigAssetDataSource(private val read: (path: String) -> String?) {

    constructor(context: Context) : this(assetReader(context))

    suspend fun getBodyRig(partSet: String, body: String): CatBodyRigVO? = withContext(Dispatchers.IO) {
        val dir = CatRigParser.partDir(partSet, body)
        read(dir + CatRigParser.RIG_FILE)?.let { CatRigParser.bodyRig(it, dir) }
    }

    suspend fun getSkin(id: String): CatCharacterSpec? = withContext(Dispatchers.IO) {
        read(CatRigParser.skinPath(id))?.let { CatRigParser.skin(it) }
    }

    suspend fun getItems(): List<AttachmentItemVO> = withContext(Dispatchers.IO) {
        read(CatRigParser.ITEMS_PATH)?.let { CatRigParser.items(it) }.orEmpty()
    }

    private companion object {
        fun assetReader(context: Context): (String) -> String? = { path ->
            runCatching { context.assets.open(path).bufferedReader().use { it.readText() } }.getOrNull()
        }
    }
}
