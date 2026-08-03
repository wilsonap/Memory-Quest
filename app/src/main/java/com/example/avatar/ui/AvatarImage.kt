package com.example.avatar.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.avatar.model.AvatarPreset
import com.example.avatar.model.AvatarType
import com.example.ui.theme.ImmersivePrimary
import java.io.File

@Composable
fun AvatarImage(
    avatarType: String = AvatarType.PRESET.name,
    avatarPresetId: String = "avatar_01",
    avatarLocalPath: String = "",
    size: Dp = 48.dp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isImageError by remember(avatarLocalPath, avatarType) { mutableStateOf(false) }

    val preset = remember(avatarPresetId) { AvatarPreset.getById(avatarPresetId) }
    val isCustom = avatarType.equals(AvatarType.CUSTOM.name, ignoreCase = true) && avatarLocalPath.isNotBlank() && !isImageError

    val file = remember(avatarLocalPath) { if (avatarLocalPath.isNotBlank()) File(avatarLocalPath) else null }
    val fileExists = file != null && file.exists() && file.length() > 0

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        if (isCustom && fileExists) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar do jogador",
                contentScale = ContentScale.Crop,
                onError = { result ->
                    Log.e("AvatarImage", "Falha ao carregar imagem local de avatar: ${result.result.throwable.message}")
                    isImageError = true
                },
                modifier = baseModifier.background(Brush.linearGradient(preset.bgColors))
            )
        } else {
            // Preset / Fallback rendering
            Box(
                modifier = baseModifier.background(Brush.linearGradient(preset.bgColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.emoji,
                    fontSize = (size.value * 0.5f).sp,
                    lineHeight = (size.value * 0.55f).sp
                )
            }
        }

        if (showEditBadge) {
            Box(
                modifier = Modifier
                    .size((size.value * 0.35f).coerceAtLeast(18f).dp)
                    .clip(CircleShape)
                    .background(ImmersivePrimary)
                    .border(1.5.dp, Color(0xFF1D192B), CircleShape)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Alterar avatar",
                    tint = Color(0xFF1D192B),
                    modifier = Modifier.size((size.value * 0.22f).coerceAtLeast(11f).dp)
                )
            }
        }
    }
}
