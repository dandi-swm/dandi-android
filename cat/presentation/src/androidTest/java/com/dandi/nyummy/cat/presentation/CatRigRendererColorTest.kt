package com.dandi.nyummy.cat.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dandi.nyummy.cat.domain.rig.CatRigFrame
import com.dandi.nyummy.cat.domain.rig.CatShape
import com.dandi.nyummy.cat.domain.rig.CatShapeOp
import com.dandi.nyummy.cat.entity.rig.PartMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * 코드로 그리는 색 검사: 벌린 입이 스킨 털 색을 따라가는지(QA: 고등어 스킨에서 입만 원화 갈색),
 * 임시 착용 아이템 외곽선이 아이템 데이터의 공식 외곽선 색(#764829)인지(디자이너 폴리싱).
 */
@RunWith(AndroidJUnit4::class)
class CatRigRendererColorTest {

    /** 벌린 입 하나만 그린다. 입 테두리(위쪽, 볼터치 안쪽 타원보다 위) 색을 돌려준다. */
    private fun openMouthRim(skin: String): Int {
        val character = RigCapture.character(skin, "NORMAL")
        val op = CatShapeOp(z = 0, order = 0, shape = CatShape.OPEN_MOUTH).apply {
            floatArrayOf(1f, 0f, 0f, 1f, 0f, 0f).copyInto(matrix)
            floatArrayOf(68f, 40f, 40f, 40f, 0f, 0f).copyInto(params) // 가운데 x, 위 y, 폭, 높이
        }
        val bitmap = createBitmap(RIG_SIZE.toInt(), RIG_SIZE.toInt())
        CatRigRenderer().draw(
            canvas = Canvas(bitmap),
            frame = CatRigFrame(listOf(op)),
            textures = CatRigTextures(emptyMap()),
            spec = character.spec,
            palette = character.bodyRig.palette,
            scale = 1f,
        )
        return bitmap.getPixel(68, 48) // 위 y + 높이의 0.2: 테두리 안, 볼터치(0.45부터) 밖
    }

    @Test
    fun 벌린_입은_스킨_털_색을_따른다() {
        val nyami = RigCapture.character("nyami", "NORMAL")
        assertClose(nyami.bodyRig.palette.line, openMouthRim("nyami"))

        val mackerel = RigCapture.character("mackerel", "NORMAL")
        val expected = CatRigRenderer.tinted(mackerel.bodyRig.palette.line, mackerel.spec.colorOf(PartMaterial.FUR))
        val rim = openMouthRim("mackerel")
        assertClose(expected, rim)
        assertNotEquals("고등어 입이 원화 갈색이면 안 된다", mackerel.bodyRig.palette.line, rim)
    }

    @Test
    fun 임시_착용_아이템_외곽선은_공식_원화_외곽선_색이다() {
        val vest = RigCapture.items.first { it.id == "sample_vest" }
        assertEquals(OFFICIAL_LINE, vest.lineColor)
        val bitmap = requireNotNull(SampleAttachmentArt.draw("sample:vest", vest.width, vest.height, RigCapture.SAMPLE_COLORS, vest.lineColor!!))
        // 가운데 세로 선(외곽선 색)의 한가운데
        val center = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        assertClose(OFFICIAL_LINE, center)
        assertTrue("예전 검정 외곽선이 남아 있으면 안 된다", countColor(bitmap, RigCapture.SAMPLE_COLORS.ink) == 0)
    }

    private fun countColor(b: Bitmap, color: Int): Int {
        var n = 0
        for (y in 0 until b.height) for (x in 0 until b.width) if (b.getPixel(x, y) == color) n++
        return n
    }

    private fun assertClose(expected: Int, actual: Int) {
        val ok = (0..2).all { c -> abs(((expected shr (c * 8)) and 0xFF) - ((actual shr (c * 8)) and 0xFF)) <= 2 }
        assertTrue("기대 #%06X, 실제 #%06X".format(expected and 0xFFFFFF, actual and 0xFFFFFF), ok && (actual ushr 24) > 240)
    }

    private companion object {
        const val OFFICIAL_LINE = 0xFF764829.toInt()
    }
}
