package com.example.btrapp.blocking.domain

import com.example.btrapp.blocking.data.EmergencyUnlock
import com.example.btrapp.blocking.data.LockRule
import com.example.btrapp.blocking.data.RuleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LockPolicyTest {

    private val zone = ZoneId.of("Asia/Singapore")
    private val app = "com.example.social"

    // 2026-10-05 is a Monday.
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun snapshot(rule: LockRule, vararg grants: EmergencyUnlock) =
        LockSnapshot(listOf(rule), mapOf(rule.id to setOf(app)), grants.toList())

    private fun grant(until: Long) = EmergencyUnlock(
        id = 1, packageName = app, reason = "r", exercise = "PUSH_UPS", amount = 10,
        durationMin = 5, createdAt = until - 5 * 60_000, grantedUntil = until,
    )

    private val weekdays9to5 = LockRule(
        id = 1, type = RuleType.SCHEDULE, daysMask = 0b0011111,
        startMinute = 9 * 60, endMinute = 17 * 60,
    )

    private val nightly22to7 = LockRule(
        id = 2, type = RuleType.SCHEDULE, daysMask = 0b0010000, // Friday nights only
        startMinute = 22 * 60, endMinute = 7 * 60,
    )

    @Test
    fun `session locks only inside its window`() {
        val rule = LockRule(id = 3, type = RuleType.SESSION, startAt = at(5, 10), endAt = at(5, 12))
        val s = snapshot(rule)
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(5, 9), s, zone))
        assertEquals(LockStatus.Locked(at(5, 12), 3), LockPolicy.status(app, at(5, 11), s, zone))
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(5, 12), s, zone))
    }

    @Test
    fun `schedule locks on selected day within hours`() {
        val s = snapshot(weekdays9to5)
        assertEquals(LockStatus.Locked(at(5, 17), 1), LockPolicy.status(app, at(5, 9), s, zone))
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(5, 8, 59), s, zone))
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(5, 17), s, zone))
    }

    @Test
    fun `schedule does not lock on unselected day`() {
        // 2026-10-10 is a Saturday.
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(10, 12), snapshot(weekdays9to5), zone))
    }

    @Test
    fun `overnight schedule wraps into the next morning`() {
        val s = snapshot(nightly22to7)
        // Friday 23:00 -> locked until Saturday 07:00.
        assertEquals(LockStatus.Locked(at(10, 7), 2), LockPolicy.status(app, at(9, 23), s, zone))
        // Saturday 06:00 still belongs to Friday's window.
        assertEquals(LockStatus.Locked(at(10, 7), 2), LockPolicy.status(app, at(10, 6), s, zone))
        // Friday 06:00 belongs to Thursday's (unselected) window.
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(9, 6), s, zone))
    }

    @Test
    fun `disabled rule and uncovered app are not locked`() {
        val s = snapshot(weekdays9to5.copy(enabled = false))
        assertEquals(LockStatus.Unlocked, LockPolicy.status(app, at(5, 10), s, zone))
        assertEquals(
            LockStatus.Unlocked,
            LockPolicy.status("com.other", at(5, 10), snapshot(weekdays9to5), zone),
        )
    }

    @Test
    fun `active grant overrides the lock until it expires`() {
        val s = snapshot(weekdays9to5, grant(until = at(5, 10, 5)))
        assertEquals(
            LockStatus.GrantActive(until = at(5, 10, 5), lockedUntil = at(5, 17)),
            LockPolicy.status(app, at(5, 10), s, zone),
        )
        assertEquals(LockStatus.Locked(at(5, 17), 1), LockPolicy.status(app, at(5, 10, 5), s, zone))
    }

    @Test
    fun `latest ending rule wins when several overlap`() {
        val session = LockRule(id = 9, type = RuleType.SESSION, startAt = at(5, 16), endAt = at(5, 19))
        val s = LockSnapshot(
            rules = listOf(weekdays9to5, session),
            ruleApps = mapOf(1L to setOf(app), 9L to setOf(app)),
            grants = emptyList(),
        )
        assertEquals(LockStatus.Locked(at(5, 19), 9), LockPolicy.status(app, at(5, 16, 30), s, zone))
    }
}
