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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import br.com.whatsdireto.ui.theme.WhatsAppGreen
import br.com.whatsdireto.ui.theme.WhatsAppDarkGreen

@Composable
fun TutorialBubble(
    step: Int,
    targetContent: @Composable () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    highlightField: String? = null,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        TutorialStep(
            title = "📱 Campo de Telefone",
            description = "Digite ou cole o número do WhatsApp aqui. A máscara (XX) XXXXX-XXXX aparece automaticamente.",
            field = "phone"
        ),
        TutorialStep(
            title = "👤 Botão Contatos",
            description = "Toque aqui para selecionar um contato da agenda. O número será preenchido pra você.",
            field = "contacts"
        ),
        TutorialStep(
            title = "💬 Campo de Mensagem",
            description = "Digite uma mensagem ou escolha um template pronto. Ela será enviada junto com o número.",
            field = "message"
        ),
        TutorialStep(
            title = "📎 Botão Anexar",
            description = "Anexe fotos, vídeos ou documentos. O arquivo será enviado junto com a mensagem.",
            field = "attach"
        ),
        TutorialStep(
            title = "📋 Botão Colar",
            description = "Cole um número que você copiou. O app identifica e formata automaticamente.",
            field = "paste"
        ),
        TutorialStep(
            title = "📜 Histórico",
            description = "Seus últimos números aparecem aqui. Toque para reutilizar rapidamente.",
            field = "history"
        )
    )

    if (step < steps.size) {
        val currentStep = steps[step]
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val screenWidth = configuration.screenWidthDp.dp

        // Posicionamento específico para cada campo
        val bubbleOffset = when (currentStep.field) {
            "phone" -> IntOffset(0, -screenHeight.value.toInt() / 3)
            "contacts" -> IntOffset(screenWidth.value.toInt() / 4, -screenHeight.value.toInt() / 3)
            "message" -> IntOffset(0, -screenHeight.value.toInt() / 5)
            "attach" -> IntOffset(0, screenHeight.value.toInt() / 4)
            "paste" -> IntOffset(-screenWidth.value.toInt() / 4, screenHeight.value.toInt() / 4)
            "history" -> IntOffset(0, screenHeight.value.toInt() / 3)
            else -> IntOffset(0, 0)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
                .then(modifier)
        ) {
            // Conteúdo original
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                targetContent()
            }

            // Balão de ajuda
            Popup(
                alignment = Alignment.Center,
                offset = bubbleOffset,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                Box {
                    // Seta apontando para o campo
                    Box(
                        modifier = Modifier
                            .align(if (step < 3) Alignment.BottomCenter else Alignment.TopCenter)
                            .offset {
                                if (step < 3) {
                                    IntOffset(0, 8)
                                } else {
                                    IntOffset(0, -8)
                                }
                            }
                            .size(20.dp)
                            .rotate(if (step < 3) 0f else 180f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(WhatsAppGreen)
                    )

                    // Card principal do balão
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = WhatsAppDarkGreen.copy(alpha = 0.95f)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .width(280.dp)
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Título
                            Text(
                                text = currentStep.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Descrição
                            Text(
                                text = currentStep.description,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center,
                                color = Color.White.copy(alpha = 0.95f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Barra de progresso
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(steps.size) { index ->
                                    val scale by animateFloatAsState(
                                        targetValue = if (index == step) 1.2f else 0.8f,
                                        animationSpec = tween(300)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(
                                                if (index == step) 10.dp else 6.dp
                                            )
                                            .padding(horizontal = 3.dp)
                                            .clip(CircleShape)
                                            .scale(scale)
                                            .background(
                                                if (index <= step) Color.White
                                                else Color.White.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botões de navegação
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (step > 0) {
                                    TextButton(
                                        onClick = onPrevious,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Text(
                                            "← Anterior",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(48.dp))
                                }

                                // Botão principal
                                IconButton(
                                    onClick = if (step == steps.size - 1) onSkip else onNext,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                ) {
                                    if (step == steps.size - 1) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Concluir",
                                            tint = WhatsAppDarkGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                            contentDescription = "Próximo",
                                            tint = WhatsAppDarkGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Botão pular
                                TextButton(
                                    onClick = onSkip,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        "Pular",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Indicador de passo
                            Text(
                                text = "${step + 1} de ${steps.size}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TutorialStep(
    val title: String,
    val description: String,
    val field: String
)