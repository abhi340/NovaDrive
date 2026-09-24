package com.example.tvdrive.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvdrive.HomeTab
import com.example.tvdrive.theme.*
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.GlassButton

@Composable
fun HomeScreen(
    selectedTab: HomeTab,
    onTabChange: (HomeTab) -> Unit,
    onOpenFolder: (id: String, name: String) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val driveFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        driveFocusRequester.requestFocus()
    }

    AmbientGlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 56.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP BAR: Branding & Quick Action Buttons ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.tvdrive.R.drawable.app_logo),
                        contentDescription = "NovaDrive Logo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = "NovaDrive",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TV",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        text = "Search",
                        iconVector = Icons.Rounded.Search,
                        isPrimary = false,
                        onClick = onOpenSearch
                    )
                    GlassButton(
                        text = "Downloads",
                        iconVector = Icons.Rounded.Download,
                        isPrimary = false,
                        onClick = onOpenDownloads
                    )
                    GlassButton(
                        text = "Settings",
                        iconVector = Icons.Rounded.Settings,
                        isPrimary = false,
                        onClick = onOpenSettings
                    )
                }
            }

            // ── CENTER: 2 Main Service Cards (Google Drive vs Google Photos) ───
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SELECT SERVICE",
                        color = Color(0xFF2563EB),
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "What would you like to browse?",
                        color = Color(0xFF0F172A),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option 1: Google Drive
                    ServiceCard(
                        title = "Google Drive",
                        subtitle = "Access your cloud folders, stream videos, view documents & recent files",
                        badgeText = "FILES & CLOUD",
                        iconVector = Icons.Rounded.Folder,
                        accentColor = Color(0xFF2563EB),
                        onClick = { onOpenFolder("root", "My Drive") },
                        focusRequester = driveFocusRequester,
                        modifier = Modifier
                            .width(380.dp)
                            .height(230.dp)
                    )

                    // Option 2: Google Photos
                    ServiceCard(
                        title = "Google Photos",
                        subtitle = "Browse picture albums, family memories, full-screen slideshows & clips",
                        badgeText = "PHOTOS & MEMORIES",
                        iconVector = Icons.Rounded.PhotoLibrary,
                        accentColor = Color(0xFF059669),
                        onClick = onOpenAlbums,
                        modifier = Modifier
                            .width(380.dp)
                            .height(230.dp)
                    )
                }
            }

            // ── BOTTOM: D-Pad Navigation Hint ────────────────────────────────
            Text(
                text = "Use TV remote Left / Right arrows to choose service  •  Press OK to open",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
private fun ServiceCard(
    title: String,
    subtitle: String,
    badgeText: String,
    iconVector: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(130),
        label = "service_card_scale"
    )

    val shape = RoundedCornerShape(22.dp)
    val backgroundColor = if (isFocused) accentColor.copy(alpha = 0.08f) else Color.White
    val borderColor = if (isFocused) accentColor else Color(0xFFE2E8F0)
    val borderWidth = if (isFocused) 3.dp else 1.dp

    Box(
        modifier = modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .padding(24.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFocused) accentColor.copy(alpha = 0.12f) else Color(0xFFF1F5F9))
                        .border(1.dp, if (isFocused) accentColor.copy(alpha = 0.3f) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = if (isFocused) accentColor else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isFocused) accentColor else Color(0xFFCBD5E1))
                )
                Text(
                    text = if (isFocused) "Press OK to open" else "Select to browse",
                    color = if (isFocused) accentColor else Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
