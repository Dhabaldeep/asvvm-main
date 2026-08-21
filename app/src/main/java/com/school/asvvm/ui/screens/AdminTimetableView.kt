package com.school.asvvm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.asvvm.data.model.TimetablePeriod
import com.school.asvvm.ui.components.ModernCard
import com.school.asvvm.ui.components.ModernButton
import com.school.asvvm.ui.components.PremiumTextField
import com.school.asvvm.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTimetableView(
    viewModel: AdminViewModel,
    className: String
) {
    val timetable by viewModel.timetable.collectAsState()
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    var selectedDay by remember { mutableStateOf(daysOfWeek[0]) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Day Selector
        ScrollableTabRow(
            selectedTabIndex = daysOfWeek.indexOf(selectedDay),
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            divider = {}
        ) {
            daysOfWeek.forEach { day ->
                Tab(
                    selected = selectedDay == day,
                    onClick = { selectedDay = day },
                    text = { 
                        Text(
                            day, 
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    }
                )
            }
        }

        val periodsForDay = timetable.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime }

        if (periodsForDay.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No periods scheduled for $selectedDay.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(periodsForDay) { period ->
                    ModernCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(period.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Teacher: ${period.teacherName}", style = MaterialTheme.typography.bodyMedium)
                                Text("Time: ${period.startTime} - ${period.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { viewModel.deleteTimetablePeriod(period.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimetablePeriodDialog(
    className: String,
    viewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var teacherName by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf("Monday") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    
    val allTeachers by viewModel.teachers.collectAsState()
    val allSubjects by viewModel.allSubjectConfigs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Timetable Period for $className") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Day Selection
                Text("Day of Week", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    daysOfWeek.take(3).forEach { day ->
                        FilterChip(selected = dayOfWeek == day, onClick = { dayOfWeek = day }, label = { Text(day) })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    daysOfWeek.drop(3).forEach { day ->
                        FilterChip(selected = dayOfWeek == day, onClick = { dayOfWeek = day }, label = { Text(day) })
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumTextField(value = startTime, onValueChange = { startTime = it }, label = "Start (e.g. 10:00 AM)", modifier = Modifier.weight(1f))
                    PremiumTextField(value = endTime, onValueChange = { endTime = it }, label = "End (e.g. 10:45 AM)", modifier = Modifier.weight(1f))
                }

                // Simplified subject and teacher selection for now using TextFields (could be dropdowns)
                PremiumTextField(value = subject, onValueChange = { subject = it }, label = "Subject")
                PremiumTextField(value = teacherName, onValueChange = { teacherName = it }, label = "Teacher Name")
            }
        },
        confirmButton = {
            ModernButton(
                onClick = {
                    if (startTime.isNotBlank() && endTime.isNotBlank() && subject.isNotBlank() && teacherName.isNotBlank()) {
                        viewModel.saveTimetablePeriod(className, dayOfWeek, startTime, endTime, subject, teacherName)
                        onDismiss()
                    }
                },
                text = "Save Period"
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
