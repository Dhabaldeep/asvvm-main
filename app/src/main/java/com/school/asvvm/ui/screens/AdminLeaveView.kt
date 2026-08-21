@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.school.asvvm.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.asvvm.ui.components.ModernCard
import com.school.asvvm.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminLeaveView(viewModel: AdminViewModel) {
    val leaveRequests by viewModel.leaveRequests.collectAsState()

    if (leaveRequests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No leave requests found.")
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(request.teacherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            val statusColor = when (request.status) {
                                "Approved" -> MaterialTheme.colorScheme.primary
                                "Rejected" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            SuggestionChip(
                                onClick = {},
                                label = { Text(request.status, color = statusColor, fontWeight = FontWeight.Bold) },
                                border = SuggestionChipDefaults.suggestionChipBorder(borderColor = statusColor)
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        Text("Type: ${request.type}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Dates: ${request.startDate} to ${request.endDate}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text("Reason: ${request.reason}", style = MaterialTheme.typography.bodySmall)
                        
                        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        Text("Applied on: ${sdf.format(Date(request.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)

                        if (request.status == "Pending") {
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { viewModel.updateLeaveStatus(request.id, "Rejected") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reject")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.updateLeaveStatus(request.id, "Approved") }
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Approve")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
