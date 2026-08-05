package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.local.entity.PlayerEntity
import com.example.ui.components.BannerAdContainer
import com.example.ui.components.TopGameBar

import com.example.config.LegalConfig
import com.example.data.model.UserConsentState
import com.example.data.repository.DeleteAccountResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    player: PlayerEntity?,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    vibrationEnabled: Boolean,
    musicVolume: Float = 0.5f,
    sfxVolume: Float = 0.8f,
    language: String,
    onSetSound: (Boolean) -> Unit,
    onSetMusic: (Boolean) -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onSetMusicVolume: (Float) -> Unit = {},
    onSetSfxVolume: (Float) -> Unit = {},
    onSetLanguage: (String) -> Unit,
    onResetDefaults: () -> Unit,
    onResetGameProgress: () -> Unit = {},
    onDeleteAccount: (onResult: (DeleteAccountResult) -> Unit) -> Unit = {},
    onDeleteAccountSuccess: () -> Unit = {},
    onBackClick: () -> Unit,
    isAdsRemoved: Boolean = false,
    consentState: UserConsentState? = null,
    modifier: Modifier = Modifier
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showDeleteAccountFinalDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    var isDeletingAccount by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deleteConfirmationKeyword = stringResource(R.string.delete_account_confirmation_keyword)

    fun startDeleteAccount() {
        if (isDeletingAccount) return
        isDeletingAccount = true
        onDeleteAccount { result ->
            isDeletingAccount = false
            when (result) {
                DeleteAccountResult.Success -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.delete_account_success)
                        )
                        onDeleteAccountSuccess()
                    }
                }
                else -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            deleteAccountResultMessage(context, result)
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Reiniciar Jogo?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            },
            text = {
                Text(
                    text = "Deseja realmente reiniciar o jogo do início? Sua fase atual (retornará à Fase 1), moedas e estatísticas serão reiniciadas.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetGameProgress()
                        Toast.makeText(context, "Jogo reiniciado do início!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sim, Reiniciar", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false }
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) showDeleteAccountDialog = false
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_account_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_account_message),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        deleteConfirmationText = ""
                        showDeleteAccountFinalDialog = true
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = stringResource(R.string.delete_account_confirm),
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { if (!isDeletingAccount) showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text(stringResource(R.string.delete_account_cancel))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    if (showDeleteAccountFinalDialog) {
        val isConfirmationValid = deleteConfirmationText == deleteConfirmationKeyword

        AlertDialog(
            onDismissRequest = {
                if (!isDeletingAccount) {
                    showDeleteAccountFinalDialog = false
                    deleteConfirmationText = ""
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_account_final_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.delete_account_final_message),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDeletingAccount,
                        singleLine = true,
                        label = { Text(stringResource(R.string.delete_account_confirmation_hint)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isConfirmationValid || isDeletingAccount) return@Button
                        showDeleteAccountFinalDialog = false
                        deleteConfirmationText = ""
                        startDeleteAccount()
                    },
                    enabled = isConfirmationValid && !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = stringResource(R.string.delete_account_final_confirm),
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        if (!isDeletingAccount) {
                            showDeleteAccountFinalDialog = false
                            deleteConfirmationText = ""
                        }
                    },
                    enabled = !isDeletingAccount
                ) {
                    Text(stringResource(R.string.delete_account_cancel))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BannerAdContainer(isAdsRemoved = isAdsRemoved)
        },
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopGameBar(
                coins = player?.coins ?: 0,
                title = stringResource(R.string.settings_title),
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. PREFERÊNCIAS DE ÁUDIO E EFEITOS
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_audio_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Efeitos Sonoros Toggle
                        SettingToggleRow(
                            title = stringResource(R.string.settings_sfx),
                            icon = Icons.Default.VolumeUp,
                            checked = soundEnabled,
                            onCheckedChange = onSetSound
                        )

                        if (soundEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.settings_sfx_volume),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                )
                                Text(
                                    "${(sfxVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                )
                            }
                            Slider(
                                value = sfxVolume,
                                onValueChange = onSetSfxVolume,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.tertiary,
                                    activeTrackColor = MaterialTheme.colorScheme.tertiary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Música de Fundo Toggle
                        SettingToggleRow(
                            title = stringResource(R.string.settings_music),
                            icon = Icons.Default.MusicNote,
                            checked = musicEnabled,
                            onCheckedChange = onSetMusic
                        )

                        if (musicEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.settings_music_volume),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                )
                                Text(
                                    "${(musicVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                )
                            }
                            Slider(
                                value = musicVolume,
                                onValueChange = onSetMusicVolume,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Vibração
                        SettingToggleRow(
                            title = stringResource(R.string.settings_vibration),
                            icon = Icons.Default.Vibration,
                            checked = vibrationEnabled,
                            onCheckedChange = onSetVibration
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Botão Testar Efeitos Sonoros
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    com.example.audio.GameAudioManager.getInstance(context).testEffectsSequence()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Testar Efeitos (Sequência SFX)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // 2. VISUAL E IDIOMA
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_visual_language_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Idioma Toggle (Português / Inglês)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.settings_language), color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val isPtSelected = language.equals("PT", ignoreCase = true) || language.equals("PT-BR", ignoreCase = true)
                                val isEnSelected = language.equals("EN", ignoreCase = true)

                                OutlinedButton(
                                    onClick = { onSetLanguage("PT") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isPtSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isPtSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                ) { Text("Português") }

                                OutlinedButton(
                                    onClick = { onSetLanguage("EN") },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isEnSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isEnSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                ) { Text("English") }
                            }
                        }
                    }
                }

                // 3. LEGAL E PRIVACIDADE & SUPORTE
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.legal_and_privacy),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Política de Privacidade
                        NavigationLinkRow(
                            icon = Icons.Default.PrivacyTip,
                            title = stringResource(R.string.settings_privacy_policy),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalConfig.PRIVACY_URL))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Política de Privacidade: Todos os seus dados são guardados localmente no seu aparelho.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Termos de Uso
                        NavigationLinkRow(
                            icon = Icons.Default.Description,
                            title = "Termos de Uso",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LegalConfig.TERMS_URL))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Termos de Uso: Memory Quest é um jogo educativo de memória.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Suporte e Ajuda
                        NavigationLinkRow(
                            icon = Icons.Default.Help,
                            title = "Suporte e Ajuda",
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://memory.autocheckia.com.br/support"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Suporte Memory Quest: wilsonap1910@gmail.com", Toast.LENGTH_LONG).show()
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Exclusão de conta (dados remotos + locais + auth)
                        NavigationLinkRow(
                            icon = Icons.Default.Delete,
                            title = stringResource(R.string.delete_account_menu_title),
                            enabled = !isDeletingAccount,
                            onClick = { showDeleteAccountDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Reinício local de progresso
                        NavigationLinkRow(
                            icon = Icons.Default.RestartAlt,
                            title = stringResource(R.string.reset_game_menu_title),
                            onClick = { showResetConfirmDialog = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))

                        // Detalhes do Consentimento
                        val termsVer = consentState?.termsVersionAccepted?.ifEmpty { LegalConfig.TERMS_VERSION } ?: LegalConfig.TERMS_VERSION
                        val privacyVer = consentState?.privacyVersionAccepted?.ifEmpty { LegalConfig.PRIVACY_VERSION } ?: LegalConfig.PRIVACY_VERSION
                        val acceptedDateStr = if (consentState != null && consentState.acceptedAtLocal > 0L) {
                            try {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(consentState.acceptedAtLocal))
                            } catch (e: Exception) {
                                "-"
                            }
                        } else {
                            "-"
                        }
                        val syncStatusStr = if (consentState?.consentSyncPending == true) {
                            stringResource(R.string.consent_status_pending)
                        } else {
                            stringResource(R.string.consent_status_synced)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.consent_terms_version, termsVer),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = stringResource(R.string.consent_privacy_version, privacyVer),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = stringResource(R.string.consent_accepted_date, acceptedDateStr),
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = stringResource(R.string.consent_sync_status, syncStatusStr),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (consentState?.consentSyncPending == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(R.string.consent_delete_data_info),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // 4. SOBRE O APLICATIVO
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Memory Quest",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = stringResource(R.string.settings_developer) + " • v1.0.0",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }

                // 5. RESTAURAR CONFIGURAÇÕES PADRÃO
                OutlinedButton(
                    onClick = onResetDefaults,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_reset_defaults),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

        if (isDeletingAccount) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                        Text(
                            text = stringResource(R.string.delete_account_in_progress),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

private fun deleteAccountResultMessage(context: Context, result: DeleteAccountResult): String {
    return when (result) {
        DeleteAccountResult.Success -> context.getString(R.string.delete_account_success)
        DeleteAccountResult.NoInternet -> context.getString(R.string.delete_account_error_no_internet)
        DeleteAccountResult.InvalidSession -> context.getString(R.string.delete_account_error_invalid_session)
        DeleteAccountResult.PermissionDenied -> context.getString(R.string.delete_account_error_permission)
        DeleteAccountResult.RequiresRecentLogin -> context.getString(R.string.delete_account_error_recent_login)
        is DeleteAccountResult.AuthDeleteFailure -> result.cause.message?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.delete_account_error_auth)
        is DeleteAccountResult.RemoteFailure -> result.cause.message?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.delete_account_error_generic)
    }
}

@Composable
private fun NavigationLinkRow(
    icon: ImageVector,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface))
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
