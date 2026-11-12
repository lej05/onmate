package com.onmate.app.domain

/**
 * 차량이 직접 감지할 수 있는 '열리는 곳'(문·트렁크).
 * 학습은 이 단위로 한다. 운전석은 운전자가 매번 여닫으므로 물건 신호에서 제외한다.
 */
enum class Zone(val label: String, val learnable: Boolean) {
    DRIVER("운전석", learnable = false),
    PASSENGER("조수석", learnable = true),
    REAR_LEFT("뒷좌석 왼쪽", learnable = true),
    REAR_RIGHT("뒷좌석 오른쪽", learnable = true),
    TRUNK("트렁크", learnable = true);

    companion object {
        val learnableZones: List<Zone> = entries.filter { it.learnable }
    }
}

/**
 * 화면의 차량 지도에 그려지는 수납함.
 * [zone]은 그 수납함에 접근할 때 열리는 문(센서)이다.
 * 콘솔·컵홀더·대시보드처럼 센서가 없는 곳은 앞문 개폐로만 추정한다.
 */
enum class Compartment(val label: String, val zone: Zone, val hasOwnSensor: Boolean) {
    DASHBOARD("대시보드 수납함", Zone.DRIVER, false),
    GLOVEBOX("글로브박스", Zone.PASSENGER, false), // 차종에 따라 센서 있을 수 있음(확인 필요)
    CONSOLE_FRONT("콘솔 하단 수납함", Zone.DRIVER, false),
    CUP_HOLDER("컵홀더", Zone.DRIVER, false),
    ARMREST("암레스트 수납함", Zone.DRIVER, false),
    DRIVER_DOOR_POCKET("운전석 도어 수납함", Zone.DRIVER, true),
    PASSENGER_DOOR_POCKET("조수석 도어 수납함", Zone.PASSENGER, true),
    REAR_LEFT_DOOR_POCKET("뒷좌석 도어 수납함", Zone.REAR_LEFT, true),
    REAR_RIGHT_DOOR_POCKET("뒷좌석 도어 수납함", Zone.REAR_RIGHT, true),
    REAR_LEFT_SEAT("뒷좌석 왼쪽", Zone.REAR_LEFT, true),
    REAR_RIGHT_SEAT("뒷좌석 오른쪽", Zone.REAR_RIGHT, true),
    TRUNK("트렁크", Zone.TRUNK, true),
}

/** 차량에서 들어오는 원시 이벤트. 실제 SDK든 가짜 데이터든 이 형태로 바꿔서 넘긴다. */
sealed interface VehicleEvent {
    val timestampMs: Long

    data class DoorChanged(val zone: Zone, val open: Boolean, override val timestampMs: Long) : VehicleEvent
    data class ParkingBrakeChanged(val engaged: Boolean, override val timestampMs: Long) : VehicleEvent
    data class SpeedChanged(val metersPerSecond: Float, override val timestampMs: Long) : VehicleEvent
}

enum class DayType(val label: String) { WEEKDAY("평일"), WEEKEND("주말") }

enum class TimeSlot(val label: String) {
    MORNING("아침"), DAYTIME("낮"), EVENING("저녁"), NIGHT("밤");

    companion object {
        fun fromHour(hour: Int): TimeSlot = when (hour) {
            in 5..10 -> MORNING
            in 11..16 -> DAYTIME
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}

/**
 * 습관을 나누는 상황. 목적지는 NaviHelper로 받을 수 있을 때만 채운다(확인 필요).
 * 목적지가 없으면 요일 유형 + 시간대로만 나눈다.
 */
data class SituationKey(
    val dayType: DayType,
    val timeSlot: TimeSlot,
    val destination: String? = null,
) {
    val label: String
        get() = buildString {
            append(dayType.label).append(' ').append(timeSlot.label)
            if (destination != null) append(" · ").append(destination)
        }
}

/** 사용자가 하차 알림에 답한 내용 */
enum class Feedback { TOOK_IT, LEFT_INTENTIONALLY }

/** 한 트립에서 한 칸(문)에 대한 기록 */
data class ZoneRecord(
    val zone: Zone,
    val openedBefore: Boolean,
    val openedAfter: Boolean,
    val item: String? = null,
    val feedback: Feedback? = null,
)

/** 출발부터 하차까지 한 번의 이동 */
data class Trip(
    val id: Long,
    val departedAtMs: Long,
    val arrivedAtMs: Long?,
    val situation: SituationKey,
    val origin: String? = null,
    val records: List<ZoneRecord> = emptyList(),
) {
    fun record(zone: Zone): ZoneRecord? = records.firstOrNull { it.zone == zone }
}

/** 칸마다 한 번만 등록해 두는 물건 이름과 알림 여부 */
data class ZoneTag(val zone: Zone, val item: String, val notify: Boolean = true)
