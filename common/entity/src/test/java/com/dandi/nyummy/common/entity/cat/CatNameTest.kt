package com.dandi.nyummy.common.entity.cat

import org.junit.Assert.assertEquals
import org.junit.Test

class CatNameTest {

    @Test
    fun `받침 없는 이름은 받침 없는 조사를 쓴다`() {
        assertEquals("냐미가 냐미를 냐미는 냐미와 냐미야 냐미랑 냐미로", fill("냐미"))
    }

    @Test
    fun `받침 있는 이름은 받침 있는 조사를 쓴다`() {
        assertEquals("달봉이 달봉을 달봉은 달봉과 달봉이야 달봉이랑 달봉으로", fill("달봉"))
    }

    @Test
    fun `ㄹ 받침 이름은 으로 대신 로를 쓴다`() {
        assertEquals("보리별이 보리별로", CatName.fill("{name}(이)가 {name}(으)로", "보리별"))
    }

    @Test
    fun `숫자와 영문 이름은 읽는 소리로 고른다`() {
        assertEquals("나비3이", CatName.fill("{name}(이)가", "나비3"))
        assertEquals("나비2가", CatName.fill("{name}(이)가", "나비2"))
        assertEquals("Tom이", CatName.fill("{name}(이)가", "Tom"))
        assertEquals("Coco가", CatName.fill("{name}(이)가", "Coco"))
        assertEquals("King이", CatName.fill("{name}(이)가", "King"))
    }

    @Test
    fun `조사 표기가 없는 문장은 그대로 둔다`() {
        assertEquals("달봉에게 밥을 챙겼어요!", CatName.fill("{name}에게 밥을 챙겼어요!", "달봉"))
        assertEquals("You fed Tom!", CatName.resolveParticles("You fed Tom!"))
    }

    private fun fill(name: String) =
        CatName.fill("{name}(이)가 {name}(을)를 {name}(은)는 {name}(과)와 {name}(이)야 {name}(이)랑 {name}(으)로", name)
}
