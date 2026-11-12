package com.onmate.app.domain

/** 한 상황에서 한 칸에 대한 학습 결과 */
data class HabitStat(
    val zone: Zone,
    /** 출발 전에 연 횟수 */
    val openedCount: Int,
    /** 그중 도착 후 다시 연 횟수 */
    val reopenedCount: Int,
    /** '일부러 두는 거예요'를 고른 횟수 */
    val intentionalCount: Int,
    val minSamples: Int,
) {
    /** 재개방률. 기록이 없으면 null */
    val rate: Float? get() = if (openedCount == 0) null else reopenedCount.toFloat() / openedCount

    /** 기록이 [minSamples]회 이상이면 학습 완료로 본다 */
    val isLearned: Boolean get() = openedCount >= minSamples
}

/**
 * 재개방률 = 출발 전에 열고 도착 후 다시 연 횟수 ÷ 출발 전에 연 횟수.
 * 같은 상황(요일 유형·시간대·목적지)의 트립만 모아 계산한다.
 */
class HabitLearner(val minSamples: Int = 3) {

    fun stats(history: List<Trip>, situation: SituationKey?): Map<Zone, HabitStat> {
        val trips = if (situation == null) history else history.filter { matches(it.situation, situation) }
        return Zone.learnableZones.associateWith { zone ->
            var opened = 0
            var reopened = 0
            var intentional = 0
            for (trip in trips) {
                val r = trip.record(zone) ?: continue
                if (!r.openedBefore) continue
                opened++
                if (r.openedAfter) reopened++
                if (r.feedback == Feedback.LEFT_INTENTIONALLY) intentional++
            }
            HabitStat(zone, opened, reopened, intentional, minSamples)
        }
    }

    /** 습관 화면의 상황 탭 목록: 기록이 많은 상황부터 */
    fun situations(history: List<Trip>): List<SituationKey> =
        history.groupingBy { it.situation }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }

    companion object {
        /** 목적지는 둘 다 알 때만 비교한다(NaviHelper 미지원 시에도 동작하도록). */
        fun matches(a: SituationKey, b: SituationKey): Boolean {
            if (a.dayType != b.dayType || a.timeSlot != b.timeSlot) return false
            if (a.destination != null && b.destination != null) return a.destination == b.destination
            return true
        }
    }
}
