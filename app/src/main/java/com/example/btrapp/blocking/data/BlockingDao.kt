package com.example.btrapp.blocking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockingDao {

    @Query("SELECT * FROM blocked_apps ORDER BY label COLLATE NOCASE")
    fun observeBlockedApps(): Flow<List<BlockedApp>>

    @Upsert
    suspend fun upsertBlockedApp(app: BlockedApp)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteBlockedApp(packageName: String)

    @Query("SELECT * FROM lock_rules ORDER BY id DESC")
    fun observeRules(): Flow<List<LockRule>>

    @Query("SELECT * FROM lock_rules WHERE id = :id")
    suspend fun getRule(id: Long): LockRule?

    @Query("SELECT * FROM lock_rule_apps")
    fun observeRuleApps(): Flow<List<LockRuleApp>>

    @Query("SELECT packageName FROM lock_rule_apps WHERE ruleId = :ruleId")
    suspend fun getRulePackages(ruleId: Long): List<String>

    @Insert
    suspend fun insertRule(rule: LockRule): Long

    @Update
    suspend fun updateRule(rule: LockRule)

    @Query("UPDATE lock_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM lock_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM lock_rules WHERE type = 'SESSION' AND endAt <= :now")
    suspend fun deleteEndedSessions(now: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuleApps(apps: List<LockRuleApp>)

    @Query("DELETE FROM lock_rule_apps WHERE ruleId = :ruleId")
    suspend fun clearRuleApps(ruleId: Long)

    @Transaction
    suspend fun saveRule(rule: LockRule, packageNames: Collection<String>): Long {
        val id = if (rule.id == 0L) insertRule(rule) else rule.id.also { updateRule(rule) }
        clearRuleApps(id)
        insertRuleApps(packageNames.map { LockRuleApp(id, it) })
        return id
    }

    @Query("SELECT * FROM emergency_unlocks ORDER BY createdAt DESC")
    fun observeUnlocks(): Flow<List<EmergencyUnlock>>

    @Query("SELECT * FROM emergency_unlocks WHERE grantedUntil > :since")
    fun observeGrantsEndingAfter(since: Long): Flow<List<EmergencyUnlock>>

    @Query("SELECT COUNT(*) FROM emergency_unlocks WHERE createdAt >= :since")
    suspend fun countUnlocksSince(since: Long): Int

    @Insert
    suspend fun insertUnlock(unlock: EmergencyUnlock): Long
}
