package com.onmate.app.domain

/** 트립 상태 머신의 단계 */
enum class TripPhase {
    /** 정차 중. 이때 연 칸은 '출발 전 열림' */
    PARKED,
    /** 주행 중. 화면은 기록만 한다 */
    DRIVING,
    /** 도착(속도 0 + 주차 브레이크). 하차 전 알림을 띄우는 시점 */
    ARRIVED,
    /** 도착 후 운전석 문이 열림 = 하차 중. 남은 칸을 깜빡인다 */
    EXITING,
}

/** 상태 머신이 화면·저장소에 알려주는 신호 */
sealed interface TripSignal {
    /** 정차 중 칸을 열었다 닫음 → '출발 전 물건 입력' 화면을 띄울 수 있다 */
    data class ZoneClosedWhileParked(val zone: Zone) : TripSignal
    data class Departed(val atMs: Long, val openedBefore: Set<Zone>) : TripSignal
    data class Arrived(val atMs: Long, val openedBefore: Set<Zone>) : TripSignal
    /** 도착 후 칸을 다시 엶 → 그 칸의 알림·깜빡임을 끈다 */
    data class ZoneOpenedAfterArrival(val zone: Zone) : TripSignal
    data object DriverExiting : TripSignal
    data class TripCompleted(
        val departedAtMs: Long,
        val arrivedAtMs: Long,
        val openedBefore: Set<Zone>,
        val openedAfter: Set<Zone>,
    ) : TripSignal
}

/**
 * 차량 이벤트(문·주차 브레이크·속도)를 받아 트립 단위로 묶는 상태 머신.
 *
 * PARKED --(주차 브레이크 해제 + 속도>0)--> DRIVING
 * DRIVING --(속도 0 + 주차 브레이크 체결)--> ARRIVED
 * ARRIVED --(운전석 문 열림)--> EXITING
 * ARRIVED/EXITING --(finishTrip() 또는 다음 출발)--> PARKED
 *
 * 순수 Kotlin이라 SDK·화면 없이 단위 테스트할 수 있다.
 */
class TripDetector(
    private val departSpeedMps: Float = 0.5f,
    private val stopSpeedMps: Float = 0.1f,
) {
    var phase: TripPhase = TripPhase.PARKED
        private set

    private var parkingBrakeEngaged = true
    private var speedMps = 0f
    private val openDoors = mutableSetOf<Zone>()

    private val openedBefore = mutableSetOf<Zone>()
    private val openedAfter = mutableSetOf<Zone>()
    private var departedAtMs: Long = 0L
    private var arrivedAtMs: Long = 0L

    val currentOpenedBefore: Set<Zone> get() = openedBefore.toSet()
    val currentOpenedAfter: Set<Zone> get() = openedAfter.toSet()

    fun process(event: VehicleEvent): List<TripSignal> {
        val out = mutableListOf<TripSignal>()
        when (event) {
            is VehicleEvent.DoorChanged -> onDoor(event, out)
            is VehicleEvent.ParkingBrakeChanged -> {
                parkingBrakeEngaged = event.engaged
                evaluateMotion(event.timestampMs, out)
            }
            is VehicleEvent.SpeedChanged -> {
                speedMps = event.metersPerSecond
                evaluateMotion(event.timestampMs, out)
            }
        }
        return out
    }

    /** 하차가 끝났다고 판단될 때(문 잠금·시동 끔·일정 시간 경과) 호출해 트립을 저장한다. */
    fun finishTrip(): TripSignal.TripCompleted? {
        if (phase != TripPhase.ARRIVED && phase != TripPhase.EXITING) return null
        val done = completed()
        openedBefore.clear()
        openedAfter.clear()
        phase = TripPhase.PARKED
        return done
    }

    private fun onDoor(e: VehicleEvent.DoorChanged, out: MutableList<TripSignal>) {
        if (e.open) openDoors += e.zone else openDoors -= e.zone
        when (phase) {
            TripPhase.PARKED -> {
                if (e.open) {
                    openedBefore += e.zone
                } else if (e.zone.learnable) {
                    out += TripSignal.ZoneClosedWhileParked(e.zone)
                }
            }
            TripPhase.DRIVING -> Unit // 주행 중 개폐는 무시
            TripPhase.ARRIVED, TripPhase.EXITING -> {
                if (e.open) {
                    if (e.zone == Zone.DRIVER) {
                        if (phase == TripPhase.ARRIVED) {
                            phase = TripPhase.EXITING
                            out += TripSignal.DriverExiting
                        }
                    } else if (openedAfter.add(e.zone)) {
                        out += TripSignal.ZoneOpenedAfterArrival(e.zone)
                    }
                }
            }
        }
    }

    private fun evaluateMotion(nowMs: Long, out: MutableList<TripSignal>) {
        val moving = speedMps > departSpeedMps && !parkingBrakeEngaged
        val stopped = speedMps < stopSpeedMps && parkingBrakeEngaged
        when (phase) {
            TripPhase.PARKED -> if (moving) depart(nowMs, out)
            TripPhase.DRIVING -> if (stopped) {
                phase = TripPhase.ARRIVED
                arrivedAtMs = nowMs
                out += TripSignal.Arrived(nowMs, openedBefore.toSet())
            }
            TripPhase.ARRIVED, TripPhase.EXITING -> if (moving) {
                // 하차 없이 다시 출발: 이전 트립을 마감하고,
                // 도착 후 연 칸은 새 트립의 '출발 전 열림'으로 넘긴다(예: 잠깐 정차해서 짐 싣기).
                val carried = openedAfter.toSet()
                out += completed()
                openedBefore.clear()
                openedAfter.clear()
                openedBefore += carried
                phase = TripPhase.PARKED
                depart(nowMs, out)
            }
        }
    }

    private fun depart(nowMs: Long, out: MutableList<TripSignal>) {
        phase = TripPhase.DRIVING
        departedAtMs = nowMs
        out += TripSignal.Departed(nowMs, openedBefore.toSet())
    }

    private fun completed() = TripSignal.TripCompleted(
        departedAtMs = departedAtMs,
        arrivedAtMs = arrivedAtMs,
        openedBefore = openedBefore.toSet(),
        openedAfter = openedAfter.toSet(),
    )
}
