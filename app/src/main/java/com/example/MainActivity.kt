package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.AuthManager
import com.example.data.ThemeMode
import com.example.model.AttendanceStatus
import com.example.model.UiState
import com.example.notifications.ClassNotificationWorker
import com.example.notifications.NotificationHelper
import com.example.ui.components.GlassBackdropBox
import com.example.ui.screens.AdminPinDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DokumentasiScreen
import com.example.ui.screens.GuruPinDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InfoScreen
import com.example.ui.screens.IzinScreen
import com.example.ui.screens.JadwalScreen
import com.example.ui.screens.KasScreen
import com.example.ui.screens.MateriScreen
import com.example.ui.screens.PresensiScreen
import com.example.ui.screens.QrCardsScreen
import com.example.ui.screens.QrScannerDialog
import com.example.ui.screens.RekapScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashWelcomeOverlay
import com.example.ui.screens.StudentQrDialog
import com.example.ui.theme.ClassXiTheme
import com.example.ui.theme.EmeraldKas
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBright
import com.example.ui.theme.RoyalBlueDark
import com.example.viewmodel.ClassPortalData
import com.example.viewmodel.ClassViewModel
import com.example.viewmodel.ToastEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object BerandaRoute
@Serializable data object PresensiRoute
@Serializable data object KasRoute
@Serializable data object SuratIzinRoute
@Serializable data object JadwalRoute
@Serializable data object QrRoute
@Serializable data object DokumentasiRoute
@Serializable data object InfoRoute
@Serializable data object MateriRoute
@Serializable data object RekapRoute
@Serializable data object SettingsRoute

