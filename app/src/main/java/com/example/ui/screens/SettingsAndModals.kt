package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ThemeMode
import com.example.data.ThemeStyle
import com.example.notifications.NotificationHelper
import com.example.ui.components.ClassCard
import com.example.ui.components.PortalFeedback
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.viewmodel.ClassPortalData
import com.google.firebase.auth.FirebaseUser

@Composable
fun SettingsScreen(
    data: ClassPortalData,
    currentUser: FirebaseUser?,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetThemeStyle: (ThemeStyle) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onRequestAdminLogin: () -> Unit,
    onExitAdminMode: () -> Unit,
    onRequestGuruLogin: () -> Unit,
    onExitGuruMode: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val hasDevicePerm = NotificationHelper.hasNotificationPermission(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Appearance
        item {
            ClassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = RoyalBlue)
                    Text("Tampilan & Tema Aplikasi", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Mode Tema", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = data.preferences.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                    Text("Gaya Visual", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeStyle.entries.forEach { style ->
                        FilterChip(
                            selected = data.preferences.themeStyle == style,
                            onClick = { onSetThemeStyle(style) },
                            label = { Text(style.label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("settings_style_${style.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Friend Notifications
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = EmeraldKas)
                        Column {
                            Text("Notifikasi Real-Time Teman", fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
                            Text(
                                text = if (hasDevicePerm) "Izin perangkat aktif • Info, materi, & izin langsung muncul" else "Izin notifikasi sistem belum aktif",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = data.preferences.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasDevicePerm) {
                                onRequestNotificationPermission()
                            }
                            onSetNotificationsEnabled(enabled)
                        },
                        modifier = Modifier.testTag("settings_notif_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasDevicePerm) onRequestNotificationPermission()
                            else onSendTestNotification()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("settings_test_notif")
                    ) {
                        Text(if (hasDevicePerm) "Kirim Tes Notifikasi" else "Izinkan Notifikasi", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            PortalFeedback.shareText(
                                context,
                                "Gabung Portal CLASS XI SMA Puspita",
                                "Ayo install aplikasi CLASS XI SMA Puspita Kota Tangerang dan masuk dengan Google agar info, presensi, kas, & notifikasi kelas langsung sinkron ke HP kita!"
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_invite_friends")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = "Ajak teman", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ajak Teman", fontSize = 12.sp)
                    }
                }
            }
        }

        // Roles: Pengurus & Guru
        item {
            ClassCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = RoyalBlue)
                    Text("Hak Akses Portal Kelas", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Pengurus Kelas", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text(
                            text = if (data.isAdminMode) "Aktif: ${data.activePengurusRole}" else "Ketua, Sekretaris, Bendahara, Wali Kelas",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (data.isAdminMode) {
                        OutlinedButton(
                            onClick = onExitAdminMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_exit_admin")
                        ) { Text("Keluar", color = RoseAlpa) }
                    } else {
                        Button(
                            onClick = onRequestAdminLogin,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_login_admin")
                        ) { Text("Masuk PIN") }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mode Guru / Upload Materi", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        Text(
                            text = if (data.isGuruMode) "Aktif (Dapat upload Bank Materi)" else "Diperlukan untuk mengunggah modul guru",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (data.isGuruMode) {
                        OutlinedButton(
                            onClick = onExitGuruMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_exit_guru")
                        ) { Text("Keluar", color = RoseAlpa) }
                    } else {
                        OutlinedButton(
                            onClick = onRequestGuruLogin,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("settings_login_guru")
                        ) { Text("PIN Guru") }
                    }
                }
            }
        }

        // Google Account
        item {
            ClassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val photoUrl = currentUser?.photoUrl?.toString()
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto Profil Google",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.School, contentDescription = null, tint = RoyalBlue)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Pengguna CLASS XI",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = currentUser?.email ?: "-",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusBadge(
                        text = "Terhubung",
                        containerColor = Color(0xFFECFDF5),
                        contentColor = EmeraldDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseAlpa),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("settings_sign_out")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar Akun Google", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var wrongPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masuk Mode Pengurus", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Masukkan 4 digit PIN Pengurus (Ketua, Sekretaris, Bendahara, atau Wali Kelas) untuk mengedit data kelas.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(6)
                        wrongPin = false
                    },
                    label = { Text("PIN Pengurus") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = wrongPin,
                    supportingText = if (wrongPin) {
                        { Text("PIN tidak sesuai dengan daftar pengurus.") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerify(pin)) onDismiss() else wrongPin = true
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("admin_pin_submit")
            ) {
                Text("Verifikasi")
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
fun GuruPinDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var wrongPin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Masuk Mode Guru", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Masukkan PIN Guru / Wali Kelas untuk mengunggah modul ke Bank Materi CLASS XI.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(6)
                        wrongPin = false
                    },
                    label = { Text("PIN Guru") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = wrongPin,
                    supportingText = if (wrongPin) {
                        { Text("PIN guru tidak sesuai.") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guru_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerify(pin)) onDismiss() else wrongPin = true
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("guru_pin_submit")
            ) {
                Text("Buka Mode Guru")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("Batal")
            }
        }
    )
}
