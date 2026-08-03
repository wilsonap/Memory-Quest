package com.example.avatar.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {
    private const val LOG_TAG = "ImageCompressor"
    private const val TARGET_SIZE = 512
    private const val MAX_FILE_SIZE_BYTES = 500 * 1024 // 500 KB

    suspend fun processAndSaveAvatar(
        context: Context,
        imageUri: Uri,
        scale: Float = 1.0f,
        panXNormalized: Float = 0.5f,
        panYNormalized: Float = 0.5f
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri) ?: return@withContext null

            // 1. Check bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(boundsStream, null, options)
            boundsStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth <= 0 || origHeight <= 0) {
                inputStream.close()
                return@withContext null
            }

            // Downsample during decode
            var inSampleSize = 1
            val minDim = Math.min(origWidth, origHeight)
            while (minDim / (inSampleSize * 2) >= TARGET_SIZE) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val decodedStream = contentResolver.openInputStream(imageUri)
            val fullBitmap = BitmapFactory.decodeStream(decodedStream, null, decodeOptions)
            decodedStream?.close()
            inputStream.close()

            if (fullBitmap == null) return@withContext null

            // 2. Square Crop Calculation
            val bmpWidth = fullBitmap.width
            val bmpHeight = fullBitmap.height
            val baseSquareDim = Math.min(bmpWidth, bmpHeight)

            val cropSize = (baseSquareDim / scale.coerceAtLeast(1.0f)).toInt().coerceIn(1, baseSquareDim)

            val maxStartX = (bmpWidth - cropSize).coerceAtLeast(0)
            val maxStartY = (bmpHeight - cropSize).coerceAtLeast(0)

            val startX = (panXNormalized * maxStartX).toInt().coerceIn(0, maxStartX)
            val startY = (panYNormalized * maxStartY).toInt().coerceIn(0, maxStartY)

            val croppedBmp = Bitmap.createBitmap(fullBitmap, startX, startY, cropSize, cropSize)
            if (croppedBmp != fullBitmap) {
                fullBitmap.recycle()
            }

            // 3. Resize to 512 x 512
            val resizedBmp = if (croppedBmp.width != TARGET_SIZE || croppedBmp.height != TARGET_SIZE) {
                val scaled = Bitmap.createScaledBitmap(croppedBmp, TARGET_SIZE, TARGET_SIZE, true)
                if (scaled != croppedBmp) {
                    croppedBmp.recycle()
                }
                scaled
            } else {
                croppedBmp
            }

            // 4. Compress to WebP <= 500 KB
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }

            val avatarFile = File(avatarsDir, "profile_avatar.webp")

            var quality = 85
            var compressedBytes: ByteArray
            do {
                val baos = ByteArrayOutputStream()
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                resizedBmp.compress(format, quality, baos)
                compressedBytes = baos.toByteArray()
                quality -= 10
            } while (compressedBytes.size > MAX_FILE_SIZE_BYTES && quality >= 30)

            FileOutputStream(avatarFile).use { fos ->
                fos.write(compressedBytes)
                fos.flush()
            }

            resizedBmp.recycle()

            Log.d(LOG_TAG, "Avatar salvo em ${avatarFile.absolutePath} (${compressedBytes.size / 1024} KB)")
            avatarFile.absolutePath
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao processar e comprimir avatar: ${e.message}", e)
            null
        }
    }
}
