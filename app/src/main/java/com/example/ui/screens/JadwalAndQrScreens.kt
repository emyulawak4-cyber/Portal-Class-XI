package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.ClassConstants
import com.example.model.ScheduleDay
import com.example.ui.components.ClassCard
import com.example.ui.components.PortalFeedback
import com.example.ui.components.QrCodeImage
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.viewmodel.ClassPortalData
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun JadwalScreen(
    data: ClassPortalData,
    onSelectDay: (ScheduleDay) -> Unit,
    onOpenStudentQr: (Int) -> Unit
) {
    val selectedDay = data.selectedScheduleDay
    val subjects = ClassConstants.SCHEDULE[selectedDay].orEmpty()
    val piketStudents = remember(selectedDay) {
        ClassConstants.STUDENTS.filter { it.piket == selectedDay.key }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ClassCard {
                Text(
                    text = "Jadwal Pelajaran & Piket",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${selectedDay.label}: ${selectedDay.hours}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScheduleDay.entries.forEach { day ->
                        FilterChip(
                            selected = day == selectedDay,
                            onClick = { onSelectDay(day) },
                            label = { Text(day.label, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("jadwal_day_${day.key.lowercase()}")
                        )
                    }
                }
            }
        }

        items(subjects, key = { "${selectedDay.key}_${it.no}_${it.name}" }) { subject ->
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subject.no.toString(),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBlue
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${subject.time} • ${subject.room}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            ClassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CleaningServices,
                        contentDescription = null,
                        tint = EmeraldKas
                    )
                    Column {
                        Text(
                            text = "Petugas Piket Hari ${selectedDay.label}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Ketuk nama siswa untuk membuka Kartu QR",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                piketStudents.forEach { st ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenStudentQr(st.id) }
                            .minimumInteractiveComponentSize()
                            .testTag("jadwal_piket_${st.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${st.noAbsen}. ${st.name}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                            Icon(
                                imageVector = Icons.Rounded.QrCode,
                                contentDescription = "Lihat QR ${st.name}",
                                tint = RoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrCardsScreen(
    data: ClassPortalData,
    onOpenStudentQr: (Int) -> Unit,
    onOpenScanner: () -> Unit
) {
    val q = data.globalSearch.trim().lowercase()
    val filteredStudents = remember(q) {
        if (q.isBlank()) ClassConstants.STUDENTS
        else ClassConstants.STUDENTS.filter {
            it.name.lowercase().contains(q) || it.noAbsen.contains(q) || it.piket.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ClassCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kartu QR 47 Siswa",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Format SMAPUSPITA_CLASSXI_<id>_<absen>",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("qr_open_scanner_button")
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR", fontWeight = FontWeight.Bold)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cols = when {
                maxWidth >= 900.dp -> 4
                maxWidth >= 600.dp -> 3
                else -> 2
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenStudentQr(student.id) }
                            .testTag("qr_card_${student.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QrCodeImage(
                                payload = "SMAPUSPITA_CLASSXI_${student.id}_${student.noAbsen}",
                                modifier = Modifier.size(104.dp)
                            )
                            StatusBadge(
                                text = "Absen ${student.noAbsen} • ${student.piket}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = RoyalBlueDark
                            )
                            Text(
                                text = student.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentQrDialog(
    studentId: Int,
    isAdminMode: Boolean,
    onMarkPresent: (Int) -> Unit,
    onRequestAdminLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val student = remember(studentId) {
        ClassConstants.STUDENTS.firstOrNull { it.id == studentId } ?: ClassConstants.STUDENTS.first()
    }
    val payload = "SMAPUSPITA_CLASSXI_${student.id}_${student.noAbsen}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Kartu Identitas QR", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(
                    text = "CLASS XI • SMA Puspita",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QrCodeImage(
                    payload = payload,
                    modifier = Modifier.size(210.dp)
                )
                Text(
                    text = student.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                StatusBadge(
                    text = "No. Absen ${student.noAbsen} • Piket ${student.piket}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = RoyalBlue
                )
                Text(
                    text = payload,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (isAdminMode) {
                Button(
                    onClick = {
                        PortalFeedback.playScanChime(context, true)
                        onMarkPresent(student.id)
                        onDismiss()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("qr_dialog_mark_present")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catat Hadir")
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onRequestAdminLogin()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("qr_dialog_unlock_admin")
                ) {
                    Text("Masuk Pengurus")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Tutup")
            }
        }
    )
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerDialog(
    isAdminMode: Boolean,
    onScanQr: (String) -> Pair<Boolean, String>,
    onRequestAdminLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var lastScanText by remember { mutableStateOf("") }
    var lastScanMs by remember { mutableLongStateOf(0L) }
    val scanLog = remember { mutableStateListOf<Pair<Boolean, String>>() }

    var manualStudentId by remember { mutableIntStateOf(ClassConstants.STUDENTS.first().id) }
    var showManualDropdown by remember { mutableStateOf(false) }
    val manualStudent = remember(manualStudentId) {
        ClassConstants.STUDENTS.firstOrNull { it.id == manualStudentId } ?: ClassConstants.STUDENTS.first()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Scan QR Presensi",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isAdminMode) "Arahkan kamera ke Kartu QR siswa" else "Butuh Mode Pengurus untuk mencatat",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        if (hasCameraPermission) {
                            IconButton(
                                onClick = { useFrontCamera = !useFrontCamera },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("scanner_flip_camera")
                            ) {
                                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Ganti kamera")
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("scanner_close")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tutup scanner")
                        }
                    }
                }

                if (!isAdminMode) {
                    Button(
                        onClick = {
                            onDismiss()
                            onRequestAdminLogin()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("scanner_login_pengurus")
                    ) {
                        Text("Masuk Mode Pengurus Terlebih Dahulu")
                    }
                } else {
                    if (hasCameraPermission) {
                        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
                        val scannerClient = remember { BarcodeScanning.getClient() }
                        DisposableEffect(Unit) {
                            onDispose {
                                scannerClient.close()
                                cameraExecutor.shutdown()
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx)
                                },
                                update = { previewView ->
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { imageAnalysis ->
                                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                                    val mediaImage = imageProxy.image
                                                    if (mediaImage != null) {
                                                        val img = InputImage.fromMediaImage(
                                                            mediaImage,
                                                            imageProxy.imageInfo.rotationDegrees
                                                        )
                                                        scannerClient.process(img)
                                                            .addOnSuccessListener { barcodes ->
                                                                val raw = barcodes.firstOrNull()?.rawValue
                                                                val now = System.currentTimeMillis()
                                                                if (!raw.isNullOrBlank() && (raw != lastScanText || now - lastScanMs > 3000L)) {
                                                                    lastScanText = raw
                                                                    lastScanMs = now
                                                                    val result = onScanQr(raw)
                                                                    PortalFeedback.playScanChime(context, result.first)
                                                                    scanLog.add(0, result)
                                                                    if (scanLog.size > 4) scanLog.removeAt(scanLog.lastIndex)
                                                                }
                                                            }
                                                            .addOnCompleteListener {
                                                                imageProxy.close()
                                                            }
                                                    } else {
                                                        imageProxy.close()
                                                    }
                                                }
                                            }
                                        val selector = if (useFrontCamera) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }
                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                selector,
                                                preview,
                                                analysis
                                            )
                                        } catch (_: Exception) {
                                        }
                                    }, ContextCompat.getMainExecutor(context))
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Izin kamera belum diberikan. Anda bisa mengaktifkan kamera atau memilih siswa secara manual di bawah.",
                                    fontSize = 12.sp
                                )
                                OutlinedButton(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text("Izinkan Kamera")
                                }
                            }
                        }
                    }

                    // Manual fallback picker
                    Text(
                        text = "Alternatif Scan Manual Cepat:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showManualDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("scanner_manual_student_picker")
                            ) {
                                Text(
                                    text = "${manualStudent.noAbsen}. ${manualStudent.name}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp
                                )
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Pilih siswa manual")
                            }
                            DropdownMenu(
                                expanded = showManualDropdown,
                                onDismissRequest = { showManualDropdown = false }
                            ) {
                                ClassConstants.STUDENTS.forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text("${st.noAbsen}. ${st.name}") },
                                        onClick = {
                                            manualStudentId = st.id
                                            showManualDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val code = "SMAPUSPITA_CLASSXI_${manualStudent.id}_${manualStudent.noAbsen}"
                                val res = onScanQr(code)
                                PortalFeedback.playScanChime(context, res.first)
                                scanLog.add(0, res)
                                if (scanLog.size > 4) scanLog.removeAt(scanLog.lastIndex)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("scanner_manual_submit")
                        ) {
                            Text("Hadir", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (scanLog.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scanLog.forEach { (ok, msg) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (ok) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = msg,
                                        color = if (ok) EmeraldDark else RoseAlpa,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
