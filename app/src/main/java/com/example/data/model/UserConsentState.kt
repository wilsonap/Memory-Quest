package com.example.data.model

data class UserConsentState(
    val termsAccepted: Boolean = false,
    val privacyAccepted: Boolean = false,
    val termsVersionAccepted: String = "",
    val privacyVersionAccepted: String = "",
    val acceptedAtLocal: Long = 0L,
    val consentSyncPending: Boolean = false
) {
    fun isValid(requiredTermsVersion: String, requiredPrivacyVersion: String): Boolean {
        return termsAccepted &&
                privacyAccepted &&
                termsVersionAccepted == requiredTermsVersion &&
                privacyVersionAccepted == requiredPrivacyVersion
    }
}
