package br.com.whatsdireto.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val GUIDE_STEPS = listOf(
    GuideStep(
        icon = Icons.Outlined.Phone,
        title = "Digite o número sem salvar",
        description = "Escolha o país (com busca rápida) e digite o telefone com DDD. A formatação e validação acontecem automaticamente."
    ),
    GuideStep(
        icon = Icons.Outlined.ContentPaste,
        title = "Cole números ou links",
        description = "Cole um telefone copiado ou um link wa.me — o WhatsDireto detecta o país, extrai a mensagem e preenche tudo."
    ),
    GuideStep(
        icon = Icons.Outlined.Bolt,
        title = "Mensagens rápidas editáveis",
        description = "Use modelos prontos ou crie suas próprias respostas frequentes para enviar com um toque."
    ),
    GuideStep(
        icon = Icons.Outlined.QrCode2,
        title = "Links e QR Code instantâneos",
        description = "Além de abrir o WhatsApp (ou Business no Android), copie o link wa.me ou gere um QR Code escaneável na hora."
    ),
    GuideStep(
        icon = Icons.Outlined.History,
        title = "Histórico com etiquetas e fixados",
        description = "Dê apelidos aos números recentes, fixe os mais importantes no topo e filtre a lista em tempo real."
    )
)

@Composable
fun TutorialDialog(
    step: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onFinish: () -> Unit
) {
    val safeStep = step.coerceIn(0, GUIDE_STEPS.lastIndex)
    val current = GUIDE_STEPS[safeStep]
    val isLastStep = safeStep == GUIDE_STEPS.lastIndex

    Dialog(onDismissRequest = onFinish) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${safeStep + 1} de ${GUIDE_STEPS.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onFinish,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar guia",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = current.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GUIDE_STEPS.indices.forEach { index ->
                        val width by animateDpAsState(
                            targetValue = if (index == safeStep) 22.dp else 8.dp,
                            label = "indicatorWidth"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (index == safeStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (safeStep > 0) {
                        TextButton(onClick = onPrevious) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Voltar")
                        }
                    } else {
                        TextButton(onClick = onFinish) {
                            Text("Pular")
                        }
                    }

                    Button(
                        onClick = if (isLastStep) onFinish else onNext,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (isLastStep) "Começar" else "Próximo")
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
