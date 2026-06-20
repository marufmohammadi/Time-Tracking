package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DatabaseProvider
import com.example.data.remote.FirestoreSyncManager
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TimeViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: TimeViewModel

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room database & repository
        val repository = DatabaseProvider.getRepository(applicationContext)
        viewModel = TimeViewModel(application, repository)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(MainTab.TRACKER) }
                var showSyncExplanationDialog by remember { mutableStateOf(false) }

                val syncStatus by viewModel.syncStatus.collectAsState()
                val isOnline by viewModel.isOnline.collectAsState()
                val context = LocalContext.current

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Time Progress",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEADDFF),
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = if (syncStatus == FirestoreSyncManager.SyncStatus.SUCCESS || syncStatus == FirestoreSyncManager.SyncStatus.IDLE) Color(0xFF4ADE80) else Color(0xFF938F99),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(
                                        text = when (syncStatus) {
                                            FirestoreSyncManager.SyncStatus.UNCONFIGURED -> "Offline SQLite Local"
                                            FirestoreSyncManager.SyncStatus.SYNCING -> "Syncing..."
                                            FirestoreSyncManager.SyncStatus.ERROR -> "Sync Error"
                                            else -> "Synced with Firestore"
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF938F99),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Circular Cloud Sync Bullet Trigger box
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4A4458))
                                    .clickable {
                                        if (syncStatus == FirestoreSyncManager.SyncStatus.UNCONFIGURED) {
                                            showSyncExplanationDialog = true
                                        } else {
                                            viewModel.forceSync()
                                            Toast.makeText(context, "Cloud sync triggered!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .testTag("sync_status_bubble"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (syncStatus) {
                                        FirestoreSyncManager.SyncStatus.UNCONFIGURED -> Icons.Default.CloudOff
                                        FirestoreSyncManager.SyncStatus.SYNCING -> Icons.Default.CloudQueue
                                        FirestoreSyncManager.SyncStatus.ERROR -> Icons.Default.CloudSync
                                        else -> Icons.Default.CloudDone
                                    },
                                    contentDescription = "Cloud Synced",
                                    tint = Color(0xFFEADDFF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    bottomBar = {
                        Column {
                            HorizontalDivider(color = Color(0xFF49454F), thickness = 1.dp)
                            NavigationBar(
                                containerColor = Color(0xFF211F26),
                                tonalElevation = 0.dp,
                                modifier = Modifier.testTag("app_bottom_navigation")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == MainTab.TRACKER,
                                    onClick = { currentTab = MainTab.TRACKER },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == MainTab.TRACKER) Icons.Filled.Timer else Icons.Outlined.Timer,
                                            contentDescription = "Tracker"
                                        )
                                    },
                                    label = { Text("Tracker", fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFD0BCFF),
                                        unselectedIconColor = Color(0xFF938F99),
                                        selectedTextColor = Color(0xFFD0BCFF),
                                        unselectedTextColor = Color(0xFF938F99),
                                        indicatorColor = Color(0xFF4A4458)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_tracker")
                                )

                                NavigationBarItem(
                                    selected = currentTab == MainTab.HISTORY,
                                    onClick = { currentTab = MainTab.HISTORY },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == MainTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                            contentDescription = "History"
                                        )
                                    },
                                    label = { Text("History", fontWeight = FontWeight.Bold) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFFD0BCFF),
                                        unselectedIconColor = Color(0xFF938F99),
                                        selectedTextColor = Color(0xFFD0BCFF),
                                        unselectedTextColor = Color(0xFF938F99),
                                        indicatorColor = Color(0xFF4A4458)
                                    ),
                                    modifier = Modifier.testTag("nav_tab_history")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                            }
                        ) { tab ->
                            when (tab) {
                                MainTab.TRACKER -> {
                                    TrackerScreen(viewModel = viewModel)
                                }
                                MainTab.HISTORY -> {
                                    HistoryScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }

                // Edu explanation popup for unconfigured Firestore SDK state
                if (showSyncExplanationDialog) {
                    AlertDialog(
                        onDismissRequest = { showSyncExplanationDialog = false },
                        title = {
                            Text(
                                text = "SQLite-Local Database Mode",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Your tracking data is completely safe, stored offline-first in SQLite database on your device. The app is fully operational without any internet access.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "To enable Cloud backup and Firestore syncing, simply place your Firebase configuration 'google-services.json' file into your Android project's /app/ folder under Gradle.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "The app's sync engine automatically binds upon launch and begins backing up newly saved intervals.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { showSyncExplanationDialog = false },
                                modifier = Modifier.testTag("dismiss_explanation_button")
                            ) {
                                Text("Got it")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SyncStatusBubble(
    status: FirestoreSyncManager.SyncStatus,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    val bubbleColor: Color
    val textColor: Color
    val text: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    when (status) {
        FirestoreSyncManager.SyncStatus.UNCONFIGURED -> {
            bubbleColor = MaterialTheme.colorScheme.surfaceVariant
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
            text = "Offline SQLite"
            icon = Icons.Outlined.CloudOff
        }
        FirestoreSyncManager.SyncStatus.SYNCING -> {
            bubbleColor = MaterialTheme.colorScheme.primaryContainer
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
            text = "Syncing..."
            icon = Icons.Default.CloudQueue
        }
        FirestoreSyncManager.SyncStatus.ERROR -> {
            bubbleColor = MaterialTheme.colorScheme.errorContainer
            textColor = MaterialTheme.colorScheme.onErrorContainer
            text = "Retry Sync"
            icon = Icons.Default.CloudSync
        }
        else -> {
            // IDLE / SUCCESS
            if (isOnline) {
                bubbleColor = Color(0xFFDCFCE7) // Rich pale light green
                textColor = Color(0xFF166534)
                text = "Cloud Synced"
                icon = Icons.Default.CloudDone
            } else {
                bubbleColor = MaterialTheme.colorScheme.surfaceVariant
                textColor = MaterialTheme.colorScheme.onSurfaceVariant
                text = "SQLite Local"
                icon = Icons.Outlined.CloudQueue
            }
        }
    }

    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bubbleColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("sync_status_bubble"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = textColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

enum class MainTab {
    TRACKER,
    HISTORY
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
