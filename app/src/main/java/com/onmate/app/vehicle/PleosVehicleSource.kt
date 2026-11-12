package com.onmate.app.vehicle

import android.content.Context
import com.onmate.app.domain.VehicleEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 실제 Pleos Vehicle SDK 연동 자리.
 *
 * TODO(차량 담당): Pleos 문서를 보고 아래를 구현한다. API 이름은 추측하지 말고 문서에서 확인할 것.
 *  1. app/build.gradle.kts 에서 `ai.pleos.playground:Vehicle` 의존성 주석 해제
 *  2. Vehicle(context) 생성 → initialize(), 종료 시 release()
 *  3. Door: 문별(앞좌·앞우·뒤좌·뒤우, 트렁크 지원 여부 확인) 열림 상태 변화 → VehicleEvent.DoorChanged
 *  4. Brake: 주차 브레이크 상태 → VehicleEvent.ParkingBrakeChanged
 *  5. 속도(m/s) → VehicleEvent.SpeedChanged
 *  6. 변화 구독(listener) API가 없으면 짧은 주기로 조회(polling)해서 값이 바뀔 때만 emit
 *  7. 권한(AndroidManifest): CAR_DOORS, CAR_DRIVE, CAR_SPEED
 *
 * 문서: https://document.pleos.ai/docs/pleos-only/getting-started
 *       https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/intro
 */
class PleosVehicleSource(@Suppress("unused") private val context: Context) : VehicleSource {

    private val flow = MutableSharedFlow<VehicleEvent>(extraBufferCapacity = 64)
    override val events: Flow<VehicleEvent> = flow

    override fun start() {
        TODO("Pleos Vehicle SDK 연동 구현 필요 — 위 주석 참고")
    }

    override fun stop() {
        // vehicle.release()
    }
}
