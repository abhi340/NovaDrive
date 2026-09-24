package com.example.tvdrive.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.data.local.DownloadEntity
import com.example.tvdrive.theme.*
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.TvFocusableItem

@Composable
fun DownloadsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val downloads by container.database.downloadDao().observeAll()
        .collectAsState(initial = emptyList())

    AmbientGlowBackground {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFE2E8F0))
                    .padding(horizontal = 32.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TvFocusableItem(onClick = onBack, cornerRadius = 8.dp) { focused ->
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (focused) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = if (focused) Color(0xFF2563EB) else Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (focused) Color(0xFF2563EB) else Color(0xFF334155),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (downloads.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No downloaded offline files yet",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloads, key = { it.fileId }) { download ->
                        DownloadRow(download)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(download: DownloadEntity) {
    val (icon, tint) = when {
        download.mimeType.startsWith("video") -> Icons.Rounded.PlayCircle to Color(0xFFDC2626)
        download.mimeType.startsWith("audio") -> Icons.Rounded.Audiotrack to Color(0xFF7C3AED)
        else -> Icons.Rounded.Description to Color(0xFF2563EB)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = download.fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = download.localPath,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val statusColor = when (download.status) {
            "COMPLETED" -> Color(0xFF059669)
            "FAILED" -> Color(0xFFDC2626)
            else -> Color(0xFFD97706)
        }
        Text(
            text = download.status,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            fontWeight = FontWeight.Bold
        )
    }
}
