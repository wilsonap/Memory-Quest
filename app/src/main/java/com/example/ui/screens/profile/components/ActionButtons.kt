package com.example.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

import com.example.data.repository.UsernameChangeEligibility

@Composable
fun ActionButtons(
    onEditNameClick: () -> Unit,
    onEditAvatarClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewStatsClick: () -> Unit,
    usernameEligibility: UsernameChangeEligibility = UsernameChangeEligibility.Allowed,
    modifier: Modifier = Modifier
) {
    val editNameSubtitle = when (usernameEligibility) {
        is UsernameChangeEligibility.Cooldown -> {
            if (usernameEligibility.remainingDays > 0) {
                stringResource(R.string.username_change_status_cooldown_days, usernameEligibility.remainingDays)
            } else if (usernameEligibility.remainingHours > 0) {
                stringResource(R.string.username_change_status_cooldown_hours, usernameEligibility.remainingHours)
            } else {
                stringResource(R.string.username_change_status_cooldown_minutes, usernameEligibility.remainingMinutes.coerceAtLeast(1))
            }
        }
        is UsernameChangeEligibility.Offline -> stringResource(R.string.username_change_offline_error)
        is UsernameChangeEligibility.Unauthenticated -> stringResource(R.string.username_change_unauthenticated_error)
        is UsernameChangeEligibility.FirestoreUnavailable -> stringResource(R.string.username_change_firestore_unavailable_error)
        is UsernameChangeEligibility.PermissionDenied -> stringResource(R.string.username_change_permission_denied_error, usernameEligibility.docPath)
        else -> stringResource(R.string.username_change_status_allowed)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "AÇÕES DO PERFIL",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ImmersiveGold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileActionButton(
                label = stringResource(R.string.profile_edit_name),
                subtitle = editNameSubtitle,
                icon = Icons.Default.Edit,
                onClick = onEditNameClick,
                testTag = "action_edit_name"
            )

            ProfileActionButton(
                label = stringResource(R.string.profile_change_avatar),
                icon = Icons.Default.AccountCircle,
                onClick = onEditAvatarClick,
                testTag = "action_edit_avatar"
            )

            ProfileActionButton(
                label = stringResource(R.string.profile_share),
                icon = Icons.Default.Share,
                onClick = onShareClick,
                testTag = "action_share_profile"
            )

            ProfileActionButton(
                label = stringResource(R.string.profile_view_stats),
                icon = Icons.Default.BarChart,
                onClick = onViewStatsClick,
                testTag = "action_view_stats"
            )
        }
    }
}

@Composable
private fun ProfileActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    subtitle: String? = null
) {
    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ImmersivePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = ImmersivePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextPrimary,
                            fontSize = 15.sp
                        )
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ImmersiveTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ImmersiveTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
