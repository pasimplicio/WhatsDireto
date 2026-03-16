@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package br.com.whatsdireto.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.whatsdireto.data.HistoryEntry
import br.com.whatsdireto.domain.PhoneMask
import br.com.whatsdireto.ui.theme.ChatBubble
import br.com.whatsdireto.ui.theme.DoodleLine
import br.com.whatsdireto.ui.theme.WhatsAppBg
import br.com.whatsdireto.ui.theme.WhatsAppDarkGreen
import br.com.whatsdireto.ui.theme.WhatsAppGreen
import br.com.whatsdireto.ui.theme.WhatsAppHeaderGreen
import kotlinx.coroutines.launch

@Composable
fun WhatsAppScreen() {
    val context = LocalContext.current
    val viewModel: WhatsAppViewModel = viewModel(
        factory = remember(context) { WhatsAppViewModel.factory(context) }
    )

    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estado para controlar se o tutorial deve ser mostrado manualmente
    var showTutorialManually by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }

    // Detecção automática de número copiado
    LaunchedEffect(Unit) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        manager?.addPrimaryClipChangedListener {
            viewModel.checkClipboardForNumber(context)
        }
    }

    // Contact Picker
    if (state.showContactPicker) {
        ContactPicker(
            onContactSelected = { normalizedPhone ->
                val formatted = PhoneMask.formatForDisplay(normalizedPhone)
                viewModel.onPhoneChanged(TextFieldValue(formatted))
            },
            onDismiss = viewModel::toggleContactPicker
        )
    }

    // Quick Messages Bottom Sheet
    if (state.showQuickMessages) {
        QuickMessagesSheet(
            messages = state.quickMessages,
            onMessageSelected = viewModel::onQuickMessageSelected,
            onDeleteMessage = viewModel::deleteQuickMessage,
            onReset = viewModel::resetQuickMessages,
            onDismiss = viewModel::toggleQuickMessages
        )
    }

    // Tutorial Dialog - Mostra automaticamente só no primeiro acesso
    if (state.isFirstTimeUser) {
        TutorialBubble(
            step = state.currentTutorialStep,
            onNext = viewModel::nextTutorialStep,
            onPrevious = viewModel::previousTutorialStep,
            onSkip = viewModel::skipTutorial,
            onClose = viewModel::skipTutorial
        )
    }

    // Tutorial Dialog - Mostra manualmente quando clicar no botão de ajuda
    if (showTutorialManually) {
        TutorialBubble(
            step = 0, // Começa do início quando aberto manualmente
            onNext = {
                if (state.currentTutorialStep < 5) {
                    viewModel.nextTutorialStep()
                } else {
                    showTutorialManually = false
                    viewModel.completeTutorial()
                }
            },
            onPrevious = viewModel::previousTutorialStep,
            onSkip = {
                showTutorialManually = false
                viewModel.completeTutorial()
            },
            onClose = {
                showTutorialManually = false
                viewModel.completeTutorial()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsDireto") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppHeaderGreen,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Botão de ajuda para abrir o tutorial manualmente - usando AutoMirrored
                    IconButton(
                        onClick = {
                            showTutorialManually = true
                            viewModel.completeTutorial() // Marca que já viu o tutorial
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = "Ajuda",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            DeveloperFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = WhatsAppBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WhatsAppBg)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            WhatsAppDoodleBackground()

            // Conteúdo principal
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 8.dp,
                        start = 12.dp,
                        end = 12.dp
                    )
                    .safeDrawingPadding()
                    .navigationBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 8.dp,
                    bottom = 88.dp
                )
            ) {
                item {
                    ModernInputCard(
                        phoneInput = state.phoneInput,
                        messageInput = state.messageInput,
                        onPhoneChanged = viewModel::onPhoneChanged,
                        onMessageChanged = viewModel::onMessageChanged,
                        onPasteClick = { viewModel.pasteFromClipboard(context) },
                        onContactsClick = viewModel::toggleContactPicker,
                        onQuickMessagesClick = viewModel::toggleQuickMessages,
                        onSendClick = { viewModel.onSendClick(context) }
                    )
                }

                item {
                    ModernHistoryCard(
                        history = if (state.isSearching) state.filteredHistory else state.history,
                        historyLabel = viewModel::historyLabel,
                        onSelect = viewModel::onHistorySelected,
                        onClear = viewModel::clearHistory,
                        onSearch = viewModel::searchHistory,
                        onClearSearch = viewModel::clearSearch,
                        isSearching = state.isSearching
                    )
                }
            }

            // Barra de rolagem personalizada
            LazyColumnScrollbar(
                listState = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 4.dp)
            )
        }
    }
}

