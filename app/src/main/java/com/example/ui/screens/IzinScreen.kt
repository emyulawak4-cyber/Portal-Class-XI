package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassConstants
import com.example.model.GOOGLE_FORM_IZIN_URL
import com.example.model.LeaveRequest
import com.example.ui.components.ClassCard
import com.example.ui.components.PortalFeedback
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberSakit
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyBlue
import com.example.viewmodel.ClassPortalData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IzinScreen(
    data: ClassPortalData,
    currentUid: String,
    onRequestAdminLogin: () -> Unit,
    onSubmitLeave: (studentId: Int, date: String, type: String, reason: String, gformConfirmed: Boolean, onDone: () -> Unit) -> Unit,
    onVerifyLeave: (LeaveRequest, Boolean) -> Unit,
    onDeleteLeave: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    fun offsetDate(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return fmt.format(cal.time)
    }

    var leaveType by remember { mutableStateOf("Sakit") }
    var selectedStudentId by remember { mutableIntStateOf(ClassConstants.STUDENTS.first().id) }
    var studentQuery by remember { mutableStateOf("") }
    var showStudentDropdown by remember { mutableStateOf(false) }
    var dateInput by remember { mutableStateOf(offsetDate(0)) }
    var reason by remember { mutableStateOf("") }
    var gformConfirmed by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    var statusFilter by remember { mutableStateOf("Semua") }

    val pendingCount = data.leaveRequests.count { it.status == "Menunggu" }
    val approvedCount = data.leaveRequests.count { it.status == "Disetujui" }
    val rejectedCount = data.leaveRequests.count { it.status == "Ditolak" }

    val selectedStudent = remember(selectedStudentId) {
        ClassConstants.STUDENTS.firstOrNull { it.id == selectedStudentId } ?: ClassConstants.STUDENTS.first()
    }
    val matchingStudents = remember(studentQuery) {
        val sq = studentQuery.trim().lowercase()
        if (sq.isBlank()) ClassConstants.STUDENTS
        else ClassConstants.STUDENTS.filter {
            it.name.lowercase().contains(sq) || it.noAbsen.contains(sq)
        }
    }

    val globalQ = data.globalSearch.trim().lowercase()
    val filteredLeaves = remember(data.leaveRequests, statusFilter, globalQ) {
        data.leaveRequests.filter { item ->
            val matchStatus = statusFilter == "Semua" || item.status == statusFilter
            val matchSearch = globalQ.isBlank() ||
                item.studentName.lowercase().contains(globalQ) ||
                item.reason.lowercase().contains(globalQ) ||
                item.noAbsen.contains(globalQ)
            matchStatus && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LeaveStatBox("Menunggu", pendingCount, AmberSakit, Modifier.weight(1f))
                LeaveStatBox("Disetujui", approvedCount, EmeraldKas, Modifier.weight(1f))
                LeaveStatBox("Ditolak", rejectedCount, RoseAlpa, Modifier.weight(1f))
            }
        }

        // Step 1 Google Forms
        item {
            ClassCard {
                StatusBadge(
                    text = "LANGKAH 1",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Isi Google Forms Resmi Wali Kelas",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sebelum atau setelah mengirim pengajuan di aplikasi, pastikan mengisi formulir resmi Google Forms agar tercatat wali kelas.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { PortalFeedback.openUrl(context, GOOGLE_FORM_IZIN_URL) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("izin_open_gform")
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Forms", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            PortalFeedback.copyToClipboard(context, "Google Forms Izin CLASS XI", GOOGLE_FORM_IZIN_URL)
                            onShowMessage("Link Google Forms disalin.")
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("izin_copy_gform")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin link Google Forms", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin")
                    }
                }
            }
        }

        // Step 2 Digital Leave Form
        item {
            ClassCard {
                StatusBadge(
                    text = "LANGKAH 2",
                    containerColor = Color(0xFFECFDF5),
                    contentColor = EmeraldDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ajukan Surat Sakit / Izin Digital",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Sakit", "Izin").forEach { typeOption ->
                        FilterChip(
                            selected = leaveType == typeOption,
                            onClick = { leaveType = typeOption },
                            label = { Text(typeOption, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("izin_type_${typeOption.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showStudentDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("izin_student_selector")
                    ) {
                        Text(
                            text = "${selectedStudent.noAbsen}. ${selectedStudent.name} (Piket ${selectedStudent.piket})",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Pilih siswa")
                    }
                    DropdownMenu(
                        expanded = showStudentDropdown,
                        onDismissRequest = { showStudentDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        OutlinedTextField(
                            value = studentQuery,
                            onValueChange = { studentQuery = it },
                            placeholder = { Text("Cari nama / absen...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        matchingStudents.take(15).forEach { st ->
                            DropdownMenuItem(
                                text = { Text("${st.noAbsen}. ${st.name}") },
                                onClick = {
                                    selectedStudentId = st.id
                                    showStudentDropdown = false
                                    studentQuery = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it.take(10) },
                    label = { Text("Tanggal Izin (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("izin_date_input")
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kemarin" to -1, "Hari Ini" to 0, "Besok" to 1).forEach { (lbl, offset) ->
                        SuggestionChip(
                            onClick = { dateInput = offsetDate(offset) },
                            label = { Text(lbl, fontSize = 11.5.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(200) },
                    label = { Text("Alasan / Keterangan") },
                    supportingText = { Text("${reason.length}/200 karakter") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("izin_reason_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ClassConstants.LEAVE_REASONS[leaveType].orEmpty().forEach { template ->
                        SuggestionChip(
                            onClick = { reason = template },
                            label = { Text(template, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = gformConfirmed,
                        onCheckedChange = { gformConfirmed = it },
                        modifier = Modifier.testTag("izin_gform_checkbox")
                    )
                    Text(
                        text = "Saya sudah mengisi Google Forms resmi",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (formError != null) {
                    Text(formError!!, color = RoseAlpa, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Button(
                    onClick = {
                        when {
                            dateInput.trim().length != 10 -> formError = "Format tanggal harus YYYY-MM-DD."
                            reason.trim().length < 4 -> formError = "Tuliskan alasan izin minimal 4 karakter."
                            else -> {
                                formError = null
                                onSubmitLeave(
                                    selectedStudentId,
                                    dateInput.trim(),
                                    leaveType,
                                    reason.trim(),
                                    gformConfirmed
                                ) {
                                    reason = ""
                                    gformConfirmed = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("izin_submit_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kirim Pengajuan $leaveType", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Riwayat & Verifikasi
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "Menunggu", "Disetujui", "Ditolak").forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st, fontSize = 12.sp) },
                        modifier = Modifier.testTag("izin_filter_${st.lowercase()}")
                    )
                }
            }
        }

        if (filteredLeaves.isEmpty()) {
            item {
                ClassCard {
                    Text(
                        text = "Belum ada riwayat surat izin pada filter ini.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredLeaves, key = { it.id }) { leave ->
                LeaveHistoryCard(
                    leave = leave,
                    isAdminMode = data.isAdminMode,
                    canDelete = leave.userId == currentUid,
                    onApprove = { onVerifyLeave(leave, true) },
                    onReject = { onVerifyLeave(leave, false) },
                    onRequestAdminLogin = onRequestAdminLogin,
                    onDelete = { onDeleteLeave(leave.id) }
                )
            }
        }
    }
}

@Composable
private fun LeaveStatBox(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = accent
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LeaveHistoryCard(
    leave: LeaveRequest,
    isAdminMode: Boolean,
    canDelete: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRequestAdminLogin: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (leave.status) {
        "Disetujui" -> EmeraldKas
        "Ditolak" -> RoseAlpa
        else -> AmberSakit
    }
    ClassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(
                        text = leave.type,
                        containerColor = if (leave.type == "Sakit") Color(0xFFFEF3C7) else Color(0xFFE0F2FE),
                        contentColor = if (leave.type == "Sakit") Color(0xFFB45309) else SkyBlue
                    )
                    StatusBadge(
                        text = leave.status,
                        containerColor = statusColor.copy(alpha = 0.14f),
                        contentColor = statusColor
                    )
                    if (leave.gformConfirmed) {
                        StatusBadge(
                            text = "Forms ✓",
                            containerColor = Color(0xFFECFDF5),
                            contentColor = EmeraldDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${leave.noAbsen}. ${leave.studentName}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tanggal: ${leave.date} • Piket ${leave.piket}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("izin_delete_${leave.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus riwayat surat",
                        tint = RoseAlpa
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = leave.reason,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (leave.verifiedBy.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Diverifikasi oleh ${leave.verifiedBy}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (leave.status == "Menunggu") {
            Spacer(modifier = Modifier.height(10.dp))
            if (isAdminMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldKas),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("izin_approve_${leave.id}")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Setujui", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("izin_reject_${leave.id}")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = RoseAlpa, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tolak", color = RoseAlpa, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onRequestAdminLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("izin_unlock_admin_${leave.id}")
                ) {
                    Text("Masuk Mode Pengurus untuk Verifikasi", color = RoyalBlue, fontSize = 12.sp)
                }
            }
        }
    }
}
