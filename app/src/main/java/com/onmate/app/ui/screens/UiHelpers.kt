package com.onmate.app.ui.screens

import com.onmate.app.UiState
import com.onmate.app.domain.Compartment
import com.onmate.app.domain.Zone
import com.onmate.app.ui.components.AreaLook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 받침이 있으면 '을', 없으면 '를' */
fun String.eulReul(): String {
    val c = lastOrNull() ?: return this
    if (c !in '가'..'힣') return this + "을"
    return if ((c - '가') % 28 != 0) this + "을" else this + "를"
}

fun hhmm(ms: Long): String = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(ms))

/** 그 문을 열면 접근하는(센서로 감지되는) 수납함들 */
fun compartmentsOf(zone: Zone): List<Compartment> =
    Compartment.entries.filter { it.zone == zone && it.hasOwnSensor }

/** 지도 칸 안에 물건 이름을 쓸 수 있는 수납함(뒷좌석·트렁크) */
fun seatCompartmentOf(zone: Zone): Compartment? = when (zone) {
    Zone.REAR_LEFT -> Compartment.REAR_LEFT_SEAT
    Zone.REAR_RIGHT -> Compartment.REAR_RIGHT_SEAT
    Zone.TRUNK -> Compartment.TRUNK
    else -> null
}

fun itemFor(st: UiState, zone: Zone): String? = st.tripItems[zone] ?: st.tags[zone]?.item

fun looksFor(zones: Map<Zone, AreaLook>): Map<Compartment, AreaLook> =
    zones.flatMap { (z, look) -> compartmentsOf(z).map { it to look } }.toMap()
