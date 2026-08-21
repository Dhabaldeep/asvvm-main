package com.school.asvvm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.school.asvvm.data.repository.SchoolRepository
import com.school.asvvm.ui.screens.AdminDashboard
import com.school.asvvm.ui.screens.LoginScreen
import com.school.asvvm.ui.screens.TeacherDashboard
import com.school.asvvm.ui.theme.ASVVMSchoolTheme
import com.school.asvvm.ui.viewmodel.AdminViewModel
import com.school.asvvm.ui.viewmodel.AuthViewModel
import com.school.asvvm.ui.viewmodel.TeacherViewModel

import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start Background Worker for Notifications
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.school.asvvm.util.NoticeWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NoticeWorker",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        // Request Notification Permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            ASVVMSchoolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf("splash") }

    var updateInfo by remember { mutableStateOf<com.school.asvvm.util.UpdateInfo?>(null) }
    var downloadReadyUri by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action == "com.school.asvvm.ACTION_DOWNLOAD_READY") {
                    downloadReadyUri = intent.getStringExtra("downloadUri")
                }
            }
        }
        context.registerReceiver(
            receiver, 
            IntentFilter("com.school.asvvm.ACTION_DOWNLOAD_READY"),
            Context.RECEIVER_EXPORTED
        )
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val onCheckUpdate: () -> Unit = {
        coroutineScope.launch {
            val info = com.school.asvvm.util.UpdateManager.checkForUpdate(com.school.asvvm.BuildConfig.VERSION_NAME)
            if (info != null) {
                updateInfo = info
            } else {
                android.widget.Toast.makeText(context, "You are on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            updateInfo = com.school.asvvm.util.UpdateManager.checkForUpdate(com.school.asvvm.BuildConfig.VERSION_NAME)
        }
        
        // Wait for navController to initialize its graph
        kotlinx.coroutines.delay(100) 
        
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == null || currentRoute == "splash") {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null && user.email != null) {
                val email = user.email!!.lowercase().trim()
                val role = if (email == "admin@school.com") {
                    "Admin"
                } else {
                    try {
                        val doc = withTimeoutOrNull(5000L) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("Staff")
                                .document(email)
                                .get()
                                .await()
                        }
                        doc?.getString("role") ?: "Teacher"
                    } catch (e: Exception) { 
                        "Teacher" 
                    }
                }
                
                if (role == "Admin") {
                    navController.navigate("admin_dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("teacher_dashboard/$email") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            } else {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController, 
        startDestination = "splash",
        enterTransition = { androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }) },
        exitTransition = { androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -50 }) },
        popEnterTransition = { androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -50 }) },
        popExitTransition = { androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 }) }
    ) {
        composable("splash") {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Authenticating", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
            composable("login") {
                val viewModel: AuthViewModel = hiltViewModel()
                LoginScreen(
                    onLoginSuccess = { role, u ->
                        if (role == "Admin") {
                            navController.navigate("admin_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else if (role == "Student") {
                            navController.navigate("student_dashboard/$u") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("teacher_dashboard/$u") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }, viewModel = viewModel)
            }
            composable("admin_dashboard") {
                val viewModel: AdminViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                AdminDashboard(
                    viewModel = viewModel,
                    onLogout = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onCheckUpdate = onCheckUpdate
                )
            }
            composable("teacher_dashboard/{username}") { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: "Teacher"
                val viewModel: TeacherViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel(backStackEntry)
                
                TeacherDashboard(
                    teacherName = username,
                    viewModel = viewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onCheckUpdate = onCheckUpdate
                )
            }
            
            composable("student_dashboard/{username}") { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: "Student"
                val viewModel: com.school.asvvm.ui.viewmodel.StudentViewModel = hiltViewModel()
                
                // We fetch the full student info using the ID (username here is the ID)
                // For simplicity, we create a stub student to pass in. The ViewModel should really fetch it by ID.
                val student = com.school.asvvm.data.model.Student(id = username, name = "Student")
                
                com.school.asvvm.ui.screens.StudentDashboard(
                    student = student,
                    viewModel = viewModel,
                    onLogout = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

    updateInfo?.let { info ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { updateInfo = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            title = { Text("Update Available", fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("A new version ", style = MaterialTheme.typography.bodyMedium)
                    Text("v${info.version}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                    Text(" is available!", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Release notes:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(info.releaseNotes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.school.asvvm.util.UpdateManager.startDownload(context, info)
                        updateInfo = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Download Update")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { updateInfo = null }) {
                    Text("Later")
                }
            }
        )
    }

    downloadReadyUri?.let { uriString ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { downloadReadyUri = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            title = { Text("Download Complete", fontWeight = FontWeight.Bold) },
            text = { Text("The update has been successfully downloaded. Would you like to install it now?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse(uriString), "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        try {
                            context.startActivity(installIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        downloadReadyUri = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Install Now")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { downloadReadyUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
