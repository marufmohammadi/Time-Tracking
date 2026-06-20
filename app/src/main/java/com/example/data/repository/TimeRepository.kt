package com.example.data.repository

import com.example.data.local.TimeSessionDao
import com.example.data.model.TimeSession
import com.example.data.remote.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TimeRepository(
    private val dao: TimeSessionDao,
    private val syncManager: FirestoreSyncManager
) {
    val activeSession: Flow<TimeSession?> = dao.getActiveSessionFlow()
    val completedSessions: Flow<List<TimeSession>> = dao.getCompletedSessionsFlow()
    val syncStatus: Flow<FirestoreSyncManager.SyncStatus> = syncManager.syncStatus
    val isOnline: Flow<Boolean> = syncManager.isOnline

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private fun triggerBackgroundSync() {
        repositoryScope.launch {
            syncManager.syncPendingSessions()
        }
    }

    suspend fun startNewSession() {
        val currentActive = dao.getActiveSession()
        if (currentActive == null) {
            val newSession = TimeSession()
            dao.insertSession(newSession)
            triggerBackgroundSync()
        }
    }

    suspend fun pauseSession() {
        val active = dao.getActiveSession() ?: return
        if (!active.isPaused) {
            val now = System.currentTimeMillis()
            val totalAccumulated = active.accumulatedTime + (now - active.lastResumeTime)
            val pausedSession = active.copy(
                accumulatedTime = totalAccumulated,
                isPaused = true,
                isSynced = false // Mark unsynced for updates
            )
            dao.insertSession(pausedSession)
            triggerBackgroundSync()
        }
    }

    suspend fun resumeSession() {
        val active = dao.getActiveSession() ?: return
        if (active.isPaused) {
            val now = System.currentTimeMillis()
            val resumedSession = active.copy(
                lastResumeTime = now,
                isPaused = false,
                isSynced = false
            )
            dao.insertSession(resumedSession)
            triggerBackgroundSync()
        }
    }

    suspend fun resetSession(reason: String) {
        val active = dao.getActiveSession()
        val now = System.currentTimeMillis()

        if (active != null) {
            // Update active session to complete it
            val finalAccumulated = if (active.isPaused) {
                active.accumulatedTime
            } else {
                active.accumulatedTime + (now - active.lastResumeTime)
            }

            val completedSession = active.copy(
                endTime = now,
                accumulatedTime = finalAccumulated,
                resetReason = reason,
                isSynced = false
            )
            dao.insertSession(completedSession)
        }

        // Auto-start a new session
        val newSession = TimeSession(startTime = now, lastResumeTime = now)
        dao.insertSession(newSession)
        
        triggerBackgroundSync()
    }

    suspend fun forceSync() {
        syncManager.syncPendingSessions()
    }

    suspend fun deleteSession(id: String) {
        dao.deleteSessionById(id)
        triggerBackgroundSync()
    }
}
