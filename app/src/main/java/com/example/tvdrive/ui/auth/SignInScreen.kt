package com.example.tvdrive.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvdrive.LocalAppContainer
import com.example.tvdrive.auth.AuthState
import com.example.tvdrive.auth.DeviceFlowState
import com.example.tvdrive.theme.*
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
    val deviceAuthManager = container.deviceAuthManager
    val deviceFlowState by deviceAuthManager.deviceFlowState.collectAsState()

    // Start device code generation on mount
    LaunchedEffect(Unit) {
        deviceAuthManager.startDeviceFlow { accessToken, refreshToken, expiresIn, email ->
            container.authManager.setDeviceAuthorizedToken(accessToken, refreshToken, expiresIn, email)
        }
    }

    AmbientGlowBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP HEADER: Sleek Minimalist Brand Bar ───────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.tvdrive.R.drawable.app_logo),
                    contentDescription = "NovaDrive Logo",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Text(
                    text = "NovaDrive",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
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
                Text(
                    text = "•   Connect Google Drive & Photos",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }

            // ── MAIN CONTENT: 2-Column TV Layout ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentFlow = deviceFlowState
                val isReady = currentFlow is DeviceFlowState.CodeReady
                val isLoading = currentFlow is DeviceFlowState.Loading
                val isError = currentFlow is DeviceFlowState.Error

                val qrUrl = (currentFlow as? DeviceFlowState.CodeReady)?.directQrUrl
                    ?: "https://www.google.com/device"

                // ── LEFT COLUMN: QR Code Panel ─────────────────────────
                GlassBox(
                    modifier = Modifier
                        .width(340.dp)
                        .height(355.dp)
                        .padding(end = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color.White,
                    borderColor = Color(0xFFE2E8F0),
                    borderWidth = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "Scan with Phone",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        // QR Code frame
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                .size(186.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isReady) {
                                QrCodeImage(
                                    content = qrUrl,
                                    size = 170.dp
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF2563EB),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Connecting to Google...",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isReady) "Point camera at QR code to sign in instantly" else "Contacting Google OAuth...",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ── RIGHT COLUMN: Code Verification & TV Actions ───────────
                val rawUserCode = (currentFlow as? DeviceFlowState.CodeReady)?.userCode ?: ""
                val formattedCode = when {
                    isLoading -> "CONNECTING..."
                    isError -> "ERROR"
                    rawUserCode.contains("-") -> rawUserCode.replace("-", " - ")
                    rawUserCode.length == 8 -> "${rawUserCode.substring(0, 4)} - ${rawUserCode.substring(4)}"
                    rawUserCode.isNotBlank() -> rawUserCode
                    else -> "WAITING..."
                }

                val verifyUrl = (currentFlow as? DeviceFlowState.CodeReady)?.verificationUrl ?: "google.com/device"
                val cleanVerifyUrl = verifyUrl.removePrefix("https://").removePrefix("http://")

                GlassBox(
                    modifier = Modifier
                        .width(440.dp)
                        .height(355.dp)
                        .padding(start = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color.White,
                    borderColor = Color(0xFFE2E8F0),
                    borderWidth = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 26.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header instructions
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Or enter code manually:",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "1. Visit",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = cleanVerifyUrl,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "on phone or laptop",
                                    color = Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Prominent verification code card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "VERIFICATION CODE",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = formattedCode,
                                    color = if (isError) Color(0xFFDC2626) else Color(0xFF0F172A),
                                    fontSize = if (formattedCode.length > 14) 22.sp else 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = if (formattedCode.length > 14) 2.sp else 4.sp
                                )
                            }
                        }

                        // Status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2563EB),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when {
                                    isLoading -> "Contacting Google authentication..."
                                    isError -> (currentFlow as DeviceFlowState.Error).message
                                    else -> "Waiting for authorization on your device..."
                                },
                                color = if (isError) Color(0xFFDC2626) else Color(0xFF64748B),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        // Actions
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassButton(
                                text = "Sign in directly with TV account",
                                isPrimary = true,
                                onClick = onStartSignIn,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (authState is AuthState.Error) {
                            val errorText = if (authState.message.contains("10")) {
                                "Tip: Add debug keystore SHA-1 to Firebase, or scan QR code"
                            } else {
                                authState.message
                            }
                            Text(
                                text = errorText,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ── BOTTOM FOOTER: Navigation Hint ──────────────────────────────
            Text(
                text = "Use TV remote arrows to navigate  •  Press OK to select",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}
