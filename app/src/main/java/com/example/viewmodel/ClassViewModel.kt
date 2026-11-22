package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.ClassRepository
import com.example.data.PreferencesRepository
import com.example.data.ThemeMode
import com.example.data.ThemeStyle
import com.example.data.friendlyFirestoreErrorMessage
import com.example.model.Announcement
import com.example.model.AttendanceDayDoc
import com.example.model.AttendanceStatus
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
import com.example.model.RekapRow
import com.example.model.ResolvedStudentAttendance
import com.example.model.ScheduleDay
import com.example.model.UiState
import com.example.notifications.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ClassPortalData(
    val classConfig: ClassConfig? = null,
    val attendanceDays: Map<String, AttendanceDayDoc> = emptyMap(),
    val announcements: List<Announcement> = emptyList(),
    val leaveRequests: List<LeaveRequest> = emptyList(),
    val kasPeriods: Map<String, KasPeriodDoc> = emptyMap(),
    val kasTransactions: List<KasTransaction> = emptyList(),
    val dokumentasi: List<DokumentasiPost> = emptyList(),
    val materi: List<MateriItem> = emptyList(),
    val notifications: List<ClassNotification> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val selectedKasPeriod: String = ClassConstants.DEFAULT_KAS_PERIODS.first(),
    val selectedKasNominal: Long = 5000L,
    val selectedScheduleDay: ScheduleDay = ScheduleDay.fromTodayCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)),
    val globalSearch: String = "",
    val likedDokIds: Set<String> = emptySet(),
    val isOnlineSynced: Boolean = true
) {
    val activePengurusRole: String get() = preferences.activePengurusRole
    val isAdminMode: Boolean get() = activePengurusRole.isNotBlank()
    val isGuruMode: Boolean get() = preferences.guruModeActive
    val kasPeriodNames: List<String>
        get() = classConfig?.kasPeriods?.takeIf { it.isNotEmpty() } ?: ClassConstants.DEFAULT_KAS_PERIODS

    val pendingLeaveCount: Int
        get() = leaveRequests.count { it.status == "Menunggu" }

    fun attendanceForDate(dateKey: String = selectedDate): List<ResolvedStudentAttendance> {
        val dayRecords = attendanceDays[dateKey]?.records ?: emptyMap()
        return ClassConstants.STUDENTS.map { student ->
            val rec = dayRecords[student.id.toString()]
            val approvedLeave = leaveRequests.firstOrNull {
                it.studentId == student.id && it.date == dateKey && it.status == "Disetujui"
            }
            ResolvedStudentAttendance(
                student = student,
                status = AttendanceStatus.fromRaw(rec?.status),
                scanTime = rec?.scanTime.orEmpty(),
                approvedLeaveType = approvedLeave?.type
            )
        }
    }

    fun kasSummaryForPeriod(periodName: String = selectedKasPeriod): KasSummary {
        val activeTx = kasTransactions.filter { !it.voided }
        val totalIn = activeTx.filter { it.type == "Pemasukan" }.sumOf { it.amount }
        val totalOut = activeTx.filter { it.type == "Pengeluaran" }.sumOf { it.amount }
        val saldo = totalIn - totalOut
        val payments = kasPeriods[periodName]?.payments ?: emptyMap()
        val paidCount = ClassConstants.STUDENTS.count { payments[it.id.toString()]?.paid == true }
        val unpaidCount = ClassConstants.STUDENTS.size - paidCount
        val percent = Math.round((paidCount.toFloat() / ClassConstants.STUDENTS.size) * 100f)
        return KasSummary(totalIn, totalOut, saldo, paidCount, unpaidCount, percent, payments)
    }

    fun buildRekap(monthFilter: String): Pair<Int, List<RekapRow>> {
        val matchedDates = attendanceDays.keys.filter { key ->
            monthFilter == "YEARLY" || key.startsWith(monthFilter)
        }
        val effectiveDays = matchedDates.size.coerceAtLeast(1)
        val rows = ClassConstants.STUDENTS.map { student ->
            var h = 0; var i = 0; var s = 0; var d = 0; var a = 0
            matchedDates.forEach { dateKey ->
                val rawStatus = attendanceDays[dateKey]?.records?.get(student.id.toString())?.status
                when (AttendanceStatus.fromRaw(rawStatus)) {
                    AttendanceStatus.Hadir -> h++
                    AttendanceStatus.Izin -> i++
                    AttendanceStatus.Sakit -> s++
                    AttendanceStatus.Dispensasi -> d++
                    AttendanceStatus.Alpa -> a++
                }
            }
            val pct = Math.round(((h + d).toFloat() / effectiveDays) * 100f)
            RekapRow(student, h, i, s, d, a, pct)
        }
        return effectiveDays to rows
    }
}

