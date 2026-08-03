package com.example.avatar.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AvatarStorageManager(private val context: Context) {

    companion object {
        private const val LOG_TAG = "AvatarStorageManager"
    }

    suspend fun removeCustomAvatarFile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars")
            val file = File(avatarsDir, "profile_avatar.webp")
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(LOG_TAG, "Arquivo de avatar customizado removido: $deleted")
                deleted
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Erro ao remover arquivo de avatar: ${e.message}", e)
            false
        }
    }

    fun isCustomAvatarFileValid(path: String?): Boolean {
        if (path.isNull_or_blank()) return false
        val file = File(path)
        return file.exists() && file.length() > 0
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
