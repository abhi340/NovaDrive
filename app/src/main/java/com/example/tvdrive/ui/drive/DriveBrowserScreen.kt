package com.example.tvdrive.ui.drive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.model.DriveFile
import com.example.tvdrive.data.model.FileCategory
import com.example.tvdrive.theme.*
import com.example.tvdrive.ui.components.FileCard
import com.example.tvdrive.ui.components.GlassButton
import com.example.tvdrive.ui.components.TvFocusableItem

@Composable
fun DriveBrowserScreen(
    folderId: String,
    folderName: String,
    onOpenFolder: (String, String) -> Unit,
    onPlayVideo: (url: String, id: String, title: String) -> Unit,
    onPlayAudio: (url: String, id: String, title: String) -> Unit,
    onViewImage: (urls: List<String>, idx: Int, title: String) -> Unit,
    onOpenPdf: (id: String, title: String) -> Unit,
    onStartSlideshow: (urls: List<String>) -> Unit,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val vm: DriveBrowserViewModel = viewModel(
        key = folderId,
        factory = DriveBrowserViewModel.Factory(container.driveRepository, folderId, folderName)
    )
    val state by vm.state.collectAsState()

    val currentImageUrls = remember(state) {
        (state as? DriveUiState.Success)?.files?.filter { it.isImage }?.map { vm.streamUrl(it.id) } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 32.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GlassButton(
                text = "Back",
                iconVector = Icons.Rounded.ArrowBack,
                onClick = onBack
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEFF6FF), androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = folderName,
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(Modifier.weight(1f))

            if (currentImageUrls.isNotEmpty()) {
                GlassButton(
                    text = "Slideshow",
                    iconVector = Icons.Rounded.PlayArrow,
                    isPrimary = true,
                    onClick = { onStartSlideshow(currentImageUrls) }
                )
            }

            GlassButton(
                text = "Refresh",
                iconVector = Icons.Rounded.Refresh,
                onClick = { vm.refresh() }
            )
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (val s = state) {
            is DriveUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            }
            is DriveUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = s.message,
                            color = Color(0xFF475569),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        GlassButton(
                            text = "Retry",
                            isPrimary = true,
                            onClick = { vm.refresh() }
                        )
                    }
                }
            }
            is DriveUiState.Success -> {
                if (s.files.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = "This folder is empty",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            GlassButton(
                                text = "Refresh",
                                iconVector = Icons.Rounded.Refresh,
                                isPrimary = true,
                                onClick = { vm.refresh() }
                            )
                        }
                    }
                } else {
                    // Collect all image URLs for viewer navigation
                    val imageUrls = remember(s.files) {
                        s.files.filter { it.isImage }.map { vm.streamUrl(it.id) }
                    }

                    var unsupportedFile by remember { mutableStateOf<DriveFile?>(null) }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(s.files, key = { it.id }) { file ->
                            FileCard(
                                file = file,
                                onClick = {
                                    handleFileClick(
                                        file = file,
                                        imageUrls = imageUrls,
                                        allFiles = s.files,
                                        vm = vm,
                                        onOpenFolder = onOpenFolder,
                                        onPlayVideo = onPlayVideo,
                                        onPlayAudio = onPlayAudio,
                                        onViewImage = onViewImage,
                                        onOpenPdf = onOpenPdf,
                                        onUnsupported = { unsupportedFile = it }
                                    )
                                }
                            )
                        }
                    }

                    // Unsupported File Dialog
                    unsupportedFile?.let { file ->
                        val ext = file.name.substringAfterLast('.', "").uppercase()
                        val typeLabel = when {
                            ext.isNotEmpty() -> "$ext format"
                            file.mimeType.contains("spreadsheet") || file.mimeType.contains("excel") -> "Spreadsheet"
                            file.mimeType.contains("presentation") || file.mimeType.contains("powerpoint") -> "Presentation"
                            file.mimeType.contains("document") || file.mimeType.contains("word") -> "Word Document"
                            else -> "this type of"
                        }
                        AlertDialog(
                            onDismissRequest = { unsupportedFile = null },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            containerColor = Color.White,
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFFFEF2F2), androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "File Format Unsupported",
                                    color = Color(0xFF0F172A),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "We currently don't support $typeLabel files (\"${file.name}\").",
                                        color = Color(0xFF334155),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "NovaDrive TV supports Videos, Music/Audio, Photos, and PDF documents.",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            confirmButton = {
                                GlassButton(
                                    text = "OK",
                                    isPrimary = true,
                                    onClick = { unsupportedFile = null }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun handleFileClick(
    file: DriveFile,
    imageUrls: List<String>,
    allFiles: List<DriveFile>,
    vm: DriveBrowserViewModel,
    onOpenFolder: (String, String) -> Unit,
    onPlayVideo: (String, String, String) -> Unit,
    onPlayAudio: (String, String, String) -> Unit,
    onViewImage: (List<String>, Int, String) -> Unit,
    onOpenPdf: (String, String) -> Unit,
    onUnsupported: (DriveFile) -> Unit
) {
    val streamUrl = vm.streamUrl(file.id)
    when (file.fileCategory) {
        FileCategory.FOLDER -> onOpenFolder(file.id, file.name)
        FileCategory.VIDEO  -> onPlayVideo(streamUrl, file.id, file.name)
        FileCategory.AUDIO  -> onPlayAudio(streamUrl, file.id, file.name)
        FileCategory.IMAGE  -> {
            val idx = imageUrls.indexOf(streamUrl).coerceAtLeast(0)
            onViewImage(imageUrls, idx, file.name)
        }
        FileCategory.PDF    -> onOpenPdf(file.id, file.name)
        else -> onUnsupported(file)
    }
}
