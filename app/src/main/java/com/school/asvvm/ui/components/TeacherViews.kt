@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.school.asvvm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.asvvm.data.model.Mark
import com.school.asvvm.data.model.Student
import com.school.asvvm.ui.theme.*
import com.school.asvvm.ui.viewmodel.TeacherViewModel
import com.school.asvvm.util.PdfGenerator

@Composable
fun TeacherClassSelector(classes: List<String>, selectedClass: String?, onClassSelected: (String) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = classes.indexOf(selectedClass).coerceAtLeast(0),
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        divider = {}
    ) {
        classes.forEach { cls ->
            val isSelected = selectedClass == cls
            Tab(
                selected = isSelected,
                onClick = { onClassSelected(cls) },
                text = { 
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            cls, 
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    }
                }
            )
        }
    }
}


@Composable
fun TeacherProfileView(
    teacher: com.school.asvvm.data.model.Teacher?,
    onApplyLeave: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    if (teacher == null) return
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.size(108.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AccountCircle, 
                    contentDescription = null, 
                    modifier = Modifier.size(72.dp), 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            teacher.name, 
            style = MaterialTheme.typography.headlineLarge, 
            fontWeight = FontWeight.ExtraBold, 
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                teacher.email, 
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSecondaryContainer, 
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(Modifier.height(20.dp))
        
        // Managed Stats Grid (Classes & Leave Quick Card)
        val safeClasses = teacher.assignedClasses ?: emptyList()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            M3StatCard(
                title = "Classes Managed",
                value = "${safeClasses.size}",
                icon = Icons.Default.Class,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onApplyLeave),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Faculty Leave",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Apply Now",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Icon(
                        Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        ModernCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                ProfileInfoRow("Phone Number", teacher.phone.takeIf { it.isNotBlank() } ?: "Not Set")
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                ProfileInfoRow("Gender Identity", teacher.gender.takeIf { it.isNotBlank() } ?: "Not Set")
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                ProfileInfoRow("Assigned Roster", safeClasses.joinToString().ifBlank { "None" })
            }
        }

        Spacer(Modifier.height(24.dp))

        // Account Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onChangePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("CHANGE PASSWORD", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("SIGN OUT", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}


@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}


