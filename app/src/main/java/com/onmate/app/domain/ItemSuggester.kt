package com.onmate.app.domain

/** '출발 전 물건 입력' 화면의 추천과 자주 쓰는 물건 목록 */
class ItemSuggester(
    private val streak: Int = 3,
    private val defaults: List<String> = listOf("운동 가방", "노트북 가방", "장바구니", "아이 가방", "우산"),
) {
    /** 최근 [streak]번 모두 이 칸에 같은 물건을 뒀다면 그 물건을 추천한다. */
    fun suggest(zone: Zone, history: List<Trip>): String? {
        val recent = history
            .sortedByDescending { it.departedAtMs }
            .mapNotNull { trip -> trip.record(zone)?.takeIf { it.openedBefore }?.item }
            .take(streak)
        if (recent.size < streak) return null
        return recent.distinct().singleOrNull()
    }

    /** 기록에 자주 나온 물건 순서, 부족하면 기본 목록으로 채움 */
    fun frequentItems(history: List<Trip>, exclude: String? = null, limit: Int = 5): List<String> {
        val counted = history.flatMap { t -> t.records.mapNotNull { it.item } }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }
        return (counted + defaults).distinct().filter { it != exclude }.take(limit)
    }
}
