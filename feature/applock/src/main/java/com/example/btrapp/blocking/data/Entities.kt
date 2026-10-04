package com.example.btrapp.blocking.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val addedAt: Long,
)

enum class RuleType { SESSION, SCHEDULE }

/**
 * A period during which the rule's apps are locked.
 *
 * SESSION rules use [startAt]/[endAt] (epoch millis).
 * SCHEDULE rules use [daysMask] (bit 0 = Monday … bit 6 = Sunday) and minute-of-day bounds;
 * [endMinute] < [startMinute] means the window wraps past midnight.
 */
@Entity(tableName = "lock_rules")
data class LockRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RuleType,
    val enabled: Boolean = true,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val daysMask: Int? = null,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
)

@Entity(
    tableName = "lock_rule_apps",
    primaryKeys = ["ruleId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = LockRule::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BlockedApp::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packageName")],
)
data class LockRuleApp(
    val ruleId: Long,
    val packageName: String,
)

/** An emergency unlock: the audit-log entry and, until [grantedUntil], an active grant. */
@Entity(tableName = "emergency_unlocks", indices = [Index("packageName"), Index("grantedUntil")])
data class EmergencyUnlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val reason: String,
    val exercise: String,
    val amount: Int,
    val durationMin: Int,
    val createdAt: Long,
    val grantedUntil: Long,
)
