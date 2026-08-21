package com.school.asvvm.data.model

data class Student(
    var id: String = "",
    var rollNo: String = "",
    var name: String = "",
    var className: String = "",
    var guardian: String = "",
    var lockedTerms: List<String> = emptyList()
)

data class Teacher(
    var name: String = "",
    var assignedClasses: List<String> = emptyList(),
    var email: String = "",
    var phone: String = "",
    var gender: String = ""
)

data class Attendance(
    var id: String = "",
    var studentId: String = "",
    var className: String = "",
    var date: String = "",
    var status: String = "Present", // "Present", "Absent", "Late"
    var teacherId: String = ""
)

data class Notice(
    var id: String = "",
    var title: String = "",
    var message: String = "",
    var timestamp: Long = 0L,
    var author: String = ""
)

enum class ExamTerm(val title: String) {
    FIRST_HALF("1st Half"),
    SECOND_HALF("2nd Half"),
    ANNUAL("Annually")
}

data class Mark(
    var id: String = "", 
    var studentId: String = "",
    var subject: String = "",
    var term: String = ExamTerm.FIRST_HALF.name,
    var writtenMarks: String = "",
    var oralMarks: String = "",
    var className: String = ""
)

data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val id: String? = null,
    val role: String? = null,
    val user: String? = null,
    val `class`: String? = null
)

data class StudentResult(
    var studentId: String = "",
    var className: String = "",
    var term: String = "",
    var total: String = "",
    var percentage: String = "",
    var grade: String = ""
)

enum class SchoolClass(val value: String) {
    NURSERY("Nursery"),
    KG1("KG1"),
    KG2("KG2"),
    CLASS1("Class 1"),
    CLASS2("Class 2"),
    CLASS3("Class 3"),
    CLASS4("Class 4")
}

data class SubjectConfig(
    var id: String = "",
    var className: String = "",
    var term: String = "",
    var subjectName: String = "",
    var maxWritten: Int = 40,
    var hasOral: Boolean = true,
    var maxOral: Int = 10
)

// Subject Mappings
private val nurserySubjects = listOf("Math", "Bengali", "English", "Science", "Dictation")
private val kgSubjects = listOf("Math", "Bengali", "English", "Science", "Dictation", "Drawing")
private val secondarySubjects = listOf("Math", "Bengali", "English", "Science", "History", "Geography", "Craft", "Dictation", "Drawing")

fun getSubjectsForClass(className: String): List<String> {
    return when {
        className.contains("Nursery", ignoreCase = true) -> nurserySubjects
        className.contains("KG", ignoreCase = true) -> kgSubjects
        else -> secondarySubjects
    }
}

data class TimetablePeriod(
    var id: String = "",
    var className: String = "",
    var dayOfWeek: String = "", // e.g., "Monday"
    var startTime: String = "", // e.g., "10:00 AM"
    var endTime: String = "",   // e.g., "10:45 AM"
    var subject: String = "",
    var teacherName: String = ""
)
