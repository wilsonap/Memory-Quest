package com.example.config

import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Estado global de inicialização do Firebase.
 * Marcado como pronto somente após [FirebaseApp.initializeApp] retornar app válido.
 */
object FirebaseBootstrap {
    private const val TAG = "MemoryQuestFirebase"

    @Volatile
    var isReady: Boolean = false
        private set

    fun markReady() {
        isReady = true
        Log.i(TAG, "FirebaseBootstrap: pronto")
    }

    fun requireReady() {
        check(isReady) {
            "Default FirebaseApp is not initialized in this process"
        }
    }
}
