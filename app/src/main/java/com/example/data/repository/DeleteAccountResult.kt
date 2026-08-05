package com.example.data.repository

sealed class DeleteAccountResult {
    data object Success : DeleteAccountResult()

    data object NoInternet : DeleteAccountResult()

    data object InvalidSession : DeleteAccountResult()

    data object PermissionDenied : DeleteAccountResult()

    data object RequiresRecentLogin : DeleteAccountResult()

    data class AuthDeleteFailure(val cause: Throwable) : DeleteAccountResult()

    data class RemoteFailure(val cause: Throwable) : DeleteAccountResult()
}
