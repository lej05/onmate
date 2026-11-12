package com.onmate.app.vehicle

import com.onmate.app.domain.VehicleEvent
import kotlinx.coroutines.flow.Flow

/**
 * 차량 데이터의 출처. 앱 로직은 이 인터페이스만 안다.
 * - [FakeVehicleSource]: 개발·테스트·시연 영상용 가짜 데이터
 * - [PleosVehicleSource]: 실제 Pleos Vehicle SDK 연동 (구현 예정)
 */
interface VehicleSource {
    val events: Flow<VehicleEvent>
    fun start() {}
    fun stop() {}
}

/** 목적지 정보 출처. NaviHelper SDK로 받을 수 있는지 확인 전이라 분리해 둠. */
interface DestinationSource {
    fun currentDestination(): String?
    fun currentOrigin(): String? = null
    fun remainingMinutes(): Int? = null
}
