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
import androidx.compose.ui.platform.LocalContext
import com.school.asvvm.data.model.Mark
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.SubjectConfig
import com.school.asvvm.ui.components.ModernButton
import com.school.asvvm.ui.components.NoticeBoardDialog
import com.school.asvvm.ui.components.SchoolTopBar
import com.school.asvvm.ui.theme.*
import com.school.asvvm.ui.viewmodel.StudentViewModel
import com.school.asvvm.util.PdfGenerator

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
    val marks by viewModel.marks.collectAsState()
    val subjectConfigs by viewModel.subjectConfigs.collectAsState()
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
                    2 -> StudentReportTab(studentProfile!!, marks, subjectConfigs)
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
fun StudentReportTab(student: Student, marks: List<Mark>, subjectConfigs: List<SubjectConfig>) {
    val context = LocalContext.current
    if (marks.isEmpty()) {
        com.school.asvvm.ui.components.EmptyStateView(
            icon = Icons.Default.Assessment,
            title = "No Marks Published Yet",
            subtitle = "Your terminal report card will appear here once evaluation is completed by your teachers."
        )
        return
    }

    // Determine completed terms from student's marks
    val completedTerms = marks.map { it.term }.toSet()
    val isAnnualCompleted = completedTerms.contains("ANNUAL")
    val isSecondHalfCompleted = completedTerms.contains("SECOND_HALF")
    val isFirstHalfCompleted = completedTerms.contains("FIRST_HALF")

    // Filter subjects for student's class
    val uniqueSubjects = subjectConfigs.map { it.subjectName }.distinct()

    // Calculate dynamic totals strictly for COMPLETED terms
    var dynamicObtainedTotal = 0
    var dynamicMaxTotal = 0

    uniqueSubjects.forEach { subject ->
        val sConfigs = subjectConfigs.filter { it.subjectName == subject }
        val m1 = marks.find { it.subject == subject && it.term == "FIRST_HALF" }
        val m2 = marks.find { it.subject == subject && it.term == "SECOND_HALF" }
        val ma = marks.find { it.subject == subject && it.term == "ANNUAL" }

        val c1 = sConfigs.find { it.term == "FIRST_HALF" }
        val c2 = sConfigs.find { it.term == "SECOND_HALF" }
        val ca = sConfigs.find { it.term == "ANNUAL" }

        if (m1 != null && c1 != null) {
            dynamicMaxTotal += c1.maxWritten + (if (c1.hasOral) c1.maxOral else 0)
            dynamicObtainedTotal += (m1.writtenMarks.toIntOrNull() ?: 0) + (m1.oralMarks.toIntOrNull() ?: 0)
        }
        if (m2 != null && c2 != null) {
            dynamicMaxTotal += c2.maxWritten + (if (c2.hasOral) c2.maxOral else 0)
            dynamicObtainedTotal += (m2.writtenMarks.toIntOrNull() ?: 0) + (m2.oralMarks.toIntOrNull() ?: 0)
        }
        if (ma != null && ca != null) {
            dynamicMaxTotal += ca.maxWritten + (if (ca.hasOral) ca.maxOral else 0)
            dynamicObtainedTotal += (ma.writtenMarks.toIntOrNull() ?: 0) + (ma.oralMarks.toIntOrNull() ?: 0)
        }
    }

    val percentage = if (dynamicMaxTotal > 0) (dynamicObtainedTotal.toDouble() / dynamicMaxTotal * 100) else 0.0
    val overallGrade = when {
        percentage >= 90 -> "A+"
        percentage >= 80 -> "A"
        percentage >= 70 -> "B+"
        percentage >= 60 -> "B"
        percentage >= 45 -> "C"
        percentage >= 35 -> "P"
        else -> "F"
    }

    val resultStatus = when {
        isAnnualCompleted -> if (overallGrade == "F") "DETAINED" else "PROMOTED TO NEXT CLASS"
        isSecondHalfCompleted -> if (overallGrade == "F") "NEEDS IMPROVEMENT" else "2ND TERM COMPLETED"
        isFirstHalfCompleted -> if (overallGrade == "F") "NEEDS IMPROVEMENT" else "1ST TERM COMPLETED"
        else -> "RESULT AWAITED"
    }

    var selectedTermFilter by remember { mutableStateOf("ALL") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Report Card Overview Surface
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Academic Performance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Grade Card",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        com.school.asvvm.ui.components.M3StatusChip(
                            text = resultStatus,
                            icon = if (overallGrade == "F") Icons.Default.Warning else Icons.Default.Verified,
                            containerColor = if (overallGrade == "F") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = if (overallGrade == "F") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Performance Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("TOTAL MARKS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Spacer(Modifier.height(4.dp))
                                Text("$dynamicObtainedTotal / $dynamicMaxTotal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("PERCENTAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                Spacer(Modifier.height(4.dp))
                                Text("${"%.1f".format(percentage)}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GRADE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                Spacer(Modifier.height(4.dp))
                                Text(overallGrade, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Download / Print PDF Button
                    ModernButton(
                        onClick = {
                            PdfGenerator.generateAndPrint(
                                context = context,
                                student = student,
                                marks = marks,
                                subjectConfigs = subjectConfigs,
                                total = "$dynamicObtainedTotal / $dynamicMaxTotal",
                                percentage = "%.1f".format(percentage),
                                grade = overallGrade
                            )
                        },
                        text = "DOWNLOAD OFFICIAL REPORT CARD (PDF)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Term Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All Terms", "FIRST_HALF" to "1st Term", "SECOND_HALF" to "2nd Term", "ANNUAL" to "Annual").forEach { (termKey, label) ->
                    val isSelected = selectedTermFilter == termKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTermFilter = termKey },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }
        }

        // Subject Grade Details List
        val filteredMarks = if (selectedTermFilter == "ALL") marks else marks.filter { it.term == selectedTermFilter }
        val displayedSubjects = uniqueSubjects.filter { subject ->
            if (selectedTermFilter == "ALL") true else filteredMarks.any { it.subject == subject }
        }

        items(displayedSubjects) { subject ->
            val subjectMarks = marks.filter { it.subject == subject }
            val colors = listOf(SubjectMath, SubjectScience, SubjectEnglish, SubjectArt, SubjectHistory)
            val subjectColor = colors[subject.hashCode().let { if (it < 0) -it else it } % colors.size]

            com.school.asvvm.ui.components.ModernCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
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
                            Text(
                                subject,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))

                    // Term breakdown rows
                    subjectMarks.forEach { mark ->
                        val termTitle = when (mark.term) {
                            "FIRST_HALF" -> "1st Term"
                            "SECOND_HALF" -> "2nd Term"
                            "ANNUAL" -> "Annual Exam"
                            else -> mark.term
                        }
                        val written = mark.writtenMarks.takeIf { it.isNotBlank() } ?: "-"
                        val oral = mark.oralMarks.takeIf { it.isNotBlank() && it != "0" } ?: "-"
                        val total = (mark.writtenMarks.toIntOrNull() ?: 0) + (mark.oralMarks.toIntOrNull() ?: 0)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(termTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Written: $written ${if (oral != "-") "| Oral: $oral " else ""}-> Total: $total",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
