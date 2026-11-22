package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val CLASS_ID = "class_xi_puspita"
const val JAM_MASUK_NORMAL = "13:00"
const val GOOGLE_FORM_IZIN_URL = "https://forms.gle/GSbesr2VFVgRu3pK7"

enum class AttendanceStatus(val label: String, val shortLabel: String) {
    Hadir("Hadir", "Hadir"),
    Izin("Izin", "Izin"),
    Sakit("Sakit", "Sakit"),
    Dispensasi("Dispensasi", "Dispen"),
    Alpa("Alpa", "Alpa");

    companion object {
        fun fromRaw(raw: String?): AttendanceStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true) } ?: Hadir
    }
}

enum class ScheduleDay(val key: String, val label: String, val shortLabel: String, val hours: String) {
    Senin("Senin", "Senin", "Sen", "13:00 - 17:00 WIB"),
    Selasa("Selasa", "Selasa", "Sel", "13:00 - 17:30 WIB"),
    Rabu("Rabu", "Rabu", "Rab", "13:00 - 17:30 WIB"),
    Kamis("Kamis", "Kamis", "Kam", "13:00 - 17:30 WIB"),
    Jumat("Jumat", "Jum'at", "Jum", "13:00 - 16:30 WIB"),
    Sabtu("Sabtu", "Sabtu", "Sab", "13:00 - 15:30 WIB");

    companion object {
        fun fromTodayCalendar(dayOfWeek: Int): ScheduleDay = when (dayOfWeek) {
            java.util.Calendar.TUESDAY -> Selasa
            java.util.Calendar.WEDNESDAY -> Rabu
            java.util.Calendar.THURSDAY -> Kamis
            java.util.Calendar.FRIDAY -> Jumat
            java.util.Calendar.SATURDAY -> Sabtu
            else -> Senin
        }
    }
}

data class StudentMaster(
    val id: Int,
    val noAbsen: String,
    val name: String,
    val piket: String
)

data class SubjectSchedule(
    val no: Int,
    val name: String,
    val time: String,
    val room: String
)

object ClassConstants {
    val ROLE_PINS: Map<String, String> = linkedMapOf(
        "Ketua Kelas" to "1310",
        "Sekretaris 1" to "4444",
        "Sekretaris 2" to "2727",
        "Bendahara1" to "4910",
        "Wali Kelas" to "1116",
        "bendahara2" to "0812",
        "Guru Mapel" to "1991"
    )

    val GURU_PINS: Set<String> = setOf("1991", "1116")

    val MAPEL_LIST: List<String> = listOf(
        "Matematika (MTK)",
        "Bahasa Indonesia",
        "Bahasa Inggris",
        "Bahasa Arab",
        "Biologi",
        "Fisika",
        "Kimia",
        "Sejarah",
        "Ekonomi",
        "PKN",
        "PAI",
        "TIK (Informatika)",
        "SBDP",
        "PKWU",
        "PJOK"
    )

    val LEAVE_REASONS: Map<String, List<String>> = mapOf(
        "Sakit" to listOf(
            "Demam dan flu",
            "Sakit perut",
            "Pusing / kurang enak badan",
            "Kontrol ke dokter"
        ),
        "Izin" to listOf(
            "Keperluan keluarga mendesak",
            "Acara keluarga di luar kota",
            "Mengurus dokumen penting",
            "Mengikuti kegiatan / lomba"
        )
    )

    val KAS_CATEGORIES: List<String> = listOf(
        "Iuran Kas Siswa",
        "Dana Tambahan / Donasi",
        "Perlengkapan Kebersihan Kelas",
        "Fotokopi & Print Materi",
        "Konsumsi & Acara Kelas",
        "Jenguk Siswa Sakit / Sosial",
        "Lainnya"
    )

    val KAS_NOMINALS: List<Long> = listOf(2000L, 5000L, 10000L, 15000L, 20000L)

    val DEFAULT_KAS_PERIODS: List<String> = listOf(
        "Minggu 1 - Nov 2026",
        "Minggu 2 - Nov 2026",
        "Minggu 3 - Nov 2026",
        "Minggu 4 - Nov 2026"
    )

    val DOK_CATEGORIES: List<String> = listOf(
        "Kegiatan Kelas",
        "Praktikum",
        "Kebersamaan",
        "Olahraga & Event"
    )

    val REKAP_MONTHS: List<Pair<String, String>> = listOf(
        "2026-11" to "November 2026",
        "2026-10" to "Oktober 2026",
        "2026-09" to "September 2026",
        "2026-08" to "Agustus 2026",
        "2026-07" to "Juli 2026",
        "YEARLY" to "Rekap Tahunan (2026)"
    )

