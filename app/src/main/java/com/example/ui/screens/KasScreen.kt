package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassConstants
import com.example.model.KasTransaction
import com.example.ui.components.ClassCard
import com.example.ui.components.PortalFeedback
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueDark
import com.example.viewmodel.ClassPortalData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KasScreen(
    data: ClassPortalData,
    onSelectPeriod: (String) -> Unit,
    onSelectNominal: (Long) -> Unit,
    onAddPeriod: (String, () -> Unit) -> Unit,
    onToggleStudentPayment: (Int) -> Unit,
    onMarkAllPaid: () -> Unit,
    onAddTransaction: (String, Long, String, String, String, () -> Unit) -> Unit,
    onDeleteTransaction: (KasTransaction) -> Unit,
    onRequestAdminLogin: () -> Unit
) {
    val context = LocalContext.current
    val summary = data.kasSummaryForPeriod(data.selectedKasPeriod)
    var subView by remember { mutableStateOf("siswa") } // "siswa" | "transaksi"
    var studentFilter by remember { mutableStateOf("ALL") } // "ALL" | "PAID" | "UNPAID"
    var txFilter by remember { mutableStateOf("ALL") } // "ALL" | "Pemasukan" | "Pengeluaran"
    var periodMenuOpen by remember { mutableStateOf(false) }
    var nominalMenuOpen by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var showTxDialog by remember { mutableStateOf<String?>(null) }

    val csvContent = remember(data.selectedKasPeriod, summary, data.kasTransactions) {
        buildString {
            appendLine("No Absen,Nama Siswa,Periode Kas,Status Pembayaran,Nominal,Tanggal Bayar")
            ClassConstants.STUDENTS.forEach { st ->
                val info = summary.payments[st.id.toString()]
                val paid = info?.paid == true
                appendLine("\"${st.noAbsen}\",\"${st.name}\",\"${data.selectedKasPeriod}\",\"${if (paid) "Sudah Bayar (Lunas)" else "Belum Bayar"}\",${if (paid) info?.amount ?: 0 else 0},\"${if (paid) info?.date ?: "-" else "-"}\"")
            }
            appendLine()
            appendLine("Tanggal,Jenis Transaksi,Kategori,Nominal,Keterangan,Petugas")
            data.kasTransactions.filter { !it.voided }.forEach { tx ->
                appendLine("\"${tx.date}\",\"${tx.type}\",\"${tx.category}\",${tx.amount},\"${tx.note.replace("\"", "\"\"")}\",\"${tx.author}\"")
            }
        }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Laporan Uang Kas (.CSV) tersimpan.", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Gagal menyimpan CSV.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val q = data.globalSearch.trim().lowercase()
    val filteredStudents = ClassConstants.STUDENTS.filter { st ->
        val paid = summary.payments[st.id.toString()]?.paid == true
        val matchSearch = q.isBlank() || st.name.lowercase().contains(q) || st.noAbsen.contains(q)
        val matchFilter = when (studentFilter) {
            "PAID" -> paid
            "UNPAID" -> !paid
            else -> true
        }
        matchSearch && matchFilter
    }

    val filteredTx = data.kasTransactions.filter { tx ->
        !tx.voided && (txFilter == "ALL" || tx.type == txFilter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Saldo Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlue, RoyalBlueDark)))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("UANG KAS CLASS XI", color = Color(0xFFBFDBFE), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Standar Iuran: Rp 10.000 / Bulan", color = Color(0xFFDBEAFE), fontSize = 11.sp)
                        }
                        Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f)) {
                            Text(
                                text = "${summary.paidPercent}% Lunas",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = formatRupiah(summary.saldo),
                        color = Color.White,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Pemasukan", color = Color(0xFFDBEAFE), fontSize = 11.sp)
                                Text(formatRupiah(summary.totalIn), color = Color.White, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Pengeluaran", color = Color(0xFFDBEAFE), fontSize = 11.sp)
                                Text(formatRupiah(summary.totalOut), color = Color.White, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    // Progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.22f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (summary.paidPercent / 100f).coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${summary.paidCount}/47 Siswa Lunas", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${summary.unpaidCount} Belum Bayar", color = Color(0xFFDBEAFE), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Period & Nominal Controls
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1.4f)) {
                        OutlinedButton(
                            onClick = { periodMenuOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("kas_period_dropdown")
                        ) {
                            Text(data.selectedKasPeriod, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = periodMenuOpen, onDismissRequest = { periodMenuOpen = false }) {
                            data.kasPeriodNames.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p) },
                                    onClick = { onSelectPeriod(p); periodMenuOpen = false }
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { nominalMenuOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("kas_nominal_dropdown")
                        ) {
                            Text(formatRupiah(data.selectedKasNominal), fontSize = 12.sp, fontFamily = JetBrainsMono)
                        }
                        DropdownMenu(expanded = nominalMenuOpen, onDismissRequest = { nominalMenuOpen = false }) {
                            ClassConstants.KAS_NOMINALS.forEach { nom ->
                                DropdownMenuItem(
                                    text = { Text(formatRupiah(nom)) },
                                    onClick = { onSelectNominal(nom); nominalMenuOpen = false }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            if (data.isAdminMode) showAddPeriodDialog = true else onRequestAdminLogin()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("kas_add_period_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Tambah Periode Kas", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { subView = "siswa" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subView == "siswa") RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (subView == "siswa") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("kas_sub_siswa_btn")
                    ) {
                        Text("Status 47 Siswa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { subView = "transaksi" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subView == "transaksi") RoyalBlue else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (subView == "transaksi") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("kas_sub_tx_btn")
                    ) {
                        Text("Transaksi (${data.kasTransactions.count { !it.voided }})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (subView == "siswa") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = studentFilter == "ALL",
                            onClick = { studentFilter = "ALL" },
                            label = { Text("Semua (47)") }
                        )
                        FilterChip(
                            selected = studentFilter == "PAID",
                            onClick = { studentFilter = "PAID" },
                            label = { Text("Sudah Bayar (${summary.paidCount})") }
                        )
                        FilterChip(
                            selected = studentFilter == "UNPAID",
                            onClick = { studentFilter = "UNPAID" },
                            label = { Text("Belum Bayar (${summary.unpaidCount})") }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val unpaidLines = ClassConstants.STUDENTS.mapNotNull { st ->
                                    val info = summary.payments[st.id.toString()]
                                    if (info?.paid == true) null else "⏳ ${st.noAbsen}. ${st.name}"
                                }
                                val msg = buildString {
                                    appendLine("*REKAP PEMBAYARAN UANG KAS CLASS XI*")
                                    appendLine("Periode: *${data.selectedKasPeriod}*")
                                    appendLine("Lunas: ${summary.paidCount}/47 Siswa | Belum: ${summary.unpaidCount} Siswa")
                                    appendLine()
                                    if (unpaidLines.isEmpty()) appendLine("Alhamdulillah semua 47 siswa sudah lunas!")
                                    else unpaidLines.forEach { appendLine(it) }
                                }
                                PortalFeedback.shareText(context, "Tagihan Kas ${data.selectedKasPeriod}", msg)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldKas),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("kas_share_unpaid_btn")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Kirim Tagihan WA", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Tagih WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (data.isAdminMode) {
                            Button(
                                onClick = onMarkAllPaid,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("kas_mark_all_paid_btn")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Lunaskan Semua", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("Lunaskan Semua", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(filteredStudents, key = { it.id }) { st ->
                val payInfo = summary.payments[st.id.toString()]
                val paid = payInfo?.paid == true
                ClassCard(borderColor = if (paid) Color(0xFFA7F3D0) else Color(0xFFFECDD3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (paid) Color(0xFFD1FAE5) else Color(0xFFFFE4E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st.noAbsen,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (paid) EmeraldDark else RoseAlpa
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(st.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = if (paid) "${formatRupiah(payInfo?.amount ?: 0L)} • ${payInfo?.date ?: ""}" else "Tagihan ${data.selectedKasPeriod}",
                                    fontSize = 11.sp,
                                    color = if (paid) EmeraldDark else RoseAlpa,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (paid) EmeraldKas else Color(0xFFFFF1F2),
                            modifier = Modifier
                                .clickable {
                                    if (data.isAdminMode) onToggleStudentPayment(st.id) else onRequestAdminLogin()
                                }
                                .testTag("kas_toggle_${st.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (paid) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                                    contentDescription = if (paid) "Lunas" else "Belum Bayar",
                                    tint = if (paid) Color.White else RoseAlpa,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (paid) "Lunas" else "Belum Bayar",
                                    color = if (paid) Color.White else RoseAlpa,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = txFilter == "ALL", onClick = { txFilter = "ALL" }, label = { Text("Semua") })
                        FilterChip(selected = txFilter == "Pemasukan", onClick = { txFilter = "Pemasukan" }, label = { Text("Pemasukan") })
                        FilterChip(selected = txFilter == "Pengeluaran", onClick = { txFilter = "Pengeluaran" }, label = { Text("Pengeluaran") })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (data.isAdminMode) showTxDialog = "Pemasukan" else onRequestAdminLogin() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldKas),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("kas_add_in_btn")
                        ) {
                            Text("+ Pemasukan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { if (data.isAdminMode) showTxDialog = "Pengeluaran" else onRequestAdminLogin() },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseAlpa),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("kas_add_out_btn")
                        ) {
                            Text("- Pengeluaran", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val safeName = "Laporan_Uang_Kas_CLASS_XI_${data.selectedKasPeriod.replace(" ", "_")}.csv"
                                createCsvLauncher.launch(safeName)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("kas_export_csv_btn")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Simpan CSV", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Simpan CSV", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val recent = data.kasTransactions.filter { !it.voided }.take(8).joinToString("\n") { tx ->
                                    val sign = if (tx.type == "Pemasukan") "+" else "-"
                                    "• [${tx.date}] $sign${formatRupiah(tx.amount)} (${tx.note})"
                                }.ifBlank { "Belum ada transaksi." }
                                val msg = "*LAPORAN UANG KAS CLASS XI*\nSaldo: *${formatRupiah(summary.saldo)}*\nPemasukan: ${formatRupiah(summary.totalIn)}\nPengeluaran: ${formatRupiah(summary.totalOut)}\n\n$recent"
                                PortalFeedback.shareText(context, "Laporan Kas CLASS XI", msg)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("kas_share_report_btn")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Bagikan WA", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Bagikan WA", fontSize = 12.sp)
                        }
                    }
                }
            }

            if (filteredTx.isEmpty()) {
                item {
                    ClassCard {
                        Text("Belum Ada Riwayat Transaksi Kas", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Catat pemasukan atau pengeluaran kelas agar seluruh siswa dapat memantau transparansi kas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredTx, key = { it.id }) { tx ->
                    val isIn = tx.type == "Pemasukan"
                    ClassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isIn) Color(0xFFECFDF5) else Color(0xFFFFF1F2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isIn) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                                        contentDescription = tx.type,
                                        tint = if (isIn) EmeraldKas else RoseAlpa
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(
                                            text = tx.category,
                                            containerColor = if (isIn) Color(0xFFD1FAE5) else Color(0xFFFFE4E6),
                                            contentColor = if (isIn) EmeraldDark else Color(0xFF9F1239)
                                        )
                                        Text(tx.date, fontSize = 10.5.sp, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(tx.note, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("Oleh: ${tx.author}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isIn) "+" else "-"} ${formatRupiah(tx.amount)}",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isIn) EmeraldKas else RoseAlpa
                                )
                                if (data.isAdminMode) {
                                    IconButton(onClick = { onDeleteTransaction(tx) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus Transaksi", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showAddPeriodDialog) {
        var periodName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPeriodDialog = false },
            title = { Text("Tambah Periode Kas") },
            text = {
                OutlinedTextField(
                    value = periodName,
                    onValueChange = { periodName = it },
                    label = { Text("Contoh: Minggu 1 - Des 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onAddPeriod(periodName) { showAddPeriodDialog = false } },
                    enabled = periodName.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPeriodDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showTxDialog != null) {
        var txType by remember(showTxDialog) { mutableStateOf(showTxDialog!!) }
        var amountStr by remember { mutableStateOf("") }
        var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
        var category by remember(showTxDialog) {
            mutableStateOf(
                if (showTxDialog == "Pemasukan") "Dana Tambahan / Donasi" else "Perlengkapan Kebersihan Kelas"
            )
        }
        var catMenuOpen by remember { mutableStateOf(false) }
        var noteStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showTxDialog = null },
            title = { Text("Catat Transaksi Kas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { txType = "Pemasukan"; category = "Dana Tambahan / Donasi" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "Pemasukan") EmeraldKas else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (txType == "Pemasukan") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Pemasukan", fontSize = 12.sp) }
                        Button(
                            onClick = { txType = "Pengeluaran"; category = "Perlengkapan Kebersihan Kelas" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (txType == "Pengeluaran") RoseAlpa else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (txType == "Pengeluaran") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("Pengeluaran", fontSize = 12.sp) }
                    }
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter(Char::isDigit) },
                        label = { Text("Nominal (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Tanggal (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box {
                        OutlinedButton(onClick = { catMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = catMenuOpen, onDismissRequest = { catMenuOpen = false }) {
                            ClassConstants.KAS_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catMenuOpen = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("Keterangan") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val amt = amountStr.toLongOrNull() ?: 0L
                Button(
                    onClick = {
                        onAddTransaction(txType, amt, dateStr, category, noteStr) { showTxDialog = null }
                    },
                    enabled = amt > 0 && noteStr.isNotBlank()
                ) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showTxDialog = null }) { Text("Batal") }
            }
        )
    }
}
