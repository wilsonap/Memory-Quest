package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MemoryQuestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val firebaseApp = FirebaseApp.initializeApp(this)

        if (firebaseApp == null) {
            Log.e(
                "MemoryQuestFirebase",
                "FirebaseApp.initializeApp retornou null. Verifique google-services.json e o plugin google-services."
            )
            return
        }

        try {
            val appCheck = FirebaseAppCheck.getInstance(firebaseApp)

            if (BuildConfig.DEBUG) {
                try {
                    val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val method = clazz.getMethod("getInstance")
                    val factory = method.invoke(null) as AppCheckProviderFactory
                    appCheck.installAppCheckProviderFactory(factory)
                    Log.d("MemoryQuestFirebase", "DebugAppCheckProviderFactory instanciado com sucesso em DEBUG.")
                } catch (e: Exception) {
                    Log.e("MemoryQuestFirebase", "Falha ao instanciar DebugAppCheckProviderFactory: ${e.message}", e)
                }
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }

            Log.i(
                "MemoryQuestFirebase",
                "Firebase e App Check inicializados com sucesso."
            )
        } catch (e: Exception) {
            Log.e(
                "MemoryQuestFirebase",
                "Falha ao inicializar App Check.",
                e
            )
        }
    }
}