    val STUDENTS: List<StudentMaster> = listOf(
        "Adara Adzra Aprillia" to "Senin",
        "Aira Nuraini Imanullah" to "Selasa",
        "Al Varizky Ramadhan Putra" to "Selasa",
        "Aldo Dayoja" to "Rabu",
        "Alysca Zahra Indahputri" to "Rabu",
        "Arumi Alissa Putri" to "Jumat",
        "Ayu Novalia Aziz" to "Sabtu",
        "Azalia Indy Puteri" to "Sabtu",
        "Azzahra Salsabila" to "Jumat",
        "Bintang Ayu Zahla Zhelda" to "Sabtu",
        "Daiva Kalyca" to "Senin",
        "Dimas Jayeng Satrio" to "Selasa",
        "Elvita Kheyrani" to "Selasa",
        "Firgy Permana Azhari" to "Rabu",
        "Greshana Ringestiani Fajzrin" to "Jumat",
        "Hafidh Fatullah Akbar" to "Sabtu",
        "Josua Alamsyah" to "Selasa",
        "Julian Alvino" to "Sabtu",
        "Kevin Juhannes Lubis" to "Kamis",
        "Kiki Sabilillah" to "Senin",
        "Leli Hasanah" to "Rabu",
        "Magdalena Ang Wijaya" to "Kamis",
        "Marhamah Aldan" to "Sabtu",
        "Martin Junior Bernadus" to "Jumat",
        "Mohamad Kristiano Revanza" to "Senin",
        "Muhamad Geraldi Patiwel" to "Kamis",
        "Muhamad Ilham Al-ikhsan" to "Jumat",
        "Muhamad Pasya Komara" to "Senin",
        "Muhamad Sahrul Mukarom" to "Kamis",
        "Muhamad Arman Maulana" to "Rabu",
        "Muhamad Wisnu Adriyan" to "Jumat",
        "Nur Elia" to "Rabu",
        "Nurul Marliyah" to "Selasa",
        "Ratnadhita Arshanta" to "Senin",
        "Ratnamaya Arishanti" to "Kamis",
        "Ravva Dwi Andika" to "Kamis",
        "Razqa Fabian" to "Kamis",
        "Rifqi Reswara" to "Sabtu",
        "Rizky Saputra" to "Selasa",
        "Safa Khalifah Sakhi" to "Sabtu",
        "Satria Vidianto" to "Senin",
        "Siti Fatimah Azzahra" to "Rabu",
        "Tadzkiyya Tunnufus Al Bantani" to "Kamis",
        "Vincent Altobeli Siagian" to "Rabu",
        "Wisnu Febrianto" to "Senin",
        "Yuga Priyatna" to "Jumat",
        "Zaidan Bariq" to "Jumat"
    ).mapIndexed { index, (name, piket) ->
        val id = index + 1
        StudentMaster(
            id = id,
            noAbsen = id.toString().padStart(2, '0'),
            name = name,
            piket = piket
        )
    }

