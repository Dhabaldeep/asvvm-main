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
fun StudentRow(
    student: Student, 
    modifier: Modifier = Modifier, 
    onClick: () -> Unit, 
    onEdit: () -> Unit, 
    onDelete: () -> Unit, 
    onUnlock: () -> Unit
) {
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
            ModernCard(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(SubjectMath, SubjectScience, SubjectEnglish, SubjectArt, SubjectHistory)
                    val color = colors[student.rollNo.hashCode().let { if (it < 0) -it else it } % colors.size]
                    
                    ColoredIconBox(
                        icon = if (student.lockedTerms.isEmpty()) Icons.Default.Person else Icons.Default.Lock,
                        color = if (student.lockedTerms.isEmpty()) color else MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(Modifier.width(14.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                student.name, 
                                style = MaterialTheme.typography.titleMedium, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (student.lockedTerms.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                M3StatusChip(
                                    text = "LOCKED",
                                    icon = Icons.Default.Lock,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Roll No: ${student.rollNo} • Guardian: ${student.guardian.ifBlank { "N/A" }}", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                        if (student.lockedTerms.isNotEmpty()) {
                            IconButton(
                                onClick = onUnlock,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Unlock", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun TeacherRow(
    teacher: Teacher, 
    modifier: Modifier = Modifier, 
    onEdit: () -> Unit, 
    onAssignClass: () -> Unit, 
    onDelete: () -> Unit
) {
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
            ModernCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColoredIconBox(icon = Icons.Default.School, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            teacher.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        val safeClasses = teacher.assignedClasses ?: emptyList()
                        if (safeClasses.isNotEmpty()) {
                            M3StatusChip(
                                text = "Classes: ${safeClasses.joinToString()}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            M3StatusChip(
                                text = "No Assigned Class",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onAssignClass) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = "Assign Class", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }
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
    var expanded by remember { mutableStateOf(false) }

    ModernCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val subjectColor = when {
                    subjectName.contains("Math", true) -> SubjectMath
                    subjectName.contains("Sci", true) -> SubjectScience
                    subjectName.contains("Eng", true) -> SubjectEnglish
                    subjectName.contains("Art", true) -> SubjectArt
                    else -> MaterialTheme.colorScheme.primary
                }

                ColoredIconBox(icon = Icons.Default.Book, color = subjectColor)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        subjectName, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${configs.size} Terms Configured", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Subject", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
                
                configs.forEach { config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        M3StatusChip(
                            text = config.term,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Written: ${config.maxWritten} | Oral: ${if (config.hasOral) config.maxOral else "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
