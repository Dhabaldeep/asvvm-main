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

    private val _subjectConfigs = MutableStateFlow<List<SubjectConfig>>(emptyList())
    val subjectConfigs: StateFlow<List<SubjectConfig>> = _subjectConfigs.asStateFlow()

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
                launch {
                    schoolRepository.listenToTimetable(student.className).collect { periods ->
                        _timetable.value = periods
                    }
                }

                // Fetch marks for this student
                launch {
                    schoolRepository.getMarksForStudent(student.id).collect { marksList ->
                        _marks.value = marksList
                    }
                }

                // Fetch subject configs for student's class
                launch {
                    schoolRepository.getSubjectConfigs(student.className).collect { configs ->
                        _subjectConfigs.value = configs
                    }
                }
            } else {
                _message.value = "Failed to load student profile."
            }
            _isLoading.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
