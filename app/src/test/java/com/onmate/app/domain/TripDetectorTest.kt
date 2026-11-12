package com.onmate.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDetectorTest {

    private var t = 0L
    private fun door(z: Zone, open: Boolean) = VehicleEvent.DoorChanged(z, open, ++t)
    private fun brake(on: Boolean) = VehicleEvent.ParkingBrakeChanged(on, ++t)
    private fun speed(v: Float) = VehicleEvent.SpeedChanged(v, ++t)

    private fun TripDetector.feed(vararg e: VehicleEvent) = e.flatMap { process(it) }

    @Test
    fun `뒷문 열고 출발해서 도착하면 출발 전 열림으로 기록된다`() {
        val d = TripDetector()
        val s1 = d.feed(door(Zone.REAR_LEFT, true), door(Zone.REAR_LEFT, false))
        assertTrue(s1.contains(TripSignal.ZoneClosedWhileParked(Zone.REAR_LEFT)))

        val s2 = d.feed(brake(false), speed(8f))
        assertEquals(TripPhase.DRIVING, d.phase)
        val departed = s2.filterIsInstance<TripSignal.Departed>().single()
        assertEquals(setOf(Zone.REAR_LEFT), departed.openedBefore)

        val s3 = d.feed(speed(0f), brake(true))
        assertEquals(TripPhase.ARRIVED, d.phase)
        assertEquals(setOf(Zone.REAR_LEFT), s3.filterIsInstance<TripSignal.Arrived>().single().openedBefore)
    }

    @Test
    fun `운전석 문은 출발 전 물건 입력을 띄우지 않는다`() {
        val d = TripDetector()
        val s = d.feed(door(Zone.DRIVER, true), door(Zone.DRIVER, false))
        assertTrue(s.none { it is TripSignal.ZoneClosedWhileParked })
    }

    @Test
    fun `주차 브레이크가 걸려 있으면 속도가 있어도 출발로 보지 않는다`() {
        val d = TripDetector()
        d.feed(speed(3f))
        assertEquals(TripPhase.PARKED, d.phase)
    }

    @Test
    fun `도착 후 운전석 문을 열면 하차 중, 다른 칸을 열면 해당 칸 해제 신호`() {
        val d = TripDetector()
        d.feed(door(Zone.REAR_LEFT, true), door(Zone.REAR_LEFT, false), brake(false), speed(8f), speed(0f), brake(true))
        val s1 = d.feed(door(Zone.DRIVER, true))
        assertTrue(s1.contains(TripSignal.DriverExiting))
        assertEquals(TripPhase.EXITING, d.phase)

        val s2 = d.feed(door(Zone.REAR_LEFT, true))
        assertTrue(s2.contains(TripSignal.ZoneOpenedAfterArrival(Zone.REAR_LEFT)))

        val done = d.finishTrip()
        assertNotNull(done)
        assertEquals(setOf(Zone.REAR_LEFT), done!!.openedBefore)
        assertEquals(setOf(Zone.REAR_LEFT), done.openedAfter)
        assertEquals(TripPhase.PARKED, d.phase)
    }

    @Test
    fun `하차 없이 다시 출발하면 이전 트립을 마감하고 도착 후 연 칸을 다음 트립으로 넘긴다`() {
        val d = TripDetector()
        d.feed(brake(false), speed(8f), speed(0f), brake(true))
        d.feed(door(Zone.TRUNK, true), door(Zone.TRUNK, false))
        val s = d.feed(brake(false), speed(8f))
        val completed = s.filterIsInstance<TripSignal.TripCompleted>().single()
        assertEquals(setOf(Zone.TRUNK), completed.openedAfter)
        val departed = s.filterIsInstance<TripSignal.Departed>().single()
        assertEquals(setOf(Zone.TRUNK), departed.openedBefore)
    }

    @Test
    fun `주행 중이 아니면 finishTrip은 아무것도 하지 않는다`() {
        assertNull(TripDetector().finishTrip())
    }
}
