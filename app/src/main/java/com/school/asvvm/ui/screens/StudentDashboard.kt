@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.school.asvvm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.asvvm.data.model.Student
import com.school.asvvm.ui.components.ModernButton
import com.school.asvvm.ui.components.NoticeBoardDialog
import com.school.asvvm.ui.components.SchoolTopBar
import com.school.asvvm.ui.viewmodel.StudentViewModel

@Composable
fun StudentDashboard(
    studentId: String,
    viewModel: StudentViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showNotices by remember { mutableStateOf(false) }

    val notices by viewModel.notices.collectAsState()
    val timetable by viewModel.timetable.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val studentProfile by viewModel.studentProfile.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.initialize(studentId)
    }

    Scaffold(
        topBar = {
            SchoolTopBar(
                title = "Student Portal",
                subtitle = studentProfile?.name ?: "Loading...",
                onLogout = onLogout,
                actions = {
                    IconButton(onClick = { showNotices = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notices", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text("Timetable") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("Report Card") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading || studentProfile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> StudentOverviewTab(studentProfile!!)
                    1 -> StudentTimetableTab(timetable)
                    2 -> StudentReportTab() // Placeholder
                }
            }
        }
    }

    if (showNotices) {
        NoticeBoardDialog(
            notices = notices,
            onDismiss = { showNotices = false }
        )
    }
}

@Composable
fun StudentOverviewTab(student: Student) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profile Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Name: ${student.name}")
                Text("Roll No: ${student.rollNo}")
                Text("Class: ${student.className}")
                Text("Guardian: ${student.guardian}")
            }
        }
        
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Your attendance functionality is being setup.")
            }
        }
    }
}

@Composable
fun StudentTimetableTab(timetable: List<com.school.asvvm.data.model.TimetablePeriod>) {
    if (timetable.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No timetable available for your class.")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(timetable.groupBy { it.dayOfWeek }.toList()) { (day, periods) ->
                Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                periods.sortedBy { it.startTime }.forEach { period ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(period.subject, fontWeight = FontWeight.Bold)
                                Text(period.teacherName, style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${period.startTime} - ${period.endTime}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StudentReportTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Report cards will appear here.")
    }
}
