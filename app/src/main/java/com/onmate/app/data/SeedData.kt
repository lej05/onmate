package com.onmate.app.data

import com.onmate.app.domain.DayType
import com.onmate.app.domain.Feedback
import com.onmate.app.domain.SituationKey
import com.onmate.app.domain.TimeSlot
import com.onmate.app.domain.Trip
import com.onmate.app.domain.Zone
import com.onmate.app.domain.ZoneRecord
import com.onmate.app.domain.ZoneTag

/**
 * 시연용 학습 데이터(약 2주치). 피그마 '습관 대시보드' 수치와 맞춰 둠.
 * 평일 아침·회사: 뒷좌석 왼쪽 13회 중 12회, 트렁크 5회 중 4회, 조수석 5회 중 2회, 뒷좌석 오른쪽 2회
 */
object SeedData {
    private const val DAY = 24 * 60 * 60 * 1000L
    private const val HOUR = 60 * 60 * 1000L

    fun build(nowMs: Long = System.currentTimeMillis()): Pair<List<Trip>, List<ZoneTag>> {
        val trips = mutableListOf<Trip>()
        var id = 1L
        fun trip(daysAgo: Int, hour: Int, key: SituationKey, origin: String, vararg recs: ZoneRecord) {
            val dep = nowMs - daysAgo * DAY - (24 - hour) * HOUR
            trips += Trip(id++, dep, dep + 23 * 60 * 1000L, key, origin, recs.toList())
        }
        fun r(z: Zone, after: Boolean, item: String? = null, fb: Feedback? = null) =
            ZoneRecord(z, openedBefore = true, openedAfter = after, item = item, feedback = fb)

        val work = SituationKey(DayType.WEEKDAY, TimeSlot.MORNING, "회사")
        val home = SituationKey(DayType.WEEKDAY, TimeSlot.EVENING, "집")
        val mart = SituationKey(DayType.WEEKEND, TimeSlot.DAYTIME, "마트")
        val camp = SituationKey(DayType.WEEKEND, TimeSlot.DAYTIME, "캠핑장")

        // 평일 아침 · 회사 (13회)
        for (i in 0 until 13) {
            val recs = mutableListOf(r(Zone.REAR_LEFT, after = i != 6, item = "운동 가방"))
            if (i < 5) recs += r(Zone.TRUNK, after = i != 2, item = "장바구니")
            if (i in 5..9) recs += r(Zone.PASSENGER, after = i < 7, item = "노트북 가방")
            if (i in 10..11) recs += r(Zone.REAR_RIGHT, after = true)
            trip(daysAgo = 16 - i, hour = 8, key = work, origin = "집", *recs.toTypedArray())
        }
        // 평일 저녁 · 집
        for (i in 0 until 10) {
            val recs = mutableListOf(r(Zone.TRUNK, after = i < 8, item = "장바구니"))
            if (i < 9) recs += r(Zone.REAR_LEFT, after = i < 3, item = "운동 가방")
            trip(daysAgo = 16 - i, hour = 19, key = home, origin = "회사", *recs.toTypedArray())
        }
        // 주말 · 마트
        for (i in 0 until 7) {
            trip(daysAgo = 15 - i * 2, hour = 14, key = mart, origin = "집", r(Zone.TRUNK, after = true, item = "장바구니"))
        }
        // 주말 · 캠핑장: 트렁크에 캠핑 용품을 일부러 두고 내림
        for (i in 0 until 3) {
            trip(daysAgo = 14 - i * 7, hour = 13, key = camp, origin = "집",
                r(Zone.TRUNK, after = false, item = "캠핑 용품", fb = Feedback.LEFT_INTENTIONALLY))
        }

        val tags = listOf(
            ZoneTag(Zone.REAR_LEFT, "운동 가방"),
            ZoneTag(Zone.TRUNK, "장바구니"),
            ZoneTag(Zone.PASSENGER, "노트북 가방"),
        )
        return trips to tags
    }
}
