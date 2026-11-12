package com.onmate.app.vehicle

import com.onmate.app.domain.VehicleEvent
import com.onmate.app.domain.Zone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 버튼으로 차량 상태를 흉내 내는 가짜 데이터 소스.
 * 에뮬레이터·Virtual Console 없이도 전체 흐름을 재생할 수 있어 개발과 시연 영상 녹화에 쓴다.
 */
class FakeVehicleSource(
    private val clock: () -> Long = System::currentTimeMillis,
) : VehicleSource {

    private val flow = MutableSharedFlow<VehicleEvent>(extraBufferCapacity = 64)
    override val events: Flow<VehicleEvent> = flow

    private val openDoors = mutableSetOf<Zone>()
    fun isOpen(zone: Zone) = zone in openDoors

    fun setDoor(zone: Zone, open: Boolean) {
        if (open) openDoors += zone else openDoors -= zone
        flow.tryEmit(VehicleEvent.DoorChanged(zone, open, clock()))
    }

    fun toggleDoor(zone: Zone) = setDoor(zone, zone !in openDoors)

    /** 문을 열었다가 바로 닫음 (짐을 넣거나 뺀 동작) */
    fun openAndClose(zone: Zone) {
        setDoor(zone, true)
        setDoor(zone, false)
    }

    fun depart(speedMps: Float = 8f) {
        flow.tryEmit(VehicleEvent.ParkingBrakeChanged(false, clock()))
        flow.tryEmit(VehicleEvent.SpeedChanged(speedMps, clock()))
    }

    fun arrive() {
        flow.tryEmit(VehicleEvent.SpeedChanged(0f, clock()))
        flow.tryEmit(VehicleEvent.ParkingBrakeChanged(true, clock()))
    }

    /**
     * 시연 시나리오: 뒷좌석 왼쪽에 가방을 싣고 출발 → 도착 → 뒷문은 안 열고 운전석 문 열기.
     * 각 단계 사이에 [stepMs]만큼 쉬므로 화면 녹화에 쓰기 좋다.
     */
    suspend fun playDemoScenario(stepMs: Long = 2500L, onItemPrompt: suspend () -> Unit = {}) {
        openAndClose(Zone.REAR_LEFT)
        delay(stepMs)
        onItemPrompt()
        delay(stepMs)
        depart()
        delay(stepMs * 2)
        arrive()
        delay(stepMs * 2)
        setDoor(Zone.DRIVER, true)
    }
}
