package com.autoverse.launcher.core.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CarModelsTest {
    private val today = LocalDate.of(2026, 11, 10)

    @Test
    fun `new drive after the car was off 20 minutes or right after boot`() {
        assertTrue(isNewDrive(gapSinceStopMs = 25 * 60_000L, uptimeMs = 3_600_000L, firstStartThisProcess = false))
        assertFalse(isNewDrive(gapSinceStopMs = 5 * 60_000L, uptimeMs = 3_600_000L, firstStartThisProcess = false))
        assertTrue(isNewDrive(gapSinceStopMs = null, uptimeMs = 2 * 60_000L, firstStartThisProcess = true))
        assertFalse(isNewDrive(gapSinceStopMs = null, uptimeMs = 3_600_000L, firstStartThisProcess = true))
    }

    @Test
    fun `reminders turn amber 15 days or 500 km before, red when due`() {
        val puc = Reminder(ReminderKind.PUC, dueDate = today.plusDays(10))
        assertEquals(ReminderUrgency.SOON, reminderUrgency(puc, today, null))
        assertEquals(ReminderUrgency.OK, reminderUrgency(puc.copy(dueDate = today.plusDays(40)), today, null))
        assertEquals(ReminderUrgency.DUE, reminderUrgency(puc.copy(dueDate = today.minusDays(1)), today, null))
        val service = Reminder(ReminderKind.SERVICE, dueOdometerKm = 40_000)
        assertEquals(ReminderUrgency.SOON, reminderUrgency(service, today, 39_700))
        assertEquals(ReminderUrgency.DUE, reminderUrgency(service, today, 40_100))
        assertEquals("in 10 days", reminderDetail(puc, today, null))
        assertEquals("in 300 km", reminderDetail(service, today, 39_700))
    }

    @Test
    fun `fuel cost from distance, price and mileage`() {
        val f = FuelSettings(pricePerLitre = 100.0, kmPerLitre = 20.0)
        assertEquals(500.0, f.costFor(100.0), 1e-9)
        assertEquals(5.0, f.litresFor(100.0), 1e-9)
    }

    @Test
    fun `trip averages`() {
        val t = Trip(startMs = 0, endMs = 30 * 60_000L, distanceKm = 20.0, maxSpeedKmh = 80)
        assertEquals(30, t.durationMin)
        assertEquals(40, t.avgSpeedKmh)
    }

    @Test
    fun `unit class from RAM and map cache from free space`() {
        assertEquals(UnitClass.ENTRY, unitClassFor(2_048))
        assertEquals(UnitClass.MID, unitClassFor(4_096))
        assertEquals(UnitClass.HIGH, unitClassFor(8_192))
        assertEquals(50L * 1024 * 1024, mapCacheBytesFor(200L * 1024 * 1024))        // nearly full: minimum
        assertEquals(200L * 1024 * 1024, mapCacheBytesFor(20L * 1024 * 1024 * 1024)) // plenty: maximum
    }

    @Test
    fun `speaker test channels`() {
        val left = SpeakerTest.render(SpeakerTone.LEFT, 0.2)
        assertTrue((0 until left.size / 2).all { left[2 * it + 1] == 0.toShort() })
        assertTrue((0 until left.size / 2).any { left[2 * it] != 0.toShort() })
        val phase = SpeakerTest.render(SpeakerTone.PHASE, 0.2)
        assertTrue((0 until phase.size / 2).all { phase[2 * it].toInt() == -phase[2 * it + 1].toInt() })
    }
}
