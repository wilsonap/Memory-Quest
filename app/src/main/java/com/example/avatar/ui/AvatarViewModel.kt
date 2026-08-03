package com.example.avatar.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.avatar.data.AvatarRepository
import com.example.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AvatarUiState(
    val player: PlayerEntity? = null,
    val showSelectAvatarDialog: Boolean = false,
    val croppingImageUri: Uri? = null,
    val isLoading: Boolean = false
)

class AvatarViewModel(
    private val avatarRepository: AvatarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            avatarRepository.playerFlow.collectLatest { player ->
                _uiState.value = _uiState.value.copy(player = player)
            }
        }
    }

    fun openSelectAvatarDialog() {
        _uiState.value = _uiState.value.copy(showSelectAvatarDialog = true)
    }

    fun dismissSelectAvatarDialog() {
        _uiState.value = _uiState.value.copy(showSelectAvatarDialog = false)
    }

    fun startCropping(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            showSelectAvatarDialog = false,
            croppingImageUri = uri
        )
    }

    fun dismissCropper() {
        _uiState.value = _uiState.value.copy(croppingImageUri = null)
    }

    fun selectPreset(presetId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            avatarRepository.selectPresetAvatar(presetId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showSelectAvatarDialog = false
            )
        }
    }

    fun onCroppedPhotoSaved(localPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            avatarRepository.saveCustomAvatar(localPath)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                croppingImageUri = null,
                showSelectAvatarDialog = false
            )
        }
    }

    fun resetToDefaultAvatar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            avatarRepository.resetToDefaultAvatar()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showSelectAvatarDialog = false
            )
        }
    }
}
