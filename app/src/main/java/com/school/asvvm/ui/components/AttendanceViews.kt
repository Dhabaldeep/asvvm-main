package com.school.asvvm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.asvvm.data.model.Attendance
import com.school.asvvm.data.model.Student
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAttendanceView(
    students: List<Student>,
    records: List<Attendance>,
    className: String,
    teacherId: String,
    onSubmit: (List<Attendance>) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())
    
    // Map of StudentId to Status ("Present", "Absent", "Late")
    val attendanceState = remember(students, records) {
        val state = mutableMapOf<String, String>()
        students.forEach { s -> state[s.id] = "Present" }
        records.forEach { r -> state[r.studentId] = r.status }
        mutableStateMapOf(*state.toList().toTypedArray())
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Attendance for $today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = {
                val newRecords = students.map { s ->
                    Attendance(
                        studentId = s.id,
                        className = className,
                        date = today,
                        status = attendanceState[s.id] ?: "Present",
                        teacherId = teacherId
                    )
                }
                onSubmit(newRecords)
            }) {
                Text("Save")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No students in this class.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(students) { student ->
                    val status = attendanceState[student.id] ?: "Present"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold)
                                Text("Roll: ${student.rollNo}", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = status == "Present",
                                    onClick = { attendanceState[student.id] = "Present" },
                                    label = { Text("P") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF4CAF50), selectedLabelColor = Color.White)
                                )
                                FilterChip(
                                    selected = status == "Absent",
                                    onClick = { attendanceState[student.id] = "Absent" },
                                    label = { Text("A") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.error, selectedLabelColor = MaterialTheme.colorScheme.onError)
                                )
                                FilterChip(
                                    selected = status == "Late",
                                    onClick = { attendanceState[student.id] = "Late" },
                                    label = { Text("L") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFF9800), selectedLabelColor = Color.White)
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
fun AdminAttendanceView(
    students: List<Student>,
    records: List<Attendance>,
    className: String,
    date: String
) {
    val attendanceState = remember(students, records) {
        val state = mutableMapOf<String, String>()
        students.forEach { s -> state[s.id] = "Present" }
        records.forEach { r -> state[r.studentId] = r.status }
        state
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Attendance for $className on $date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        if (students.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No students in this class.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(students) { student ->
                    val status = attendanceState[student.id] ?: "Present"
                    val statusColor = when (status) {
                        "Present" -> Color(0xFF4CAF50)
                        "Absent" -> MaterialTheme.colorScheme.error
                        "Late" -> Color(0xFFFF9800)
                        else -> Color.Gray
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(student.name, fontWeight = FontWeight.Bold)
                                Text("Roll: ${student.rollNo}", style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = statusColor.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    status,
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
