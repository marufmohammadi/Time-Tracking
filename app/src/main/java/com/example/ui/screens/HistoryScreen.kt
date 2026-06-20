package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeSession
import com.example.ui.viewmodel.TimeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: TimeViewModel,
    modifier: Modifier = Modifier
) {
    val completedSessions by viewModel.completedSessions.collectAsState()
    val context = LocalContext.current
    
    // Day ranges filtering
    var currentFilter by remember { mutableStateOf(DateFilter.ALL) }
    
    // Details popup dialog
    var selectedSessionForDetail by remember { mutableStateOf<TimeSession?>(null) }
    
    // Delete validation dialog
    var sessionToDelete by remember { mutableStateOf<TimeSession?>(null) }

    // Apply filtering query in memory
    val filteredSessions = remember(completedSessions, currentFilter) {
        val now = System.currentTimeMillis()
        when (currentFilter) {
            DateFilter.ALL -> completedSessions
            DateFilter.TODAY -> completedSessions.filter { 
                now - it.startTime <= 24 * 60 * 60 * 1000L 
            }
            DateFilter.PAST_WEEK -> completedSessions.filter { 
                now - it.startTime <= 7 * 24 * 60 * 60 * 1000L 
            }
            DateFilter.PAST_MONTH -> completedSessions.filter { 
                now - it.startTime <= 30L * 24 * 60 * 60 * 1000L 
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // History list Controls banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredSessions.size} logged intervals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Export CSV action
            Button(
                onClick = { viewModel.exportHistoryToCSV(context) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("export_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload, 
                    contentDescription = "Export CSV",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilter.entries.forEach { filter ->
                FilterChip(
                    selected = (currentFilter == filter),
                    onClick = { currentFilter = filter },
                    label = { Text(text = filter.displayName, fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_${filter.name.lowercase()}_chip")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Empty History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History feed empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (completedSessions.isEmpty()) {
                            "You haven't archived any tracking sessions yet. Start tracker, wait, then press Reset to save your first logged session!"
                        } else {
                            "No logged intervals match the select timeframe filter."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = filteredSessions,
                    key = { it.id }
                ) { session ->
                    HistoryItemCard(
                        session = session,
                        onDeleteClick = { sessionToDelete = session },
                        onTap = { selectedSessionForDetail = session }
                    )
                }
            }
        }
    }

    // Full Detail Information Popup Dialog
    if (selectedSessionForDetail != null) {
        val s = selectedSessionForDetail!!
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy\n'at' hh:mm:ss a (zzzz)", Locale.getDefault())
        
        AlertDialog(
            onDismissRequest = { selectedSessionForDetail = null },
            title = {
                Text(
                    text = "Interval History Details",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(
                        label = "TRACKED LENGTH",
                        value = TimeViewModel.formatDuration(s.getDurationMs()),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                    
                    DetailRow(
                        label = "START TIME",
                        value = sdf.format(Date(s.startTime))
                    )
                    
                    DetailRow(
                        label = "END TIME",
                        value = s.endTime?.let { sdf.format(Date(it)) } ?: "Unfinished"
                    )

                    DetailRow(
                        label = "ARCHIVE REASON",
                        value = s.resetReason ?: "No reason supplied",
                        isItalic = s.resetReason == null
                    )

                    DetailRow(
                        label = "CLOUD SYNC STATUS",
                        value = if (s.isSynced) "Backed up to Firestore Cloud" else "Pending (saved locally in SQLite)",
                        valueColor = if (s.isSynced) Color(0xFF22C55E) else MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedSessionForDetail = null },
                    modifier = Modifier.testTag("close_details_button")
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Delete confirmation prompt dialog
    if (sessionToDelete != null) {
        val s = sessionToDelete!!
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Wipe Log Entry?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(text = "Are you sure you want to permanently delete this logged session from local storage? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(s.id)
                        sessionToDelete = null
                        Toast.makeText(context, "Log entry wiped!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    session: TimeSession,
    onDeleteClick: () -> Unit,
    onTap: () -> Unit
) {
    val durationText = TimeViewModel.formatDuration(session.getDurationMs())
    var isExpanded by remember { mutableStateOf(false) }
    
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(session.startTime))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .testTag("history_item_${session.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = durationText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateString,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Sync status indicator miniature cloud icon
                Icon(
                    imageVector = if (session.isSynced) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                    contentDescription = if (session.isSynced) "Synced to Firebase" else "Local Only",
                    tint = if (session.isSynced) Color(0xFF4ADE80) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reason description formatted with elegant italic quotation quotes
            Text(
                text = "\"${session.resetReason ?: "Interval archived with no reason."}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE6E1E5).copy(alpha = 0.9f),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                maxLines = if (isExpanded) 5 else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Dynamic expansion sections showing more action controls
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Detailed button trigger
                        TextButton(
                            onClick = onTap,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View Technical Details", 
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Delete entry button trigger
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Wipe Log Entry",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isItalic: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            lineHeight = 20.sp
        )
    }
}

enum class DateFilter(val displayName: String) {
    ALL("All intervals"),
    TODAY("Saved past 24h"),
    PAST_WEEK("Under 7 days"),
    PAST_MONTH("Under 30 days")
}
