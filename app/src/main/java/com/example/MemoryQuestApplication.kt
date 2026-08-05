package com.example

import android.app.Application
import android.util.Log
import com.example.config.FirebaseBootstrap
import com.google.firebase.FirebaseApp

class MemoryQuestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            var firebaseApp = FirebaseApp.initializeApp(this)

            if (firebaseApp == null) {
                val existingApps = FirebaseApp.getApps(this)
                if (existingApps.isNotEmpty()) {
                    firebaseApp = existingApps[0]
                }
            }

            if (firebaseApp != null) {
                FirebaseBootstrap.markReady()
                initAppCheck(firebaseApp)
            } else {
                Log.e(
                    "MemoryQuestFirebase",
                    "FirebaseApp.initializeApp e getApps() retornaram null. Verifique google-services.json."
                )
            }
        } catch (e: Throwable) {
            Log.e("MemoryQuestFirebase", "Erro ao inicializar Firebase em MemoryQuestApplication", e)
            try {
                if (FirebaseApp.getApps(this).isNotEmpty()) {
                    FirebaseBootstrap.markReady()
                }
            } catch (_: Throwable) {}
        }
    }

    private fun initAppCheck(firebaseApp: FirebaseApp) {
        try {
            val appCheckClass = Class.forName("com.google.firebase.appcheck.FirebaseAppCheck")
            val getInstanceMethod = appCheckClass.getMethod("getInstance", FirebaseApp::class.java)
            val appCheckInstance = getInstanceMethod.invoke(null, firebaseApp) ?: return

            var providerFactory: Any? = null

            if (BuildConfig.DEBUG) {
                try {
                    val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = debugFactoryClass.getMethod("getInstance")
                    providerFactory = getInstance.invoke(null)
                    Log.d("MemoryQuestFirebase", "DebugAppCheckProviderFactory instanciado com sucesso em DEBUG.")
                } catch (e: Throwable) {
                    Log.w("MemoryQuestFirebase", "DebugAppCheckProviderFactory nao disponivel: ${e.message}")
                }
            }

            if (providerFactory == null) {
                try {
                    val playIntegrityClass = Class.forName("com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory")
                    val getInstance = playIntegrityClass.getMethod("getInstance")
                    providerFactory = getInstance.invoke(null)
                    Log.d("MemoryQuestFirebase", "PlayIntegrityAppCheckProviderFactory instanciado com sucesso.")
                } catch (e: Throwable) {
                    Log.w("MemoryQuestFirebase", "PlayIntegrityAppCheckProviderFactory nao disponivel: ${e.message}")
                }
            }

            if (providerFactory != null) {
                val providerFactoryClass = Class.forName("com.google.firebase.appcheck.AppCheckProviderFactory")
                val installMethod = appCheckClass.getMethod("installAppCheckProviderFactory", providerFactoryClass)
                installMethod.invoke(appCheckInstance, providerFactory)
                Log.i("MemoryQuestFirebase", "Firebase e App Check inicializados com sucesso.")
            }
        } catch (e: Throwable) {
            Log.w("MemoryQuestFirebase", "Firebase App Check nao foi inicializado: ${e.message}")
        }
    }
}

