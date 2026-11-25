package br.com.whatsdireto.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.QrCodeMatrix

@Composable
fun QrLinkDialog(
    normalizedPhone: String,
    url: String,
    supportsShare: Boolean,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onDismiss: () -> Unit
) {
    val qrMatrix = remember(url) { QrCodeMatrix.encode(url) }
    val displayPhone = remember(normalizedPhone) { PhoneNumberUtils.formatFullForDisplay(normalizedPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QR Code da conversa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = displayPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (qrMatrix != null) {
                    // Always render on pure white with quiet zone for reliable scanning in dark mode too
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Canvas(modifier = Modifier.size(210.dp)) {
                                val count = qrMatrix.size
                                val cellSize = size.width / count.toFloat()
                                for (y in 0 until count) {
                                    for (x in 0 until count) {
                                        if (qrMatrix[x, y]) {
                                            drawRect(
                                                color = Color(0xFF111B21),
                                                topLeft = Offset(x * cellSize, y * cellSize),
                                                size = Size(cellSize + 0.5f, cellSize + 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "A mensagem está muito longa para caber no QR Code. Encurte a mensagem para gerar o código.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCopyLink,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar", style = MaterialTheme.typography.labelMedium)
                    }
                    if (supportsShare) {
                        FilledTonalButton(
                            onClick = onShareLink,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
