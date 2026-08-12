package com.example.update

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gerenciador centralizado e reutilizável para o sistema de verificação de atualização In-App
 * via Play Core (Google Play In-App Updates).
 */
class InAppUpdateManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)

    private val _isUpdateDownloaded = MutableStateFlow(false)
    val isUpdateDownloaded: StateFlow<Boolean> = _isUpdateDownloaded.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    @Volatile
    private var isCheckInProgress = false

    private val installStateUpdatedListener = InstallStateUpdatedListener { state: InstallState ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                Log.i(TAG, "In-App Update baixado com sucesso e pronto para instalar.")
                _isUpdateDownloaded.value = true
            }
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                Log.d(TAG, "In-App Update baixando: $bytesDownloaded / $totalBytesToDownload bytes")
            }
            InstallStatus.FAILED -> {
                Log.w(TAG, "Falha no download da atualização In-App: ${state.installErrorCode()}")
            }
            InstallStatus.CANCELED -> {
                Log.i(TAG, "Download da atualização In-App cancelado pelo usuário.")
            }
            else -> {}
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao registrar InstallStateUpdatedListener: ${e.message}", e)
        }
    }

    /**
     * Verifica se existe uma atualização disponível na Google Play Store e dispara o fluxo.
     * Suporta Flexible Update (padrão) ou Immediate Update (se [isImmediate] for true).
     */
    fun checkForUpdate(activity: Activity, isImmediate: Boolean = false) {
        if (isCheckInProgress) {
            Log.d(TAG, "Verificação de atualização já está em andamento. Ignorando chamada.")
            return
        }

        isCheckInProgress = true

        try {
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo
            appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                isCheckInProgress = false
                handleAppUpdateInfo(activity, appUpdateInfo, isImmediate)
            }.addOnFailureListener { exception ->
                isCheckInProgress = false
                Log.w(TAG, "Não foi possível verificar atualização na Play Store: ${exception.message}")
            }
        } catch (e: Exception) {
            isCheckInProgress = false
            Log.e(TAG, "Erro ao iniciar checagem de In-App Update: ${e.message}", e)
        }
    }

    private fun handleAppUpdateInfo(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        isImmediate: Boolean
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val updateAvailability = appUpdateInfo.updateAvailability()
        val installStatus = appUpdateInfo.installStatus()

        Log.d(TAG, "AppUpdateInfo recebido: availability=$updateAvailability, installStatus=$installStatus")

        // Se o download já terminou (Flexible Update completo no background)
        if (installStatus == InstallStatus.DOWNLOADED) {
            _isUpdateDownloaded.value = true
            return
        }

        val targetUpdateType = if (isImmediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE

        when {
            // Nova versão disponível e tipo permitido
            updateAvailability == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(targetUpdateType) -> {
                _isUpdateAvailable.value = true
                startUpdateFlow(activity, appUpdateInfo, targetUpdateType)
            }

            // Fallback: se pediu immediate mas só suporta flexible
            updateAvailability == UpdateAvailability.UPDATE_AVAILABLE &&
                    !isImmediate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                _isUpdateAvailable.value = true
                startUpdateFlow(activity, appUpdateInfo, AppUpdateType.IMMEDIATE)
            }

            // Atualização crítica imediata em andamento
            updateAvailability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                startUpdateFlow(activity, appUpdateInfo, AppUpdateType.IMMEDIATE)
            }
        }
    }

    private fun startUpdateFlow(
        activity: Activity,
        appUpdateInfo: AppUpdateInfo,
        @AppUpdateType updateType: Int
    ) {
        try {
            val options = AppUpdateOptions.newBuilder(updateType).build()
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                options,
                REQUEST_CODE_IN_APP_UPDATE
            )
            Log.i(TAG, "Fluxo de In-App Update iniciado ($updateType).")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar fluxo de atualização In-App: ${e.message}", e)
        }
    }

    /**
     * Deve ser chamado no `onResume` da Activity principal.
     * Verifica se há atualizações prontas para concluir ou fluxos de atualização crítica em andamento.
     */
    fun onResume(activity: Activity) {
        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    if (activity.isFinishing || activity.isDestroyed) return@addOnSuccessListener

                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        _isUpdateDownloaded.value = true
                    } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // Se era uma atualização imediata em andamento, retoma a tela da Play Store
                        startUpdateFlow(activity, appUpdateInfo, AppUpdateType.IMMEDIATE)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Falha na checagem de onResume do In-App Update: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Erro no onResume do In-App Update: ${e.message}", e)
        }
    }

    /**
     * Conclui a atualização flexível já baixada, reiniciando o aplicativo.
     */
    fun completeUpdate() {
        try {
            Log.i(TAG, "Executando completeUpdate() para reiniciar e instalar a atualização...")
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao executar completeUpdate(): ${e.message}", e)
        }
    }

    /**
     * Remove os listeners para prevenir memory leaks no encerramento da app.
     */
    fun unregisterListener() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desregistrar listener do In-App Update: ${e.message}", e)
        }
    }

    companion object {
        const val REQUEST_CODE_IN_APP_UPDATE = 1293
        private const val TAG = "InAppUpdateManager"

        @Volatile
        private var INSTANCE: InAppUpdateManager? = null

        fun getInstance(context: Context): InAppUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InAppUpdateManager(context).also { INSTANCE = it }
            }
        }
    }
}
