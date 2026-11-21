package com.dandi.nyummy.cat.data.rig

import android.content.Context
import com.dandi.nyummy.cat.entity.rig.AttachmentItemVO
import com.dandi.nyummy.cat.entity.rig.CatBodyRigVO
import com.dandi.nyummy.cat.entity.rig.CatCharacterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 앱 에셋 `cats/rig/`(파츠 묶음, 스킨, 아이템)을 읽는다. 파일이 없거나 형식이 맞지 않으면 null이다. */
class CatRigAssetDataSource(private val context: Context) {

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

    private fun read(path: String): String? = runCatching {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrNull()
}
