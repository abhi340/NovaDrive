package com.example.tvdrive.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvdrive.theme.*

/**
 * Clean, modern light background for Android TV (soft slate, glare-free, high-contrast).
 */
@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF8FAFC), // Pure soft off-white slate
                        Color(0xFFEDF2F7)  // Subtle cool gray slate
                    )
                )
            ),
        content = content
    )
}

/**
 * Modern TV Surface Card with clean, crisp borders and gentle elevation.
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0xFFFFFFFF),
    borderColor: Color = Color(0xFFE2E8F0),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

/**
 * TV D-pad focusable button with vector SVG icon and crisp high-contrast focus state.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconVector: ImageVector? = null,
    icon: String? = null,
    isPrimary: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = tween(120),
        label = "tv_btn_scale"
    )

    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = when {
        isFocused && isPrimary -> Color(0xFF1D4ED8)
        isFocused && !isPrimary -> Color(0xFFEFF6FF)
        !isFocused && isPrimary -> Color(0xFF2563EB)
        else -> Color(0xFFFFFFFF)
    }

    val contentColor = when {
        isFocused && isPrimary -> Color.White
        isFocused && !isPrimary -> Color(0xFF1D4ED8)
        !isFocused && isPrimary -> Color.White
        else -> Color(0xFF334155)
    }

    val borderColor = when {
        isFocused -> Color(0xFF2563EB)
        isPrimary -> Color(0xFF1D4ED8)
        else -> Color(0xFFCBD5E1)
    }

    val borderWidth = if (isFocused) 2.5.dp else 1.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { keyEvent ->
                if ((keyEvent.type == KeyEventType.KeyDown || keyEvent.type == KeyEventType.KeyUp) &&
                    (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                ) {
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        onClick()
                    }
                    true
                } else {
                    false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            } else if (icon != null) {
                Text(text = icon, fontSize = 16.sp)
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
