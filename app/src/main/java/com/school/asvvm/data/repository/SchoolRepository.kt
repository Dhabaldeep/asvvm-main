package com.school.asvvm.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.school.asvvm.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import android.util.Log

class SchoolRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    // Auth logic
    suspend fun login(user: String, pass: String): ApiResponse<Unit> {
        return try {
            auth.signInWithEmailAndPassword(user, pass).await() 
            
            // Fetch role from Staff collection. Source.CACHE first if offline.
            val querySnapshot = try { 
                firestore.collection("Staff").whereEqualTo("email", user).get().await() 
            } catch (e: Exception) {
                // Ignore network issues to allow cache reads or fallback
                firestore.collection("Staff").whereEqualTo("email", user).get(com.google.firebase.firestore.Source.CACHE).await()
            }
            val userDoc = querySnapshot.documents.firstOrNull()
            val baseRole = userDoc?.getString("role") ?: "Teacher"
            val role = if (user.lowercase() == "admin@school.com") "Admin" else baseRole
            
            ApiResponse(success = true, role = role, user = user)
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Login Failed: ${e.message}")
        }
    }
    
    suspend fun updatePassword(user: String, oldPass: String, newPass: String): ApiResponse<Unit> {
        return try {
            auth.signInWithEmailAndPassword(user, oldPass).await()
            auth.currentUser?.updatePassword(newPass)?.await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun register(email: String, pass: String): ApiResponse<Unit> {
        val safeEmail = email.trim().lowercase()
        return try {
            // We must create the user FIRST to authenticate them, otherwise Firestore Rules might block the get() request!
            auth.createUserWithEmailAndPassword(safeEmail, pass).await()
            
            // Check if Staff collection is empty (for initial bootstrap)
            val allStaff = try { firestore.collection("Staff").limit(1).get().await() } catch (e: Exception) { null }
            val isFirstUser = allStaff != null && allStaff.isEmpty
            
            val docRef = firestore.collection("Staff").document(safeEmail)
            val existingDoc = try { docRef.get().await() } catch (e: Exception) { null }

            // Security Check: Only allow registration if email is pre-authorized or bootstrap admin
            if (!isFirstUser && safeEmail != "admin@school.com" && (existingDoc == null || !existingDoc.exists())) {
                // If not authorized, delete the auth user we just created to prevent unauthorized zombie accounts
                auth.currentUser?.delete()?.await()
                auth.signOut()
                return ApiResponse(success = false, message = "Authorized registration only. Contact Admin to add your email.")
            }
            
            val targetRole = if (isFirstUser || safeEmail == "admin@school.com") "Admin" else {
                existingDoc?.getString("role") ?: "Teacher"
            }
            
            val staff = mapOf(
                "name" to (existingDoc?.getString("name") ?: safeEmail.substringBefore("@")), 
                "assignedClasses" to (existingDoc?.get("assignedClasses") ?: emptyList<String>()), 
                "role" to targetRole, 
                "email" to safeEmail,
                "phone" to (existingDoc?.getString("phone") ?: ""),
                "gender" to (existingDoc?.getString("gender") ?: "")
            )
            
            val task = docRef.set(staff)
            withTimeoutOrNull(3000L) { task.await() }
            
            ApiResponse(success = true, role = targetRole, user = safeEmail)
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Registration failed: ${e.localizedMessage}")
        }
    }

    // Students
    fun getStudents(className: String): Flow<List<Student>> = callbackFlow {
        val listener = firestore.collection("students")
            .whereEqualTo("className", className)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val students = snapshot?.toObjects<Student>()?.map { it.sanitize() } ?: emptyList()
                trySend(students)
            }
        awaitClose { listener.remove() }
    }

    suspend fun refreshStudents(className: String? = null) {
        // Intentionally empty: Real-time queries handle refresh
    }

    suspend fun getStudentById(studentId: String): com.school.asvvm.data.model.Student? {
        return try {
            val snapshot = firestore.collection("students").document(studentId).get().await()
            snapshot.toObject(com.school.asvvm.data.model.Student::class.java)?.sanitize()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addStudent(name: String, roll: String, className: String, guardian: String, accessCode: String): ApiResponse<Unit> {
        return try {
            val ref = firestore.collection("students").document()
            val student = com.school.asvvm.data.model.Student(id = ref.id, rollNo = roll, name = name, className = className, guardian = guardian, accessCode = accessCode)
            ref.set(student).await()
            ApiResponse(success = true, message = "Student added")
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }


    suspend fun deleteTeacher(email: String): ApiResponse<Unit> {
        return try {
            val task = firestore.collection("Staff").document(email).delete()
            task.await()
            refreshTeachers()
            ApiResponse(success = true, message = "Teacher deleted successfully")
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message ?: "Failed to delete teacher")
        }
    }

    suspend fun removeAssignedClass(email: String, className: String): ApiResponse<Unit> {
        val safeEmail = email.trim().lowercase()
        return try {
            val docRef = firestore.collection("Staff").document(safeEmail)
            val task = docRef.update("assignedClasses", com.google.firebase.firestore.FieldValue.arrayRemove(className))
            withTimeoutOrNull(3000L) { task.await() }
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun deleteStudent(id: String): ApiResponse<Unit> {
        return try {
            val task = firestore.collection("students").document(id).delete()
            withTimeoutOrNull(3000L) { task.await() } // Await network sync or timeout gracefully for offline
            ApiResponse(success = true, message = "Student deleted")
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun updateStudentProfile(id: String, name: String, rollNo: String, guardian: String): ApiResponse<Unit> {
        return try {
            val updates = mapOf(
                "name" to name.trim(),
                "rollNo" to rollNo.trim(),
                "guardian" to guardian.trim()
            )
            firestore.collection("students").document(id).update(updates).await()
            ApiResponse(success = true, message = "Student updated")
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }


    suspend fun unlockTermsForStudent(studentId: String): ApiResponse<Unit> {
        return try {
            firestore.collection("students").document(studentId).update("lockedTerms", emptyList<String>()).await()
            ApiResponse(success = true, message = "Student unlocked")
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun lockTermForStudent(studentId: String, term: String): ApiResponse<Unit> {
        return try {
            firestore.collection("students").document(studentId)
                .update("lockedTerms", com.google.firebase.firestore.FieldValue.arrayUnion(term))
                .await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // Teachers
    fun getAllTeachers(): Flow<List<Teacher>> = callbackFlow {
        val listener = firestore.collection("Staff")
            .whereEqualTo("role", "Teacher")
            .addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val teachers = snapshot?.toObjects<Teacher>()?.map { it.sanitize() } ?: emptyList()
            trySend(teachers)
        }
        awaitClose { listener.remove() }
    }

    suspend fun refreshTeachers() {}

    suspend fun createTeacherProfile(name: String, email: String, phone: String, gender: String): ApiResponse<Unit> {
        val safeEmail = email.trim().lowercase()
        return try {
            val docRef = firestore.collection("Staff").document(safeEmail)
            val doc = try { docRef.get().await() } catch (e: Exception) { null }
            if (doc != null && doc.exists()) {
                return ApiResponse(success = false, message = "Teacher already exists.")
            }
            val staff = mapOf(
                "name" to name.trim(),
                "assignedClasses" to emptyList<String>(),
                "role" to "Teacher",
                "email" to safeEmail,
                "phone" to phone.trim(),
                "gender" to gender
            )
            val task = docRef.set(staff)
            withTimeoutOrNull(3000L) { task.await() }
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun updateTeacherProfile(email: String, name: String, phone: String): ApiResponse<Unit> {
        val safeEmail = email.trim().lowercase()
        return try {
            val updates = mapOf(
                "name" to name.trim(),
                "phone" to phone.trim()
            )
            firestore.collection("Staff").document(safeEmail).update(updates).await()
            ApiResponse(success = true, message = "Teacher profile updated")
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun assignClassToTeacher(email: String, className: String): ApiResponse<Unit> {
        val safeEmail = email.trim().lowercase()
        return try {
            val docRef = firestore.collection("Staff").document(safeEmail)
            val task = docRef.update("assignedClasses", com.google.firebase.firestore.FieldValue.arrayUnion(className))
            withTimeoutOrNull(3000L) { task.await() }
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    fun listenToTeacherProfile(emailOrName: String): Flow<Teacher?> = callbackFlow {
        val safeId = emailOrName.trim().lowercase()
        val listener = firestore.collection("Staff")
            .whereEqualTo("email", safeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val teacher = snapshot?.documents?.firstOrNull()?.toObject(Teacher::class.java)?.sanitize()
                trySend(teacher)
            }
        awaitClose { listener.remove() }
    }

    suspend fun loadTeacherProfile(emailOrName: String): ApiResponse<Teacher> {
        val safeId = emailOrName.trim().lowercase()
        return try {
            // Priority 1: Try direct document fetch (Fastest & Best for cache)
            val directDoc = firestore.collection("Staff").document(safeId).get().await()
            if (directDoc.exists()) {
                val teacher = directDoc.toObject(Teacher::class.java)?.sanitize()
                if (teacher != null) return ApiResponse(success = true, data = teacher)
            }

            // Priority 2: Query by email field
            val querySnapshot = firestore.collection("Staff").whereEqualTo("email", safeId).get().await()
            var doc = querySnapshot.documents.firstOrNull()
            
            // Priority 3: Query by name field (fallback)
            if (doc == null) {
                 val queryByName = firestore.collection("Staff").whereEqualTo("name", emailOrName.trim()).get().await()
                 doc = queryByName.documents.firstOrNull()
            }
            
            if (doc != null) {
                val teacher = doc.toObject(Teacher::class.java)?.sanitize()
                if (teacher != null) {
                    ApiResponse(success = true, data = teacher)
                } else {
                    ApiResponse(success = false, message = "Could not parse Teacher profile")
                }
            } else {
                ApiResponse(success = false, message = "Profile not found. Please contact Admin.")
            }
        } catch (e: Exception) {
            ApiResponse(success = false, message = "Database Error: ${e.localizedMessage}")
        }
    }

    // Subject Configurations
    fun getSubjectConfigs(className: String): Flow<List<SubjectConfig>> = callbackFlow {
        val listener = firestore.collection("subject_configs")
            .whereEqualTo("className", className)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val configs = snapshot?.toObjects<SubjectConfig>() ?: emptyList()
                trySend(configs)
            }
        awaitClose { listener.remove() }
    }

    fun getAllSubjectConfigs(): Flow<List<SubjectConfig>> = callbackFlow {
        val listener = firestore.collection("subject_configs")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val configs = snapshot?.safeToObjects(SubjectConfig::class.java) ?: emptyList()
                trySend(configs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addSubjectConfig(config: SubjectConfig): ApiResponse<Unit> {
        return try {
            if (config.id.isBlank()) config.id = java.util.UUID.randomUUID().toString()
            val task = firestore.collection("subject_configs").document(config.id).set(config)
            withTimeoutOrNull(3000L) { task.await() }
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun deleteSubjectConfig(id: String): ApiResponse<Unit> {
        return try {
            val task = firestore.collection("subject_configs").document(id).delete()
            withTimeoutOrNull(3000L) { task.await() }
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // Marks
    fun getLocalMarks(className: String): Flow<List<Mark>> = callbackFlow {
        val listener = firestore.collection("marks")
            .whereEqualTo("className", className)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val marks = snapshot?.safeToObjects(Mark::class.java) ?: emptyList()
                trySend(marks)
            }
        awaitClose { listener.remove() }
    }

    fun getMarksForStudent(studentId: String): Flow<List<Mark>> = callbackFlow {
        val listener = firestore.collection("marks")
            .whereEqualTo("studentId", studentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val marks = snapshot?.safeToObjects(Mark::class.java) ?: emptyList()
                trySend(marks)
            }
        awaitClose { listener.remove() }
    }

    suspend fun refreshMarks(className: String) {}

    suspend fun submitMarks(marks: List<Mark>): ApiResponse<Unit> {
        return try {
            val batch = firestore.batch()
            for (m in marks) {
                val docId = "${m.studentId}_${m.subject}"
                val docRef = firestore.collection("marks").document(docId)
                batch.set(docRef, m)
            }
            batch.commit().await()
            
            // Also lock the term for the student
            if (marks.isNotEmpty()) {
                lockTermForStudent(marks[0].studentId, marks[0].term)
            }
            
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    suspend fun submitResult(result: StudentResult): ApiResponse<Unit> {
        return try {
            firestore.collection("results").document(result.studentId).set(result).await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // Attendance
    fun getAttendance(className: String, date: String): Flow<List<Attendance>> = callbackFlow {
        val listener = firestore.collection("attendance")
            .whereEqualTo("className", className)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val records = snapshot?.safeToObjects(Attendance::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitAttendance(records: List<Attendance>): ApiResponse<Unit> {
        return try {
            val batch = firestore.batch()
            for (record in records) {
                if (record.id.isBlank()) record.id = "${record.studentId}_${record.date}"
                val docRef = firestore.collection("attendance").document(record.id)
                batch.set(docRef, record)
            }
            batch.commit().await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // Notices
    fun listenToNotices(): Flow<List<Notice>> = callbackFlow {
        val listener = firestore.collection("notices")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notices = snapshot?.safeToObjects(Notice::class.java) ?: emptyList()
                trySend(notices)
            }
        awaitClose { listener.remove() }
    }

    suspend fun submitNotice(notice: Notice): ApiResponse<Unit> {
        return try {
            if (notice.id.isBlank()) notice.id = java.util.UUID.randomUUID().toString()
            firestore.collection("notices").document(notice.id).set(notice).await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // Timetable
    fun listenToTimetable(className: String): Flow<List<com.school.asvvm.data.model.TimetablePeriod>> = callbackFlow {
        val listener = firestore.collection("timetables")
            .whereEqualTo("className", className)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val periods = snapshot?.safeToObjects(com.school.asvvm.data.model.TimetablePeriod::class.java) ?: emptyList()
                trySend(periods)
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveTimetablePeriod(period: com.school.asvvm.data.model.TimetablePeriod): ApiResponse<Unit> {
        return try {
            if (period.id.isBlank()) period.id = java.util.UUID.randomUUID().toString()
            firestore.collection("timetables").document(period.id).set(period).await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }
    
    suspend fun deleteTimetablePeriod(periodId: String): ApiResponse<Unit> {
        return try {
            firestore.collection("timetables").document(periodId).delete().await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // --- Leave Management ---
    suspend fun submitLeaveRequest(request: com.school.asvvm.data.model.LeaveRequest): ApiResponse<Unit> {
        return try {
            val ref = firestore.collection("leaves").document()
            request.id = ref.id
            request.timestamp = System.currentTimeMillis()
            ref.set(request).await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    fun listenToLeaveRequests(teacherEmail: String? = null): Flow<List<com.school.asvvm.data.model.LeaveRequest>> = callbackFlow {
        var query: com.google.firebase.firestore.Query = firestore.collection("leaves")
        if (teacherEmail != null) {
            query = query.whereEqualTo("teacherEmail", teacherEmail)
        }
        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val requests = snapshot.documents.mapNotNull { it.safeToObject(com.school.asvvm.data.model.LeaveRequest::class.java) }
                trySend(requests.sortedByDescending { it.timestamp })
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateLeaveStatus(requestId: String, status: String): ApiResponse<Unit> {
        return try {
            firestore.collection("leaves").document(requestId).update("status", status).await()
            ApiResponse(success = true)
        } catch (e: Exception) {
            ApiResponse(success = false, message = e.message)
        }
    }

    // --- Student Authentication ---
    suspend fun verifyStudentAccess(rollNo: String, accessCode: String): com.school.asvvm.data.model.Student? {
        // Authenticate generically first to bypass 'request.auth != null' Firestore security rules
        try {
            auth.signInWithEmailAndPassword("student-auth@school.com", "student123").await()
        } catch (e: Exception) {
            try {
                auth.createUserWithEmailAndPassword("student-auth@school.com", "student123").await()
            } catch (ex: Exception) {
                // Ignore, we might already be signed in or another error occurred
            }
        }
        
        val snapshot = firestore.collection("students")
            .whereEqualTo("rollNo", rollNo)
            .whereEqualTo("accessCode", accessCode)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(com.school.asvvm.data.model.Student::class.java)?.sanitize()
    }

    private fun Student.sanitize(): Student = this.copy(
        id = this.id ?: "",
        rollNo = this.rollNo ?: "",
        name = this.name ?: "Unknown",
        className = this.className ?: "",
        guardian = this.guardian ?: "Not Provided",
        accessCode = this.accessCode ?: "",
        lockedTerms = this.lockedTerms ?: emptyList()
    )

    private fun Teacher.sanitize(): Teacher = this.copy(
        name = this.name ?: "Unknown",
        assignedClasses = this.assignedClasses ?: emptyList(),
        email = this.email ?: "",
        phone = this.phone ?: "",
        gender = this.gender ?: ""
    )

    private fun <T> com.google.firebase.firestore.DocumentSnapshot.safeToObject(clazz: Class<T>): T? {
        return try {
            this.toObject(clazz)
        } catch (e: Exception) {
            Log.e("SchoolRepository", "Error mapping document ${this.id} to ${clazz.simpleName}", e)
            null
        }
    }

    private fun <T> com.google.firebase.firestore.QuerySnapshot.safeToObjects(clazz: Class<T>): List<T> {
        return this.documents.mapNotNull { it.safeToObject(clazz) }
    }
}
