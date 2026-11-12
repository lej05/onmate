package com.onmate.app.vehicle

/** 시연용 목적지. 실제로는 NaviHelper SDK에서 받아오도록 교체한다(지원 여부 확인 필요). */
class FakeDestinationSource(
    var destination: String? = "회사",
    var origin: String? = "집",
    var minutes: Int? = 12,
) : DestinationSource {
    override fun currentDestination(): String? = destination
    override fun currentOrigin(): String? = origin
    override fun remainingMinutes(): Int? = minutes
}
