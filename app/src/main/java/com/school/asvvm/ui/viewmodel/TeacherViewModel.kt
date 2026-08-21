package com.school.asvvm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.asvvm.data.model.Mark
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.Teacher
import com.school.asvvm.data.repository.SchoolRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.school.asvvm.data.model.StudentResult
import com.school.asvvm.data.model.getSubjectsForClass

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)


@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val repository: SchoolRepository
) : ViewModel() {

    private val _assignedClass = MutableStateFlow<String?>(null)
    val assignedClass = _assignedClass.asStateFlow()

    private val _teacherProfile = MutableStateFlow<Teacher?>(null)
    val teacherProfile = _teacherProfile.asStateFlow()

    val students: StateFlow<List<Student>> = _assignedClass.flatMapLatest { 
        if (it != null) repository.getStudents(it) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marks: StateFlow<List<Mark>> = _assignedClass.flatMapLatest {
        if (it != null) repository.getLocalMarks(it) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectConfigs: StateFlow<List<com.school.asvvm.data.model.SubjectConfig>> = _assignedClass.flatMapLatest {
        if (it != null) repository.getSubjectConfigs(it) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices = repository.listenToNotices().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val timetable: StateFlow<List<com.school.asvvm.data.model.TimetablePeriod>> = _assignedClass.flatMapLatest {
        if (it != null) repository.listenToTimetable(it) else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _leaveRequests = MutableStateFlow<List<com.school.asvvm.data.model.LeaveRequest>>(emptyList())
    val leaveRequests: StateFlow<List<com.school.asvvm.data.model.LeaveRequest>> = _leaveRequests.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<com.school.asvvm.data.model.Attendance>>(emptyList())
    val attendanceRecords = _attendanceRecords.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private var profileJob: kotlinx.coroutines.Job? = null

    fun initialize(teacherEmailOrName: String) {
        if (teacherEmailOrName.isBlank()) {
            _message.value = "Login details missing. Please logout and login again."
            return
        }
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                launch {
                    repository.listenToTeacherProfile(teacherEmailOrName).collect { profile ->
                        _isLoading.value = false
                        if (profile != null) {
                            val currentClass = _assignedClass.value
                            _teacherProfile.value = profile
                            
                            // If they have classes and current class is null, select the first one
                            if (profile.assignedClasses.isNotEmpty()) {
                                if (currentClass == null || !profile.assignedClasses.contains(currentClass)) {
                                    setAssignedClass(profile.assignedClasses.first())
                                }
                            } else {
                                if (currentClass != null) {
                                    _assignedClass.value = null
                                }
                                _message.value = "No classes assigned to you"
                            }
                        } else {
                            _teacherProfile.value = null
                        }
                    }
                }
                // Load leave requests
                launch {
                    repository.listenToLeaveRequests(teacherEmailOrName).collect { lr ->
                        _leaveRequests.value = lr
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _message.value = "Connection Error. Please check your internet."
            }
        }
    }

    fun setAssignedClass(className: String) {
        _assignedClass.value = className
        viewModelScope.launch {
            repository.refreshStudents(className)
            repository.refreshMarks(className)
        }
    }

    fun submitMarks(marksList: List<Mark>) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitMarks(marksList)
            if (res.success && marksList.isNotEmpty()) {
                // Background calculation of results
                calculateAndSubmitResults(marksList)
                _message.value = "Marks saved & syncing..."
            }
            _isLoading.value = false
        }
    }

    private var attendanceJob: kotlinx.coroutines.Job? = null
    fun loadAttendanceForDate(className: String, date: String) {
        attendanceJob?.cancel()
        attendanceJob = viewModelScope.launch {
            repository.getAttendance(className, date).collect { records ->
                _attendanceRecords.value = records
            }
        }
    }

    fun submitAttendance(records: List<com.school.asvvm.data.model.Attendance>) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitAttendance(records)
            if (res.success) {
                _message.value = "Attendance saved!"
            } else {
                _message.value = "Failed to save attendance"
            }
            _isLoading.value = false
        }
    }

    private suspend fun calculateAndSubmitResults(marksList: List<Mark>) {
        if (marksList.isEmpty()) return
        
        val className = marksList.first().className
        val studentId = marksList.first().studentId
        val termName = marksList.first().term
        
        val termEnum = try { 
            com.school.asvvm.data.model.ExamTerm.valueOf(termName) 
        } catch (e: Exception) { 
            com.school.asvvm.data.model.ExamTerm.FIRST_HALF 
        }
        
        val termConfigs = subjectConfigs.value.filter { it.term == termEnum.name }
        
        var total = 0
        marksList.forEach { m -> 
            val w = m.writtenMarks.toIntOrNull() ?: 0
            val o = m.oralMarks.toIntOrNull() ?: 0
            total += (w + o)
        }
        
        var maxMarks = 0
        termConfigs.forEach { config ->
            maxMarks += config.maxWritten
            if (config.hasOral) {
                maxMarks += config.maxOral
            }
        }
        
        val percentage = if (maxMarks > 0) (total.toFloat() / maxMarks) * 100 else 0f
        val grade = when {
            percentage >= 80 -> "A"
            percentage >= 60 -> "B"
            percentage >= 40 -> "C"
            else -> "F"
        }

        val result = com.school.asvvm.data.model.StudentResult(
            studentId = studentId,
            className = className,
            term = termEnum.name,
            total = total.toString(),
            percentage = String.format("%.2f%%", percentage),
            grade = grade
        )
        repository.submitResult(result)
    }

    fun changePassword(username: String, oldPass: String, newPass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = repository.updatePassword(username, oldPass, newPass)
                _message.value = res.message ?: "Password updated"
            } catch (e: Exception) {
                _message.value = "Failed to update password"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
    
    fun submitLeaveRequest(startDate: String, endDate: String, reason: String, type: String) {
        val profile = _teacherProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val request = com.school.asvvm.data.model.LeaveRequest(
                teacherName = profile.name,
                teacherEmail = profile.email,
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                type = type
            )
            val result = repository.submitLeaveRequest(request)
            if (result.success) {
                _message.value = "Leave request submitted successfully."
            } else {
                _message.value = "Failed to submit leave request: ${result.message}"
            }
            _isLoading.value = false
        }
    }
}
