package com.onmate.app.data

import com.onmate.app.domain.Trip
import com.onmate.app.domain.Zone
import com.onmate.app.domain.ZoneTag

/** 트립 기록과 물건 태그 저장소. 차 안(기기)에만 저장하고 외부로 보내지 않는다. */
interface TripRepository {
    fun trips(): List<Trip>
    fun saveTrip(trip: Trip)
    fun updateTrip(trip: Trip)
    fun nextTripId(): Long

    fun tags(): Map<Zone, ZoneTag>
    fun saveTag(tag: ZoneTag)
    fun removeTag(zone: Zone)
}

open class InMemoryTripRepository(
    initialTrips: List<Trip> = emptyList(),
    initialTags: List<ZoneTag> = emptyList(),
) : TripRepository {
    protected val tripList = initialTrips.toMutableList()
    protected val tagMap = initialTags.associateBy { it.zone }.toMutableMap()

    override fun trips(): List<Trip> = tripList.sortedByDescending { it.departedAtMs }
    override fun saveTrip(trip: Trip) { tripList += trip; persist() }
    override fun updateTrip(trip: Trip) {
        val i = tripList.indexOfFirst { it.id == trip.id }
        if (i >= 0) tripList[i] = trip else tripList += trip
        persist()
    }
    override fun nextTripId(): Long = (tripList.maxOfOrNull { it.id } ?: 0L) + 1

    override fun tags(): Map<Zone, ZoneTag> = tagMap.toMap()
    override fun saveTag(tag: ZoneTag) { tagMap[tag.zone] = tag; persist() }
    override fun removeTag(zone: Zone) { tagMap.remove(zone); persist() }

    protected open fun persist() {}
}
