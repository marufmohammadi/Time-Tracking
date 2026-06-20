package com.example.data.local

import androidx.room.*
import com.example.data.model.TimeSession
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeSessionDao {
    @Query("SELECT * FROM time_sessions WHERE endTime IS NULL LIMIT 1")
    fun getActiveSessionFlow(): Flow<TimeSession?>

    @Query("SELECT * FROM time_sessions WHERE endTime IS NULL LIMIT 1")
    suspend fun getActiveSession(): TimeSession?

    @Query("SELECT * FROM time_sessions WHERE endTime IS NOT NULL ORDER BY endTime DESC")
    fun getCompletedSessionsFlow(): Flow<List<TimeSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimeSession)

    @Update
    suspend fun updateSession(session: TimeSession)

    @Query("SELECT * FROM time_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<TimeSession>

    @Query("UPDATE time_sessions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("DELETE FROM time_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}
