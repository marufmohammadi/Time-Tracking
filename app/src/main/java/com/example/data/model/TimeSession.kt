package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "time_sessions")
data class TimeSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val lastResumeTime: Long = startTime,
    val endTime: Long? = null,
    val isPaused: Boolean = false,
    val accumulatedTime: Long = 0L,
    val resetReason: String? = null,
    val isSynced: Boolean = false
) {
    /**
     * Calculates the total duration of the session in milliseconds.
     */
    fun getDurationMs(currentTime: Long = System.currentTimeMillis()): Long {
        if (endTime != null) {
            // Decided/finished session
            return accumulatedTime
        }
        return if (isPaused) {
            accumulatedTime
        } else {
            accumulatedTime + (currentTime - lastResumeTime)
        }
    }
}
