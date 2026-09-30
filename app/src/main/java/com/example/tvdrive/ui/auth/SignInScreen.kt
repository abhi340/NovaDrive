package com.example.tvdrive.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.R
import com.example.tvdrive.auth.AuthState
import com.example.tvdrive.auth.DeviceFlowState
import com.example.tvdrive.ui.components.AmbientGlowBackground
import com.example.tvdrive.ui.components.GlassBox
import com.example.tvdrive.ui.components.GlassButton
import com.example.tvdrive.ui.components.QrCodeImage

@Composable
fun SignInScreen(
    authState: AuthState,
    onStartSignIn: () -> Unit
) {
    val container = LocalAppContainer.current
    val deviceFlowState by container.deviceAuthManager.deviceFlowState.collectAsState()
    val tvSignInFocusRequester = remember { FocusRequester() }

    // Start device code flow on entry
    LaunchedEffect(Unit) {
        if (deviceFlowState is DeviceFlowState.Idle || deviceFlowState is DeviceFlowState.Error) {
            container.deviceAuthManager.startDeviceFlow { accessToken, refreshToken, expiresIn, email ->
                container.authManager.saveManualToken(accessToken, refreshToken, expiresIn, email)
            }
        }
        tvSignInFocusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            container.deviceAuthManager.cancel()
        }
    }

    AmbientGlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP HEADER ───────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "NovaDrive Logo",
                    modifier = Modifier.height(38.dp),
                    contentScale = ContentScale.Fit
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
                Text(
                    text = "•   Connect Google Drive",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            // ── MAIN DUAL SIGN-IN CARD ───────────────────────────────────────
            GlassBox(
                modifier = Modifier
                    .width(880.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = Color.White,
                borderColor = Color(0xFFE2E8F0),
                borderWidth = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ── LEFT: PHONE / QR CODE FLOW (Google RFC 8628) ────────
                    Column(
                        modifier = Modifier
                            .weight(1.15f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Sign in via Phone / PC",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Text(
                            text = "Scan with your phone camera or visit google.com/device",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        when (val state = deviceFlowState) {
                            is DeviceFlowState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFF8FAFC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }

                            is DeviceFlowState.CodeReady -> {
                                QrCodeImage(
                                    content = state.directQrUrl,
                                    size = 160.dp
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = state.userCode,
                                        color = Color(0xFF1E40AF),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Waiting for authorization on phone...",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            is DeviceFlowState.Success -> {
                                Box(
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = "Connected!",
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            is DeviceFlowState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = state.message,
                                        color = Color(0xFFDC2626),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    GlassButton(
                                        text = "Reload QR Code",
                                        iconVector = Icons.Rounded.Refresh,
                                        onClick = {
                                            container.deviceAuthManager.startDeviceFlow { acc, ref, exp, em ->
                                                container.authManager.saveManualToken(acc, ref, exp, em)
                                            }
                                        }
                                    )
                                }
                            }

                            else -> Unit
                        }
                    }

                    // ── VERTICAL DIVIDER ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(240.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    // ── RIGHT: TV 1-CLICK REMOTE SIGN-IN ────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tv,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Sign in on TV",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Text(
                            text = "Use the Google account already signed into this Android TV device.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        if (authState is AuthState.Loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Connecting...",
                                    color = Color(0xFF2563EB),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            GlassButton(
                                text = "Sign in on TV",
                                iconVector = Icons.Rounded.AccountCircle,
                                isPrimary = true,
                                onClick = onStartSignIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .focusRequester(tvSignInFocusRequester)
                            )
                        }

                        if (authState is AuthState.Error) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = authState.message,
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = "Tip: Phone QR code bypasses TV Play Services limitations.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── BOTTOM FOOTER ────────────────────────────────────────────────
            Text(
                text = "Use TV remote arrows to navigate  •  Press OK to select",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}
