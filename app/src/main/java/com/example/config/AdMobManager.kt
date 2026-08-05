package com.example.config

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MemoryQuestAds"

object AdMobManager {
    private val isInitialized = AtomicBoolean(false)
    private var canRequestAdsState = true

    /**
     * Configura o Mobile Ads SDK com tratamento não personalizado e inicializa.
     */
    fun initialize(context: Context) {
        val appContext = context.applicationContext
        if (isInitialized.getAndSet(true)) {
            return
        }

        try {
            val variant = if (BuildConfig.DEBUG) "DEBUG (ID Teste)" else "RELEASE (ID Produção)"
            Log.d(TAG, "Inicialização solicitada. Variante: $variant")

            if (BuildConfig.DEBUG) {
                val builder = MobileAds.getRequestConfiguration().toBuilder()
                    .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                MobileAds.setRequestConfiguration(builder.build())
            }

            MobileAds.initialize(appContext) { initializationStatus ->
                Log.d(TAG, "Mobile Ads inicialização concluída com sucesso.")
                if (BuildConfig.DEBUG) {
                    val statusMap = initializationStatus.adapterStatusMap
                    for ((adapterClass, status) in statusMap) {
                        Log.d(
                            TAG,
                            "Adapter: $adapterClass, State: ${status.initializationState}, Latency: ${status.latency}"
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Erro ao inicializar Mobile Ads: ${e.message}", e)
        }
    }

    /**
     * Consulta o consentimento via Google User Messaging Platform (UMP) e inicializa os anúncios quando permitido.
     */
    fun requestConsentAndInitialize(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        initialize(activity.applicationContext)

        try {
            Log.d(TAG, "Consentimento consultado via Google UMP SDK...")
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    Log.d(TAG, "Informações de consentimento UMP atualizadas.")
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) {
                            Log.w(
                                TAG,
                                "Erro no formulário de consentimento UMP - Code: ${formError.errorCode}, Message: ${formError.message}"
                            )
                        }
                        val canRequest = consentInformation.canRequestAds()
                        canRequestAdsState = canRequest
                        Log.d(TAG, "canRequestAds: $canRequest")
                        onComplete?.invoke(canRequest)
                    }
                },
                { requestConsentError ->
                    Log.w(
                        TAG,
                        "Falha ao atualizar informações de consentimento UMP - Code: ${requestConsentError.errorCode}, Message: ${requestConsentError.message}"
                    )
                    val canRequest = consentInformation.canRequestAds()
                    canRequestAdsState = canRequest
                    Log.d(TAG, "canRequestAds: $canRequest")
                    onComplete?.invoke(canRequest)
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Exceção ao solicitar consentimento UMP: ${e.message}", e)
            onComplete?.invoke(canRequestAdsState)
        }
    }

    /**
     * Indica se o aplicativo tem permissão para solicitar anúncios segundo o consentimento UMP.
     */
    fun canRequestAds(): Boolean = canRequestAdsState
}

