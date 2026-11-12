package com.onmate.app.domain

enum class AlertReason {
    /** 학습 기록이 부족해 기본 규칙(출발 전에 연 칸을 안 열면 알림)으로 알림 */
    DEFAULT_RULE,
    /** 학습된 습관과 달라서 알림 */
    HABIT,
}

data class AlertCandidate(
    val zone: Zone,
    val item: String?,
    val reason: AlertReason,
    val stat: HabitStat?,
)

/**
 * 도착 시 어떤 칸을 알릴지 판단한다.
 *
 * - 학습 3회 미만: 출발 전에 연 칸을 도착 후 아직 안 열었으면 알림(기본 규칙)
 * - 학습 3회 이상: 재개방률이 기준(70%) 이상인 칸만 알림
 * - '일부러 두는 거예요'를 고른 횟수만큼 그 상황의 기준을 올림(최대 95%)
 * - 물건 태그에서 알림을 끈 칸은 제외
 */
class AlertDecider(
    private val baseThreshold: Float = 0.7f,
    private val intentionalStep: Float = 0.1f,
    private val maxThreshold: Float = 0.95f,
) {
    fun thresholdFor(stat: HabitStat): Float =
        (baseThreshold + intentionalStep * stat.intentionalCount).coerceAtMost(maxThreshold)

    fun decide(
        openedBefore: Set<Zone>,
        openedAfter: Set<Zone>,
        stats: Map<Zone, HabitStat>,
        tags: Map<Zone, ZoneTag> = emptyMap(),
        tripItems: Map<Zone, String> = emptyMap(),
    ): List<AlertCandidate> {
        val result = mutableListOf<AlertCandidate>()
        for (zone in openedBefore) {
            if (!zone.learnable) continue
            if (zone in openedAfter) continue
            if (tags[zone]?.notify == false) continue

            val stat = stats[zone]
            val item = tripItems[zone] ?: tags[zone]?.item
            if (stat == null || !stat.isLearned) {
                result += AlertCandidate(zone, item, AlertReason.DEFAULT_RULE, stat)
            } else {
                val rate = stat.rate ?: continue
                if (rate >= thresholdFor(stat)) {
                    result += AlertCandidate(zone, item, AlertReason.HABIT, stat)
                }
            }
        }
        // 습관 일치율이 높은 칸이 위로, 기본 규칙은 그 아래
        return result.sortedWith(
            compareBy<AlertCandidate> { if (it.reason == AlertReason.HABIT) 0 else 1 }
                .thenByDescending { it.stat?.rate ?: 0f }
        )
    }
}