    val SCHEDULE: Map<ScheduleDay, List<SubjectSchedule>> = mapOf(
        ScheduleDay.Senin to listOf(
            SubjectSchedule(1, "Sejarah", "13:00 - 14:00", "Ruang CLASS XI"),
            SubjectSchedule(2, "Bahasa Inggris", "14:00 - 15:00", "Ruang CLASS XI"),
            SubjectSchedule(3, "SBDP (Seni Budaya)", "15:30 - 16:30", "Ruang CLASS XI"),
            SubjectSchedule(4, "Matematika (MTK)", "16:30 - 17:00", "Ruang CLASS XI")
        ),
        ScheduleDay.Selasa to listOf(
            SubjectSchedule(1, "Bahasa Arab", "13:00 - 14:00", "Ruang CLASS XI"),
            SubjectSchedule(2, "Bahasa Indonesia", "14:00 - 16:00", "Ruang CLASS XI"),
            SubjectSchedule(3, "Biologi", "16:00 - 17:30", "Ruang CLASS XI")
        ),
        ScheduleDay.Rabu to listOf(
            SubjectSchedule(1, "Ekonomi", "13:00 - 14:00", "Ruang CLASS XI"),
            SubjectSchedule(2, "TIK (Informatika)", "14:00 - 15:00", "Ruang CLASS XI"),
            SubjectSchedule(3, "Biologi", "15:30 - 16:30", "Ruang CLASS XI"),
            SubjectSchedule(4, "Pendidikan Agama Islam (PAI)", "16:30 - 17:30", "Ruang CLASS XI")
        ),
        ScheduleDay.Kamis to listOf(
            SubjectSchedule(1, "Matematika (MTK)", "13:00 - 14:00", "Ruang CLASS XI"),
            SubjectSchedule(2, "PKWU", "14:00 - 15:00", "Ruang CLASS XI"),
            SubjectSchedule(3, "Kimia", "15:30 - 16:30", "Ruang CLASS XI"),
            SubjectSchedule(4, "Fisika", "16:30 - 17:30", "Ruang CLASS XI")
        ),
        ScheduleDay.Jumat to listOf(
            SubjectSchedule(1, "Pendidikan Kewarganegaraan (PKN)", "13:00 - 14:00", "Ruang CLASS XI"),
            SubjectSchedule(2, "Kimia", "13:30 - 14:00", "Ruang CLASS XI"),
            SubjectSchedule(3, "Fisika", "14:30 - 15:00", "Ruang CLASS XI"),
            SubjectSchedule(4, "PJOK", "15:30 - 16:30", "Lapangan Sekolah")
        ),
        ScheduleDay.Sabtu to listOf(
            SubjectSchedule(1, "Fisika", "13:00 - 14:10", "Ruang CLASS XI"),
            SubjectSchedule(2, "Kimia", "14:10 - 15:00", "Ruang CLASS XI")
        )
    )
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val studentId: Int = 0,
    val notifyEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClassConfig(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val name: String = "CLASS XI",
    val school: String = "SMA Puspita Kota Tangerang",
    val kasPeriods: List<String> = ClassConstants.DEFAULT_KAS_PERIODS,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentAttendanceRecord(
    val status: String = AttendanceStatus.Hadir.label,
    val scanTime: String = ""
)

@Keep
@IgnoreExtraProperties
data class AttendanceDayDoc(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val dateKey: String = "",
    val records: Map<String, StudentAttendanceRecord> = emptyMap(),
    val lastUpdatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val title: String = "",
    val category: String = "Penting",
    val deadline: String = "Hari Ini",
    val desc: String = "",
    val imageBase64: String? = null,
    val author: String = "",
    val dateLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaveRequest(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val studentId: Int = 1,
    val noAbsen: String = "01",
    val studentName: String = "",
    val piket: String = "Senin",
    val date: String = "",
    val type: String = "Sakit",
    val reason: String = "",
    val gformConfirmed: Boolean = false,
    val status: String = "Menunggu",
    val submittedAt: String = "",
    val verifiedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KasPaymentEntry(
    val paid: Boolean = false,
    val amount: Long = 0L,
    val date: String = "",
    val by: String = "",
    val txId: String = ""
)

@Keep
@IgnoreExtraProperties
data class KasPeriodDoc(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val name: String = "",
    val payments: Map<String, KasPaymentEntry> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KasTransaction(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val type: String = "Pemasukan",
    val amount: Long = 0L,
    val date: String = "",
    val category: String = "",
    val note: String = "",
    val author: String = "",
    val studentId: Int? = null,
    val period: String? = null,
    val voided: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DokumentasiPost(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val title: String = "",
    val category: String = "Kegiatan Kelas",
    val mediaType: String = "photo",
    val mediaUrl: String = "",
    val fileName: String = "",
    val caption: String = "",
    val author: String = "",
    val date: String = "",
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DokumentasiLike(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DokumentasiComment(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val dokId: String = "",
    val userId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timeLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MateriItem(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val title: String = "",
    val guru: String = "",
    val mapel: String = "",
    val desc: String = "",
    val dateLabel: String = "",
    val dl: Long = 0L,
    val fileName: String = "",
    val fileData: String? = null,
    val imgData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ClassNotification(
    @DocumentId val id: String = "",
    val classId: String = CLASS_ID,
    val userId: String = "",
    val senderName: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "info",
    val targetTab: String = "info",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class ResolvedStudentAttendance(
    val student: StudentMaster,
    val status: AttendanceStatus,
    val scanTime: String,
    val approvedLeaveType: String? = null
) {
    val lateArrival: Boolean
        get() = status == AttendanceStatus.Hadir && scanTime.isNotBlank() && scanTime > JAM_MASUK_NORMAL
}

data class RekapRow(
    val student: StudentMaster,
    val h: Int,
    val i: Int,
    val s: Int,
    val d: Int,
    val a: Int,
    val percent: Int
) {
    val statusLabel: String
        get() = when {
            percent < 75 || a > 2 -> "Perhatian BK"
            percent < 90 -> "Cukup"
            else -> "Baik"
        }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
