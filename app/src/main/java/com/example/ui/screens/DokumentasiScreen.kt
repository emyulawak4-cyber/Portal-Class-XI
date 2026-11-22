package com.example.ui.screens

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ClassConstants
import com.example.model.DokumentasiComment
import com.example.model.DokumentasiPost
import com.example.ui.components.ClassCard
import com.example.ui.components.MediaEncodingUtil
import com.example.ui.components.MediaImageBox
import com.example.ui.components.PortalFeedback
import com.example.ui.components.StatusBadge
import com.example.ui.theme.RoseAlpa
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.SkyBlue
import com.example.viewmodel.ClassPortalData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DokumentasiScreen(
    data: ClassPortalData,
    currentUid: String,
    observeComments: (String) -> Flow<List<DokumentasiComment>>,
    onRequestAdminLogin: () -> Unit,
    onAddPost: (
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String,
        fileName: String,
        caption: String,
        date: String,
        onDone: () -> Unit
    ) -> Unit,
    onToggleLike: (DokumentasiPost) -> Unit,
    onAddComment: (dokId: String, customName: String, text: String, onDone: () -> Unit) -> Unit,
    onDeleteComment: (dokId: String, commentId: String) -> Unit,
    onDeletePost: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("Semua") }
    var showUploadDialog by remember { mutableStateOf(false) }
    var pendingSavePost by remember { mutableStateOf<DokumentasiPost?>(null) }

    val savePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        val post = pendingSavePost
        pendingSavePost = null
        if (uri != null && post != null && post.mediaUrl.startsWith("data:image")) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val b64 = post.mediaUrl.substringAfter("base64,", "")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                onShowMessage(if (ok) "Foto berhasil disimpan ke perangkat." else "Gagal menyimpan foto.")
            }
        }
    }

    val filterOptions = listOf("Semua", "Foto", "Video") + ClassConstants.DOK_CATEGORIES
    val q = data.globalSearch.trim().lowercase()
    val filteredPosts = remember(data.dokumentasi, selectedFilter, q) {
        data.dokumentasi.filter { post ->
            val matchFilter = when (selectedFilter) {
                "Semua" -> true
                "Foto" -> post.mediaType == "photo"
                "Video" -> post.mediaType == "video"
                else -> post.category == selectedFilter
            }
            val matchSearch = q.isBlank() ||
                post.title.lowercase().contains(q) ||
                post.caption.lowercase().contains(q) ||
                post.category.lowercase().contains(q) ||
                post.author.lowercase().contains(q)
            matchFilter && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            text = "Galeri Dokumentasi",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${data.dokumentasi.size} momen kegiatan CLASS XI",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (data.isAdminMode) showUploadDialog = true else onRequestAdminLogin()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dok_upload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { option ->
                        FilterChip(
                            selected = selectedFilter == option,
                            onClick = { selectedFilter = option },
                            label = { Text(option, fontSize = 12.sp) },
                            modifier = Modifier.testTag("dok_filter_${option.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        if (filteredPosts.isEmpty()) {
            item {
                ClassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = "Galeri Kosong",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Belum ada dokumentasi pada filter ini",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pengurus kelas dapat mengunggah foto kegiatan atau link video dokumentasi.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPosts, key = { it.id }) { post ->
                val comments by observeComments(post.id).collectAsStateWithLifecycle(initialValue = emptyList())
                DokumentasiPostCard(
                    post = post,
                    isLiked = post.id in data.likedDokIds,
                    canDeletePost = post.userId == currentUid,
                    currentUid = currentUid,
                    comments = comments,
                    onToggleLike = { onToggleLike(post) },
                    onAddComment = { customName, text, onDone ->
                        onAddComment(post.id, customName, text, onDone)
                    },
                    onDeleteComment = { cid -> onDeleteComment(post.id, cid) },
                    onDeletePost = { onDeletePost(post.id) },
                    onSavePhoto = {
                        pendingSavePost = post
                        val baseName = post.fileName.ifBlank { "CLASS_XI_${post.id}.jpg" }
                        savePhotoLauncher.launch(if (baseName.endsWith(".jpg", true)) baseName else "$baseName.jpg")
                    },
                    onOpenExternalMedia = {
                        if (post.mediaUrl.startsWith("http://") || post.mediaUrl.startsWith("https://")) {
                            PortalFeedback.openUrl(context, post.mediaUrl)
                        } else {
                            PortalFeedback.shareText(
                                context,
                                "Dokumentasi CLASS XI: ${post.title}",
                                "${post.title} (${post.category})\n${post.caption}\n${post.mediaUrl}"
                            )
                        }
                    }
                )
            }
        }
    }

    if (showUploadDialog) {
        UploadDokumentasiDialog(
            onDismiss = { showUploadDialog = false },
            onSubmit = { title, cat, mediaType, mediaUrl, fileName, caption, date ->
                onAddPost(title, cat, mediaType, mediaUrl, fileName, caption, date) {
                    showUploadDialog = false
                }
            }
        )
    }
}

@Composable
private fun DokumentasiPostCard(
    post: DokumentasiPost,
    isLiked: Boolean,
    canDeletePost: Boolean,
    currentUid: String,
    comments: List<DokumentasiComment>,
    onToggleLike: () -> Unit,
    onAddComment: (customName: String, text: String, onDone: () -> Unit) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDeletePost: () -> Unit,
    onSavePhoto: () -> Unit,
    onOpenExternalMedia: () -> Unit
) {
    val context = LocalContext.current
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    ClassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(
                        text = post.category,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    StatusBadge(
                        text = if (post.mediaType == "video") "Video" else "Foto",
                        containerColor = if (post.mediaType == "video") Color(0xFFE0F2FE) else Color(0xFFECFDF5),
                        contentColor = if (post.mediaType == "video") Color(0xFF0369A1) else Color(0xFF047857)
                    )
                }
                Text(
                    text = post.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${post.author} • ${post.date}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canDeletePost) {
                IconButton(
                    onClick = onDeletePost,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dok_delete_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus postingan ${post.title}",
                        tint = RoseAlpa
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (post.mediaType == "photo" && post.mediaUrl.isNotBlank()) {
            MediaImageBox(
                url = post.mediaUrl,
                contentDescription = "Foto dokumentasi ${post.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        } else if (post.mediaType == "video") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RoyalBlueDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RoyalBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = "Video Dokumentasi",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = post.fileName.ifBlank { "Video Dokumentasi CLASS XI" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = post.mediaUrl,
                                color = Color(0xFFBFDBFE),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenExternalMedia,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dok_open_video_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = "Buka video",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka", fontSize = 12.sp)
                    }
                }
            }
        }

        if (post.caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = post.caption,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dok_like_${post.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Sukai postingan",
                    tint = if (isLiked) RoseAlpa else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${post.likesCount}",
                    color = if (isLiked) RoseAlpa else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = { showComments = !showComments },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dok_comment_toggle_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = "Buka komentar",
                    tint = RoyalBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${comments.size} Komentar",
                    color = RoyalBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            }

            if (post.mediaType == "photo" && post.mediaUrl.startsWith("data:image")) {
                IconButton(
                    onClick = onSavePhoto,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dok_download_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Simpan foto ke perangkat",
                        tint = SkyBlue
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        PortalFeedback.shareText(
                            context,
                            "Dokumentasi CLASS XI: ${post.title}",
                            "${post.title} (${post.category})\n${post.caption}\n${post.mediaUrl}"
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dok_share_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Bagikan dokumentasi",
                        tint = SkyBlue
                    )
                }
            }
        }

        if (showComments) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (comments.isEmpty()) {
                        Text(
                            text = "Belum ada komentar. Jadilah yang pertama berkomentar!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        comments.forEach { comment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${comment.senderName} • ${comment.timeLabel}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                    Text(
                                        text = comment.text,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (comment.userId == currentUid) {
                                    IconButton(
                                        onClick = { onDeleteComment(comment.id) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("dok_del_comment_${comment.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Hapus komentar",
                                            tint = RoseAlpa,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it.take(40) },
                        label = { Text("Nama (opsional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dok_comment_name_${post.id}")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it.take(240) },
                            placeholder = { Text("Tulis komentar...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dok_comment_input_${post.id}")
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onAddComment(customName, commentText) {
                                        commentText = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dok_comment_send_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Kirim komentar",
                                tint = RoyalBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadDokumentasiDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        category: String,
        mediaType: String,
        mediaUrl: String,
        fileName: String,
        caption: String,
        date: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ClassConstants.DOK_CATEGORIES.first()) }
    var mediaType by remember { mutableStateOf("photo") }
    var mediaUrl by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var encodingPhoto by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            encodingPhoto = true
            errorMsg = null
            scope.launch {
                val encoded = MediaEncodingUtil.encodeCompressedImageDataUrl(context, uri)
                encodingPhoto = false
                if (encoded != null) {
                    mediaUrl = encoded
                    fileName = MediaEncodingUtil.getFileName(context, uri)
                } else {
                    errorMsg = "Ukuran gambar terlalu besar atau gagal diproses."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Upload Dokumentasi CLASS XI", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mediaType == "photo",
                        onClick = { mediaType = "photo" },
                        label = { Text("Foto") },
                        modifier = Modifier.testTag("dok_type_photo")
                    )
                    FilterChip(
                        selected = mediaType == "video",
                        onClick = { mediaType = "video" },
                        label = { Text("Link Video") },
                        modifier = Modifier.testTag("dok_type_video")
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Judul Kegiatan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dok_dialog_title")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClassConstants.DOK_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                if (mediaType == "photo") {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("dok_pick_photo_button")
                    ) {
                        if (encodingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengompresi Foto...")
                        } else {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (fileName.isBlank()) "Pilih Foto dari Galeri" else "Terpilih: $fileName")
                        }
                    }
                }

                OutlinedTextField(
                    value = if (mediaUrl.startsWith("data:image")) "(Foto terlampir dari galeri)" else mediaUrl,
                    onValueChange = {
                        if (!it.startsWith("(Foto terlampir")) {
                            mediaUrl = it.trim()
                        }
                    },
                    label = { Text(if (mediaType == "video") "URL Video (YouTube / Drive)" else "Atau tempel URL Foto HTTPS") },
                    singleLine = true,
                    enabled = !mediaUrl.startsWith("data:image"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dok_dialog_url")
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it.take(10) },
                    label = { Text("Tanggal (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(300) },
                    label = { Text("Keterangan / Caption") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dok_dialog_caption")
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = RoseAlpa, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !encodingPhoto,
                onClick = {
                    when {
                        title.isBlank() -> errorMsg = "Judul kegiatan wajib diisi."
                        mediaUrl.isBlank() -> errorMsg = "Pilih foto atau isi URL media terlebih dahulu."
                        else -> onSubmit(title.trim(), category, mediaType, mediaUrl, fileName, caption.trim(), date)
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dok_dialog_submit")
            ) {
                Text("Publikasikan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Batal")
            }
        }
    )
}
