package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.config.LegalConfig
import com.example.data.local.DataStoreManager
import com.example.data.model.UserConsentState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.Locale

class ConsentRepository(
    private val dataStoreManager: DataStoreManager,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "ConsentRepository"
    }

    val userConsentState: Flow<UserConsentState> = dataStoreManager.userConsentState

    suspend fun ensureAuthenticated(): String? {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                currentUser.uid
            } else {
                val result = auth.signInAnonymously().await()
                result.user?.uid
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during anonymous authentication: ${e.message}", e)
            auth.currentUser?.uid
        }
    }

    suspend fun recordConsent(
        termsVersion: String = LegalConfig.TERMS_VERSION,
        privacyVersion: String = LegalConfig.PRIVACY_VERSION
    ): Boolean {
        val now = System.currentTimeMillis()
        val uid = ensureAuthenticated()

        dataStoreManager.saveConsentLocally(
            termsVersion = termsVersion,
            privacyVersion = privacyVersion,
            acceptedAt = now,
            syncPending = true
        )

        if (uid != null) {
            val remoteSuccess = pushConsentToFirestore(uid, termsVersion, privacyVersion)
            if (remoteSuccess) {
                dataStoreManager.setConsentSyncPending(false)
                return true
            }
        }
        return false
    }

    suspend fun syncConsentOnline(): Boolean {
        val localState = userConsentState.firstOrNull() ?: return false
        if (!localState.isValid(LegalConfig.TERMS_VERSION, LegalConfig.PRIVACY_VERSION)) {
            return false
        }
        val uid = ensureAuthenticated() ?: return false
        val success = pushConsentToFirestore(
            uid,
            localState.termsVersionAccepted.ifEmpty { LegalConfig.TERMS_VERSION },
            localState.privacyVersionAccepted.ifEmpty { LegalConfig.PRIVACY_VERSION }
        )
        if (success) {
            dataStoreManager.setConsentSyncPending(false)
        }
        return success
    }

    suspend fun checkAndSyncRemoteConsent(): Boolean {
        val uid = ensureAuthenticated() ?: return false
        return try {
            val docSnap = firestore.collection("user_consents").document(uid).get().await()
            if (docSnap.exists()) {
                val termsAccepted = docSnap.getBoolean("termsAccepted") ?: false
                val privacyAccepted = docSnap.getBoolean("privacyAccepted") ?: false
                val termsVersion = docSnap.getString("termsVersion") ?: ""
                val privacyVersion = docSnap.getString("privacyVersion") ?: ""

                if (termsAccepted && privacyAccepted &&
                    termsVersion == LegalConfig.TERMS_VERSION &&
                    privacyVersion == LegalConfig.PRIVACY_VERSION
                ) {
                    dataStoreManager.saveConsentLocally(
                        termsVersion = termsVersion,
                        privacyVersion = privacyVersion,
                        acceptedAt = System.currentTimeMillis(),
                        syncPending = false
                    )
                    return true
                } else {
                    syncConsentOnline()
                }
            } else {
                syncConsentOnline()
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking remote consent: ${e.message}", e)
            false
        }
    }

    private suspend fun pushConsentToFirestore(
        uid: String,
        termsVersion: String,
        privacyVersion: String
    ): Boolean {
        return try {
            val localeTag = Locale.getDefault().toLanguageTag().ifEmpty { "pt-BR" }
            val consentData = hashMapOf<String, Any>(
                "uid" to uid,
                "termsAccepted" to true,
                "privacyAccepted" to true,
                "termsVersion" to termsVersion,
                "privacyVersion" to privacyVersion,
                "acceptedAt" to FieldValue.serverTimestamp(),
                "appVersion" to BuildConfig.VERSION_NAME,
                "locale" to localeTag,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("user_consents")
                .document(uid)
                .set(consentData, SetOptions.merge())
                .await()

            Log.d(TAG, "Consent record saved to Firestore for user $uid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save consent to Firestore: ${e.message}", e)
            false
        }
    }
}
