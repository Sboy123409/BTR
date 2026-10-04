package com.example.btrapp.blocking.data

import com.example.btrapp.blocking.domain.LockSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId

data class RuleWithApps(val rule: LockRule, val packageNames: Set<String>)

class BlockingRepository(
    private val dao: BlockingDao,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val blockedApps: Flow<List<BlockedApp>> = dao.observeBlockedApps()

    val unlockHistory: Flow<List<EmergencyUnlock>> = dao.observeUnlocks()

    val rules: Flow<List<RuleWithApps>> =
        combine(dao.observeRules(), dao.observeRuleApps()) { rules, ruleApps ->
            val byRule = ruleApps.groupBy({ it.ruleId }, { it.packageName })
            rules.map { RuleWithApps(it, byRule[it.id].orEmpty().toSet()) }
        }

    /**
     * Kept hot from app start so the accessibility service can decide synchronously.
     * Grants that ended before the app started are irrelevant; everything inserted later qualifies.
     */
    val snapshot: StateFlow<LockSnapshot> =
        combine(
            dao.observeRules(),
            dao.observeRuleApps(),
            dao.observeGrantsEndingAfter(clock()),
        ) { rules, ruleApps, grants ->
            LockSnapshot(
                rules = rules,
                ruleApps = ruleApps.groupBy({ it.ruleId }, { it.packageName })
                    .mapValues { it.value.toSet() },
                grants = grants,
            )
        }.stateIn(scope, SharingStarted.Eagerly, LockSnapshot.EMPTY)

    suspend fun setBlocked(packageName: String, label: String, blocked: Boolean) {
        if (blocked) {
            dao.upsertBlockedApp(BlockedApp(packageName, label, clock()))
        } else {
            dao.deleteBlockedApp(packageName)
        }
    }

    suspend fun startSession(minutes: Int, packageNames: Collection<String>): Long {
        val now = clock()
        val rule = LockRule(type = RuleType.SESSION, startAt = now, endAt = now + minutes * 60_000L)
        return dao.saveRule(rule, packageNames)
    }

    suspend fun saveRule(rule: LockRule, packageNames: Collection<String>): Long =
        dao.saveRule(rule, packageNames)

    suspend fun getRule(id: Long): RuleWithApps? =
        dao.getRule(id)?.let { RuleWithApps(it, dao.getRulePackages(id).toSet()) }

    suspend fun setRuleEnabled(id: Long, enabled: Boolean) = dao.setRuleEnabled(id, enabled)

    suspend fun deleteRule(id: Long) = dao.deleteRule(id)

    suspend fun deleteEndedSessions() = dao.deleteEndedSessions(clock())

    suspend fun unlocksToday(zone: ZoneId = ZoneId.systemDefault()): Int {
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return dao.countUnlocksSince(startOfDay)
    }

    /**
     * Records an emergency unlock and waits until the in-memory snapshot contains it, so the
     * service doesn't re-block the app in the moment between the insert and the flow update.
     */
    suspend fun recordEmergencyUnlock(
        packageName: String,
        reason: String,
        exercise: String,
        amount: Int,
        durationMin: Int,
    ) {
        val now = clock()
        val id = dao.insertUnlock(
            EmergencyUnlock(
                packageName = packageName,
                reason = reason,
                exercise = exercise,
                amount = amount,
                durationMin = durationMin,
                createdAt = now,
                grantedUntil = now + durationMin * 60_000L,
            )
        )
        withTimeoutOrNull(2_000) { snapshot.first { s -> s.grants.any { it.id == id } } }
    }
}
