@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
package com.school.asvvm.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.school.asvvm.data.model.SchoolClass
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.Teacher
import com.school.asvvm.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState

@Composable
fun StudentRow(student: Student, modifier: Modifier = Modifier, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onUnlock: () -> Unit) {
    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToStart || it == DismissValue.DismissedToEnd) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismiss(
        modifier = modifier,
        state = dismissState,
        background = {
            val alignment = if (dismissState.dismissDirection == DismissDirection.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        },
        dismissContent = {
            ModernCard(onClick = onClick) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(SubjectMath, SubjectScience, SubjectEnglish, SubjectArt, SubjectHistory)
                    val color = colors[student.rollNo.hashCode().let { if (it < 0) -it else it } % colors.size]
                    
                    ColoredIconBox(
                        icon = if (student.lockedTerms.isEmpty()) Icons.Default.Person else Icons.Default.Lock,
                        color = if (student.lockedTerms.isEmpty()) color else MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(Modifier.width(16.dp))
                     Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            if (student.lockedTerms.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "LOCKED", 
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text("Roll No: ${student.rollNo} • Guardian: ${student.guardian}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (student.lockedTerms.isNotEmpty()) {
                        IconButton(onClick = onUnlock) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Unlock", tint = SubjectScience)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TeacherRow(teacher: Teacher, modifier: Modifier = Modifier, onEdit: () -> Unit, onAssignClass: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToStart || it == DismissValue.DismissedToEnd) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismiss(
        modifier = modifier,
        state = dismissState,
        background = {
            val alignment = if (dismissState.dismissDirection == DismissDirection.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        },
        dismissContent = {
            ModernCard(onClick = {}) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColoredIconBox(icon = Icons.Default.School, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(teacher.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val safeClasses = teacher.assignedClasses ?: emptyList()
                        if (safeClasses.isNotEmpty()) {
                            Text("Classes: ${safeClasses.joinToString()}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                        } else {
                            Text("No Assigned Classes", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onAssignClass) {
                        Icon(Icons.Default.Assignment, contentDescription = "Assign", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    )
}

@Composable
fun UnifiedSubjectRow(
    subjectName: String,
    configs: List<com.school.asvvm.data.model.SubjectConfig>,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ModernCard {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subjectColor = when {
                    subjectName.contains("Math", true) -> SubjectMath
                    subjectName.contains("Sci", true) -> SubjectScience
                    subjectName.contains("Eng", true) -> SubjectEnglish
                    subjectName.contains("Art", true) -> SubjectArt
                    else -> MaterialTheme.colorScheme.primary
                }

                ColoredIconBox(icon = Icons.Default.LibraryBooks, color = subjectColor)
                
                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        subjectName, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.SemiBold, 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${configs.size} Terms Configured", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = subjectColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.DeleteSweep, 
                        contentDescription = "Delete All", 
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
                
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))
                    
                    configs.sortedBy { 
                        when(it.term) {
                            com.school.asvvm.data.model.ExamTerm.FIRST_HALF.name -> 1
                            com.school.asvvm.data.model.ExamTerm.SECOND_HALF.name -> 2
                            else -> 3
                        }
                    }.forEach { config ->
                        val termTitle = try { 
                            com.school.asvvm.data.model.ExamTerm.valueOf(config.term).title 
                        } catch(e:Exception) { config.term }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                termTitle, 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(80.dp)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "W: ${config.maxWritten}" + if(config.hasOral) " | O: ${config.maxOral}" else "",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

