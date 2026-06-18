package com.tora_tech.smssync.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.tora_tech.smssync.ui.theme.QuikAvatarColors

/** QUIK-style circular avatar: a contact initial on a stable per-address color. */
@Composable
fun ContactAvatar(address: String, size: Dp = 52.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor(address)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitial(address),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

private fun avatarInitial(address: String): String {
    val ch = address.trim().firstOrNull() ?: return "#"
    return if (ch.isLetterOrDigit()) ch.uppercaseChar().toString() else "#"
}

private fun avatarColor(address: String): Color {
    var hash = 0
    for (c in address) hash = (hash * 31 + c.code) and 0x7fffffff
    return QuikAvatarColors[hash % QuikAvatarColors.size]
}