data class KasSummary(
    val totalIn: Long,
    val totalOut: Long,
    val saldo: Long,
    val paidCount: Int,
    val unpaidCount: Int,
    val paidPercent: Int,
    val payments: Map<String, KasPaymentEntry>
)

sealed interface ToastEvent {
    data class Message(val text: String, val isSuccess: Boolean = false, val isError: Boolean = false) : ToastEvent
}

class ClassViewModel(
    private val appContext: Context,
    private val repository: ClassRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    private val selectedKasPeriod = MutableStateFlow(ClassConstants.DEFAULT_KAS_PERIODS.first())
    private val selectedKasNominal = MutableStateFlow(5000L)
    private val selectedScheduleDay = MutableStateFlow(
        ScheduleDay.fromTodayCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    )
    private val globalSearch = MutableStateFlow("")
    private val likedDokIds = MutableStateFlow<Set<String>>(emptySet())

    private val _toastEvents = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 16)
    val toastEvents = _toastEvents.asSharedFlow()

    private val notifiedIds = mutableSetOf<String>()

    private data class CoreStreams(
        val config: ClassConfig?,
        val attendance: Map<String, AttendanceDayDoc>,
        val announcements: List<Announcement>,
        val leaves: List<LeaveRequest>,
        val kasPeriods: Map<String, KasPeriodDoc>
    )

    private data class MediaStreams(
        val kasTx: List<KasTransaction>,
        val docs: List<DokumentasiPost>,
        val materi: List<MateriItem>,
        val notifs: List<ClassNotification>,
        val prefs: AppPreferences
    )

    private data class LocalUiFilters(
        val date: String,
        val kasPeriod: String,
        val kasNominal: Long,
        val scheduleDay: ScheduleDay,
        val search: String,
        val likedIds: Set<String>
    )

    private val coreFlow: Flow<CoreStreams> = combine(
        repository.observeClassConfig(),
        repository.observeAttendanceDays(),
        repository.observeAnnouncements(),
        repository.observeLeaveRequests(),
        repository.observeKasPeriods()
    ) { cfg, att, ann, leaves, kasP ->
        CoreStreams(cfg, att, ann, leaves, kasP)
    }

    private val mediaFlow: Flow<MediaStreams> = combine(
        repository.observeKasTransactions(),
        repository.observeDokumentasi(),
        repository.observeMateri(),
        repository.observeClassNotifications(),
        prefsRepo.preferencesFlow
    ) { tx, docs, mat, notifs, prefs ->
        MediaStreams(tx, docs, mat, notifs, prefs)
    }

    private val filterFlow: Flow<LocalUiFilters> = combine(
        selectedDate,
        selectedKasPeriod,
        selectedKasNominal,
        selectedScheduleDay,
        globalSearch
    ) { d, kp, kn, sd, q ->
        LocalUiFilters(d, kp, kn, sd, q, emptySet())
    }.combine(likedDokIds) { f, liked -> f.copy(likedIds = liked) }

    val uiState: StateFlow<UiState<ClassPortalData>> = combine(
        coreFlow,
        mediaFlow,
        filterFlow
    ) { core, media, filters ->
        val validPeriods = core.config?.kasPeriods?.takeIf { it.isNotEmpty() } ?: ClassConstants.DEFAULT_KAS_PERIODS
        val activePeriod = if (validPeriods.contains(filters.kasPeriod)) filters.kasPeriod else validPeriods.first()
        ClassPortalData(
            classConfig = core.config,
            attendanceDays = core.attendance,
            announcements = core.announcements,
            leaveRequests = core.leaves,
            kasPeriods = core.kasPeriods,
            kasTransactions = media.kasTx,
            dokumentasi = media.docs,
            materi = media.materi,
            notifications = media.notifs,
            preferences = media.prefs,
            selectedDate = filters.date,
            selectedKasPeriod = activePeriod,
            selectedKasNominal = filters.kasNominal,
            selectedScheduleDay = filters.scheduleDay,
            globalSearch = filters.search,
            likedDokIds = filters.likedIds,
            isOnlineSynced = true
        )
    }
        .map<ClassPortalData, UiState<ClassPortalData>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error(friendlyFirestoreErrorMessage(firestoreError ?: error, "Gagal memuat data CLASS XI")))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureBootstrapData()
        }
        // Listen for incoming friend notifications while app is open
        viewModelScope.launch {
            val uid = Firebase.auth.currentUser?.uid ?: return@launch
            var initialLastSeen = prefsRepo.getLastSeenNotificationMs()
            if (initialLastSeen == 0L) {
                initialLastSeen = System.currentTimeMillis() - 60_000L
                prefsRepo.updateLastSeenNotificationMs(initialLastSeen)
            }
            repository.observeClassNotifications()
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                }
                .collect { notifs ->
                    val enabled = prefsRepo.isNotificationsEnabled()
                    if (!enabled) return@collect
                    val currentLastSeen = prefsRepo.getLastSeenNotificationMs()
                    var newestMs = currentLastSeen
                    notifs.filter { n ->
                        val ms = n.createdAt?.toDate()?.time ?: 0L
                        ms > currentLastSeen && n.userId != uid && !notifiedIds.contains(n.id)
                    }.sortedBy { it.createdAt?.toDate()?.time ?: 0L }.forEach { n ->
                        val ms = n.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        if (ms > newestMs) newestMs = ms
                        notifiedIds.add(n.id)
                        NotificationHelper.showClassNotification(
                            context = appContext,
                            notifId = (n.id.hashCode() and 0x7FFFFFFF),
                            title = n.title,
                            body = n.body,
                            senderName = n.senderName,
                            targetTab = n.targetTab
                        )
                    }
                    if (newestMs > currentLastSeen) {
                        prefsRepo.updateLastSeenNotificationMs(newestMs)
                    }
                }
        }
        // Sync initial liked state for existing dokumentasi posts
        viewModelScope.launch {
            repository.observeDokumentasi()
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                }
                .collect { posts ->
                    val liked = mutableSetOf<String>()
                    posts.forEach { p ->
                        if (repository.isDokumentasiLikedByMe(p.id)) {
                            liked.add(p.id)
                        }
                    }
                    likedDokIds.value = liked
                }
        }
    }

    fun observeComments(dokId: String): Flow<List<DokumentasiComment>> {
        return repository.observeDokumentasiComments(dokId)
            .catch { error ->
                val fe = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fe == null && error is CancellationException) throw error
                emit(emptyList())
            }
    }

    private fun emitToast(msg: String, success: Boolean = false, error: Boolean = false) {
        _toastEvents.tryEmit(ToastEvent.Message(msg, isSuccess = success, isError = error))
    }

    fun setGlobalSearch(q: String) { globalSearch.value = q }
    fun setSelectedDate(dateKey: String) { selectedDate.value = dateKey }
    fun setTodayDate() { selectedDate.value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    fun setSelectedKasPeriod(period: String) { selectedKasPeriod.value = period }
    fun setSelectedKasNominal(nominal: Long) { selectedKasNominal.value = nominal }
    fun setSelectedScheduleDay(day: ScheduleDay) { selectedScheduleDay.value = day }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setThemeStyle(style: ThemeStyle) {
        viewModelScope.launch { prefsRepo.setThemeStyle(style) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setNotificationsEnabled(enabled)
            emitToast(
                if (enabled) "Notifikasi real-time teman diaktifkan." else "Notifikasi perangkat dinonaktifkan.",
                success = enabled
            )
        }
    }

    fun sendTestFriendNotification() {
        val currentState = (uiState.value as? UiState.Success)?.data
        val sender = currentState?.activePengurusRole?.ifBlank { null }
            ?: Firebase.auth.currentUser?.displayName
            ?: "Teman CLASS XI"
        NotificationHelper.showClassNotification(
            context = appContext,
            notifId = (System.currentTimeMillis() % 100000).toInt(),
            title = "Tes Notifikasi CLASS XI",
            body = "Notifikasi perangkat aktif! Info & surat izin dari teman akan langsung muncul di sini.",
            senderName = sender,
            targetTab = "info"
        )
        emitToast("Notifikasi percobaan dikirim ke bilah notifikasi.", success = true)
    }

    fun verifyAdminPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        val matched = ClassConstants.ROLE_PINS.entries.firstOrNull { it.value == cleanPin }?.key
        return if (matched != null) {
            viewModelScope.launch { prefsRepo.setPengurusRole(matched) }
            emitToast("Login berhasil sebagai $matched.", success = true)
            true
        } else {
            emitToast("PIN tidak cocok dengan akun pengurus.", error = true)
            false
        }
    }

    fun exitAdminMode() {
        viewModelScope.launch {
            prefsRepo.setPengurusRole("")
            emitToast("Keluar dari Mode Pengurus.")
        }
    }

    fun verifyGuruPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        return if (cleanPin in ClassConstants.GURU_PINS) {
            viewModelScope.launch { prefsRepo.setGuruMode(true) }
            emitToast("Mode Guru aktif.", success = true)
            true
        } else {
            emitToast("PIN guru tidak cocok.", error = true)
            false
        }
    }

    fun exitGuruMode() {
        viewModelScope.launch {
            prefsRepo.setGuruMode(false)
            emitToast("Keluar dari Mode Guru.")
        }
    }

    fun updateStudentAttendance(studentId: Int, status: AttendanceStatus) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) {
            emitToast("Mode Siswa hanya lihat. Masuk Mode Pengurus untuk mengubah data.")
            return
        }
        viewModelScope.launch {
            val res = repository.updateStudentAttendance(
                dateKey = data.selectedDate,
                studentId = studentId,
                newStatus = status,
                updatedByRole = data.activePengurusRole
            )
            res.onFailure {
                emitToast(friendlyFirestoreErrorMessage(it, "Gagal memperbarui presensi."), error = true)
            }
        }
    }

    fun processScannedStudentQr(qrText: String): Pair<Boolean, String> {
        val data = (uiState.value as? UiState.Success)?.data
            ?: return false to "Data belum siap."
        if (!data.isAdminMode) {
            emitToast("Masuk Mode Pengurus terlebih dahulu untuk scan QR.", error = true)
            return false to "Perlu Mode Pengurus"
        }
        val parts = qrText.trim().split("_")
        val id = (parts.getOrNull(2) ?: parts.getOrNull(1) ?: qrText).toIntOrNull()
        val student = id?.let { ClassConstants.STUDENTS.getOrNull(it - 1) }
        return if (student != null) {
            updateStudentAttendance(student.id, AttendanceStatus.Hadir)
            val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val lateSuffix = if (nowTime > "13:00") " · Terlambat ($nowTime)" else ""
            val msg = "${student.noAbsen}. ${student.name} tercatat Hadir$lateSuffix."
            emitToast(msg, success = true)
            true to msg
        } else {
            emitToast("Kode QR tidak valid.", error = true)
            false to "QR tidak valid"
        }
    }

    fun setAllAttendance(status: AttendanceStatus) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) {
            emitToast("Masuk Mode Pengurus terlebih dahulu.")
            return
        }
        viewModelScope.launch {
            repository.setAllAttendance(data.selectedDate, status, data.activePengurusRole)
                .onSuccess { emitToast("Seluruh 47 siswa ditandai ${status.label}.", success = true) }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal memperbarui presensi massal."), error = true) }
        }
    }

    fun addAnnouncement(title: String, category: String, deadline: String, desc: String, imageBase64: String?) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) {
            emitToast("Masuk Mode Pengurus untuk mempublikasikan pengumuman.")
            return
        }
        viewModelScope.launch {
            repository.addAnnouncement(title, category, deadline, desc, imageBase64, data.activePengurusRole)
                .onSuccess { emitToast("Broadcast pengumuman & notifikasi terkirim ke teman CLASS XI!", success = true) }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal mengirim broadcast."), error = true) }
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
                .onSuccess { emitToast("Pengumuman dihapus.") }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Hanya pembuat pengumuman yang dapat menghapusnya."), error = true) }
        }
    }

    fun submitLeaveRequest(studentId: Int, date: String, type: String, reason: String, gformConfirmed: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.submitLeaveRequest(studentId, date, type, reason, gformConfirmed)
                .onSuccess {
                    emitToast("Pengajuan $type terkirim & notifikasi dikirim ke pengurus.", success = true)
                    onDone()
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal mengirim pengajuan surat."), error = true) }
        }
    }

    fun verifyLeaveRequest(leave: LeaveRequest, approved: Boolean) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) {
            emitToast("Masuk Mode Pengurus untuk memverifikasi surat.")
            return
        }
        viewModelScope.launch {
            repository.verifyLeaveRequest(leave, approved, data.activePengurusRole)
                .onSuccess {
                    val label = if (approved) "Disetujui & presensi otomatis diperbarui!" else "Pengajuan ditolak."
                    emitToast(label, success = approved)
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal memverifikasi surat."), error = true) }
        }
    }

    fun deleteLeaveRequest(leaveId: String) {
        viewModelScope.launch {
            repository.deleteLeaveRequest(leaveId)
                .onSuccess { emitToast("Riwayat pengajuan dihapus.") }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Hanya pengirim yang dapat menghapus riwayat."), error = true) }
        }
    }

    fun addKasPeriod(name: String, onDone: () -> Unit) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) return
        viewModelScope.launch {
            repository.addKasPeriod(name)
                .onSuccess {
                    selectedKasPeriod.value = name.trim()
                    emitToast("Periode kas \"$name\" berhasil ditambahkan.", success = true)
                    onDone()
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal menambah periode kas."), error = true) }
        }
    }

    fun toggleStudentKasPayment(studentId: Int) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) {
            emitToast("Masuk Mode Pengurus / Bendahara untuk mengubah pembayaran kas.")
            return
        }
        val periodName = data.selectedKasPeriod
        val existingEntry = data.kasPeriods[periodName]?.payments?.get(studentId.toString())
        val studentName = ClassConstants.STUDENTS.getOrNull(studentId - 1)?.name ?: "Siswa"
        viewModelScope.launch {
            repository.toggleStudentKasPayment(
                periodName = periodName,
                studentId = studentId,
                nominal = data.selectedKasNominal,
                existingEntry = existingEntry,
                byRole = data.activePengurusRole
            ).onSuccess { nowPaid ->
                emitToast(
                    if (nowPaid) "$studentName ditandai Lunas." else "Status kas $studentName dikembalikan menjadi Belum Bayar.",
                    success = nowPaid
                )
            }.onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal memperbarui pembayaran kas."), error = true) }
        }
    }

    fun markAllKasPaid() {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) return
        val periodName = data.selectedKasPeriod
        val currentPayments = data.kasPeriods[periodName]?.payments ?: emptyMap()
        viewModelScope.launch {
            repository.markAllKasPaid(periodName, data.selectedKasNominal, currentPayments, data.activePengurusRole)
                .onSuccess { count ->
                    if (count == 0) emitToast("Seluruh 47 siswa sudah lunas pada periode ini.")
                    else emitToast("$count siswa ditandai Lunas ($periodName).", success = true)
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal melunaskan kas."), error = true) }
        }
    }

    fun addKasTransaction(type: String, amount: Long, date: String, category: String, note: String, onDone: () -> Unit) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) return
        viewModelScope.launch {
            repository.addKasTransaction(type, amount, date, category, note, data.activePengurusRole)
                .onSuccess {
                    emitToast("$type kas berhasil dicatat.", success = true)
                    onDone()
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal mencatat transaksi."), error = true) }
        }
    }

    fun voidOrDeleteKasTransaction(tx: KasTransaction) {
        viewModelScope.launch {
            repository.voidOrDeleteKasTransaction(tx)
                .onSuccess { emitToast("Transaksi kas dihapus.") }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal menghapus transaksi."), error = true) }
        }
    }

    fun addDokumentasiPost(
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String,
        fileName: String,
        caption: String,
        date: String,
        onDone: () -> Unit
    ) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        if (!data.isAdminMode) return
        viewModelScope.launch {
            repository.addDokumentasiPost(
                title, category, mediaType, mediaUrl, fileName, caption, date, data.activePengurusRole
            ).onSuccess {
                emitToast("Dokumentasi dipublikasikan ke galeri CLASS XI!", success = true)
                onDone()
            }.onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal mengunggah dokumentasi."), error = true) }
        }
    }

    fun toggleDokumentasiLike(post: DokumentasiPost) {
        viewModelScope.launch {
            repository.toggleDokumentasiLike(post)
                .onSuccess { likedNow ->
                    likedDokIds.value = if (likedNow) likedDokIds.value + post.id else likedDokIds.value - post.id
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal menyukai postingan."), error = true) }
        }
    }

    fun addDokumentasiComment(dokId: String, customName: String, text: String, onDone: () -> Unit) {
        val data = (uiState.value as? UiState.Success)?.data ?: return
        val sender = if (data.isAdminMode) {
            "${data.activePengurusRole} (Pengurus)"
        } else {
            customName.trim().ifBlank {
                Firebase.auth.currentUser?.displayName ?: "Siswa CLASS XI"
            }
        }
        viewModelScope.launch {
            repository.addDokumentasiComment(dokId, sender, text)
                .onSuccess {
                    emitToast("Komentar ditambahkan.", success = true)
                    onDone()
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal mengirim komentar."), error = true) }
        }
    }

    fun deleteDokumentasiComment(dokId: String, commentId: String) {
        viewModelScope.launch {
            repository.deleteDokumentasiComment(dokId, commentId)
                .onSuccess { emitToast("Komentar dihapus.") }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Hanya pembuat komentar yang dapat menghapusnya."), error = true) }
        }
    }

    fun deleteDokumentasiPost(dokId: String) {
        viewModelScope.launch {
            repository.deleteDokumentasiPost(dokId)
                .onSuccess { emitToast("Postingan dihapus.") }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Hanya pengunggah yang dapat menghapus postingan."), error = true) }
        }
    }

    fun addMateri(
        title: String,
        guru: String,
        mapel: String,
        desc: String,
        fileName: String,
        fileData: String?,
        imgData: String?,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addMateri(title, guru, mapel, desc, fileName, fileData, imgData)
                .onSuccess {
                    emitToast("Materi berhasil dipublikasikan & notifikasi dikirim ke teman!", success = true)
                    onDone()
                }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Gagal mengunggah materi."), error = true) }
        }
    }

    fun incrementMateriDownload(materi: MateriItem) {
        viewModelScope.launch {
            repository.incrementMateriDownload(materi)
        }
    }

    fun deleteMateri(materiId: String) {
        viewModelScope.launch {
            repository.deleteMateri(materiId)
                .onSuccess { emitToast("Materi dihapus.") }
                .onFailure { emitToast(friendlyFirestoreErrorMessage(it, "Hanya guru pengunggah yang dapat menghapus materi."), error = true) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            return ClassViewModel(
                appContext = appCtx,
                repository = ClassRepository(appCtx),
                prefsRepo = PreferencesRepository(appCtx)
            ) as T
        }
    }
}
