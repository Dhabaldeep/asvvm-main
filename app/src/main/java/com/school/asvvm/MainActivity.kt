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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        launch {
            updateInfo = com.school.asvvm.util.UpdateManager.checkForUpdate(com.school.asvvm.BuildConfig.VERSION_NAME)
        }
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
                startDestination = "admin_dashboard"
            } else {
                startDestination = "teacher_dashboard/$email"
            }
        } else {
            startDestination = "login"
        }
    }

    if (startDestination == "splash") {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Authenticating", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        NavHost(
            navController = navController, 
            startDestination = startDestination,
            enterTransition = { androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }) },
            exitTransition = { androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -50 }) },
            popEnterTransition = { androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -50 }) },
            popExitTransition = { androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { 50 }) }
        ) {
            composable("login") {
                val viewModel: AuthViewModel = hiltViewModel()
                LoginScreen(
                    onLoginSuccess = { role, u ->
                    if (role == "Admin") {
                        navController.navigate("admin_dashboard") {
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
                    }
                )
            }
            composable("teacher_dashboard/{username}") { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: ""
                val viewModel: TeacherViewModel = hiltViewModel()
                val authViewModel: AuthViewModel = hiltViewModel()
                TeacherDashboard(
                    teacherName = username, 
                    viewModel = viewModel,
                    onLogout = {
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    updateInfo?.let { info ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Update Available") },
            text = { Text("A new version (${info.version}) is available!\n\nRelease notes:\n${info.releaseNotes}") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    com.school.asvvm.util.UpdateManager.startDownload(context, info)
                    updateInfo = null
                }) {
                    Text("Download & Install")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { updateInfo = null }) {
                    Text("Later")
                }
            }
        )
    }
}