@Composable
fun TeacherStudentListView(students: List<Student>) {
    if (students.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.GroupOff,
            title = "No Students Found",
            subtitle = "There are no students enrolled in this class yet."
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(students) { student ->
            ModernCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                onClick = {}
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(SubjectMath, SubjectScience, SubjectEnglish, SubjectArt, SubjectHistory)
                    val color = colors[student.rollNo.hashCode().let { if (it < 0) -it else it } % colors.size]
                    
                    ColoredIconBox(
                        icon = Icons.Default.Person,
                        color = color
                    )
                    
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            student.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Roll No: ${student.rollNo} • Guardian: ${student.guardian.ifBlank { "N/A" }}", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}


@Composable
fun TeacherGradingView(students: List<Student>, className: String, viewModel: TeacherViewModel) {
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    val selectedStudent = students.find { it.id == selectedStudentId } ?: students.firstOrNull()
    var selectedTerm by remember { mutableStateOf(com.school.asvvm.data.model.ExamTerm.FIRST_HALF) }
    val marks by viewModel.marks.collectAsState()
    val subjectConfigs by viewModel.subjectConfigs.collectAsState()
    
    // Map of Subject -> Pair(Written, Oral)
    val scores = remember { androidx.compose.runtime.mutableStateMapOf<String, Pair<String, String>>() }

    // Pre-populate marks when student or term changes
    LaunchedEffect(selectedStudent, selectedTerm, marks) {
        if (selectedStudent != null) {
            val existing = marks.filter { it.studentId == selectedStudent?.id && it.term == selectedTerm.name }
            scores.clear()
            existing.forEach { m ->
                scores[m.subject] = Pair(m.writtenMarks, m.oralMarks)
            }
        }
    }

    val isLocked = remember(selectedStudent, selectedTerm) {
        selectedStudent?.lockedTerms?.contains(selectedTerm.name) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // Expressive Student Selection Tabs
        ScrollableTabRow(
            selectedTabIndex = students.indexOf(selectedStudent).coerceAtLeast(0),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            divider = {}
        ) {
            students.forEach { student ->
                val isSelected = selectedStudent == student
                Tab(
                    selected = isSelected,
                    onClick = { selectedStudentId = student.id },
                    text = { 
                        Surface(
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    student.name, 
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                ) 
                                if (student.lockedTerms.isNotEmpty()) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                )
            }
        }

        if (selectedStudent != null) {
            val termConfigs = subjectConfigs.filter { it.term == selectedTerm.name }
            
            // Calculate live totals
            val currentTotalObtained = termConfigs.sumOf { config ->
                val score = scores[config.subjectName] ?: Pair("0", "0")
                (score.first.toIntOrNull() ?: 0) + (score.second.toIntOrNull() ?: 0)
            }
            val currentTotalMax = termConfigs.sumOf { it.maxWritten + (if (it.hasOral) it.maxOral else 0) }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    // Student & Term Header Card with Live Score Preview
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Grading Sheet", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        selectedStudent.name, 
                                        style = MaterialTheme.typography.headlineMedium, 
                                        fontWeight = FontWeight.ExtraBold, 
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "TOTAL SCORE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            "$currentTotalObtained / $currentTotalMax",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            if (isLocked) {
                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "MARKS LOCKED: This term is locked for the student. Contact Admin to modify.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text("Term Selection:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))

                            // One-Tap Term Pill Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                com.school.asvvm.data.model.ExamTerm.entries.forEach { term ->
                                    val isTermSelected = selectedTerm == term
                                    val isTermLocked = selectedStudent.lockedTerms.contains(term.name)
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(50))
                                            .clickable { selectedTerm = term },
                                        shape = RoundedCornerShape(50),
                                        color = if (isTermSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                term.title.replace(" Examination", "").replace(" Term", ""),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isTermSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = if (isTermSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (isTermLocked) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (termConfigs.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.MenuBook,
                            title = "No Subjects Configured",
                            subtitle = "No subjects have been configured for ${selectedTerm.title} yet."
                        )
                    }
                }

                items(termConfigs, key = { it.id }) { config ->
                    val subjectColor = when {
                        config.subjectName.contains("Math", true) -> SubjectMath
                        config.subjectName.contains("Sci", true) -> SubjectScience
                        config.subjectName.contains("Eng", true) -> SubjectEnglish
                        config.subjectName.contains("Art", true) -> SubjectArt
                        else -> MaterialTheme.colorScheme.primary
                    }
                    
                    ModernCard(containerColor = subjectColor.copy(alpha = 0.04f)) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBox(
                                    icon = if (config.hasOral) Icons.Default.RecordVoiceOver else Icons.Default.EditNote,
                                    color = subjectColor
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    config.subjectName, 
                                    style = MaterialTheme.typography.titleMedium, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = subjectColor
                                )
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            val currentScore = scores[config.subjectName] ?: Pair("", "")
                            
                            if (!config.hasOral) {
                                PremiumTextField(
                                    value = currentScore.first,
                                    enabled = !isLocked,
                                    onValueChange = { 
                                        val intVal = it.toIntOrNull()
                                        if (it.isEmpty() || (intVal != null && intVal <= config.maxWritten)) {
                                            scores[config.subjectName] = Pair(it, "0")
                                        }
                                    },
                                    label = "Written Marks (Max ${config.maxWritten})",
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = subjectColor.copy(alpha = 0.6f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumTextField(
                                            value = currentScore.first,
                                            enabled = !isLocked,
                                            onValueChange = { 
                                                val intVal = it.toIntOrNull()
                                                if (it.isEmpty() || (intVal != null && intVal <= config.maxWritten)) {
                                                    scores[config.subjectName] = Pair(it, currentScore.second)
                                                }
                                            },
                                            label = "Written (Max ${config.maxWritten})",
                                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = subjectColor.copy(alpha = 0.6f)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumTextField(
                                            value = currentScore.second,
                                            enabled = !isLocked,
                                            onValueChange = { 
                                                val intVal = it.toIntOrNull()
                                                if (it.isEmpty() || (intVal != null && intVal <= config.maxOral)) {
                                                    scores[config.subjectName] = Pair(currentScore.first, it)
                                                }
                                            },
                                            label = "Oral (Max ${config.maxOral})",
                                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = subjectColor.copy(alpha = 0.6f)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    if (!isLocked) {
                        Text(
                            "⚠️ Warning: After submission, marks for this term will be locked and cannot be changed without Admin permission.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        ModernButton(
                            onClick = {
                                if (selectedStudent != null) {
                                    val marksList = scores.map { (subject, scorePair) ->
                                        Mark(
                                            id = java.util.UUID.randomUUID().toString(),
                                            studentId = selectedStudent!!.id,
                                            subject = subject,
                                            term = selectedTerm.name,
                                            writtenMarks = scorePair.first,
                                            oralMarks = scorePair.second,
                                            className = className
                                        )
                                    }
                                    viewModel.submitMarks(marksList)
                                }
                            },
                            text = "SUBMIT GRADES"
                        )
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("TERM LOCKED")
                        }
                    }
                }
            }
        } else {
            if (students.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.GroupOff,
                    title = "No Students to Grade",
                    subtitle = "There are no students available in this class."
                )
            } else {
                EmptyStateView(
                    icon = Icons.Default.EditNote,
                    title = "Select a Student",
                    subtitle = "Please select a student from the tabs above to begin grading."
                )
            }
        }
    }
}


@Composable
fun TeacherReportsView(students: List<Student>, marks: List<Mark>, subjectConfigs: List<com.school.asvvm.data.model.SubjectConfig>) {
    val context = LocalContext.current
    if (students.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Analytics,
            title = "No Reports Available",
            subtitle = "There are no students or data to generate reports."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(students) { student ->
            val studentMarks = marks.filter { it.studentId == student.id }
            val marksByTerm = studentMarks.groupBy { it.term }
            
            ModernCard {
                Column(modifier = Modifier.padding(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColoredIconBox(icon = Icons.Default.Analytics, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(student.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (marksByTerm.isNotEmpty() && subjectConfigs.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        
                        var yearlyTotal = 0
                        var yearlyMaxMarks = 0
                        
                        // All unique subjects Configured
                        val uniqueSubjects = subjectConfigs.map { it.subjectName }.distinct()
                        
                        uniqueSubjects.forEach { subject ->
                            val sMarks = studentMarks.filter { it.subject == subject }
                            
                            val t1 = sMarks.find { it.term == com.school.asvvm.data.model.ExamTerm.FIRST_HALF.name }?.let { (it.writtenMarks.toIntOrNull() ?: 0) + (it.oralMarks.toIntOrNull() ?: 0) } ?: 0
                            val t2 = sMarks.find { it.term == com.school.asvvm.data.model.ExamTerm.SECOND_HALF.name }?.let { (it.writtenMarks.toIntOrNull() ?: 0) + (it.oralMarks.toIntOrNull() ?: 0) } ?: 0
                            val ta = sMarks.find { it.term == com.school.asvvm.data.model.ExamTerm.ANNUAL.name }?.let { (it.writtenMarks.toIntOrNull() ?: 0) + (it.oralMarks.toIntOrNull() ?: 0) } ?: 0
                            
                            yearlyTotal += (t1 + t2 + ta)
                        }
                        
                        subjectConfigs.forEach {
                            yearlyMaxMarks += it.maxWritten + (if(it.hasOral) it.maxOral else 0)
                        }
                        
                        val yearlyPercentage = if(yearlyMaxMarks > 0) (yearlyTotal.toFloat() / yearlyMaxMarks) * 100 else 0f
                        val yearlyGrade = when {
                            yearlyPercentage >= 80 -> "A+"
                            yearlyPercentage >= 60 -> "A"
                            yearlyPercentage >= 40 -> "B"
                            else -> "C"
                        }

                        ModernButton(
                            onClick = {
                                PdfGenerator.generateAndPrint(
                                    context, student, studentMarks, subjectConfigs,
                                    "$yearlyTotal / $yearlyMaxMarks", 
                                    String.format("%.1f%%", yearlyPercentage), 
                                    yearlyGrade
                                )
                            },
                            text = "GENERATE ANNUAL REPORT",
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                        
                        Spacer(Modifier.height(16.dp))
                    } else if (marksByTerm.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No marks entered yet for this student.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    marksByTerm.forEach { (termStr, termMarks) ->
                        val termEnum = try { com.school.asvvm.data.model.ExamTerm.valueOf(termStr) } catch(e:Exception) { com.school.asvvm.data.model.ExamTerm.FIRST_HALF }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(termEnum.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                
                                var termTotal = 0
                                termMarks.forEach { m ->
                                    val w = m.writtenMarks.toIntOrNull() ?: 0
                                    val o = m.oralMarks.toIntOrNull() ?: 0
                                    termTotal += (w + o)
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(m.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(if (o > 0) "$w + $o" else "$w", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                
                                val termSpecificConfigs = subjectConfigs.filter { it.term == termEnum.name }
                                var maxMarks = 0
                                termSpecificConfigs.forEach { subject ->
                                    maxMarks += subject.maxWritten + (if(subject.hasOral) subject.maxOral else 0)
                                }
                                
                                val percentage = if(maxMarks > 0) (termTotal.toFloat() / maxMarks) * 100 else 0f
                                val grade = when {
                                    percentage >= 80 -> "A+"
                                    percentage >= 60 -> "A"
                                    percentage >= 40 -> "B"
                                    else -> "C"
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Column {
                                        Text("Term Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$termTotal / $maxMarks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            "Grade: $grade", 
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium, 
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ChangePasswordDialog(username: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account Settings") },
        text = {
            Column {
                Text("Update password for $username", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                PremiumTextField(value = oldPass, onValueChange = { oldPass = it }, label = "Current Password")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = newPass, onValueChange = { newPass = it }, label = "New Password")
            }
        },
        confirmButton = {
            Button(onClick = { if (oldPass.isNotBlank() && newPass.isNotBlank()) onConfirm(oldPass, newPass) }) { Text("UPDATE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}


