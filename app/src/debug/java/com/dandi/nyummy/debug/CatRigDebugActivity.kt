package com.dandi.nyummy.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dandi.nyummy.cat.data.dto.CatAnimationSetDTO
import com.dandi.nyummy.cat.data.rig.CatRigDebugStore
import com.dandi.nyummy.cat.data.rig.CatRigParser
import com.dandi.nyummy.cat.entity.CatState
import com.dandi.nyummy.cat.entity.rig.CatRigCharacterVO
import com.dandi.nyummy.cat.presentation.CatRigMotion
import com.dandi.nyummy.cat.presentation.CatRigView
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButton
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonSize
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyButtonStyle
import com.dandi.nyummy.common.presentation.designsystem.component.NyummyText
import com.dandi.nyummy.common.presentation.designsystem.theme.NyummyTheme
import kotlinx.serialization.json.Json

/**
 * 리그 냐미 확인용 디버그 화면(debug 빌드에만 포함). 체형, 스킨, 착용 아이템, 상황, 동작을 골라 바로 재생해 본다.
 * "홈·마이룸에 적용"을 켜면 원격 구성과 상관없이 이 기기의 홈과 마이룸 냐미가 리그로 바뀐다(고른 스킨, 아이템 포함).
 *
 * 실행: adb shell am start -n com.dandi.nyummy/.debug.CatRigDebugActivity
 *   [--es body PLUMP] [--es skin mackerel] [--es outfit full_over] [--es motion pop] [--ez apply true|false]
 */
class CatRigDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = CatRigDebugStore(this)
        if (intent.hasExtra(EXTRA_APPLY)) store.enabledOverride = intent.getBooleanExtra(EXTRA_APPLY, false)
        val initial = DebugChoice(
            body = intent.getStringExtra(EXTRA_BODY) ?: "NORMAL",
            skin = intent.getStringExtra(EXTRA_SKIN) ?: store.skin ?: "nyami",
            outfit = intent.getStringExtra(EXTRA_OUTFIT) ?: "none",
            motion = intent.getStringExtra(EXTRA_MOTION),
        )
        if (intent.hasExtra(EXTRA_SKIN) || intent.hasExtra(EXTRA_OUTFIT)) {
            store.skin = initial.skin
            store.equipped = OUTFITS.getValue(initial.outfit)
        }
        setContent {
            NyummyTheme {
                CatRigDebugScreen(initial, store)
            }
        }
    }

    private companion object {
        const val EXTRA_BODY = "body"
        const val EXTRA_SKIN = "skin"
        const val EXTRA_OUTFIT = "outfit"
        const val EXTRA_MOTION = "motion"
        const val EXTRA_APPLY = "apply"
    }
}

private data class DebugChoice(val body: String, val skin: String, val outfit: String, val motion: String?)

private val BODIES = listOf("LEAN", "SLIM", "NORMAL", "CHUBBY", "PLUMP")
private val SKINS = listOf("nyami", "mackerel")
private val OUTFITS = mapOf(
    "none" to emptyList(),
    "beanie" to listOf("sample_beanie"),
    "crown" to listOf("sample_crown"),
    "full_over" to listOf("sample_beanie", "sample_vest", "sample_bell_collar", "sample_glasses"),
    "full_between" to listOf("sample_crown", "sample_vest", "sample_bell_collar", "sample_glasses"),
)
private val json = Json { ignoreUnknownKeys = true }

@Composable
private fun CatRigDebugScreen(initial: DebugChoice, store: CatRigDebugStore) {
    val context = LocalContext.current
    var body by remember { mutableStateOf(initial.body) }
    var skin by remember { mutableStateOf(initial.skin) }
    var outfit by remember { mutableStateOf(initial.outfit) }
    var state by remember { mutableStateOf(CatState.HOME_DAY) }
    var motion by remember { mutableStateOf(initial.motion ?: "blink") }
    var playId by remember { mutableIntStateOf(1) }
    var applied by remember { mutableStateOf(store.enabledOverride) }

    fun read(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    val character = remember(body, skin, outfit) {
        val spec = CatRigParser.skin(read(CatRigParser.skinPath(skin)))!!
        val dir = CatRigParser.partDir(spec.partSet, body)
        val items = CatRigParser.items(read(CatRigParser.ITEMS_PATH)).filter { it.id in OUTFITS.getValue(outfit) }
        CatRigCharacterVO(spec, CatRigParser.bodyRig(read(dir + CatRigParser.RIG_FILE), dir)!!, items.associateBy { it.slot })
    }
    val animations = remember(body) {
        json.decodeFromString<CatAnimationSetDTO>(read("cats/$body/animations.json")).toVO(body, "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NyummyTheme.colors.bg.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(300.dp).align(Alignment.CenterHorizontally)) {
            CatRigView(
                character = character,
                motion = CatRigMotion(motion = motion, state = state, playId = playId, restMillis = 800),
                onMotionFinished = {},
                modifier = Modifier.size(300.dp),
            )
        }
        NyummyText("지금: $skin / $body / $outfit / $state / $motion", style = NyummyTheme.typography.labelM)
        Chips("체형", BODIES, body) { body = it }
        Chips("스킨", SKINS, skin) { skin = it }
        Chips("착용", OUTFITS.keys.toList(), outfit) { outfit = it }
        Chips("상황", animations.animations.keys.map { it.name }, state.name) { state = CatState.from(it) }
        val motions = animations.animations[state]?.motions.orEmpty().map { it.name }
        Chips("동작", motions, motion) {
            motion = it
            playId++
        }
        NyummyText("홈, 마이룸 적용: ${applied ?: "원격 구성 따름"}", style = NyummyTheme.typography.labelM)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NyummyButton("켜기", onClick = {
                store.enabledOverride = true
                store.skin = skin
                store.equipped = OUTFITS.getValue(outfit)
                applied = true
            }, size = NyummyButtonSize.M)
            NyummyButton("끄기", onClick = {
                store.enabledOverride = false
                applied = false
            }, size = NyummyButtonSize.M, style = NyummyButtonStyle.Neutral)
            NyummyButton("원격 구성", onClick = {
                store.enabledOverride = null
                applied = null
            }, size = NyummyButtonSize.M, style = NyummyButtonStyle.Text)
        }
    }
}

@Composable
private fun Chips(label: String, values: List<String>, selected: String, onSelect: (String) -> Unit) {
    NyummyText(label, style = NyummyTheme.typography.labelS)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        values.forEach { v ->
            NyummyButton(
                text = v,
                onClick = { onSelect(v) },
                size = NyummyButtonSize.M,
                style = if (v == selected) NyummyButtonStyle.Primary else NyummyButtonStyle.Tonal,
            )
        }
    }
}
