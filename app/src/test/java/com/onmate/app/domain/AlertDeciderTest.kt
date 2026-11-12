package com.onmate.app.domain

import com.onmate.app.data.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertDeciderTest {

    private val learner = HabitLearner()
    private val decider = AlertDecider()
    private val work = SituationKey(DayType.WEEKDAY, TimeSlot.MORNING, "회사")
    private val camp = SituationKey(DayType.WEEKEND, TimeSlot.DAYTIME, "캠핑장")
    private val history = SeedData.build(nowMs = 2_000_000_000_000L).first

    @Test
    fun `시드 데이터의 평일 아침 회사 재개방률이 피그마 수치와 같다`() {
        val stats = learner.stats(history, work)
        assertEquals(13, stats.getValue(Zone.REAR_LEFT).openedCount)
        assertEquals(12, stats.getValue(Zone.REAR_LEFT).reopenedCount)
        assertEquals(4, stats.getValue(Zone.TRUNK).reopenedCount)
        assertEquals(5, stats.getValue(Zone.TRUNK).openedCount)
        assertEquals(2, stats.getValue(Zone.PASSENGER).reopenedCount)
        assertEquals(2, stats.getValue(Zone.REAR_RIGHT).openedCount)
    }

    @Test
    fun `습관이 강한 칸을 안 열면 알림, 약한 칸은 알림 안 함`() {
        val stats = learner.stats(history, work)
        val result = decider.decide(
            openedBefore = setOf(Zone.REAR_LEFT, Zone.PASSENGER),
            openedAfter = emptySet(),
            stats = stats,
        )
        assertEquals(listOf(Zone.REAR_LEFT), result.map { it.zone })
        assertEquals(AlertReason.HABIT, result.single().reason)
    }

    @Test
    fun `도착 후 이미 연 칸은 알리지 않는다`() {
        val stats = learner.stats(history, work)
        val result = decider.decide(setOf(Zone.REAR_LEFT), setOf(Zone.REAR_LEFT), stats)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `학습 3회 미만이면 기본 규칙으로 알림`() {
        val stats = learner.stats(history, work)
        val result = decider.decide(setOf(Zone.REAR_RIGHT), emptySet(), stats)
        assertEquals(AlertReason.DEFAULT_RULE, result.single().reason)
    }

    @Test
    fun `일부러 두는 상황에서는 알림을 줄인다`() {
        val stats = learner.stats(history, camp)
        val result = decider.decide(setOf(Zone.TRUNK), emptySet(), stats)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `태그에서 알림을 끈 칸은 제외하고, 물건 이름은 태그에서 가져온다`() {
        val stats = learner.stats(history, work)
        val tags = mapOf(
            Zone.REAR_LEFT to ZoneTag(Zone.REAR_LEFT, "운동 가방"),
            Zone.TRUNK to ZoneTag(Zone.TRUNK, "장바구니", notify = false),
        )
        val result = decider.decide(setOf(Zone.REAR_LEFT, Zone.TRUNK), emptySet(), stats, tags)
        assertEquals("운동 가방", result.single().item)
    }

    @Test
    fun `최근 3번 같은 물건이면 추천, 아니면 추천 없음`() {
        val s = ItemSuggester()
        assertEquals("운동 가방", s.suggest(Zone.REAR_LEFT, history))
        assertNull(s.suggest(Zone.REAR_RIGHT, history))
    }
}
