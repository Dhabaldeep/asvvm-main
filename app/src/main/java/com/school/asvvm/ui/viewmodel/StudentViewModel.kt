package com.school.asvvm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.school.asvvm.data.model.*
import com.school.asvvm.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _studentProfile = MutableStateFlow<Student?>(null)
    val studentProfile: StateFlow<Student?> = _studentProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _attendance = MutableStateFlow<List<Attendance>>(emptyList())
    val attendance: StateFlow<List<Attendance>> = _attendance.asStateFlow()

    private val _marks = MutableStateFlow<List<Mark>>(emptyList())
    val marks: StateFlow<List<Mark>> = _marks.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetablePeriod>>(emptyList())
    val timetable: StateFlow<List<TimetablePeriod>> = _timetable.asStateFlow()

    val notices: StateFlow<List<Notice>> = schoolRepository.listenToNotices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initialize(studentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val student = schoolRepository.getStudentById(studentId)
            if (student != null) {
                _studentProfile.value = student
                // Fetch timetable
                schoolRepository.listenToTimetable(student.className).collect { periods ->
                    _timetable.value = periods
                }
            } else {
                _message.value = "Failed to load student profile."
            }
            _isLoading.value = false
        }
        
        // Fetch marks and attendance (In a real app this would have dedicated endpoints for a specific student)
        // For now, we will assume the Student portal is largely read-only of existing data
        viewModelScope.launch {
            // Need a way to fetch marks for a student across all subjects
            // Since we don't have a direct endpoint for all marks of a student, we will leave this empty for now
            // or we could add one to SchoolRepository.
        }
        _isLoading.value = false
    }

    fun clearMessage() {
        _message.value = null
    }
}
