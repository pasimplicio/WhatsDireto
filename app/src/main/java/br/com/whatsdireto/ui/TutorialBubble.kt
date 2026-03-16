package br.com.whatsdireto.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.whatsdireto.ui.theme.WhatsAppGreen

@Composable
fun TutorialBubble(
    step: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        TutorialStep(
            title = "📱 Número com DDD",
            description = "Digite ou cole o número do WhatsApp. A máscara (XX) XXXXX-XXXX aparece automaticamente."
        ),
        TutorialStep(
            title = "👤 Contatos",
            description = "Toque aqui para selecionar um contato da agenda. O número será preenchido pra você."
        ),
        TutorialStep(
            title = "💬 Mensagem Rápida",
            description = "Digite uma mensagem ou escolha um template clicando no ícone de enviar."
        ),
        TutorialStep(
            title = "📋 Colar",
            description = "Cole um número que você copiou. O app identifica e formata automaticamente."
        ),
        TutorialStep(
            title = "📜 Histórico",
            description = "Seus últimos números aparecem aqui. Toque para reutilizar rapidamente."
        )
    )

    if (step < steps.size) {
        val currentStep = steps[step]

        Dialog(
            onDismissRequest = { /* Não permite fechar clicando fora */ },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(modifier)
            ) {
                // Overlay escuro com transparência ajustada (mais claro)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { }
                )

                // Tutorial centralizado
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = WhatsAppGreen.copy(alpha = 0.75f)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .width(320.dp)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Botão de fechar no canto superior direito
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = onClose,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Título
                            Text(
                                text = currentStep.title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        offset = Offset(2f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Descrição
                            Text(
                                text = currentStep.description,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.95f)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Barra de progresso
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(steps.size) { index ->
                                    val scale by animateFloatAsState(
                                        targetValue = if (index == step) 1.2f else 0.8f,
                                        animationSpec = tween(300),
                                        label = "scale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(if (index == step) 12.dp else 8.dp)
                                            .padding(horizontal = 4.dp)
                                            .clip(CircleShape)
                                            .scale(scale)
                                            .background(
                                                if (index <= step) Color.White
                                                else Color.White.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Botões de navegação
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (step > 0) {
                                    IconButton(
                                        onClick = onPrevious,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Anterior",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(48.dp))
                                }

                                IconButton(
                                    onClick = if (step == steps.size - 1) onClose else onNext,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                ) {
                                    if (step == steps.size - 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Concluir",
                                            tint = WhatsAppGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Próximo",
                                            tint = WhatsAppGreen,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = onClose,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Pular", color = Color.White, fontSize = 14.sp)
                                }
                            }

                            Text(
                                text = "${step + 1} de ${steps.size}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}