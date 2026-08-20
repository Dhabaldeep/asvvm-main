package com.school.asvvm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.Teacher
import com.school.asvvm.data.repository.SchoolRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)

data class SubjectTermConfig(
    val term: com.school.asvvm.data.model.ExamTerm,
    val maxWritten: Int,
    val hasOral: Boolean,
    val maxOral: Int
)


@HiltViewModel
class AdminViewModel @Inject constructor(private val repository: SchoolRepository) : ViewModel() {

    private val _selectedClass = MutableStateFlow("Nursery")
    val selectedClass = _selectedClass.asStateFlow()

    val students: StateFlow<List<Student>> = _selectedClass.flatMapLatest { 
        repository.getStudents(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teachers: StateFlow<List<Teacher>> = repository.getAllTeachers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectConfigs: StateFlow<List<com.school.asvvm.data.model.SubjectConfig>> = _selectedClass.flatMapLatest {
        repository.getSubjectConfigs(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjectConfigs: StateFlow<List<com.school.asvvm.data.model.SubjectConfig>> = repository.getAllSubjectConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        refreshData()
    }

    fun selectClass(className: String) {
        _selectedClass.value = className
        viewModelScope.launch { repository.refreshStudents(className) }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshTeachers()
            repository.refreshStudents(_selectedClass.value)
            _isLoading.value = false
        }
    }

    fun addStudent(name: String, roll: String, className: String, guardian: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addStudent(name, roll, className, guardian)
            _message.value = res.message ?: "Student added"
            _isLoading.value = false
        }
    }


    fun deleteTeacher(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteTeacher(email)
            _message.value = res.message ?: "Teacher deleted"
            _isLoading.value = false
        }
    }

    fun deleteStudent(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteStudent(id)
            _message.value = res.message ?: "Student deleted"
            _isLoading.value = false
        }
    }

    fun unlockStudent(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.unlockTermsForStudent(id)
            _message.value = res.message ?: "Student unlocked"
            _isLoading.value = false
        }
    }

    fun bulkUnlockStudents(students: List<com.school.asvvm.data.model.Student>) {
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            for (student in students) {
                if (student.lockedTerms.isNotEmpty()) {
                    val res = repository.unlockTermsForStudent(student.id)
                    if (res.success) successCount++
                }
            }
            _message.value = "Unlocked $successCount students"
            _isLoading.value = false
        }
    }

    fun bulkLockStudents(students: List<com.school.asvvm.data.model.Student>) {
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            for (student in students) {
                if (student.lockedTerms.isEmpty()) {
                    val res = repository.lockTermForStudent(student.id, "ALL_TERMS")
                    if (res.success) successCount++
                }
            }
            _message.value = "Locked $successCount students"
            _isLoading.value = false
        }
    }

    fun addSubject(classNames: List<String>, subjectName: String, configs: List<SubjectTermConfig>) {
        viewModelScope.launch {
            _isLoading.value = true
            var allSuccess = true
            
            for (className in classNames) {
                for (termConfig in configs) {
                    val config = com.school.asvvm.data.model.SubjectConfig(
                        className = className,
                        term = termConfig.term.name,
                        subjectName = subjectName,
                        maxWritten = termConfig.maxWritten,
                        hasOral = termConfig.hasOral,
                        maxOral = termConfig.maxOral
                    )
                    val res = repository.addSubjectConfig(config)
                    if (!res.success) allSuccess = false
                }
            }
            
            _message.value = if (allSuccess) "Subject added successfully to ${classNames.size} classes" else "Error adding subject for some terms or classes"
            _isLoading.value = false
        }
    }

    fun deleteSubject(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteSubjectConfig(id)
            _message.value = res.message ?: "Subject deleted"
            _isLoading.value = false
        }
    }

    fun deleteSubjectGroup(configs: List<com.school.asvvm.data.model.SubjectConfig>) {
        viewModelScope.launch {
            _isLoading.value = true
            var allSuccess = true
            configs.forEach { config ->
                val res = repository.deleteSubjectConfig(config.id)
                if (!res.success) allSuccess = false
            }
            _message.value = if (allSuccess) "Subject deleted from all terms" else "Failed to delete from some terms"
            _isLoading.value = false
        }
    }

    fun createTeacher(name: String, email: String, phone: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createTeacherProfile(name, email, phone, gender)
            _message.value = res.message ?: "Teacher $name Registered"
            _isLoading.value = false
        }
    }

    fun assignClassToTeacher(email: String, className: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.assignClassToTeacher(email, className)
            _message.value = res.message ?: "Class $className assigned to $email"
            _isLoading.value = false
        }
    }

    fun removeAssignedClass(email: String, className: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.removeAssignedClass(email, className)
            _message.value = res.message ?: "Class $className removed from $email"
            _isLoading.value = false
        }
    }

    fun updateStudent(id: String, name: String, rollNo: String, guardian: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateStudentProfile(id, name, rollNo, guardian)
            _message.value = res.message ?: "Student updated"
            _isLoading.value = false
        }
    }

    fun updateTeacher(email: String, name: String, phone: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateTeacherProfile(email, name, phone)
            _message.value = res.message ?: "Teacher updated"
            _isLoading.value = false
        }
    }

    fun getMarksForStudent(studentId: String) = repository.getMarksForStudent(studentId)

    fun clearMessage() {
        _message.value = null
    }
}
