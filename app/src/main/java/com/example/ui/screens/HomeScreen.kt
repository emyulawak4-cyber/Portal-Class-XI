package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.ClassConstants
import com.example.ui.components.ClassCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRupiah
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

private data class QuickMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val route: String
)

@Composable
fun HomeScreen(
    data: ClassPortalData,
    onNavigate: (String) -> Unit,
    onOpenStudentQr: (Int) -> Unit,
    onOpenScanner: () -> Unit,
    onFilterPresensi: (AttendanceStatus) -> Unit
) {
    val attendanceList = data.attendanceForDate(data.selectedDate)
    val total = attendanceList.size.coerceAtLeast(47)
    val hadirCount = attendanceList.count { it.status == AttendanceStatus.Hadir }
    val izinCount = attendanceList.count { it.status == AttendanceStatus.Izin }
    val sakitCount = attendanceList.count { it.status == AttendanceStatus.Sakit }
    val dispenCount = attendanceList.count { it.status == AttendanceStatus.Dispensasi }
    val alpaCount = attendanceList.count { it.status == AttendanceStatus.Alpa }
    val hadirPct = Math.round(((hadirCount + dispenCount).toFloat() / total) * 100f)

    val kasSummary = data.kasSummaryForPeriod(data.selectedKasPeriod)
    val todayDay = data.selectedScheduleDay
    val todaySubjects = ClassConstants.SCHEDULE[todayDay].orEmpty()
    val todayPicket = ClassConstants.STUDENTS.filter { it.piket == todayDay.key }

    val quickMenuItems = listOf(
        QuickMenuItem("presensi", "Presensi", Icons.Rounded.FactCheck, 0, "presensi"),
        QuickMenuItem("kas", "Uang Kas", Icons.Rounded.AccountBalanceWallet, 0, "kas"),
        QuickMenuItem("izin", "Surat Izin", Icons.Rounded.MailOutline, data.pendingLeaveCount, "izin"),
        QuickMenuItem("jadwal", "Jadwal", Icons.Rounded.CalendarMonth, 0, "jadwal"),
        QuickMenuItem("qr", "Kartu QR", Icons.Rounded.QrCode, 0, "qr"),
        QuickMenuItem("dok", "Galeri", Icons.Rounded.PhotoLibrary, data.dokumentasi.size, "dokumentasi"),
        QuickMenuItem("info", "Info & Notif", Icons.Rounded.Campaign, data.announcements.size, "info"),
        QuickMenuItem("materi", "Bank Materi", Icons.Rounded.MenuBook, data.materi.size, "materi"),
        QuickMenuItem("rekap", "Rekap Guru", Icons.Rounded.Assessment, 0, "rekap"),
        QuickMenuItem("settings", "Pengaturan", Icons.Rounded.Settings, 0, "settings")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Attendance Summary Hero Card
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kehadiran Hari Ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusBadge(
                        text = if (data.isAdminMode) "Mode ${data.activePengurusRole}" else "Mode Siswa (Lihat)",
                        containerColor = if (data.isAdminMode) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (data.isAdminMode) EmeraldDark else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$hadirPct%",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "dari 47 siswa (${data.selectedDate})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // 5-segment progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    listOf(
                        hadirCount to RoyalBlue,
                        izinCount to SkyBlue,
                        sakitCount to AmberSakit,
                        dispenCount to VioletDispen,
                        alpaCount to RoseAlpa
                    ).forEach { (count, col) ->
                        if (count > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(count.toFloat())
                                    .fillMaxSize()
                                    .background(col)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple(AttendanceStatus.Hadir, hadirCount, RoyalBlue),
                        Triple(AttendanceStatus.Izin, izinCount, SkyBlue),
                        Triple(AttendanceStatus.Sakit, sakitCount, AmberSakit),
                        Triple(AttendanceStatus.Dispensasi, dispenCount, VioletDispen),
                        Triple(AttendanceStatus.Alpa, alpaCount, RoseAlpa)
                    ).forEach { (st, count, dotColor) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFilterPresensi(st) }
                                .padding(vertical = 8.dp)
                                .testTag("home_stat_${st.name.lowercase()}")
                        ) {
                            Text(
                                text = count.toString(),
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Text(
                                    text = st.shortLabel,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Kas Kelas Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlue, RoyalBlueDark))
                    )
                    .clickable { onNavigate("kas") }
                    .padding(18.dp)
                    .testTag("home_kas_hero_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SALDO UANG KAS CLASS XI",
                            color = Color(0xFFBFDBFE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = formatRupiah(kasSummary.saldo),
                            color = Color.White,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "${data.selectedKasPeriod} • ${kasSummary.paidCount}/47 Siswa Lunas",
                            color = Color(0xFFDBEAFE),
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "Buka Uang Kas",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 3. Quick Menu Grid (10 features)
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menu Utama CLASS XI",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = onOpenScanner,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("home_quick_scan_btn")
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Scan QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    quickMenuItems.chunked(5).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowItems.forEach { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigate(item.route) }
                                        .padding(vertical = 4.dp)
                                        .testTag("quick_menu_${item.id}")
                                ) {
                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        if (item.badgeCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 4.dp, y = (-4).dp)
                                                    .clip(CircleShape)
                                                    .background(RoseAlpa)
                                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.badgeCount.toString(),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Latest Info Highlight
        if (data.announcements.isNotEmpty()) {
            val topAnn = data.announcements.first()
            item {
                ClassCard(
                    modifier = Modifier.clickable { onNavigate("info") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(
                            text = "Pengumuman Terbaru • ${topAnn.category}",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = topAnn.dateLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(topAnn.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = topAnn.desc,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 5. Today's Subject Schedule Preview
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mata Pelajaran Hari ${todayDay.label}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = todayDay.hours,
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMono,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    todaySubjects.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigate("jadwal") }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sub.no}. ${sub.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = sub.time,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 6. Today's Picket Students Preview
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Petugas Piket Hari ${todayDay.label}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    StatusBadge(
                        text = "${todayPicket.size} Siswa",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    todayPicket.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { st ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpenStudentQr(st.id) }
                                ) {
                                    Text(
                                        text = "${st.noAbsen}. ${st.name}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
