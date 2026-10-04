package com.example.btrapp.blocking.domain

import com.example.btrapp.blocking.data.EmergencyUnlock
import com.example.btrapp.blocking.data.LockRule
import com.example.btrapp.blocking.data.RuleType
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Everything needed to decide whether an app is locked, held in memory for the service. */
data class LockSnapshot(
    val rules: List<LockRule>,
    val ruleApps: Map<Long, Set<String>>,
    val grants: List<EmergencyUnlock>,
) {
    val coveredPackages: Set<String> by lazy { ruleApps.values.flatten().toSet() }

    companion object {
        val EMPTY = LockSnapshot(emptyList(), emptyMap(), emptyList())
    }
}

sealed interface LockStatus {
    data object Unlocked : LockStatus
    data class Locked(val until: Long, val ruleId: Long) : LockStatus
    data class GrantActive(val until: Long, val lockedUntil: Long) : LockStatus
}

object LockPolicy {

    fun status(
        packageName: String,
        now: Long,
        snapshot: LockSnapshot,
        zone: ZoneId = ZoneId.systemDefault(),
    ): LockStatus {
        val lock = snapshot.rules
            .asSequence()
            .filter { it.enabled && packageName in snapshot.ruleApps[it.id].orEmpty() }
            .mapNotNull { rule -> activeUntil(rule, now, zone)?.let { it to rule.id } }
            .maxByOrNull { it.first }
            ?: return LockStatus.Unlocked

        val grantUntil = snapshot.grants
            .filter { it.packageName == packageName && it.grantedUntil > now }
            .maxOfOrNull { it.grantedUntil }

        return if (grantUntil != null) {
            LockStatus.GrantActive(until = grantUntil, lockedUntil = lock.first)
        } else {
            LockStatus.Locked(until = lock.first, ruleId = lock.second)
        }
    }

    /** End of the rule's currently active window, or null if the rule isn't active at [now]. */
    fun activeUntil(rule: LockRule, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? =
        when (rule.type) {
            RuleType.SESSION -> {
                val start = rule.startAt ?: return null
                val end = rule.endAt ?: return null
                if (now in start until end) end else null
            }
            RuleType.SCHEDULE -> scheduleActiveUntil(rule, now, zone)
        }

    private fun scheduleActiveUntil(rule: LockRule, now: Long, zone: ZoneId): Long? {
        val mask = rule.daysMask ?: return null
        val start = rule.startMinute ?: return null
        val end = rule.endMinute ?: return null

        val local = Instant.ofEpochMilli(now).atZone(zone)
        val minute = local.hour * 60 + local.minute
        val today = local.toLocalDate()
        fun endOn(daysFromToday: Long): Long =
            ZonedDateTime.of(today.plusDays(daysFromToday), java.time.LocalTime.MIN, zone)
                .plusMinutes(end.toLong())
                .toInstant()
                .toEpochMilli()

        val todayOn = isDayOn(mask, local.dayOfWeek.value)
        return when {
            // Same-day window, e.g. 09:00–17:00.
            start < end -> if (todayOn && minute in start until end) endOn(0) else null
            // Overnight window, e.g. 22:00–07:00. The window belongs to the day it starts on.
            start > end -> when {
                todayOn && minute >= start -> endOn(1)
                isDayOn(mask, local.dayOfWeek.minus(1).value) && minute < end -> endOn(0)
                else -> null
            }
            // start == end: a full 24h window starting at start.
            else -> when {
                todayOn && minute >= start -> endOn(1)
                isDayOn(mask, local.dayOfWeek.minus(1).value) && minute < start -> endOn(0)
                else -> null
            }
        }
    }

    /** [isoDay] is 1 (Monday) … 7 (Sunday). */
    fun isDayOn(mask: Int, isoDay: Int): Boolean = mask and (1 shl (isoDay - 1)) != 0
}
