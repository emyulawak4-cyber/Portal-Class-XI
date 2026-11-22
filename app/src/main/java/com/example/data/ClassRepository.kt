package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Announcement
import com.example.model.AttendanceDayDoc
import com.example.model.AttendanceStatus
import com.example.model.CLASS_ID
import com.example.model.ClassConfig
import com.example.model.ClassConstants
import com.example.model.ClassNotification
import com.example.model.DokumentasiComment
import com.example.model.DokumentasiPost
import com.example.model.KasPaymentEntry
import com.example.model.KasPeriodDoc
import com.example.model.KasTransaction
import com.example.model.LeaveRequest
import com.example.model.MateriItem
import com.example.model.StudentAttendanceRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClassRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan akun Google terlebih dahulu.")
    }

    private fun currentDisplayName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() }?.take(60)
            ?: user?.email?.substringBefore("@")?.take(60)
            ?: "Siswa CLASS XI"
    }

    fun sanitizeDocId(raw: String): String {
        return raw.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(120)
            .ifBlank { "item_${System.currentTimeMillis()}" }
    }

    suspend fun ensureBootstrapData(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val userPayload = mapOf(
                    "userId" to uid,
                    "displayName" to currentDisplayName(),
                    "email" to (auth.currentUser?.email ?: "").take(120),
                    "studentId" to 0,
                    "notifyEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userPayload).await()
            }

            val classRef = db.collection("classes").document(CLASS_ID)
            val classSnap = classRef.get().await()
            if (!classSnap.exists()) {
                val classPayload = mapOf(
                    "classId" to CLASS_ID,
                    "userId" to uid,
                    "name" to "CLASS XI",
                    "school" to "SMA Puspita Kota Tangerang",
                    "kasPeriods" to ClassConstants.DEFAULT_KAS_PERIODS,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                classRef.set(classPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "classes/$CLASS_ID")
            Result.failure(e)
        }
    }

    private inline fun <T> Flow<T>.handleListenerErrors(uid: String, path: String, op: OperationType): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    fun observeClassConfig(): Flow<ClassConfig?> = flow {
        val uid = requireUserId()
        val path = "classes/$CLASS_ID"
        emitAll(
            db.collection("classes").document(CLASS_ID)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(ClassConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .handleListenerErrors(uid, path, OperationType.GET)
        )
    }

    fun observeAttendanceDays(): Flow<Map<String, AttendanceDayDoc>> = flow {
        val uid = requireUserId()
        val path = "attendance_days"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(AttendanceDayDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.associateBy { it.dateKey }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeLeaveRequests(): Flow<List<LeaveRequest>> = flow {
        val uid = requireUserId()
        val path = "leave_requests"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(LeaveRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeKasPeriods(): Flow<Map<String, KasPeriodDoc>> = flow {
        val uid = requireUserId()
        val path = "kas_periods"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(KasPeriodDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.associateBy { it.name }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeKasTransactions(): Flow<List<KasTransaction>> = flow {
        val uid = requireUserId()
        val path = "kas_transactions"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(KasTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.filter { !it.voided }
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeDokumentasi(): Flow<List<DokumentasiPost>> = flow {
        val uid = requireUserId()
        val path = "dokumentasi"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(DokumentasiPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeDokumentasiComments(dokId: String): Flow<List<DokumentasiComment>> = flow {
        val uid = requireUserId()
        val path = "dokumentasi/$dokId/comments"
        emitAll(
            db.collection("dokumentasi").document(dokId).collection("comments")
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(DokumentasiComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeMateri(): Flow<List<MateriItem>> = flow {
        val uid = requireUserId()
        val path = "materi"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(MateriItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    fun observeClassNotifications(): Flow<List<ClassNotification>> = flow {
        val uid = requireUserId()
        val path = "class_notifications"
        emitAll(
            db.collection(path)
                .whereEqualTo("classId", CLASS_ID)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc ->
                        doc.toObject(ClassNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .handleListenerErrors(uid, path, OperationType.LIST)
        )
    }

    suspend fun emitFriendNotification(
        title: String,
        body: String,
        senderName: String,
        category: String,
        targetTab: String
    ): Result<Unit> {
        var docPath = "class_notifications"
        return try {
            val uid = requireUserId()
            ensureBootstrapData()
            val ref = db.collection("class_notifications").document()
            docPath = ref.path
            val payload = mapOf(
                "classId" to CLASS_ID,
                "userId" to uid,
                "senderName" to senderName.trim().take(60).ifBlank { currentDisplayName() },
                "title" to title.trim().take(140).ifBlank { "Pembaruan CLASS XI" },
                "body" to body.trim().take(350).ifBlank { "Ada pembaruan baru di portal CLASS XI." },
                "category" to category.takeIf { it in setOf("info", "izin", "materi", "dokumentasi", "kas") }.orEmpty().ifBlank { "info" },
                "targetTab" to targetTab.trim().take(40).ifBlank { "info" },
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- ATTENDANCE MUTATIONS ---

    private fun defaultDayRecordsMap(): MutableMap<String, Map<String, Any>> {
        val map = mutableMapOf<String, Map<String, Any>>()
        ClassConstants.STUDENTS.forEach { st ->
            map[st.id.toString()] = mapOf(
                "status" to AttendanceStatus.Hadir.label,
                "scanTime" to ""
            )
        }
        return map
    }

    suspend fun updateStudentAttendance(
        dateKey: String,
        studentId: Int,
        newStatus: AttendanceStatus,
        scanTimeOverride: String? = null,
        updatedByRole: String
    ): Result<Unit> {
        val safeDateId = sanitizeDocId(dateKey)
        val docPath = "attendance_days/$safeDateId"
        return try {
            val uid = requireUserId()
            require(studentId in 1..47) { "Nomor absen tidak valid" }
            ensureBootstrapData()
            val docRef = db.collection("attendance_days").document(safeDateId)
            val snap = docRef.get().await()

            val currentRecords: MutableMap<String, Map<String, Any>> = if (snap.exists()) {
                val parsed = snap.toObject(AttendanceDayDoc::class.java)?.records ?: emptyMap()
                val mutable = mutableMapOf<String, Map<String, Any>>()
                ClassConstants.STUDENTS.forEach { st ->
                    val existing = parsed[st.id.toString()]
                    mutable[st.id.toString()] = mapOf(
                        "status" to (existing?.status ?: AttendanceStatus.Hadir.label),
                        "scanTime" to (existing?.scanTime ?: "")
                    )
                }
                mutable
            } else {
                defaultDayRecordsMap()
            }

            val prevScan = (currentRecords[studentId.toString()]?.get("scanTime") as? String).orEmpty()
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val resolvedScanTime = when {
                newStatus != AttendanceStatus.Hadir -> ""
                !scanTimeOverride.isNullOrBlank() -> scanTimeOverride
                prevScan.isNotBlank() -> prevScan
                else -> nowTime
            }

            currentRecords[studentId.toString()] = mapOf(
                "status" to newStatus.label,
                "scanTime" to resolvedScanTime
            )

            val editor = updatedByRole.ifBlank { currentDisplayName() }.take(60)
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "records" to currentRecords,
                        "lastUpdatedBy" to editor,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "dateKey" to safeDateId,
                        "records" to currentRecords,
                        "lastUpdatedBy" to editor,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setAllAttendance(
        dateKey: String,
        status: AttendanceStatus,
        updatedByRole: String
    ): Result<Unit> {
        val safeDateId = sanitizeDocId(dateKey)
        val docPath = "attendance_days/$safeDateId"
        return try {
            val uid = requireUserId()
            ensureBootstrapData()
            val docRef = db.collection("attendance_days").document(safeDateId)
            val snap = docRef.get().await()
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val existingParsed = if (snap.exists()) {
                snap.toObject(AttendanceDayDoc::class.java)?.records ?: emptyMap()
            } else emptyMap()

            val updatedRecords = mutableMapOf<String, Map<String, Any>>()
            ClassConstants.STUDENTS.forEach { st ->
                val prevScan = existingParsed[st.id.toString()]?.scanTime.orEmpty()
                val resolvedTime = if (status == AttendanceStatus.Hadir) prevScan.ifBlank { nowTime } else ""
                updatedRecords[st.id.toString()] = mapOf(
                    "status" to status.label,
                    "scanTime" to resolvedTime
                )
            }

            val editor = updatedByRole.ifBlank { currentDisplayName() }.take(60)
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "records" to updatedRecords,
                        "lastUpdatedBy" to editor,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "dateKey" to safeDateId,
                        "records" to updatedRecords,
                        "lastUpdatedBy" to editor,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- ANNOUNCEMENTS + FRIEND NOTIFICATION ---

    suspend fun addAnnouncement(
        title: String,
        category: String,
        deadline: String,
        desc: String,
        imageBase64: String?,
        author: String
    ): Result<Unit> {
        var docPath = "announcements"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanDesc = desc.trim().take(1000)
            val cleanDeadline = deadline.trim().ifBlank { "Hari Ini" }.take(60)
            val cleanAuthor = author.trim().ifBlank { "Pengurus CLASS XI" }.take(60)
            val cleanCat = if (category in listOf("Penting", "Tugas", "Event")) category else "Penting"
            require(cleanTitle.isNotEmpty() && cleanDesc.isNotEmpty()) { "Judul dan isi pengumuman wajib diisi." }

            ensureBootstrapData()
            val ref = db.collection("announcements").document()
            docPath = ref.path
            val dateLabel = SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID")).format(Date())

            val payload = mutableMapOf<String, Any?>(
                "classId" to CLASS_ID,
                "userId" to uid,
                "title" to cleanTitle,
                "category" to cleanCat,
                "deadline" to cleanDeadline,
                "desc" to cleanDesc,
                "imageBase64" to imageBase64?.takeIf { it.isNotBlank() && it.length <= 450000 },
                "author" to cleanAuthor,
                "dateLabel" to dateLabel,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            ref.set(payload).await()
            emitFriendNotification(
                title = "[$cleanCat] $cleanTitle",
                body = "$cleanDesc (Tenggat: $cleanDeadline)",
                senderName = cleanAuthor,
                category = "info",
                targetTab = "info"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val docPath = "announcements/$announcementId"
        return try {
            requireUserId()
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- LEAVE REQUESTS (SURAT IZIN & SAKIT) ---

    suspend fun submitLeaveRequest(
        studentId: Int,
        date: String,
        type: String,
        reason: String,
        gformConfirmed: Boolean
    ): Result<Unit> {
        var docPath = "leave_requests"
        return try {
            val uid = requireUserId()
            val student = ClassConstants.STUDENTS.getOrNull(studentId - 1)
                ?: throw IllegalArgumentException("Siswa tidak ditemukan")
            val cleanReason = reason.trim().take(200)
            require(cleanReason.isNotEmpty()) { "Alasan izin/sakit wajib diisi." }
            val cleanType = if (type == "Izin") "Izin" else "Sakit"
            val cleanDate = date.trim().take(20)
            val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            ensureBootstrapData()
            val ref = db.collection("leave_requests").document()
            docPath = ref.path
            val payload = mapOf(
                "classId" to CLASS_ID,
                "userId" to uid,
                "studentId" to student.id,
                "noAbsen" to student.noAbsen,
                "studentName" to student.name,
                "piket" to student.piket,
                "date" to cleanDate,
                "type" to cleanType,
                "reason" to cleanReason,
                "gformConfirmed" to gformConfirmed,
                "status" to "Menunggu",
                "submittedAt" to "$cleanDate • $timeLabel WIB",
                "verifiedBy" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
            emitFriendNotification(
                title = "Surat $cleanType Baru",
                body = "${student.noAbsen}. ${student.name} mengajukan $cleanType ($cleanDate): $cleanReason",
                senderName = student.name,
                category = "izin",
                targetTab = "izin"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyLeaveRequest(
        leave: LeaveRequest,
        approved: Boolean,
        verifiedByRole: String
    ): Result<Unit> {
        val docPath = "leave_requests/${leave.id}"
        return try {
            requireUserId()
            val newStatus = if (approved) "Disetujui" else "Ditolak"
            val verifier = verifiedByRole.ifBlank { currentDisplayName() }.take(60)
            db.collection("leave_requests").document(leave.id).update(
                mapOf(
                    "status" to newStatus,
                    "verifiedBy" to verifier,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (approved) {
                val attStatus = if (leave.type == "Izin") AttendanceStatus.Izin else AttendanceStatus.Sakit
                updateStudentAttendance(
                    dateKey = leave.date,
                    studentId = leave.studentId,
                    newStatus = attStatus,
                    scanTimeOverride = "",
                    updatedByRole = verifier
                )
            }

            emitFriendNotification(
                title = "Surat ${leave.type} $newStatus",
                body = "Pengajuan ${leave.studentName} (${leave.date}) telah $newStatus oleh $verifier.",
                senderName = verifier,
                category = "izin",
                targetTab = "izin"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteLeaveRequest(leaveId: String): Result<Unit> {
        val docPath = "leave_requests/$leaveId"
        return try {
            requireUserId()
            db.collection("leave_requests").document(leaveId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- UANG KAS KELAS ---

    suspend fun addKasPeriod(periodName: String): Result<Unit> {
        val cleanName = periodName.trim().take(80)
        val docPath = "classes/$CLASS_ID"
        return try {
            val uid = requireUserId()
            require(cleanName.isNotEmpty()) { "Nama periode wajib diisi." }
            ensureBootstrapData()

            val classRef = db.collection("classes").document(CLASS_ID)
            val snap = classRef.get().await()
            val existingPeriods = snap.toObject(ClassConfig::class.java)?.kasPeriods ?: ClassConstants.DEFAULT_KAS_PERIODS
            if (!existingPeriods.contains(cleanName)) {
                val updatedPeriods = (listOf(cleanName) + existingPeriods).take(52)
                classRef.update(
                    mapOf(
                        "kasPeriods" to updatedPeriods,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val periodId = sanitizeDocId(cleanName)
            val periodRef = db.collection("kas_periods").document(periodId)
            if (!periodRef.get().await().exists()) {
                periodRef.set(
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "name" to cleanName,
                        "payments" to emptyMap<String, Any>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleStudentKasPayment(
        periodName: String,
        studentId: Int,
        nominal: Long,
        existingEntry: KasPaymentEntry?,
        byRole: String
    ): Result<Boolean> {
        val periodId = sanitizeDocId(periodName)
        val periodPath = "kas_periods/$periodId"
        return try {
            val uid = requireUserId()
            val student = ClassConstants.STUDENTS.getOrNull(studentId - 1)
                ?: throw IllegalArgumentException("Siswa tidak ditemukan")
            ensureBootstrapData()

            val periodRef = db.collection("kas_periods").document(periodId)
            val periodSnap = periodRef.get().await()
            val existingPayments = periodSnap.toObject(KasPeriodDoc::class.java)?.payments ?: emptyMap()

            val updatedPayments = existingPayments.mapValues { (_, entry) ->
                mapOf(
                    "paid" to entry.paid,
                    "amount" to entry.amount,
                    "date" to entry.date,
                    "by" to entry.by,
                    "txId" to entry.txId
                )
            }.toMutableMap()

            val actor = byRole.ifBlank { "Bendahara" }.take(60)
            val nowPaid: Boolean
            if (existingEntry != null && existingEntry.paid) {
                updatedPayments.remove(studentId.toString())
                if (existingEntry.txId.isNotBlank()) {
                    val txRef = db.collection("kas_transactions").document(existingEntry.txId)
                    val txSnap = txRef.get().await()
                    if (txSnap.exists() && txSnap.getBoolean("voided") != true) {
                        txRef.update(
                            mapOf(
                                "voided" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
                nowPaid = false
            } else {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val txRef = db.collection("kas_transactions").document()
                val txId = txRef.id
                txRef.set(
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "type" to "Pemasukan",
                        "amount" to nominal,
                        "date" to todayStr,
                        "category" to "Iuran Kas Siswa",
                        "note" to "Pembayaran Kas ($periodName) — ${student.noAbsen}. ${student.name}".take(250),
                        "author" to actor,
                        "studentId" to student.id,
                        "period" to periodName.take(80),
                        "voided" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                updatedPayments[studentId.toString()] = mapOf(
                    "paid" to true,
                    "amount" to nominal,
                    "date" to todayStr,
                    "by" to actor,
                    "txId" to txId
                )
                nowPaid = true
            }

            if (periodSnap.exists()) {
                periodRef.update(
                    mapOf(
                        "payments" to updatedPayments,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                periodRef.set(
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "name" to periodName.take(80),
                        "payments" to updatedPayments,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(nowPaid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, periodPath)
            Result.failure(e)
        }
    }

    suspend fun markAllKasPaid(
        periodName: String,
        nominal: Long,
        currentPayments: Map<String, KasPaymentEntry>,
        byRole: String
    ): Result<Int> {
        val periodId = sanitizeDocId(periodName)
        val periodPath = "kas_periods/$periodId"
        return try {
            val uid = requireUserId()
            ensureBootstrapData()
            val actor = byRole.ifBlank { "Bendahara" }.take(60)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val updatedPayments = currentPayments.mapValues { (_, entry) ->
                mapOf(
                    "paid" to entry.paid,
                    "amount" to entry.amount,
                    "date" to entry.date,
                    "by" to entry.by,
                    "txId" to entry.txId
                )
            }.toMutableMap()

            val batch = db.batch()
            var count = 0
            ClassConstants.STUDENTS.forEach { student ->
                val existing = currentPayments[student.id.toString()]
                if (existing == null || !existing.paid) {
                    val txRef = db.collection("kas_transactions").document()
                    batch.set(
                        txRef,
                        mapOf(
                            "classId" to CLASS_ID,
                            "userId" to uid,
                            "type" to "Pemasukan",
                            "amount" to nominal,
                            "date" to todayStr,
                            "category" to "Iuran Kas Siswa",
                            "note" to "Pembayaran Kas ($periodName) — ${student.noAbsen}. ${student.name}".take(250),
                            "author" to actor,
                            "studentId" to student.id,
                            "period" to periodName.take(80),
                            "voided" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    updatedPayments[student.id.toString()] = mapOf(
                        "paid" to true,
                        "amount" to nominal,
                        "date" to todayStr,
                        "by" to actor,
                        "txId" to txRef.id
                    )
                    count++
                }
            }

            if (count == 0) return Result.success(0)

            val periodRef = db.collection("kas_periods").document(periodId)
            val periodSnap = periodRef.get().await()
            if (periodSnap.exists()) {
                batch.update(
                    periodRef,
                    mapOf(
                        "payments" to updatedPayments,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    periodRef,
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "name" to periodName.take(80),
                        "payments" to updatedPayments,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, periodPath)
            Result.failure(e)
        }
    }

    suspend fun addKasTransaction(
        type: String,
        amount: Long,
        date: String,
        category: String,
        note: String,
        author: String
    ): Result<Unit> {
        var docPath = "kas_transactions"
        return try {
            val uid = requireUserId()
            require(amount in 1..100_000_000L) { "Nominal transaksi harus antara Rp 1 dan Rp 100.000.000." }
            val cleanNote = note.trim().take(250)
            require(cleanNote.isNotEmpty()) { "Keterangan transaksi wajib diisi." }
            val cleanType = if (type == "Pengeluaran") "Pengeluaran" else "Pemasukan"

            ensureBootstrapData()
            val ref = db.collection("kas_transactions").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "classId" to CLASS_ID,
                    "userId" to uid,
                    "type" to cleanType,
                    "amount" to amount,
                    "date" to date.trim().take(20),
                    "category" to category.trim().take(80),
                    "note" to cleanNote,
                    "author" to author.trim().ifBlank { "Bendahara" }.take(60),
                    "voided" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun voidOrDeleteKasTransaction(tx: KasTransaction): Result<Unit> {
        val docPath = "kas_transactions/${tx.id}"
        return try {
            val uid = requireUserId()
            if (tx.studentId != null && !tx.period.isNullOrBlank()) {
                val periodId = sanitizeDocId(tx.period)
                val periodRef = db.collection("kas_periods").document(periodId)
                val periodSnap = periodRef.get().await()
                if (periodSnap.exists()) {
                    val existingPayments = periodSnap.toObject(KasPeriodDoc::class.java)?.payments ?: emptyMap()
                    val updated = existingPayments.filterKeys { it != tx.studentId.toString() }
                        .mapValues { (_, entry) ->
                            mapOf(
                                "paid" to entry.paid,
                                "amount" to entry.amount,
                                "date" to entry.date,
                                "by" to entry.by,
                                "txId" to entry.txId
                            )
                        }
                    periodRef.update(
                        mapOf(
                            "payments" to updated,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            if (tx.userId == uid) {
                db.collection("kas_transactions").document(tx.id).delete().await()
            } else {
                db.collection("kas_transactions").document(tx.id).update(
                    mapOf(
                        "voided" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- DOKUMENTASI GALERI ---

    suspend fun addDokumentasiPost(
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String,
        fileName: String,
        caption: String,
        date: String,
        author: String
    ): Result<Unit> {
        var docPath = "dokumentasi"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty() && mediaUrl.isNotBlank()) { "Judul dan media dokumentasi wajib diisi." }
            require(mediaUrl.length <= 450_000) { "Ukuran media terlalu besar untuk penyimpanan cloud." }

            ensureBootstrapData()
            val ref = db.collection("dokumentasi").document()
            docPath = ref.path
            val cleanAuthor = author.trim().ifBlank { "Pengurus CLASS XI" }.take(60)
            ref.set(
                mapOf(
                    "classId" to CLASS_ID,
                    "userId" to uid,
                    "title" to cleanTitle,
                    "category" to category.trim().ifBlank { "Kegiatan Kelas" }.take(40),
                    "mediaType" to if (mediaType == "video") "video" else "photo",
                    "mediaUrl" to mediaUrl.trim(),
                    "fileName" to fileName.trim().ifBlank { "Dokumentasi_CLASS_XI.jpg" }.take(120),
                    "caption" to caption.trim().take(500),
                    "author" to cleanAuthor,
                    "date" to date.trim().take(20),
                    "likesCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            emitFriendNotification(
                title = "Dokumentasi Baru: $cleanTitle",
                body = caption.trim().ifBlank { "Momen kegiatan kelas baru saja diunggah ke Galeri." }.take(200),
                senderName = cleanAuthor,
                category = "dokumentasi",
                targetTab = "dokumentasi"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun isDokumentasiLikedByMe(dokId: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            db.collection("dokumentasi").document(dokId)
                .collection("likes").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun toggleDokumentasiLike(post: DokumentasiPost): Result<Boolean> {
        val docPath = "dokumentasi/${post.id}"
        return try {
            val uid = requireUserId()
            val postRef = db.collection("dokumentasi").document(post.id)
            val likeRef = postRef.collection("likes").document(uid)
            val currentlyLiked = likeRef.get().await().exists()

            val batch = db.batch()
            if (currentlyLiked) {
                val nextCount = (post.likesCount - 1L).coerceAtLeast(0L)
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to nextCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val nextCount = post.likesCount + 1L
                batch.set(
                    likeRef,
                    mapOf(
                        "classId" to CLASS_ID,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to nextCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(!currentlyLiked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addDokumentasiComment(
        dokId: String,
        senderName: String,
        text: String
    ): Result<Unit> {
        var docPath = "dokumentasi/$dokId/comments"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(400)
            require(cleanText.isNotEmpty()) { "Komentar tidak boleh kosong." }
            val cleanSender = senderName.trim().ifBlank { currentDisplayName() }.take(60)
            val timeLabel = SimpleDateFormat("dd MMM • HH:mm", Locale("id", "ID")).format(Date())

            val ref = db.collection("dokumentasi").document(dokId).collection("comments").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "classId" to CLASS_ID,
                    "dokId" to dokId,
                    "userId" to uid,
                    "senderName" to cleanSender,
                    "text" to cleanText,
                    "timeLabel" to timeLabel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDokumentasiComment(dokId: String, commentId: String): Result<Unit> {
        val docPath = "dokumentasi/$dokId/comments/$commentId"
        return try {
            requireUserId()
            db.collection("dokumentasi").document(dokId).collection("comments").document(commentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDokumentasiPost(dokId: String): Result<Unit> {
        val docPath = "dokumentasi/$dokId"
        return try {
            requireUserId()
            db.collection("dokumentasi").document(dokId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- BANK MATERI (MODE GURU) ---

    suspend fun addMateri(
        title: String,
        guru: String,
        mapel: String,
        desc: String,
        fileName: String,
        fileData: String?,
        imgData: String?
    ): Result<Unit> {
        var docPath = "materi"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            val cleanGuru = guru.trim().take(60)
            val cleanMapel = mapel.trim().take(60)
            require(cleanTitle.isNotEmpty() && cleanGuru.isNotEmpty()) { "Judul dan nama guru wajib diisi." }

            ensureBootstrapData()
            val ref = db.collection("materi").document()
            docPath = ref.path
            val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())

            val payload = mutableMapOf<String, Any?>(
                "classId" to CLASS_ID,
                "userId" to uid,
                "title" to cleanTitle,
                "guru" to cleanGuru,
                "mapel" to cleanMapel,
                "desc" to desc.trim().take(1000),
                "dateLabel" to dateLabel,
                "dl" to 0L,
                "fileName" to fileName.trim().take(160),
                "fileData" to fileData?.takeIf { it.isNotBlank() && it.length <= 450_000 },
                "imgData" to imgData?.takeIf { it.isNotBlank() && it.length <= 350_000 },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            ref.set(payload).await()
            emitFriendNotification(
                title = "Materi Baru ($cleanMapel)",
                body = "$cleanTitle — oleh $cleanGuru",
                senderName = cleanGuru,
                category = "materi",
                targetTab = "materi"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun incrementMateriDownload(materi: MateriItem): Result<Unit> {
        val docPath = "materi/${materi.id}"
        return try {
            requireUserId()
            db.collection("materi").document(materi.id).update(
                mapOf(
                    "dl" to (materi.dl + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMateri(materiId: String): Result<Unit> {
        val docPath = "materi/$materiId"
        return try {
            requireUserId()
            db.collection("materi").document(materiId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
