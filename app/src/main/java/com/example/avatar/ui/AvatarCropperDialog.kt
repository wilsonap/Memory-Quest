package com.example.avatar.ui

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.avatar.util.ImageCompressor
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import kotlinx.coroutines.launch

@Composable
fun AvatarCropperDialog(
    imageUri: Uri,
    onCroppedAndSaved: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isProcessing) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ImmersiveBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "Ajustar e RecortarFoto",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Arraste e use o zoom para enquadrar na área circular",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }

                // Center Crop Viewport
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val cropSize = Math.min(constraints.maxWidth, constraints.maxHeight) * 0.8f

                    Box(
                        modifier = Modifier
                            .size(cropSize.dp)
                            .clipToBounds()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1.0f, 4.0f)
                                    panX += pan.x
                                    panY += pan.y
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Image being cropped
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto para recorte",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = panX,
                                    translationY = panY
                                )
                        )

                        // Circular Mask Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val circleRadius = size.minDimension / 2f
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)

                            val circlePath = Path().apply {
                                addOval(Rect(centerOffset, circleRadius))
                            }

                            clipPath(circlePath, clipOp = ClipOp.Difference) {
                                drawRect(Color.Black.copy(alpha = 0.75f))
                            }

                            drawCircle(
                                color = ImmersivePrimary,
                                radius = circleRadius,
                                center = centerOffset,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }

                // Controls & Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = ImmersivePrimary
                        )
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 1.0f..4.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ImmersivePrimary,
                                activeTrackColor = ImmersivePrimary,
                                inactiveTrackColor = ImmersiveSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.1fx", scale),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveGold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                isProcessing = true
                                scope.launch {
                                    // Normalize pan (0..1 center alignment)
                                    val panXNorm = (0.5f - (panX / 500f)).coerceIn(0f, 1f)
                                    val panYNorm = (0.5f - (panY / 500f)).coerceIn(0f, 1f)

                                    val savedPath = ImageCompressor.processAndSaveAvatar(
                                        context = context,
                                        imageUri = imageUri,
                                        scale = scale,
                                        panXNormalized = panXNorm,
                                        panYNormalized = panYNorm
                                    )

                                    isProcessing = false
                                    if (savedPath != null) {
                                        onCroppedAndSaved(savedPath)
                                    } else {
                                        onCancel()
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("confirm_crop_button")
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF1D192B),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF1D192B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Salvar Foto",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D192B)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
