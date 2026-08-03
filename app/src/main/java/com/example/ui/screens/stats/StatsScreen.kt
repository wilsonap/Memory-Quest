package com.example.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.StatisticsEntity
import com.example.ui.components.TopGameBar
import com.example.ui.screens.profile.components.StatsGridSection
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveGold
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveSurfaceVariant
import com.example.ui.theme.ImmersiveTextPrimary
import com.example.ui.theme.ImmersiveTextSecondary

@Composable
fun StatsScreen(
    player: PlayerEntity?,
    stats: StatisticsEntity?,
    onNavigateToProfile: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val playerName = player?.name?.ifEmpty { "Explorador" } ?: "Explorador"
    val level = player?.highestLevel ?: 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            TopGameBar(
                coins = player?.coins ?: 0,
                title = stringResource(R.string.stats_title),
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Small Summary Card: "Alessandro • Nível 9" + [Ver perfil]
                Surface(
                    color = ImmersiveSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ImmersiveSurfaceVariant, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ImmersiveGold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "$playerName • Nível $level",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveTextPrimary
                                    )
                                )
                                Text(
                                    text = "Desempenho e Estatísticas de Jogo",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ImmersiveTextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToProfile,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ImmersivePrimary.copy(alpha = 0.15f),
                                contentColor = ImmersivePrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = stringResource(R.string.profile_view_profile),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                // Numbers & Performance Grid (Partidas, Vitórias, Derrotas, Taxa de Vitória, Precisão, Pares, Cartas viradas, etc.)
                StatsGridSection(
                    player = player,
                    stats = stats
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
