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
import com.school.asvvm.ui.theme.*
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
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            if (isLoading || studentProfile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                }
            } else {
                when (selectedTab) {
                    0 -> StudentOverviewTab(studentProfile!!)
                    1 -> StudentTimetableTab(timetable)
                    2 -> StudentReportTab()
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Student Profile Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.School, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Welcome back,", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                student.name, 
                                style = MaterialTheme.typography.headlineLarge, 
                                fontWeight = FontWeight.ExtraBold, 
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.school.asvvm.ui.components.M3StatusChip(
                        text = "Class: ${student.className}",
                        icon = Icons.Default.Class,
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    com.school.asvvm.ui.components.M3StatusChip(
                        text = "Roll No: ${student.rollNo}",
                        icon = Icons.Default.Numbers,
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Guardian & Status Info Card
        com.school.asvvm.ui.components.ModernCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                Text(
                    "Student Details", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Guardian Name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(student.guardian.ifBlank { "Not Specified" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    com.school.asvvm.ui.components.M3StatusChip(
                        text = if (student.lockedTerms.isEmpty()) "Active" else "Locked (${student.lockedTerms.size} terms)",
                        icon = if (student.lockedTerms.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Lock,
                        containerColor = if (student.lockedTerms.isEmpty()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (student.lockedTerms.isEmpty()) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StudentTimetableTab(timetable: List<com.school.asvvm.data.model.TimetablePeriod>) {
    if (timetable.isEmpty()) {
        com.school.asvvm.ui.components.EmptyStateView(
            icon = Icons.Default.Schedule,
            title = "No Timetable Found",
            subtitle = "Your class schedule has not been published yet."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(timetable.groupBy { it.dayOfWeek }.toList()) { (day, periods) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.school.asvvm.ui.components.M3StatusChip(
                        text = day,
                        icon = Icons.Default.CalendarToday,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    periods.sortedBy { it.startTime }.forEach { period ->
                        val colors = listOf(SubjectMath, SubjectScience, SubjectEnglish, SubjectArt, SubjectHistory)
                        val subjectColor = colors[period.subject.hashCode().let { if (it < 0) -it else it } % colors.size]

                        com.school.asvvm.ui.components.ModernCard(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    com.school.asvvm.ui.components.ColoredIconBox(
                                        icon = Icons.Default.Book,
                                        color = subjectColor
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            period.subject, 
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            period.teacherName, 
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                com.school.asvvm.ui.components.M3StatusChip(
                                    text = "${period.startTime} - ${period.endTime}",
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentReportTab() {
    com.school.asvvm.ui.components.EmptyStateView(
        icon = Icons.Default.Assessment,
        title = "Report Cards",
        subtitle = "Published terminal report cards will appear here."
    )
}
