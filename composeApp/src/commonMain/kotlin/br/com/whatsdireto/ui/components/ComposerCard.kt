package br.com.whatsdireto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.whatsdireto.domain.Country
import br.com.whatsdireto.domain.PhoneVisualTransformation
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposerCard(
    phoneDigits: String,
    messageText: String,
    selectedCountry: Country,
    isPhoneValid: Boolean,
    validationHint: String?,
    whatsAppPreference: WhatsAppAppPreference,
    capabilities: PlatformCapabilities,
    quickMessages: List<String>,
    onCountryPickerClick: () -> Unit,
    onPhoneDigitsChanged: (String) -> Unit,
    onClearPhone: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onPasteClick: () -> Unit,
    onContactsClick: () -> Unit,
    onQrScannerClick: () -> Unit,
    onQuickMessagesClick: () -> Unit,
    onSaveMessageTemplate: () -> Unit,
    onPreferenceChanged: (WhatsAppAppPreference) -> Unit,
    onOpenConversation: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit,
    onShowQrCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visualTransformation = remember(selectedCountry) {
        PhoneVisualTransformation(selectedCountry)
    }
    val canSaveTemplate = messageText.trim().isNotEmpty() &&
        quickMessages.none { it.equals(messageText.trim(), ignoreCase = true) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Nova conversa",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Envie mensagens sem precisar salvar o número na agenda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compact Country selector + Phone input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onCountryPickerClick),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedCountry.flag,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = selectedCountry.dialCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Selecionar país",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = phoneDigits,
                    onValueChange = onPhoneDigitsChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("Número (${selectedCountry.name})") },
                    placeholder = { Text(selectedCountry.exampleLocal) },
                    visualTransformation = visualTransformation,
                    singleLine = true,
                    isError = phoneDigits.isNotEmpty() && !isPhoneValid,
                    supportingText = if (phoneDigits.isNotEmpty() && validationHint != null) {
                        {
                            Text(
                                text = validationHint,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else null,
                    trailingIcon = {
                        if (phoneDigits.isNotEmpty()) {
                            IconButton(onClick = onClearPhone) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar número",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (isPhoneValid) onOpenConversation() }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )
            }

            // Quick input actions
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = onPasteClick,
                    label = { Text("Colar") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = null
                )

                if (capabilities.supportsContactsPicker) {
                    AssistChip(
                        onClick = onContactsClick,
                        label = { Text("Contatos") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PersonSearch,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null
                    )
                }

                if (capabilities.supportsQrScanner) {
                    AssistChip(
                        onClick = onQrScannerClick,
                        label = { Text("Ler QR") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            leadingIconContentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null
                    )
                }

                AssistChip(
                    onClick = onQuickMessagesClick,
                    label = { Text("Mensagens rápidas") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        leadingIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = null
                )
            }

            // Optional message input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mensagem inicial (opcional)") },
                    placeholder = { Text("Ex: Olá! Podemos conversar?") },
                    minLines = 2,
                    maxLines = 4,
                    trailingIcon = {
                        if (messageText.isNotEmpty()) {
                            IconButton(onClick = { onMessageChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar mensagem",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canSaveTemplate) {
                        TextButton(
                            onClick = onSaveMessageTemplate,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Salvar nos modelos rápidos",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (messageText.isNotEmpty()) {
                        Text(
                            text = "${messageText.length} caracteres",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // WhatsApp app variant picker on Android
            if (capabilities.supportsWhatsAppVariantPicker) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WhatsAppAppPreference.entries.forEach { pref ->
                        val selected = whatsAppPreference == pref
                        FilterChip(
                            selected = selected,
                            onClick = { onPreferenceChanged(pref) },
                            label = {
                                Text(
                                    text = pref.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Primary CTA
            Button(
                onClick = onOpenConversation,
                enabled = isPhoneValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Abrir conversa no WhatsApp",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Secondary link utilities (Copy / QR Code / Share)
            AnimatedVisibility(
                visible = isPhoneValid,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCopyLink,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar link", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = onShowQrCode,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("QR Code", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }

                    if (capabilities.supportsNativeShare) {
                        FilledTonalButton(
                            onClick = onShareLink,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Compartilhar link",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
