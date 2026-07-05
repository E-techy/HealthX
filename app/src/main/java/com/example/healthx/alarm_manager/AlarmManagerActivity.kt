package com.example.healthx.alarm_manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthx.data.local.entities.AlarmEntity
import com.example.healthx.ui.theme.HealthXTheme

class AlarmManagerActivity : ComponentActivity() {
    private val viewModel: AlarmManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthXTheme {
                AlarmManagerUI(viewModel = viewModel, onBackClicked = { finish() })
            }
        }
    }
}

enum class CardState { COLLAPSED, EXPANDED_QUICK, EDIT_FULL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmManagerUI(viewModel: AlarmManagerViewModel, onBackClicked: () -> Unit) {
    val activeAlarms by viewModel.activeAlarms.collectAsState()
    val allAlarms by viewModel.filteredAllAlarms.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()

    var showAllActive by remember { mutableStateOf(false) }
    var isCreateModeExpanded by remember { mutableStateOf(false) }
    val categories = remember(allAlarms) { allAlarms.map { it.category }.distinct() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Command Center", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- SECTION 0: CREATE ALARM (IMPORTED FROM SEPARATED FILE) ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF192A40)),
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { isCreateModeExpanded = !isCreateModeExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddAlarm, contentDescription = "Create", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create New Alarm", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Icon(if (isCreateModeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Toggle", tint = Color.White)
                        }

                        AnimatedVisibility(visible = isCreateModeExpanded) {
                            CreateAlarmSection(viewModel = viewModel, onCreated = { isCreateModeExpanded = false })
                        }
                    }
                }
            }

            // --- SECTION 1: ACTIVE / RUNNING ALARMS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Currently Scheduled", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    if (activeAlarms.size > 3) {
                        Text(
                            text = if (showAllActive) "Show Less" else "Show All",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showAllActive = !showAllActive }
                        )
                    }
                }
            }

            val visibleActiveAlarms = if (showAllActive) activeAlarms else activeAlarms.take(3)

            if (visibleActiveAlarms.isEmpty()) {
                item { Text("No upcoming alarms scheduled.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp)) }
            } else {
                items(visibleActiveAlarms) { alarm -> AlarmItemCard(alarm = alarm, viewModel = viewModel) }
            }

            // --- SECTION 2: ALL ALARMS ---
            item {
                Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))
                Text("All Alarms Database", color = Color.White, style = MaterialTheme.typography.titleLarge)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategoryFilter(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            items(allAlarms) { alarm -> AlarmItemCard(alarm = alarm, viewModel = viewModel) }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun AlarmItemCard(alarm: AlarmEntity, viewModel: AlarmManagerViewModel) {
    var cardState by remember { mutableStateOf(CardState.COLLAPSED) }

    val isCurrentlyRunning = alarm.status == "PENDING" && (alarm.triggerTimeMillis - System.currentTimeMillis() < 60_000)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable {
            cardState = when (cardState) {
                CardState.COLLAPSED -> CardState.EXPANDED_QUICK
                CardState.EXPANDED_QUICK -> CardState.EDIT_FULL
                CardState.EDIT_FULL -> CardState.COLLAPSED
            }
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // STAGE 1: COLLAPSED
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!alarm.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = alarm.logoUrl, contentDescription = "Category Logo", contentScale = ContentScale.Crop,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2C2C2C))
                    )
                } else {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF2C2C2C)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = alarm.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = alarm.category, color = Color.Gray, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = viewModel.formatTriggerTime(alarm.triggerTimeMillis), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = if (isCurrentlyRunning) "RUNNING" else alarm.status, color = if (isCurrentlyRunning) Color.Red else if (alarm.status == "PENDING") Color.Yellow else Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // STAGE 2: QUICK ACTIONS
            AnimatedVisibility(visible = cardState != CardState.COLLAPSED) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(text = alarm.description, color = Color.LightGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { viewModel.deleteAlarm(alarm) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE53935))
                        }
                        if (!isCurrentlyRunning) {
                            IconButton(onClick = { viewModel.syncAlarmWithCloud(alarm.id) }) {
                                Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color.Cyan)
                            }
                        } else {
                            IconButton(onClick = { viewModel.stopRunningAlarm(alarm.id) }) {
                                Icon(Icons.Default.StopCircle, contentDescription = "Stop Alarm", tint = Color.Yellow)
                            }
                        }
                    }
                }
            }

            // STAGE 3: EDIT (Locked if running)
            AnimatedVisibility(visible = cardState == CardState.EDIT_FULL) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCurrentlyRunning) {
                        Surface(color = Color(0xFF3E1E1E), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFE53935))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("This alarm is currently active or about to ring. You cannot edit it right now.", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text("Audio Type: ${alarm.audioPlaybackType}", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.updateAlarm(alarm); cardState = CardState.COLLAPSED },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Changes") }
                    }
                }
            }
        }
    }
}