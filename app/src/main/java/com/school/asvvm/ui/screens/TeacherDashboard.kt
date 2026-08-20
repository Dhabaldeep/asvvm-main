@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.school.asvvm.data.model.Mark
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.getSubjectsForClass
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import com.school.asvvm.ui.components.*
import com.school.asvvm.ui.theme.*
import com.school.asvvm.ui.viewmodel.TeacherViewModel
import com.school.asvvm.util.PdfGenerator

@Composable
fun TeacherDashboard(
    teacherName: String,
    viewModel: TeacherViewModel,
    onLogout: () -> Unit,
    onCheckUpdate: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    
    val teacherProfile by viewModel.teacherProfile.collectAsState()
    val assignedClass by viewModel.assignedClass.collectAsState()
    val students by viewModel.students.collectAsState()
    val marks by viewModel.marks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(teacherName) {
        viewModel.initialize(teacherName)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { viewModel.initialize(teacherProfile?.email ?: teacherName) }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SchoolTopBar(
                title = "Faculty Dashboard",
                subtitle = "${teacherProfile?.name ?: teacherName}",
                onLogout = onLogout,
                actions = {
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Password") },
                                onClick = {
                                    showSettingsMenu = false
                                    showChangePassword = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Check for Updates") },
                                onClick = {
                                    showSettingsMenu = false
                                    onCheckUpdate()
                                }
                            )
                        }
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
                    icon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    label = { Text("Students") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.LibraryAdd, contentDescription = null) },
                    label = { Text("Grading") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pullRefresh(pullRefreshState)
        ) {
            if (isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(6) { ShimmerStudentRow() }
                }
            } else if (teacherProfile == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Profile Not Found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Unable to load your profile. This might be a network issue or your account hasn't been configured by the admin yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(32.dp))
                    ModernButton(onClick = { viewModel.initialize(teacherName) }, text = "RETRY")
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("SIGN OUT")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    val profile = teacherProfile
                    if (profile != null) {
                        if (selectedTab != 3 && profile.assignedClasses.isNotEmpty()) {
                            TeacherClassSelector(
                                classes = profile.assignedClasses,
                                selectedClass = assignedClass,
                                onClassSelected = { viewModel.setAssignedClass(it) }
                            )
                        }
                        val subjectConfigs by viewModel.subjectConfigs.collectAsState()
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> TeacherStudentListView(students)
                                1 -> TeacherGradingView(students, assignedClass ?: "", viewModel)
                                2 -> TeacherReportsView(students, marks, subjectConfigs)
                                3 -> TeacherProfileView(profile)
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            username = teacherName,
            onDismiss = { showChangePassword = false },
            onConfirm = { old, new ->
                viewModel.changePassword(teacherName, old, new)
                showChangePassword = false
            }
        )
    }
}






