package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.JAM_MASUK_NORMAL
import com.example.ui.components.ClassCard
import com.example.ui.components.PortalFeedback
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberSakit
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.VioletDispen
import com.example.viewmodel.ClassPortalData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresensiScreen(
    data: ClassPortalData,
    initialFilter: AttendanceStatus?,
    onSetDate: (String) -> Unit,
    onSetToday: () -> Unit,
    onUpdateStatus: (Int, AttendanceStatus) -> Unit,
    onSetAllHadir: () -> Unit,
    onOpenStudentQr: (Int) -> Unit,
    onOpenScanner: () -> Unit,
    onRequestAdminLogin: () -> Unit
) {
    val context = LocalContext.current
    var activeFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var showDatePicker by remember { mutableStateOf(false) }

    val attendanceList = data.attendanceForDate(data.selectedDate)
    val q = data.globalSearch.trim().lowercase()
    val filtered = attendanceList.filter { item ->
        val matchSearch = q.isBlank() ||
            item.student.name.lowercase().contains(q) ||
            item.student.noAbsen.contains(q) ||
            item.student.piket.lowercase().contains(q)
        val matchFilter = activeFilter == null || item.status == activeFilter
        matchSearch && matchFilter
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                            onSetDate(formatted)
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controls Card
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Presensi Harian", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "47 Siswa • Batas Tepat Waktu $JAM_MASUK_NORMAL WIB",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("presensi_pick_date_btn")
                        ) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = "Pilih Tanggal", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(data.selectedDate, fontFamily = JetBrainsMono, fontSize = 11.5.sp)
                        }
                        IconButton(
                            onClick = onSetToday,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("presensi_today_btn")
                        ) {
                            Icon(Icons.Rounded.Today, contentDescription = "Hari Ini", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { activeFilter = null },
                        label = { Text("Semua (${attendanceList.size})", fontSize = 12.sp) },
                        modifier = Modifier.testTag("presensi_filter_all")
                    )
                    AttendanceStatus.entries.forEach { st ->
                        val count = attendanceList.count { it.status == st }
                        FilterChip(
                            selected = activeFilter == st,
                            onClick = { activeFilter = st },
                            label = { Text("${st.shortLabel} ($count)", fontSize = 12.sp) },
                            modifier = Modifier.testTag("presensi_filter_${st.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hadir = attendanceList.filter { it.status == AttendanceStatus.Hadir }
                            val izin = attendanceList.filter { it.status == AttendanceStatus.Izin }
                            val sakit = attendanceList.filter { it.status == AttendanceStatus.Sakit }
                            val dispen = attendanceList.filter { it.status == AttendanceStatus.Dispensasi }
                            val alpa = attendanceList.filter { it.status == AttendanceStatus.Alpa }

                            val report = buildString {
                                appendLine("*PRESENSI HARIAN CLASS XI*")
                                appendLine("SMA Puspita Kota Tangerang")
                                appendLine("Tanggal: ${data.selectedDate}")
                                appendLine()
                                appendLine("*Hadir:* ${hadir.size} / 47 Siswa")
                                hadir.forEach { s ->
                                    val timeInfo = s.scanTime.ifBlank { "jam belum dicatat" }
                                    val lateTag = if (s.lateArrival) " (TERLAMBAT)" else ""
                                    appendLine("- ${s.student.noAbsen}. ${s.student.name}: $timeInfo$lateTag")
                                }
                                appendLine()
                                appendLine("*Izin:* ${if (izin.isEmpty()) "Nihil" else izin.joinToString("\n") { "- ${it.student.noAbsen}. ${it.student.name}" }}")
                                appendLine("*Sakit:* ${if (sakit.isEmpty()) "Nihil" else sakit.joinToString("\n") { "- ${it.student.noAbsen}. ${it.student.name}" }}")
                                appendLine("*Dispensasi:* ${if (dispen.isEmpty()) "Nihil" else dispen.joinToString("\n") { "- ${it.student.noAbsen}. ${it.student.name}" }}")
                                appendLine("*Alpa:* ${if (alpa.isEmpty()) "Nihil" else alpa.joinToString("\n") { "- ${it.student.noAbsen}. ${it.student.name}" }}")
                            }
                            PortalFeedback.copyToClipboard(context, "Laporan Presensi", report)
                            PortalFeedback.shareText(context, "Presensi CLASS XI ${data.selectedDate}", report)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldKas),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("presensi_share_wa_btn")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Bagikan WhatsApp", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Kirim WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (data.isAdminMode) {
                        Button(
                            onClick = onSetAllHadir,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("presensi_hadir_semua_btn")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Hadir Semua", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Hadir Semua", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = onOpenScanner,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("presensi_scan_btn")
                        ) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Buka Scanner QR", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onRequestAdminLogin,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("presensi_unlock_admin_btn")
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = "Mode Pengurus", modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Mode Pengurus", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Student Cards
        items(filtered, key = { it.student.id }) { item ->
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenStudentQr(item.student.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.student.noAbsen,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.student.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Piket ${item.student.piket}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.approvedLeaveType != null) {
                                StatusBadge(
                                    text = "Ada Surat ${item.approvedLeaveType}",
                                    containerColor = Color(0xFFE0F2FE),
                                    contentColor = Color(0xFF0369A1),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else if (item.scanTime.isNotBlank()) {
                                val textCol = if (item.lateArrival) AmberSakit else EmeraldKas
                                Text(
                                    text = if (item.lateArrival) "Terlambat · ${item.scanTime} WIB" else "Datang pukul ${item.scanTime} WIB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textCol
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { onOpenStudentQr(item.student.id) },
                        modifier = Modifier.testTag("student_qr_btn_${item.student.id}")
                    ) {
                        Icon(Icons.Rounded.QrCode, contentDescription = "Lihat QR ${item.student.name}", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AttendanceStatus.entries.forEach { st ->
                        val active = item.status == st
                        val activeBg = when (st) {
                            AttendanceStatus.Hadir -> RoyalBlue
                            AttendanceStatus.Izin -> SkyBlue
                            AttendanceStatus.Sakit -> AmberSakit
                            AttendanceStatus.Dispensasi -> VioletDispen
                            AttendanceStatus.Alpa -> RoseAlpa
                        }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (active) activeBg else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable {
                                    if (data.isAdminMode) onUpdateStatus(item.student.id, st)
                                    else onRequestAdminLogin()
                                }
                                .testTag("att_${item.student.id}_${st.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = st.shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
