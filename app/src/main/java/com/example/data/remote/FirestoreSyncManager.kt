package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.example.data.model.TimeSession
import com.example.data.local.TimeSessionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreSyncManager(
    private val context: Context,
    private val dao: TimeSessionDao
) {
    private var firestore: FirebaseFirestore? = null
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.UNCONFIGURED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        setupNetworkObserver()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                _syncStatus.value = SyncStatus.IDLE
                Log.d("FirestoreSyncManager", "Firebase already initialized.")
            } else {
                FirebaseApp.initializeApp(context)
                firestore = FirebaseFirestore.getInstance()
                _syncStatus.value = SyncStatus.IDLE
                Log.d("FirestoreSyncManager", "Firebase initialized on-demand.")
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firebase config not found. Running in local-only mode: ${e.message}")
            _syncStatus.value = SyncStatus.UNCONFIGURED
        }
    }

    private fun setupNetworkObserver() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager == null) {
            _isOnline.value = false
            return
        }

        // Set initial state
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isOnline.value = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Register callback for variations
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                Log.d("FirestoreSyncManager", "Network connected. Triggering auto-sync.")
                coroutineScope.launch {
                    syncPendingSessions()
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                Log.d("FirestoreSyncManager", "Network disconnected.")
            }
        })
    }

    enum class SyncStatus {
        UNCONFIGURED, 
        IDLE,         
        SYNCING,      
        SUCCESS,      
        ERROR         
    }

    suspend fun syncPendingSessions() {
        val fs = firestore ?: run {
            _syncStatus.value = SyncStatus.UNCONFIGURED
            return
        }

        if (!_isOnline.value) {
            _syncStatus.value = SyncStatus.IDLE
            return
        }

        try {
            _syncStatus.value = SyncStatus.SYNCING
            val unsynced = dao.getUnsyncedSessions()
            if (unsynced.isEmpty()) {
                _syncStatus.value = SyncStatus.SUCCESS
                return
            }

            for (session in unsynced) {
                val data = mapOf(
                    "id" to session.id,
                    "startTime" to session.startTime,
                    "lastResumeTime" to session.lastResumeTime,
                    "endTime" to session.endTime,
                    "isPaused" to session.isPaused,
                    "accumulatedTime" to session.accumulatedTime,
                    "resetReason" to session.resetReason
                )
                
                // Firestore document set
                fs.collection("sessions")
                    .document(session.id)
                    .set(data)
                    .awaitTask()
                
                // Mark locally as synced in room
                dao.markAsSynced(listOf(session.id))
            }
            _syncStatus.value = SyncStatus.SUCCESS
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Sync Error: ${e.message}")
            _syncStatus.value = SyncStatus.ERROR
        }
    }
}

// Extension to wait for Google Tasks asynchronously
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Firebase request failed"))
        }
    }
}
