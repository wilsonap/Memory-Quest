package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.UsernameUiState
import com.example.sync.ConnectivityObserver
import com.example.util.UsernameValidator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NameEntryDialog(
    initialName: String = "",
    title: String = stringResource(R.string.dialog_welcome_title),
    subtitle: String = stringResource(R.string.dialog_welcome_subtitle),
    uiState: UsernameUiState = UsernameUiState(),
    onNameInputChange: (String, Boolean) -> Unit = { _, _ -> },
    onConfirm: (String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isOnline: Boolean by remember {
        mutableStateOf<Boolean>(ConnectivityObserver(context) {}.isNetworkAvailable())
    }

    LaunchedEffect(Unit) {
        val observer = ConnectivityObserver(context) {
            isOnline = true
        }
        observer.startListening()
    }

    var nameText by remember { mutableStateOf(initialName) }

    LaunchedEffect(initialName) {
        if (initialName.isNotEmpty()) {
            onNameInputChange(initialName, isOnline)
        }
    }

    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E143B),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎮",
                    fontSize = 44.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        onNameInputChange(it, isOnline)
                    },
                    label = { Text(stringResource(R.string.dialog_your_name)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF4CC9F0)
                        )
                    },
                    trailingIcon = {
                        when {
                            uiState.isCheckingAvailability -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF4CC9F0),
                                    strokeWidth = 2.dp
                                )
                            }
                            uiState.validationResult is UsernameValidator.ValidationResult.Valid && uiState.isAvailable == true -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.username_available),
                                    tint = Color(0xFF4EAD69)
                                )
                            }
                            uiState.validationResult is UsernameValidator.ValidationResult.Invalid || uiState.isAvailable == false -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    isError = uiState.validationResult is UsernameValidator.ValidationResult.Invalid || uiState.isAvailable == false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CC9F0),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF4CC9F0),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorBorderColor = Color(0xFFFF5252)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                val errorMessage = when {
                    uiState.validationResult is UsernameValidator.ValidationResult.Invalid -> {
                        stringResource((uiState.validationResult as UsernameValidator.ValidationResult.Invalid).errorResId)
                    }
                    uiState.isAvailable == false -> {
                        stringResource(R.string.val_err_unavailable)
                    }
                    uiState.isCheckingAvailability -> {
                        stringResource(R.string.username_checking_availability)
                    }
                    uiState.isAvailable == true -> {
                        stringResource(R.string.username_available)
                    }
                    else -> null
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (uiState.isAvailable == true) Color(0xFF4EAD69) else if (uiState.isCheckingAvailability) Color(0xFF4CC9F0) else Color(0xFFFF8A8A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isOnline == false) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = Color(0xFF332050),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFFD166),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.val_notice_offline),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFFFD166)
                                )
                            )
                        }
                    }
                }

                if (uiState.suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isOnline) stringResource(R.string.username_suggestions_title) else stringResource(R.string.username_suggestions_offline_title),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.suggestions.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF2A1B52),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CC9F0)),
                                modifier = Modifier.clickable {
                                    nameText = suggestion
                                    onNameInputChange(suggestion, isOnline)
                                }
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF4CC9F0),
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isConfirmEnabled = uiState.validationResult is UsernameValidator.ValidationResult.Valid &&
                        !uiState.isCheckingAvailability &&
                        (uiState.isAvailable != false)

                Button(
                    onClick = {
                        if (isConfirmEnabled) {
                            onConfirm(nameText.trim())
                        }
                    },
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7209B7),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF7209B7).copy(alpha = 0.4f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_name_button")
                ) {
                    Text(
                        text = stringResource(R.string.dialog_start_adventure),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
