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

        FirebaseApp.initializeApp(this)

        val appCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            try {
                val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val method = clazz.getMethod("getInstance")
                val factory = method.invoke(null) as AppCheckProviderFactory
                appCheck.installAppCheckProviderFactory(factory)
                Log.d("MemoryQuestApp", "DebugAppCheckProviderFactory instanciado com sucesso em DEBUG.")
            } catch (e: Exception) {
                Log.e("MemoryQuestApp", "Falha ao instanciar DebugAppCheckProviderFactory: ${e.message}", e)
            }
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
