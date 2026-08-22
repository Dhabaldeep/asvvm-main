@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.school.asvvm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.school.asvvm.data.model.SchoolClass
import com.school.asvvm.data.model.Teacher
import com.school.asvvm.ui.viewmodel.SubjectTermConfig

@Composable
fun AddStudentDialog(className: String, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var guardian by remember { mutableStateOf("") }

    AlertDialog(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        titleContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { Text("New Student enrolment - $className") },
        text = {
            Column {
                PremiumTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = roll, onValueChange = { roll = it }, label = "Roll Number")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = guardian, onValueChange = { guardian = it }, label = "Guardian Name")
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, roll, guardian) }) { Text("ENROL") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AddTeacherDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var genderExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        titleContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { Text("Add New Faculty") },
        text = {
            Column {
                PremiumTextField(value = name, onValueChange = { name = it }, label = "Teacher Name")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = email, onValueChange = { email = it }, label = "Teacher Email")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")
                Spacer(Modifier.height(16.dp))
                
                Text("Select Gender:", style = MaterialTheme.typography.labelSmall)
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = { genderExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(gender)
                    }
                    DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    gender = g
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank() && email.isNotBlank()) onConfirm(name, email, phone, gender) }) { Text("CREATE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AssignClassDialog(teacher: Teacher, onDismiss: () -> Unit, onConfirm: (String) -> Unit, onRemove: (String) -> Unit = {}) {
    var selectedClass by remember { mutableStateOf(SchoolClass.NURSERY.value) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        titleContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { Text("Assign Class") },
        text = {
            Column {
                Text("Assigning to: ${teacher.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                val safeClasses = teacher.assignedClasses ?: emptyList()
                if (safeClasses.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Currently assigned:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            safeClasses.forEach { cls ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cls, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    IconButton(
                                        onClick = { onRemove(cls) }, 
                                        modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Select Class Assignment:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedClass)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        val availableClasses = SchoolClass.values().map { it.value }.filter { !safeClasses.contains(it) }
                        if (availableClasses.isEmpty()) {
                            DropdownMenuItem(text = { Text("All classes assigned") }, onClick = { expanded = false })
                        } else {
                            availableClasses.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls) },
                                    onClick = {
                                        selectedClass = cls
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val safeClassesBtn = teacher.assignedClasses ?: emptyList()
                if (!safeClassesBtn.contains(selectedClass)) onConfirm(selectedClass) 
            }) { Text("ASSIGN") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AddSubjectDialog(
    className: String, 
    allExistingSubjects: List<com.school.asvvm.data.model.SubjectConfig>,
    onDismiss: () -> Unit, 
    onConfirm: (List<String>, String, List<SubjectTermConfig>) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var selectedClasses by remember { mutableStateOf(setOf(className)) }
    
    // Identify classes that already have this subject name
    val alreadyUsedClasses = remember(subjectName, allExistingSubjects) {
        if (subjectName.isBlank()) emptySet<String>()
        else {
            allExistingSubjects
                .filter { it.subjectName.trim().lowercase() == subjectName.trim().lowercase() }
                .map { it.className }
                .toSet()
        }
    }
    
    // Auto-remove any newly disabled classes from selection
    LaunchedEffect(alreadyUsedClasses) {
        val filtered = selectedClasses.filter { !alreadyUsedClasses.contains(it) }.toSet()
        if (filtered != selectedClasses) {
            selectedClasses = filtered
        }
    }
    
    val terms = com.school.asvvm.data.model.ExamTerm.values()
    // Data class for local state
    data class LocalTermState(val written: String, val hasOral: Boolean, val oral: String)
    
    val termStates = remember {
        terms.map { term ->
            mutableStateOf(
                LocalTermState(
                    written = if (term == com.school.asvvm.data.model.ExamTerm.ANNUAL) "90" else "40",
                    hasOral = true,
                    oral = "10"
                )
            )
        }
    }

    AlertDialog(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        titleContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text("Configure New Subject", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Bulk creation across multiple classes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                PremiumTextField(
                    value = subjectName, 
                    onValueChange = { subjectName = it }, 
                    label = "Subject Name (e.g. Mathematics)",
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )
                
                Spacer(Modifier.height(16.dp))
                Text("Target Classes:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                // Multi-select classes chips
                val classes = SchoolClass.values()
                classes.toList().chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { cls ->
                            val isAlreadyUsed = alreadyUsedClasses.contains(cls.value)
                            FilterChip(
                                selected = selectedClasses.contains(cls.value),
                                enabled = !isAlreadyUsed,
                                onClick = {
                                    selectedClasses = if (selectedClasses.contains(cls.value)) {
                                        if (selectedClasses.size > 1) selectedClasses - cls.value else selectedClasses
                                    } else {
                                        selectedClasses + cls.value
                                    }
                                },
                                label = { 
                                    Text(
                                        cls.value, 
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAlreadyUsed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else Color.Unspecified
                                    ) 
                                },
                                modifier = Modifier.weight(1f),
                                leadingIcon = {
                                    if (selectedClasses.contains(cls.value)) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                    } else if (isAlreadyUsed) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                    } else null
                                }
                            )
                        }
                        if (rowItems.size < 3) {
                            Spacer(Modifier.weight((3 - rowItems.size).toFloat()))
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                
                terms.forEachIndexed { index, term ->
                    val state = termStates[index]
                    val current = state.value
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                term.title, 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge, 
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    ModernCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PremiumTextField(
                                value = current.written, 
                                onValueChange = { state.value = current.copy(written = it) }, 
                                label = "Max Written Marks",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = current.hasOral, onCheckedChange = { state.value = current.copy(hasOral = it) })
                                Text("Include Oral marks component", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            if (current.hasOral) {
                                Spacer(Modifier.height(8.dp))
                                PremiumTextField(
                                    value = current.oral, 
                                    onValueChange = { state.value = current.copy(oral = it) }, 
                                    label = "Max Oral Marks",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (subjectName.isNotBlank() && selectedClasses.isNotEmpty()) {
                        val configs = terms.mapIndexed { index, term ->
                            val s = termStates[index].value
                            SubjectTermConfig(
                                term = term,
                                maxWritten = s.written.toIntOrNull() ?: 0,
                                hasOral = s.hasOral,
                                maxOral = if (s.hasOral) s.oral.toIntOrNull() ?: 0 else 0
                            )
                        }
                        onConfirm(selectedClasses.toList(), subjectName, configs)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) { 
                Text("CREATE IN ${selectedClasses.size} CLASSES", fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("CANCEL") }
        }
    )
}

@Composable
fun EditStudentDialog(student: com.school.asvvm.data.model.Student, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(student.name) }
    var roll by remember { mutableStateOf(student.rollNo) }
    var guardian by remember { mutableStateOf(student.guardian) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        titleContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { Text("Edit Student Details") },
        text = {
            Column {
                PremiumTextField(value = name, onValueChange = { name = it }, label = "Full Name")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = roll, onValueChange = { roll = it }, label = "Roll Number")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = guardian, onValueChange = { guardian = it }, label = "Guardian Name")
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, roll, guardian) }) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun EditTeacherDialog(teacher: Teacher, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf(teacher.name) }
    var phone by remember { mutableStateOf(teacher.phone) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        titleContentColor = MaterialTheme.colorScheme.primary,
        onDismissRequest = onDismiss,
        title = { Text("Teacher Details") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Profile Details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text("Email: ${teacher.email}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("Gender: ${teacher.gender.ifBlank { "Not Set" }}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        val safeClasses = teacher.assignedClasses ?: emptyList()
                        Text("Classes: ${if (safeClasses.isEmpty()) "None" else safeClasses.joinToString()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                Text("Edit Information", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                PremiumTextField(value = name, onValueChange = { name = it }, label = "Teacher Name")
                Spacer(Modifier.height(12.dp))
                PremiumTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, phone) }) { Text("SAVE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}


@Composable
fun StudentDetailsDialog(
    student: com.school.asvvm.data.model.Student,
    marksFlow: kotlinx.coroutines.flow.Flow<List<com.school.asvvm.data.model.Mark>>,
    onDismiss: () -> Unit
) {
    val marks by marksFlow.collectAsState(initial = emptyList())
    
    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Student Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text("Name: ${student.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Roll No: ${student.rollNo}", style = MaterialTheme.typography.bodyMedium)
                Text("Class: ${student.className}", style = MaterialTheme.typography.bodyMedium)
                Text("Guardian: ${student.guardian}", style = MaterialTheme.typography.bodyMedium)
                
                Spacer(Modifier.height(16.dp))
                Text("Academic Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                
                if (marks.isEmpty()) {
                    Text("No marks available yet.", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
                } else {
                    val groupedMarks = marks.groupBy { it.term }
                    groupedMarks.forEach { (term, termMarks) ->
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(term, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                termMarks.forEach { mark ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(mark.subject, style = MaterialTheme.typography.bodySmall)
                                        Text("W: ${mark.writtenMarks} | O: ${mark.oralMarks}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("CLOSE")
                }
            }
        }
    }
}
