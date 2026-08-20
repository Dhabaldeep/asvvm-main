@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
package com.school.asvvm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.school.asvvm.data.model.SchoolClass
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.Teacher
import com.school.asvvm.ui.components.*
import com.school.asvvm.ui.theme.*
import com.school.asvvm.ui.viewmodel.AdminViewModel
import com.school.asvvm.ui.viewmodel.SubjectTermConfig

@Composable
fun AdminDashboard(
    viewModel: AdminViewModel,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedClass by remember { mutableStateOf(SchoolClass.NURSERY.value) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddTeacherDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var selectedTeacherForAssign by remember { mutableStateOf<Teacher?>(null) }
    var selectedStudentForEdit by remember { mutableStateOf<Student?>(null) }
    var selectedTeacherForEdit by remember { mutableStateOf<Teacher?>(null) }
    var selectedStudentForDetails by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }
    
    val students by viewModel.students.collectAsState()
    val teachers by viewModel.teachers.collectAsState()
    val subjectConfigs by viewModel.subjectConfigs.collectAsState()
    val allExistingSubjects by viewModel.allSubjectConfigs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.refreshData() }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SchoolTopBar(
                title = "ASVVM Admin",
                subtitle = "Management Console",
                onLogout = onLogout
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Student") }
                )
            } else if (selectedTabIndex == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddTeacherDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Add Teacher") }
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAddSubjectDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = null) },
                    text = { Text("Add Subject") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Students", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Teachers", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Subjects", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0 || selectedTabIndex == 2) {
                // Secondary Class Tabs
                ScrollableTabRow(
                    selectedTabIndex = SchoolClass.values().indexOfFirst { it.value == selectedClass },
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    divider = {}
                ) {
                    SchoolClass.values().forEach { cls ->
                        Tab(
                            selected = selectedClass == cls.value,
                            onClick = { 
                                selectedClass = cls.value
                                viewModel.selectClass(cls.value)
                            },
                            text = { 
                                Text(
                                    cls.value, 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedClass == cls.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Dashboard Analytics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ModernCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Students", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${students.size}", 
                                    style = MaterialTheme.typography.headlineMedium, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        ModernCard(modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Teachers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${teachers.size}", 
                                    style = MaterialTheme.typography.headlineMedium, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (selectedTabIndex == 0 && students.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Bulk Actions", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.secondary, 
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.bulkUnlockStudents(students) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Unlock All", style = MaterialTheme.typography.labelSmall)
                                }
                                Button(
                                    onClick = { viewModel.bulkLockStudents(students) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Lock All", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(6) { ShimmerStudentRow() }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (selectedTabIndex == 0) {
                                if (students.isEmpty()) {
                                    item {
                                        EmptyStateView(
                                            icon = Icons.Default.GroupOff,
                                            title = "No Students Found",
                                            subtitle = "There are no students enrolled in $selectedClass yet."
                                        )
                                    }
                                } else {
                                    items(students, key = { it.id }) { student ->
                                        @OptIn(ExperimentalFoundationApi::class)
                                        StudentRow(
                                            modifier = Modifier.animateItemPlacement(),
                                            student = student, 
                                            onClick = { selectedStudentForDetails = student },
                                            onEdit = { selectedStudentForEdit = student },
                                            onDelete = { studentToDelete = student },
                                            onUnlock = { viewModel.unlockStudent(student.id) }
                                        )
                                    }
                                }
                            } else if (selectedTabIndex == 1) {
                                if (teachers.isEmpty()) {
                                    item {
                                        EmptyStateView(
                                            icon = Icons.Default.PersonOff,
                                            title = "No Faculty Found",
                                            subtitle = "There are currently no registered teachers."
                                        )
                                    }
                                } else {
                                    items(teachers, key = { it.email }) { teacher ->
                                        @OptIn(ExperimentalFoundationApi::class)
                                        TeacherRow(
                                            modifier = Modifier.animateItemPlacement(),
                                            teacher = teacher,
                                            onEdit = { selectedTeacherForEdit = teacher },
                                            onAssignClass = { selectedTeacherForAssign = teacher },
                                            onDelete = { teacherToDelete = teacher }
                                        )
                                    }
                                }
                            } else {
                                if (subjectConfigs.isEmpty()) {
                                    item {
                                        EmptyStateView(
                                            icon = Icons.Default.LibraryBooks,
                                            title = "No Subjects Found",
                                            subtitle = "No subject grading criteria configured for $selectedClass."
                                        )
                                    }
                                } else {
                                    val groupedSubjects = subjectConfigs.groupBy { it.subjectName }
                                    items(groupedSubjects.keys.toList()) { subjectName ->
                                        val configs = groupedSubjects[subjectName] ?: emptyList()
                                        UnifiedSubjectRow(
                                            subjectName = subjectName,
                                            configs = configs,
                                            onDelete = { viewModel.deleteSubjectGroup(configs) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                @OptIn(ExperimentalMaterialApi::class)
                PullRefreshIndicator(
                    refreshing = isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showAddDialog) {
        AddStudentDialog(
            className = selectedClass,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, roll, guardian ->
                viewModel.addStudent(name, roll, selectedClass, guardian)
                showAddDialog = false
            }
        )
    }

    if (showAddTeacherDialog) {
        AddTeacherDialog(
            onDismiss = { showAddTeacherDialog = false },
            onConfirm = { name, email, phone, gender ->
                viewModel.createTeacher(name, email, phone, gender)
                showAddTeacherDialog = false
            }
        )
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            className = selectedClass,
            allExistingSubjects = allExistingSubjects,
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { classNames, subjectName, configs ->
                viewModel.addSubject(classNames, subjectName, configs)
                showAddSubjectDialog = false
            }
        )
    }

    selectedTeacherForAssign?.let { teacher ->
        AssignClassDialog(
            teacher = teacher,
            onDismiss = { selectedTeacherForAssign = null },
            onConfirm = { className ->
                viewModel.assignClassToTeacher(teacher.email, className)
                selectedTeacherForAssign = null
            },
            onRemove = { className ->
                viewModel.removeAssignedClass(teacher.email, className)
                selectedTeacherForAssign = null
            }
        )
    }

    selectedStudentForEdit?.let { student ->
        EditStudentDialog(
            student = student,
            onDismiss = { selectedStudentForEdit = null },
            onConfirm = { name, roll, guardian ->
                viewModel.updateStudent(student.id, name, roll, guardian)
                selectedStudentForEdit = null
            }
        )
    }

    selectedTeacherForEdit?.let { teacher ->
        EditTeacherDialog(
            teacher = teacher,
            onDismiss = { selectedTeacherForEdit = null },
            onConfirm = { name, phone ->
                viewModel.updateTeacher(teacher.email, name, phone)
                selectedTeacherForEdit = null
            }
        )
    }

    selectedStudentForDetails?.let { student ->
        val marksFlow = remember(student.id) { viewModel.getMarksForStudent(student.id) }
        StudentDetailsDialog(
            student = student,
            marksFlow = marksFlow,
            onDismiss = { selectedStudentForDetails = null }
        )
    }

    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to permanently remove ${student.name} from the system? This action cannot be undone and will also delete their grading history.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.deleteStudent(student.id)
                        studentToDelete = null 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) { Text("CANCEL") }
            }
        )
    }

    teacherToDelete?.let { teacher ->
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text("Delete Teacher") },
            text = { Text("Are you sure you want to permanently remove ${teacher.name} from the system? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.deleteTeacher(teacher.email)
                        teacherToDelete = null 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) { Text("CANCEL") }
            }
        )
    }
}
