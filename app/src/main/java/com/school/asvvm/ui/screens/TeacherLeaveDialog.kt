@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.school.asvvm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.school.asvvm.ui.components.ModernButton
import com.school.asvvm.ui.components.ModernCard
import com.school.asvvm.ui.components.PremiumTextField
import com.school.asvvm.ui.viewmodel.TeacherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TeacherLeaveDialog(
    viewModel: TeacherViewModel,
    onDismiss: () -> Unit
) {
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Leave Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("My Leaves") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Apply") })
                }
                
                if (selectedTab == 0) {
                    if (leaveRequests.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("You haven't submitted any leave requests.")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(leaveRequests) { request ->
                                ModernCard {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(request.type, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                request.status, 
                                                color = when (request.status) {
                                                    "Approved" -> MaterialTheme.colorScheme.primary
                                                    "Rejected" -> MaterialTheme.colorScheme.error
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("${request.startDate} to ${request.endDate}")
                                        Text("Reason: ${request.reason}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    ApplyLeaveForm(viewModel) {
                        selectedTab = 0
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyLeaveForm(viewModel: TeacherViewModel, onSuccess: () -> Unit) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Casual Leave") }
    
    val leaveTypes = listOf("Casual Leave", "Sick Leave", "Unpaid Leave", "Other")

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Leave Type", style = MaterialTheme.typography.labelSmall)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            leaveTypes.take(2).forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            leaveTypes.drop(2).forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PremiumTextField(value = startDate, onValueChange = { startDate = it }, label = "Start Date (e.g. 10/Aug)", modifier = Modifier.weight(1f))
            PremiumTextField(value = endDate, onValueChange = { endDate = it }, label = "End Date (e.g. 12/Aug)", modifier = Modifier.weight(1f))
        }
        
        PremiumTextField(
            value = reason, 
            onValueChange = { reason = it }, 
            label = "Reason for leave", 
            modifier = Modifier.fillMaxWidth().height(100.dp)
        )
        
        Spacer(Modifier.weight(1f))
        
        ModernButton(
            onClick = {
                if (startDate.isNotBlank() && endDate.isNotBlank() && reason.isNotBlank()) {
                    viewModel.submitLeaveRequest(startDate, endDate, reason, type)
                    onSuccess()
                }
            },
            text = "SUBMIT LEAVE REQUEST",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
