package com.example.tvdrive.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import coil.annotation.ExperimentalCoilApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.theme.*
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.GlassButton
import com.example.tvdrive.ui.components.TvFocusableItem
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onOpenDownloads: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val email = container.authManager.getCurrentEmail()

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
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Account card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        SettingsSectionHeader("Google Account")
                        SettingsInfoRow("Signed in as", email)
                        Spacer(Modifier.height(16.dp))
                        GlassButton(
                            text = "Sign Out",
                            iconVector = Icons.Rounded.Logout,
                            isPrimary = true,
                            onClick = onSignOut,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }

                // Storage & Cache card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        SettingsSectionHeader("Storage & Cache")
                        SettingsInfoRow("Image Cache Size", "Disk: 200 MB max  •  RAM: 15% of available")
                        SettingsInfoRow("Cache Invalidation", "5 minutes auto-refresh on folder open")
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassButton(
                                text = "Clear Image Cache",
                                iconVector = Icons.Rounded.DeleteSweep,
                                isPrimary = false,
                                onClick = {
                                    @OptIn(ExperimentalCoilApi::class)
                                    scope.launch {
                                        container.imageLoader.diskCache?.clear()
                                        container.imageLoader.memoryCache?.clear()
                                    }
                                },
                                modifier = Modifier.width(220.dp)
                            )
                            if (onOpenDownloads != null) {
                                GlassButton(
                                    text = "Offline Downloads",
                                    iconVector = Icons.Rounded.Download,
                                    isPrimary = false,
                                    onClick = onOpenDownloads,
                                    modifier = Modifier.width(220.dp)
                                )
                            }
                        }
                    }
                }

                // About card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        SettingsSectionHeader("About NovaDrive TV")
                        SettingsInfoRow("Application", "NovaDrive TV v1.0")
                        SettingsInfoRow("Architecture", "Jetpack Compose for TV  •  Clean Architecture")
                        SettingsInfoRow("Optimised for", "Android TV & Google TV (Low RAM 1 GB Friendly)")
                        SettingsInfoRow("Cloud Protocols", "Google Drive v3 API  •  Google Photos Library API")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFF2563EB),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.SemiBold
        )
    }
}