private data class PrimaryNavItem(
    val routeObj: Any,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

private fun routeObjFromKey(key: String): Any = when (key.lowercase()) {
    "presensi" -> PresensiRoute
    "kas" -> KasRoute
    "izin", "surat_izin" -> SuratIzinRoute
    "jadwal" -> JadwalRoute
    "qr" -> QrRoute
    "dokumentasi" -> DokumentasiRoute
    "info" -> InfoRoute
    "materi" -> MateriRoute
    "rekap" -> RekapRoute
    "settings", "pengaturan" -> SettingsRoute
    else -> BerandaRoute
}

class MainActivity : ComponentActivity() {
    private var deepLinkTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        ClassNotificationWorker.schedulePeriodicSync(this)
        deepLinkTab = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_TAB)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val credentialManager = remember { CredentialManager.create(context) }
            var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
            var checkingAutoSignIn by remember { mutableStateOf(currentUser == null) }

            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    currentUser = auth.currentUser
                    if (auth.currentUser != null) {
                        checkingAutoSignIn = false
                    }
                }
                Firebase.auth.addAuthStateListener(listener)
                onDispose { Firebase.auth.removeAuthStateListener(listener) }
            }

            LaunchedEffect(Unit) {
                if (Firebase.auth.currentUser == null) {
                    AuthManager.attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = { checkingAutoSignIn = false },
                        onUnauthenticated = { checkingAutoSignIn = false },
                        scope = scope
                    )
                }
            }

            val signedInUser = currentUser
            if (signedInUser == null) {
                ClassXiTheme {
                    if (checkingAutoSignIn) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlue, RoyalBlueDark))),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                            }
                        )
                    }
                }
            } else {
                key(signedInUser.uid) {
                    val viewModel: ClassViewModel = viewModel(
                        key = signedInUser.uid,
                        factory = ClassViewModel.Factory(context)
                    )
                    ClassXiPortalRoot(
                        viewModel = viewModel,
                        currentUser = signedInUser,
                        deepLinkTab = deepLinkTab,
                        onConsumeDeepLink = { deepLinkTab = null },
                        onSignOut = {
                            AuthManager.signOut(credentialManager, scope)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkTab = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_TAB)
    }
}

@Composable
private fun ClassXiPortalRoot(
    viewModel: ClassViewModel,
    currentUser: FirebaseUser,
    deepLinkTab: String?,
    onConsumeDeepLink: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSplashOverlay by remember { mutableStateOf(true) }
    var showAdminPinDialog by remember { mutableStateOf(false) }
    var showGuruPinDialog by remember { mutableStateOf(false) }
    var activeQrStudentId by remember { mutableStateOf<Int?>(null) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var pendingPresensiFilter by remember { mutableStateOf<AttendanceStatus?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            snackbarHostState.showSnackbar(
                if (granted) "Notifikasi perangkat aktif! Info dari teman akan muncul." else "Izin notifikasi ditolak; pengumuman tetap bisa dilihat di tab Info."
            )
        }
    }

    LaunchedEffect(currentUser.uid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { event ->
            if (event is ToastEvent.Message) {
                snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    fun navigateTo(routeObj: Any) {
        navController.navigate(routeObj) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(deepLinkTab) {
        if (!deepLinkTab.isNullOrBlank()) {
            showSplashOverlay = false
            navigateTo(routeObjFromKey(deepLinkTab))
            onConsumeDeepLink()
        }
    }

    BackHandler(enabled = showSplashOverlay) {
        showSplashOverlay = false
    }

    val portalData = (uiState as? UiState.Success)?.data ?: ClassPortalData()

    ClassXiTheme(
        themeMode = portalData.preferences.themeMode,
        themeStyle = portalData.preferences.themeStyle
    ) {
        val primaryItems = listOf(
            PrimaryNavItem(BerandaRoute, stringResource(R.string.nav_beranda), Icons.Rounded.Home, "nav_beranda"),
            PrimaryNavItem(PresensiRoute, stringResource(R.string.nav_presensi), Icons.Rounded.FactCheck, "nav_presensi"),
            PrimaryNavItem(KasRoute, stringResource(R.string.nav_kas), Icons.Rounded.AccountBalanceWallet, "nav_kas"),
            PrimaryNavItem(InfoRoute, stringResource(R.string.nav_info), Icons.Rounded.Campaign, "nav_info"),
            PrimaryNavItem(SettingsRoute, stringResource(R.string.nav_pengaturan), Icons.Rounded.Settings, "nav_settings")
        )

        GlassBackdropBox {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isExpandedWidth = maxWidth >= 840.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedWidth) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            primaryItems.forEach { item ->
                                val selected = currentDest?.hasRoute(item.routeObj::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.routeObj) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, fontSize = 11.sp, maxLines = 1) },
                                    modifier = Modifier.testTag(item.tag)
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        topBar = {
                            ClassXiTopHeader(
                                data = portalData,
                                onSearchChange = viewModel::setGlobalSearch,
                                onToggleAdmin = {
                                    if (portalData.isAdminMode) viewModel.exitAdminMode() else showAdminPinDialog = true
                                },
                                onToggleDarkTheme = {
                                    val next = when (portalData.preferences.themeMode) {
                                        ThemeMode.DARK -> ThemeMode.LIGHT
                                        else -> ThemeMode.DARK
                                    }
                                    viewModel.setThemeMode(next)
                                }
                            )
                        },
                        bottomBar = {
                            if (!isExpandedWidth) {
                                NavigationBar(
                                    windowInsets = WindowInsets.navigationBars,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    primaryItems.forEach { item ->
                                        val selected = currentDest?.hasRoute(item.routeObj::class) == true
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateTo(item.routeObj) },
                                            icon = { Icon(item.icon, contentDescription = item.label) },
                                            label = {
                                                Text(
                                                    text = item.label,
                                                    fontSize = 10.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            modifier = Modifier.testTag(item.tag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val state = uiState) {
                                is UiState.Loading -> {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(color = RoyalBlue)
                                            Text("Menyinkronkan data CLASS XI...", fontSize = 13.sp)
                                        }
                                    }
                                }

                                is UiState.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("Sinkronisasi Terganggu", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                            Text(state.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                is UiState.Success -> {
                                    val data = state.data
                                    NavHost(
                                        navController = navController,
                                        startDestination = BerandaRoute,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        composable<BerandaRoute> {
                                            HomeScreen(
                                                data = data,
                                                onNavigate = { key -> navigateTo(routeObjFromKey(key)) },
                                                onOpenStudentQr = { sid -> activeQrStudentId = sid },
                                                onOpenScanner = { showScannerDialog = true },
                                                onFilterPresensi = { st ->
                                                    pendingPresensiFilter = st
                                                    navigateTo(PresensiRoute)
                                                }
                                            )
                                        }
                                        composable<PresensiRoute> {
                                            PresensiScreen(
                                                data = data,
                                                initialFilter = pendingPresensiFilter,
                                                onSetDate = viewModel::setSelectedDate,
                                                onSetToday = viewModel::setTodayDate,
                                                onUpdateStatus = viewModel::updateStudentAttendance,
                                                onSetAllHadir = { viewModel.setAllAttendance(AttendanceStatus.Hadir) },
                                                onOpenStudentQr = { sid -> activeQrStudentId = sid },
                                                onOpenScanner = { showScannerDialog = true },
                                                onRequestAdminLogin = { showAdminPinDialog = true }
                                            )
                                        }
                                        composable<KasRoute> {
                                            KasScreen(
                                                data = data,
                                                onSelectPeriod = viewModel::setSelectedKasPeriod,
                                                onSelectNominal = viewModel::setSelectedKasNominal,
                                                onAddPeriod = viewModel::addKasPeriod,
                                                onToggleStudentPayment = viewModel::toggleStudentKasPayment,
                                                onMarkAllPaid = viewModel::markAllKasPaid,
                                                onAddTransaction = viewModel::addKasTransaction,
                                                onDeleteTransaction = viewModel::voidOrDeleteKasTransaction,
                                                onRequestAdminLogin = { showAdminPinDialog = true }
                                            )
                                        }
                                        composable<SuratIzinRoute> {
                                            IzinScreen(
                                                data = data,
                                                currentUid = currentUser.uid,
                                                onRequestAdminLogin = { showAdminPinDialog = true },
                                                onSubmitLeave = viewModel::submitLeaveRequest,
                                                onVerifyLeave = viewModel::verifyLeaveRequest,
                                                onDeleteLeave = viewModel::deleteLeaveRequest,
                                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                            )
                                        }
                                        composable<JadwalRoute> {
                                            JadwalScreen(
                                                data = data,
                                                onSelectDay = viewModel::setSelectedScheduleDay,
                                                onOpenStudentQr = { sid -> activeQrStudentId = sid }
                                            )
                                        }
                                        composable<QrRoute> {
                                            QrCardsScreen(
                                                data = data,
                                                onOpenStudentQr = { sid -> activeQrStudentId = sid },
                                                onOpenScanner = { showScannerDialog = true }
                                            )
                                        }
                                        composable<DokumentasiRoute> {
                                            DokumentasiScreen(
                                                data = data,
                                                currentUid = currentUser.uid,
                                                observeComments = viewModel::observeComments,
                                                onRequestAdminLogin = { showAdminPinDialog = true },
                                                onAddPost = viewModel::addDokumentasiPost,
                                                onToggleLike = viewModel::toggleDokumentasiLike,
                                                onAddComment = viewModel::addDokumentasiComment,
                                                onDeleteComment = viewModel::deleteDokumentasiComment,
                                                onDeletePost = viewModel::deleteDokumentasiPost,
                                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                            )
                                        }
                                        composable<InfoRoute> {
                                            InfoScreen(
                                                data = data,
                                                currentUid = currentUser.uid,
                                                onRequestAdminLogin = { showAdminPinDialog = true },
                                                onRequestNotificationPermission = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                },
                                                onSendTestNotification = viewModel::sendTestFriendNotification,
                                                onAddAnnouncement = viewModel::addAnnouncement,
                                                onDeleteAnnouncement = viewModel::deleteAnnouncement
                                            )
                                        }
                                        composable<MateriRoute> {
                                            MateriScreen(
                                                data = data,
                                                currentUid = currentUser.uid,
                                                onRequestGuruLogin = { showGuruPinDialog = true },
                                                onExitGuruMode = viewModel::exitGuruMode,
                                                onAddMateri = viewModel::addMateri,
                                                onIncrementDownload = viewModel::incrementMateriDownload,
                                                onDeleteMateri = viewModel::deleteMateri,
                                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                            )
                                        }
                                        composable<RekapRoute> {
                                            RekapScreen(
                                                data = data,
                                                onOpenStudentQr = { sid -> activeQrStudentId = sid },
                                                onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                                            )
                                        }
                                        composable<SettingsRoute> {
                                            SettingsScreen(
                                                data = data,
                                                currentUser = currentUser,
                                                onSetThemeMode = viewModel::setThemeMode,
                                                onSetThemeStyle = viewModel::setThemeStyle,
                                                onSetNotificationsEnabled = viewModel::setNotificationsEnabled,
                                                onRequestNotificationPermission = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                },
                                                onSendTestNotification = viewModel::sendTestFriendNotification,
                                                onRequestAdminLogin = { showAdminPinDialog = true },
                                                onExitAdminMode = viewModel::exitAdminMode,
                                                onRequestGuruLogin = { showGuruPinDialog = true },
                                                onExitGuruMode = viewModel::exitGuruMode,
                                                onSignOut = onSignOut
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeQrStudentId != null) {
                StudentQrDialog(
                    studentId = activeQrStudentId!!,
                    isAdminMode = portalData.isAdminMode,
                    onMarkPresent = { sid -> viewModel.updateStudentAttendance(sid, AttendanceStatus.Hadir) },
                    onRequestAdminLogin = { showAdminPinDialog = true },
                    onDismiss = { activeQrStudentId = null }
                )
            }

            if (showScannerDialog) {
                QrScannerDialog(
                    isAdminMode = portalData.isAdminMode,
                    onScanQr = viewModel::processScannedStudentQr,
                    onRequestAdminLogin = { showAdminPinDialog = true },
                    onDismiss = { showScannerDialog = false }
                )
            }

            if (showAdminPinDialog) {
                AdminPinDialog(
                    onDismiss = { showAdminPinDialog = false },
                    onVerify = viewModel::verifyAdminPin
                )
            }

            if (showGuruPinDialog) {
                GuruPinDialog(
                    onDismiss = { showGuruPinDialog = false },
                    onVerify = viewModel::verifyGuruPin
                )
            }

            SplashWelcomeOverlay(
                visible = showSplashOverlay,
                onDismiss = { showSplashOverlay = false }
            )
        }
    }
}

@Composable
private fun ClassXiTopHeader(
    data: ClassPortalData,
    onSearchChange: (String) -> Unit,
    onToggleAdmin: () -> Unit,
    onToggleDarkTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(RoyalBlueBright, RoyalBlue, RoyalBlueDark)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.School, contentDescription = "Logo CLASS XI", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CLASS XI",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (data.isOnlineSynced) EmeraldKas else RoseAlpa)
                        )
                    }
                    Text(
                        text = stringResource(R.string.school_name),
                        color = Color(0xFFDBEAFE),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (data.isAdminMode) Color(0xFF10B981) else Color.White.copy(alpha = 0.18f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onToggleAdmin() }
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_admin_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = "Mode Pengurus",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (data.isAdminMode) data.activePengurusRole else "Pengurus",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onToggleDarkTheme,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (data.preferences.themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "Ganti mode tema",
                        tint = Color.White
                    )
                }
            }
        }

        OutlinedTextField(
            value = data.globalSearch,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = "Cari siswa, pengumuman, materi, atau galeri...",
                    fontSize = 12.5.sp,
                    color = Color(0xFFDBEAFE)
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFFDBEAFE), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (data.globalSearch.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("topbar_search_clear")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedContainerColor = Color.White.copy(alpha = 0.14f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.12f),
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("topbar_global_search")
        )
    }
}
