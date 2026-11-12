package com.onmate.app.data

import com.onmate.app.domain.DayType
import com.onmate.app.domain.Feedback
import com.onmate.app.domain.SituationKey
import com.onmate.app.domain.TimeSlot
import com.onmate.app.domain.Trip
import com.onmate.app.domain.Zone
import com.onmate.app.domain.ZoneRecord
import com.onmate.app.domain.ZoneTag
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 기기 내부 파일(JSON)에 저장하는 저장소. MVP용으로 가볍게 구현.
 * 데이터가 커지면 Room DB로 교체한다(인터페이스는 그대로).
 */
class JsonFileTripRepository private constructor(
    private val file: File,
    trips: List<Trip>,
    tags: List<ZoneTag>,
) : InMemoryTripRepository(trips, tags) {

    override fun persist() {
        val root = JSONObject()
        root.put("trips", JSONArray().apply { tripList.forEach { put(tripToJson(it)) } })
        root.put("tags", JSONArray().apply { tagMap.values.forEach { put(tagToJson(it)) } })
        file.writeText(root.toString())
    }

    companion object {
        /** 파일이 없으면 [seed]로 채워서 시작한다(시연용 학습 데이터). */
        fun open(file: File, seed: () -> Pair<List<Trip>, List<ZoneTag>>): JsonFileTripRepository {
            if (!file.exists()) {
                val (t, g) = seed()
                return JsonFileTripRepository(file, t, g).also { it.persist() }
            }
            val root = JSONObject(file.readText())
            val trips = root.optJSONArray("trips")?.let { arr -> (0 until arr.length()).map { tripFromJson(arr.getJSONObject(it)) } } ?: emptyList()
            val tags = root.optJSONArray("tags")?.let { arr -> (0 until arr.length()).map { tagFromJson(arr.getJSONObject(it)) } } ?: emptyList()
            return JsonFileTripRepository(file, trips, tags)
        }

        private fun tripToJson(t: Trip) = JSONObject().apply {
            put("id", t.id)
            put("departedAt", t.departedAtMs)
            put("arrivedAt", t.arrivedAtMs ?: JSONObject.NULL)
            put("dayType", t.situation.dayType.name)
            put("timeSlot", t.situation.timeSlot.name)
            put("destination", t.situation.destination ?: JSONObject.NULL)
            put("origin", t.origin ?: JSONObject.NULL)
            put("records", JSONArray().apply {
                t.records.forEach { r ->
                    put(JSONObject().apply {
                        put("zone", r.zone.name)
                        put("before", r.openedBefore)
                        put("after", r.openedAfter)
                        put("item", r.item ?: JSONObject.NULL)
                        put("feedback", r.feedback?.name ?: JSONObject.NULL)
                    })
                }
            })
        }

        private fun JSONObject.optStringOrNull(key: String): String? =
            if (isNull(key)) null else optString(key, "").ifEmpty { null }

        private fun tripFromJson(o: JSONObject): Trip {
            val recs = o.getJSONArray("records")
            return Trip(
                id = o.getLong("id"),
                departedAtMs = o.getLong("departedAt"),
                arrivedAtMs = if (o.isNull("arrivedAt")) null else o.getLong("arrivedAt"),
                situation = SituationKey(
                    DayType.valueOf(o.getString("dayType")),
                    TimeSlot.valueOf(o.getString("timeSlot")),
                    o.optStringOrNull("destination"),
                ),
                origin = o.optStringOrNull("origin"),
                records = (0 until recs.length()).map { i ->
                    val r = recs.getJSONObject(i)
                    ZoneRecord(
                        zone = Zone.valueOf(r.getString("zone")),
                        openedBefore = r.getBoolean("before"),
                        openedAfter = r.getBoolean("after"),
                        item = r.optStringOrNull("item"),
                        feedback = r.optStringOrNull("feedback")?.let { Feedback.valueOf(it) },
                    )
                },
            )
        }

        private fun tagToJson(t: ZoneTag) = JSONObject().apply {
            put("zone", t.zone.name); put("item", t.item); put("notify", t.notify)
        }

        private fun tagFromJson(o: JSONObject) =
            ZoneTag(Zone.valueOf(o.getString("zone")), o.getString("item"), o.optBoolean("notify", true))
    }
}