@Composable
fun LazyColumnScrollbar(
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val canScroll by remember {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }

    if (canScroll) {
        val targetAlpha = if (listState.isScrollInProgress) 1f else 0.3f

        Box(
            modifier = modifier,
            contentAlignment = Alignment.CenterEnd
        ) {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val totalItemsCount = listState.layoutInfo.totalItemsCount

            if (totalItemsCount > 0) {
                val scrollProgress = if (totalItemsCount > 1) {
                    firstVisibleItemIndex.toFloat() / (totalItemsCount - 1).toFloat()
                } else 0f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp)
                ) {
                    val barHeight = size.height * 0.15f
                    val barY = (size.height - barHeight) * scrollProgress

                    drawRoundRect(
                        color = WhatsAppDarkGreen.copy(alpha = targetAlpha),
                        topLeft = Offset(size.width - 4.dp.toPx(), barY),
                        size = androidx.compose.ui.geometry.Size(
                            width = 4.dp.toPx(),
                            height = barHeight
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernInputCard(
    phoneInput: TextFieldValue,
    messageInput: TextFieldValue,
    onPhoneChanged: (TextFieldValue) -> Unit,
    onMessageChanged: (TextFieldValue) -> Unit,
    onPasteClick: () -> Unit,
    onContactsClick: () -> Unit,
    onQuickMessagesClick: () -> Unit,
    onSendClick: () -> Unit,
    highlightField: String? = null
) {
    val isPhoneValid by remember(phoneInput.text) {
        derivedStateOf { PhoneMask.isValid(phoneInput.text) }
    }

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título com ícone
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Nova conversa",
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppDarkGreen,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Campo de telefone com ações
            OutlinedTextField(
                value = phoneInput,
                onValueChange = onPhoneChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (highlightField == "phone") Modifier.background(
                            WhatsAppGreen.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        ) else Modifier
                    ),
                leadingIcon = {
                    Icon(Icons.Outlined.Phone, null, tint = WhatsAppGreen)
                },
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = onContactsClick,
                            modifier = if (highlightField == "contacts")
                                Modifier.background(WhatsAppGreen.copy(alpha = 0.2f), CircleShape)
                            else Modifier
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = "Contatos",
                                tint = WhatsAppDarkGreen
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                label = { Text("Número com DDD") },
                placeholder = { Text("(11) 99999-9999") },
                singleLine = true,
                isError = phoneInput.text.isNotBlank() && !isPhoneValid,
                supportingText = {
                    if (phoneInput.text.isNotBlank() && !isPhoneValid) {
                        Text(
                            "Número inválido",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = WhatsAppGreen,
                    focusedLabelColor = WhatsAppGreen,
                    cursorColor = WhatsAppGreen,
                    errorIndicatorColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error
                )
            )

            // Campo de mensagem rápida
            OutlinedTextField(
                value = messageInput,
                onValueChange = onMessageChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (highlightField == "message") Modifier.background(
                            WhatsAppGreen.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        ) else Modifier
                    ),
                leadingIcon = {
                    IconButton(
                        onClick = onQuickMessagesClick,
                        modifier = if (highlightField == "quick")
                            Modifier.background(WhatsAppGreen.copy(alpha = 0.2f), CircleShape)
                        else Modifier
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Mensagens rápidas",
                            tint = WhatsAppDarkGreen
                        )
                    }
                },
                label = { Text("Mensagem (opcional)") },
                placeholder = { Text("Digite uma mensagem ou escolha um template") },
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = WhatsAppGreen,
                    focusedLabelColor = WhatsAppGreen,
                    cursorColor = WhatsAppGreen
                )
            )

            // Chips de ação
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onPasteClick,
                    label = { Text("Colar") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.ContentPaste,
                            null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (highlightField == "paste")
                            WhatsAppGreen.copy(alpha = 0.3f)
                        else WhatsAppBg
                    ),
                    modifier = if (highlightField == "paste")
                        Modifier.background(WhatsAppGreen.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    else Modifier
                )
            }

            // Botão principal
            Button(
                onClick = onSendClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isPhoneValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    disabledContainerColor = WhatsAppGreen.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Abrir conversa no WhatsApp",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ModernHistoryCard(
    history: List<HistoryEntry>,
    historyLabel: (HistoryEntry) -> String,
    onSelect: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    isSearching: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmation by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Cabeçalho com busca
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Histórico",
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppDarkGreen,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                if (history.isNotEmpty()) {
                    IconButton(onClick = { showClearConfirmation = true }) {
                        Icon(
                            Icons.Outlined.DeleteSweep,
                            contentDescription = "Limpar histórico",
                            tint = WhatsAppDarkGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.length >= 3) {
                        onSearch(it)
                    } else if (it.isEmpty() && isSearching) {
                        onClearSearch()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar no histórico...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            onClearSearch()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = WhatsAppGreen,
                    focusedLabelColor = WhatsAppGreen,
                    cursorColor = WhatsAppGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de histórico
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSearching)
                            "Nenhum resultado encontrado"
                        else
                            "Nenhum número usado ainda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { entry ->
                        HistoryItem(
                            entry = entry,
                            label = historyLabel(entry),
                            onClick = { onSelect(entry) }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmação para limpar histórico
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Limpar histórico") },
            text = { Text("Tem certeza que deseja limpar todo o histórico?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        showClearConfirmation = false
                    }
                ) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = WhatsAppBg.copy(alpha = 0.5f),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = WhatsAppGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = PhoneMask.formatForDisplay(entry.phone),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = entry.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickMessagesSheet(
    messages: List<String>,
    onMessageSelected: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Mensagens rápidas",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Restaurar padrões"
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(messages) { message ->
                    QuickMessageItem(
                        message = message,
                        onSelect = { onMessageSelected(message) },
                        onDelete = { onDeleteMessage(message) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickMessageItem(
    message: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WhatsAppBg
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remover",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DeveloperFooter(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Desenvolvido por Paulo Simplicio",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "pasimplicio@gmail.com",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun WhatsAppDoodleBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 140f
        var y = 40f

        while (y < size.height + 120f) {
            var x = 20f

            while (x < size.width + 120f) {
                drawCircle(
                    color = DoodleLine,
                    radius = 18f,
                    center = Offset(x, y),
                    style = Stroke(width = 2f)
                )
                x += step
            }

            y += step
        }
    }
}