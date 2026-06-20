package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.TimeSession
import com.example.data.repository.TimeRepository
import com.example.data.remote.FirestoreSyncManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeViewModel(
    application: Application,
    private val repository: TimeRepository
) : AndroidViewModel(application) {

    val activeSession: StateFlow<TimeSession?> = repository.activeSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completedSessions: StateFlow<List<TimeSession>> = repository.completedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncStatus: StateFlow<FirestoreSyncManager.SyncStatus> = repository.syncStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FirestoreSyncManager.SyncStatus.UNCONFIGURED)

    val isOnline: StateFlow<Boolean> = repository.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Current session live ticking duration in ms
    private val _liveDurationMs = MutableStateFlow(0L)
    val liveDurationMs: StateFlow<Long> = _liveDurationMs

    // User customized Goal in days (defaults to 30.0 for the 30-Day Challenge)
    private val _goalDays = MutableStateFlow(30.0)
    val goalDays: StateFlow<Double> = _goalDays

    private var tickerJob: Job? = null

    init {
        // Observe active session to start/stop the live ticking scheduler
        viewModelScope.launch {
            activeSession.collect { session ->
                stopTicker()
                if (session != null) {
                    startTicker(session)
                } else {
                    _liveDurationMs.value = 0L
                }
            }
        }
    }

    private fun startTicker(session: TimeSession) {
        tickerJob = viewModelScope.launch {
            while (true) {
                _liveDurationMs.value = session.getDurationMs()
                delay(200) // Update 5 times a second for responsive, fluid changes
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun startNewSession() {
        viewModelScope.launch {
            repository.startNewSession()
        }
    }

    fun pauseSession() {
        viewModelScope.launch {
            repository.pauseSession()
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            repository.resumeSession()
        }
    }

    fun resetSession(reason: String) {
        viewModelScope.launch {
            repository.resetSession(reason)
        }
    }

    fun forceSync() {
        viewModelScope.launch {
            repository.forceSync()
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    fun setGoalDays(days: Double) {
        _goalDays.value = days.coerceAtLeast(0.1)
    }

    /**
     * Exports completed sessions to a CSV file and shows a popup.
     */
    fun exportHistoryToCSV(context: Context) {
        val sessions = completedSessions.value
        if (sessions.isEmpty()) {
            Toast.makeText(context, "No completed history to export!", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            try {
                val filename = "TimeTracker_Export_${System.currentTimeMillis()}.csv"
                // Save in public app documents directory
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val file = File(dir, filename)
                
                val writer = FileWriter(file)
                writer.append("ID,Start Date,End Date,Duration (S),Duration Text,Reset Reason\n")
                
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                
                for (session in sessions) {
                    val startStr = sdf.format(Date(session.startTime))
                    val endStr = session.endTime?.let { sdf.format(Date(it)) } ?: ""
                    val durationSeconds = session.getDurationMs() / 1000
                    val durationText = formatDuration(session.getDurationMs())
                    val reasonCleaned = (session.resetReason ?: "N/A").replace("\"", "\"\"")
                    
                    writer.append("\"${session.id}\",\"$startStr\",\"$endStr\",$durationSeconds,\"$durationText\",\"$reasonCleaned\"\n")
                }
                
                writer.flush()
                writer.close()
                
                Toast.makeText(context, "Exported successfully to: ${file.name}", Toast.LENGTH_LONG).show()
                Log.d("TimeViewModel", "CSV saved successfully at: ${file.absolutePath}")
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("TimeViewModel", "CSV export failed", e)
            }
        }
    }

    companion object {
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val seconds = totalSeconds % 60
            val totalMinutes = totalSeconds / 60
            val minutes = totalMinutes % 60
            val totalHours = totalMinutes / 60
            val hours = totalHours % 24
            val days = totalHours / 24

            val parts = mutableListOf<String>()
            if (days > 0) parts.add("${days}d")
            if (hours > 0 || days > 0) parts.add("${hours}h")
            if (minutes > 0 || hours > 0 || days > 0) parts.add("${minutes}m")
            parts.add("${seconds}s")

            return parts.joinToString(" ")
        }
    }
}
