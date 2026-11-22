package com.example.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassConstants
import com.example.model.MateriItem
import com.example.notifications.NotificationHelper
import com.example.ui.components.ClassCard
import com.example.ui.components.MediaEncodingUtil
import com.example.ui.components.MediaImageBox
import com.example.ui.components.PortalFeedback
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberSakit
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.VioletDispen
import com.example.viewmodel.ClassPortalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RekapScreen(
    data: ClassPortalData,
    onOpenStudentQr: (Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMonth by remember { mutableStateOf(ClassConstants.REKAP_MONTHS.first().first) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val (effectiveDays, allRows) = remember(data.attendanceDays, selectedMonth) {
        data.buildRekap(selectedMonth)
    }
    val q = data.globalSearch.trim().lowercase()
    val filteredRows = remember(allRows, q) {
        if (q.isBlank()) allRows
        else allRows.filter {
            it.student.name.lowercase().contains(q) ||
                it.student.noAbsen.contains(q) ||
                it.student.piket.lowercase().contains(q)
        }
    }

    val avgPercent = remember(allRows) {
        if (allRows.isEmpty()) 0 else Math.round(allRows.map { it.percent }.average().toFloat())
    }
    val totalI = remember(allRows) { allRows.sumOf { it.i } }
    val totalS = remember(allRows) { allRows.sumOf { it.s } }
    val totalD = remember(allRows) { allRows.sumOf { it.d } }
    val totalA = remember(allRows) { allRows.sumOf { it.a } }

    val selectedMonthLabel = ClassConstants.REKAP_MONTHS.firstOrNull { it.first == selectedMonth }?.second ?: selectedMonth

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val csv = buildString {
                            appendLine("No Absen,Nama Siswa,Piket,Hadir,Izin,Sakit,Dispensasi,Alpa,Persentase,Status")
                            allRows.forEach { r ->
                                val escaped = r.student.name.replace("\"", "\"\"")
                                appendLine("${r.student.noAbsen},\"$escaped\",${r.student.piket},${r.h},${r.i},${r.s},${r.d},${r.a},${r.percent}%,${r.statusLabel}")
                            }
                        }
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(csv.toByteArray(Charsets.UTF_8))
                        }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                onShowMessage(if (ok) "Laporan CSV Rekap Guru berhasil diekspor." else "Gagal mengekspor CSV.")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rekapitulasi Kehadiran Guru",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "SMA Puspita Kota Tangerang • $effectiveDays hari tercatat",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(
                        text = "47 Siswa",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = RoyalBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showMonthDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("rekap_month_dropdown")
                    ) {
                        Text(selectedMonthLabel, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Pilih periode rekap")
                    }
                    DropdownMenu(
                        expanded = showMonthDropdown,
                        onDismissRequest = { showMonthDropdown = false }
                    ) {
                        ClassConstants.REKAP_MONTHS.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedMonth = key
                                    showMonthDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RekapMetricPill("Rata²", "$avgPercent%", RoyalBlue, Modifier.weight(1f))
                    RekapMetricPill("Izin", "$totalI", SkyBlue, Modifier.weight(1f))
                    RekapMetricPill("Sakit", "$totalS", AmberSakit, Modifier.weight(1f))
                    RekapMetricPill("Dispen", "$totalD", VioletDispen, Modifier.weight(1f))
                    RekapMetricPill("Alpa", "$totalA", RoseAlpa, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { csvLauncher.launch("Rekap_Presensi_CLASS_XI_$selectedMonth.csv") },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("rekap_export_csv")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val rowsHtml = allRows.joinToString("") { r ->
                                "<tr><td>${r.student.noAbsen}</td><td>${r.student.name}</td><td>${r.student.piket}</td><td>${r.h}</td><td>${r.i}</td><td>${r.s}</td><td>${r.d}</td><td>${r.a}</td><td>${r.percent}%</td><td>${r.statusLabel}</td></tr>"
                            }
                            val html = """
                                <html><body style="font-family:sans-serif;padding:16px;">
                                <h2>Rekapitulasi Kehadiran CLASS XI - $selectedMonthLabel</h2>
                                <p>SMA Puspita Kota Tangerang • Total $effectiveDays hari efektif</p>
                                <table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;font-size:12px;">
                                <tr><th>No</th><th>Nama</th><th>Piket</th><th>H</th><th>I</th><th>S</th><th>D</th><th>A</th><th>%</th><th>Status</th></tr>
                                $rowsHtml
                                </table></body></html>
                            """.trimIndent()
                            PortalFeedback.printHtmlReport(context, "Rekap_CLASS_XI_$selectedMonth", html)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("rekap_print_pdf")
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val summaryText = buildString {
                                appendLine("*REKAP PRESENSI CLASS XI ($selectedMonthLabel)*")
                                appendLine("SMA Puspita Kota Tangerang • $effectiveDays hari")
                                appendLine("Rata-rata hadir: $avgPercent% | I:$totalI S:$totalS D:$totalD A:$totalA")
                                val bk = allRows.filter { it.statusLabel == "Perhatian BK" }
                                if (bk.isNotEmpty()) {
                                    appendLine("\nPerhatian BK:")
                                    bk.forEach { appendLine("- ${it.student.noAbsen}. ${it.student.name} (${it.percent}%, Alpa: ${it.a})") }
                                }
                            }
                            PortalFeedback.shareText(context, "Rekap Presensi CLASS XI", summaryText)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("rekap_share_wa")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WA", fontSize = 12.sp)
                    }
                }
            }
        }

        items(filteredRows, key = { it.student.id }) { row ->
            val stColor = when (row.statusLabel) {
                "Perhatian BK" -> RoseAlpa
                "Cukup" -> AmberSakit
                else -> EmeraldKas
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenStudentQr(row.student.id) }
                    .testTag("rekap_row_${row.student.id}")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${row.student.noAbsen}. ${row.student.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "H:${row.h}  I:${row.i}  S:${row.s}  D:${row.d}  A:${row.a}  • Piket ${row.student.piket}",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "${row.percent}%",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = stColor
                        )
                        StatusBadge(
                            text = row.statusLabel,
                            containerColor = stColor.copy(alpha = 0.14f),
                            contentColor = stColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RekapMetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InfoScreen(
    data: ClassPortalData,
    currentUid: String,
    onRequestAdminLogin: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onAddAnnouncement: (title: String, category: String, deadline: String, desc: String, imageBase64: String?) -> Unit,
    onDeleteAnnouncement: (String) -> Unit
) {
    val context = LocalContext.current
    var categoryFilter by remember { mutableStateOf("Semua") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val hasDevicePerm = NotificationHelper.hasNotificationPermission(context)
    val notifActive = hasDevicePerm && data.preferences.notificationsEnabled

    val q = data.globalSearch.trim().lowercase()
    val filtered = remember(data.announcements, categoryFilter, q) {
        data.announcements.filter { ann ->
            val matchCat = categoryFilter == "Semua" || ann.category.equals(categoryFilter, ignoreCase = true)
            val matchSearch = q.isBlank() ||
                ann.title.lowercase().contains(q) ||
                ann.desc.lowercase().contains(q) ||
                ann.author.lowercase().contains(q)
            matchCat && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Friend notification status banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (notifActive) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (notifActive) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            contentDescription = "Status Notifikasi Teman",
                            tint = if (notifActive) EmeraldDark else Color(0xFFB45309)
                        )
                        Column {
                            Text(
                                text = if (notifActive) "Notifikasi Real-time Teman Aktif" else "Aktifkan Notifikasi Perangkat",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (notifActive) EmeraldDark else Color(0xFF92400E)
                            )
                            Text(
                                text = if (notifActive) "Info & surat baru langsung muncul ke seluruh HP teman." else "Izinkan notifikasi agar pengumuman teman tidak terlewat.",
                                fontSize = 11.sp,
                                color = if (notifActive) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            if (hasDevicePerm) onSendTestNotification() else onRequestNotificationPermission()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("info_notif_action")
                    ) {
                        Text(
                            text = if (hasDevicePerm) "Tes Notif" else "Izinkan",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Info & Agenda Broadcast", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(
                            text = "Pengumuman langsung mengirim notifikasi ke teman sekelas",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (data.isAdminMode) showCreateDialog = true else onRequestAdminLogin()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("info_add_button")
                    ) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Semua", "Penting", "Tugas", "Event").forEach { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = { categoryFilter = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            modifier = Modifier.testTag("info_filter_${cat.lowercase()}")
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                ClassCard {
                    Text(
                        text = "Belum ada pengumuman untuk kategori ini.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { ann ->
                val catColor = when (ann.category) {
                    "Penting" -> RoseAlpa
                    "Tugas" -> AmberSakit
                    else -> RoyalBlue
                }
                ClassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusBadge(
                                    text = ann.category,
                                    containerColor = catColor.copy(alpha = 0.14f),
                                    contentColor = catColor
                                )
                                StatusBadge(
                                    text = ann.deadline,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(ann.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
                            Text(
                                text = "${ann.author} • ${ann.dateLabel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    PortalFeedback.shareText(
                                        context,
                                        "Info CLASS XI: ${ann.title}",
                                        "*[CLASS XI - ${ann.category}]* ${ann.title}\nDeadline: ${ann.deadline}\n\n${ann.desc}"
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("info_share_${ann.id}")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = "Bagikan info ke teman", tint = RoyalBlue)
                            }
                            if (ann.userId == currentUid) {
                                IconButton(
                                    onClick = { onDeleteAnnouncement(ann.id) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("info_delete_${ann.id}")
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus info", tint = RoseAlpa)
                                }
                            }
                        }
                    }

                    if (!ann.imageBase64.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MediaImageBox(
                            url = ann.imageBase64,
                            contentDescription = "Lampiran ${ann.title}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(ann.desc, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }

    if (showCreateDialog) {
        BroadcastAnnouncementDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, cat, deadline, desc, img ->
                onAddAnnouncement(title, cat, deadline, desc, img)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun BroadcastAnnouncementDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, deadline: String, desc: String, imageBase64: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Penting") }
    var deadline by remember { mutableStateOf("Hari Ini") }
    var desc by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf<String?>(null) }
    var pickedName by remember { mutableStateOf("") }
    var encoding by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            encoding = true
            scope.launch {
                val res = MediaEncodingUtil.encodeCompressedImageDataUrl(context, uri)
                encoding = false
                if (res != null) {
                    imageBase64 = res
                    pickedName = MediaEncodingUtil.getFileName(context, uri)
                } else {
                    err = "Gagal memproses gambar."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Broadcast Info & Notifikasi", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Judul Pengumuman") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("info_dialog_title")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Penting", "Tugas", "Event").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.5.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it.take(40) },
                    label = { Text("Batas Waktu / Jadwal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it.take(500) },
                    label = { Text("Isi Pengumuman") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("info_dialog_desc")
                )
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    if (encoding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memproses Foto...")
                    } else {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (pickedName.isBlank()) "Lampirkan Foto (Opsional)" else pickedName)
                    }
                }
                if (err != null) {
                    Text(err!!, color = RoseAlpa, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !encoding,
                onClick = {
                    when {
                        title.isBlank() -> err = "Judul pengumuman wajib diisi."
                        desc.isBlank() -> err = "Isi pengumuman wajib diisi."
                        else -> onSubmit(title.trim(), category, deadline.ifBlank { "Hari Ini" }, desc.trim(), imageBase64)
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("info_dialog_submit")
            ) {
                Text("Kirim & Notifikasi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun MateriScreen(
    data: ClassPortalData,
    currentUid: String,
    onRequestGuruLogin: () -> Unit,
    onExitGuruMode: () -> Unit,
    onAddMateri: (
        title: String,
        guru: String,
        mapel: String,
        desc: String,
        fileName: String,
        fileData: String?,
        imgData: String?,
        onDone: () -> Unit
    ) -> Unit,
    onIncrementDownload: (MateriItem) -> Unit,
    onDeleteMateri: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mapelFilter by remember { mutableStateOf("Semua") }
    var sortMode by remember { mutableStateOf("Terbaru") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingDownloadItem by remember { mutableStateOf<MateriItem?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val item = pendingDownloadItem
        pendingDownloadItem = null
        if (uri != null && item?.fileData != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val b64 = item.fileData.substringAfter("base64,", "")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                onShowMessage(if (ok) "Materi berhasil disimpan ke perangkat." else "Gagal menyimpan file materi.")
            }
        }
    }

    val q = data.globalSearch.trim().lowercase()
    val filtered = remember(data.materi, mapelFilter, sortMode, q) {
        val base = data.materi.filter { m ->
            val matchMapel = mapelFilter == "Semua" || m.mapel == mapelFilter
            val matchSearch = q.isBlank() ||
                m.title.lowercase().contains(q) ||
                m.guru.lowercase().contains(q) ||
                m.mapel.lowercase().contains(q) ||
                m.desc.lowercase().contains(q)
            matchMapel && matchSearch
        }
        when (sortMode) {
            "Terlama" -> base.reversed()
            "Populer" -> base.sortedByDescending { it.dl }
            else -> base
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bank Materi CLASS XI", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            text = "${data.materi.size} modul & catatan pembelajaran tersedia",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { if (data.isGuruMode) onExitGuruMode() else onRequestGuruLogin() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("materi_guru_mode_button")
                    ) {
                        Icon(
                            imageVector = if (data.isGuruMode) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (data.isGuruMode) "Guru Aktif" else "Mode Guru", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Terbaru", "Terlama", "Populer").forEach { sm ->
                            FilterChip(
                                selected = sortMode == sm,
                                onClick = { sortMode = sm },
                                label = { Text(sm, fontSize = 11.5.sp) }
                            )
                        }
                    }
                    Button(
                        onClick = { if (data.isGuruMode) showUploadDialog = true else onRequestGuruLogin() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("materi_upload_button")
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (listOf("Semua") + ClassConstants.MAPEL_LIST).forEach { mapel ->
                        FilterChip(
                            selected = mapelFilter == mapel,
                            onClick = { mapelFilter = mapel },
                            label = { Text(mapel, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Latest materi spotlight
        data.materi.firstOrNull()?.let { latest ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlue, RoyalBlueDark)))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MATERI TERBARU • ${latest.mapel}",
                            color = Color(0xFFBFDBFE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(latest.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(
                            text = "${latest.guru} • ${latest.dateLabel} • ${latest.dl}x diunduh",
                            color = Color(0xFFDBEAFE),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                ClassCard {
                    Text(
                        text = "Belum ada materi untuk filter ini. Guru dapat mengunggah materi setelah masuk Mode Guru.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                ClassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusBadge(
                                text = item.mapel,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = RoyalBlue
                            )
                            Text(item.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
                            Text(
                                text = "${item.guru} • ${item.dateLabel}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.userId == currentUid) {
                            IconButton(
                                onClick = { onDeleteMateri(item.id) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("materi_delete_${item.id}")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus materi", tint = RoseAlpa)
                            }
                        }
                    }

                    if (!item.imgData.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        MediaImageBox(
                            url = item.imgData,
                            contentDescription = "Foto materi ${item.title}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    if (item.desc.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.desc, fontSize = 13.sp, lineHeight = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${item.fileName.ifBlank { "Ringkasan Materi" }} • ${item.dl}x",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                onIncrementDownload(item)
                                if (!item.fileData.isNullOrBlank()) {
                                    pendingDownloadItem = item
                                    saveFileLauncher.launch(item.fileName.ifBlank { "Materi_${item.id}" })
                                } else {
                                    PortalFeedback.shareText(
                                        context,
                                        "Materi CLASS XI: ${item.title}",
                                        "*${item.title}* (${item.mapel})\nGuru: ${item.guru}\n\n${item.desc}"
                                    )
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("materi_download_${item.id}")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (!item.fileData.isNullOrBlank()) "Unduh" else "Bagikan", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        UploadMateriDialog(
            onDismiss = { showUploadDialog = false },
            onSubmit = { title, guru, mapel, desc, fileName, fileData, imgData ->
                onAddMateri(title, guru, mapel, desc, fileName, fileData, imgData) {
                    showUploadDialog = false
                }
            }
        )
    }
}

@Composable
private fun UploadMateriDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        guru: String,
        mapel: String,
        desc: String,
        fileName: String,
        fileData: String?,
        imgData: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var guru by remember { mutableStateOf("") }
    var mapel by remember { mutableStateOf(ClassConstants.MAPEL_LIST.first()) }
    var showMapelMenu by remember { mutableStateOf(false) }
    var desc by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var fileData by remember { mutableStateOf<String?>(null) }
    var imgData by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            processing = true
            err = null
            scope.launch {
                val name = MediaEncodingUtil.getFileName(context, uri)
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val encoded = MediaEncodingUtil.encodeSmallFileDataUrl(context, uri, mime)
                processing = false
                if (encoded != null) {
                    fileName = name
                    fileData = encoded
                } else {
                    err = "Ukuran dokumen maksimal 300 KB."
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            processing = true
            err = null
            scope.launch {
                val encoded = MediaEncodingUtil.encodeCompressedImageDataUrl(context, uri)
                processing = false
                if (encoded != null) imgData = encoded else err = "Gagal mengompresi foto sampul."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Materi Baru", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Judul Materi / Bab") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("materi_dialog_title")
                )
                OutlinedTextField(
                    value = guru,
                    onValueChange = { guru = it.take(60) },
                    label = { Text("Nama Guru Pengajar") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("materi_dialog_guru")
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showMapelMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                    ) {
                        Text(mapel, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Pilih mata pelajaran")
                    }
                    DropdownMenu(
                        expanded = showMapelMenu,
                        onDismissRequest = { showMapelMenu = false }
                    ) {
                        ClassConstants.MAPEL_LIST.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    mapel = m
                                    showMapelMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it.take(500) },
                    label = { Text("Ringkasan / Instruksi Tugas") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (fileName.isBlank()) "Lampiran File (PDF/Doc <= 300KB)" else fileName)
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (imgData == null) "Foto Papan / Sampul (Opsional)" else "Foto Terlampir ✓")
                }
                if (processing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                if (err != null) {
                    Text(err!!, color = RoseAlpa, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !processing,
                onClick = {
                    when {
                        title.isBlank() -> err = "Judul materi wajib diisi."
                        guru.isBlank() -> err = "Nama guru wajib diisi."
                        else -> onSubmit(title.trim(), guru.trim(), mapel, desc.trim(), fileName, fileData, imgData)
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("materi_dialog_submit")
            ) {
                Text("Publikasikan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("Batal")
            }
        }
    )
}
