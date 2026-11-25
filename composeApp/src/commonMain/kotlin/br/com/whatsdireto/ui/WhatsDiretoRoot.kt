package br.com.whatsdireto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.whatsdireto.data.HistoryRepository
import br.com.whatsdireto.data.QuickMessageRepository
import br.com.whatsdireto.data.SettingsRepository
import br.com.whatsdireto.domain.AppThemeMode
import br.com.whatsdireto.domain.WhatsAppLaunchTarget
import br.com.whatsdireto.platform.PlatformBridge
import br.com.whatsdireto.platform.PlatformContactsOverlay
import br.com.whatsdireto.platform.PlatformQrScannerOverlay
import br.com.whatsdireto.ui.components.ComposerCard
import br.com.whatsdireto.ui.components.CountryPickerDialog
import br.com.whatsdireto.ui.components.EditHistoryLabelDialog
import br.com.whatsdireto.ui.components.HistoryCard
import br.com.whatsdireto.ui.components.QrLinkDialog
import br.com.whatsdireto.ui.components.QuickMessagesSheet
import br.com.whatsdireto.ui.components.TutorialDialog
import br.com.whatsdireto.ui.theme.WhatsDiretoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsDiretoRoot(
    platformBridge: PlatformBridge,
    incomingText: String? = null,
    onIncomingTextConsumed: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val controller = remember(platformBridge) {
        WhatsDiretoController(
            platform = platformBridge,
            historyRepository = HistoryRepository(
                storage = platformBridge.storage,
                nowProvider = platformBridge::currentTimeMillis
            ),
            quickMessageRepository = QuickMessageRepository(platformBridge.storage),
            settingsRepository = SettingsRepository(platformBridge.storage),
            scope = scope
        )
    }

    val state by controller.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(incomingText) {
        if (!incomingText.isNullOrBlank()) {
            controller.onIncomingExternalText(incomingText)
            onIncomingTextConsumed()
        }
    }

    LaunchedEffect(state.feedback?.id) {
        val event = state.feedback ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = event.message,
            actionLabel = event.actionLabel,
            withDismissAction = true,
            duration = if (event.undoDeletedEntry != null) SnackbarDuration.Long else SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && event.undoDeletedEntry != null) {
            controller.undoDeleteHistoryEntry(event.undoDeletedEntry)
        }
        controller.consumeFeedback()
    }

    WhatsDiretoTheme(themeMode = state.themeMode) {
        // Platform overlays (Android contacts & QR scanner)
        PlatformContactsOverlay(
            visible = state.showContactPicker,
            onContactPicked = controller::onContactPicked,
            onDismiss = { controller.setShowContactPicker(false) }
        )

        PlatformQrScannerOverlay(
            visible = state.showQrScanner,
            onCodeScanned = controller::onQrCodeScanned,
            onDismiss = { controller.setShowQrScanner(false) }
        )

        // Shared dialogs & sheets
        if (state.showCountryPicker) {
            CountryPickerDialog(
                selectedCountry = state.selectedCountry,
                onCountrySelected = controller::onCountrySelected,
                onDismiss = { controller.setShowCountryPicker(false) }
            )
        }

        if (state.showQuickMessages) {
            QuickMessagesSheet(
                messages = state.quickMessages,
                onMessageSelected = controller::onQuickMessageSelected,
                onAddMessage = controller::addQuickMessage,
                onUpdateMessage = controller::updateQuickMessage,
                onDeleteMessage = controller::deleteQuickMessage,
                onReset = controller::resetQuickMessages,
                onDismiss = { controller.setShowQuickMessages(false) }
            )
        }

        if (state.showQrGenerator && state.normalizedPhone != null && state.previewUrl != null) {
            QrLinkDialog(
                normalizedPhone = state.normalizedPhone!!,
                url = state.previewUrl!!,
                supportsShare = controller.capabilities.supportsNativeShare,
                onCopyLink = controller::copyCurrentLink,
                onShareLink = controller::shareCurrentLink,
                onDismiss = { controller.setShowQrGenerator(false) }
            )
        }

        state.editingHistoryEntry?.let { entry ->
            EditHistoryLabelDialog(
                entry = entry,
                onSave = { newLabel -> controller.saveHistoryLabel(entry.phone, newLabel) },
                onDismiss = controller::dismissEditingHistoryLabel
            )
        }

        if (state.showTutorial) {
            TutorialDialog(
                step = state.tutorialStep,
                onNext = controller::nextTutorialStep,
                onPrevious = controller::previousTutorialStep,
                onFinish = controller::completeTutorial
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                text = "WhatsDireto",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        val themeIcon = when (state.themeMode) {
                            AppThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                            AppThemeMode.LIGHT -> Icons.Outlined.LightMode
                            AppThemeMode.DARK -> Icons.Outlined.DarkMode
                        }
                        IconButton(onClick = controller::cycleThemeMode) {
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = "Alternar tema (${state.themeMode.label})"
                            )
                        }
                        IconButton(onClick = controller::openTutorial) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                contentDescription = "Guia rápido"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                val isWideLayout = maxWidth >= 840.dp

                if (isWideLayout) {
                    // Two-column responsive layout for Tablet, Desktop & Web
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 1120.dp)
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.48f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                ComposerCard(
                                    phoneDigits = state.phoneDigits,
                                    messageText = state.messageText,
                                    selectedCountry = state.selectedCountry,
                                    isPhoneValid = state.isPhoneValid,
                                    validationHint = state.validationHint,
                                    whatsAppPreference = state.whatsAppPreference,
                                    capabilities = controller.capabilities,
                                    quickMessages = state.quickMessages,
                                    onCountryPickerClick = { controller.setShowCountryPicker(true) },
                                    onPhoneDigitsChanged = controller::onPhoneDigitsChanged,
                                    onClearPhone = controller::clearPhoneInput,
                                    onMessageChanged = controller::onMessageChanged,
                                    onPasteClick = controller::pasteFromClipboard,
                                    onContactsClick = { controller.setShowContactPicker(true) },
                                    onQrScannerClick = { controller.setShowQrScanner(true) },
                                    onQuickMessagesClick = { controller.setShowQuickMessages(true) },
                                    onSaveMessageTemplate = controller::saveCurrentMessageAsQuickTemplate,
                                    onPreferenceChanged = controller::onWhatsAppPreferenceChanged,
                                    onOpenConversation = { controller.openConversation() },
                                    onOpenWhatsAppWeb = { controller.openConversation(WhatsAppLaunchTarget.WEB) },
                                    onOpenWhatsAppDesktop = { controller.openConversation(WhatsAppLaunchTarget.DESKTOP) },
                                    onCopyLink = controller::copyCurrentLink,
                                    onShareLink = controller::shareCurrentLink,
                                    onShowQrCode = { controller.setShowQrGenerator(true) }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(0.52f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                HistoryCard(
                                    entries = state.filteredHistory,
                                    totalCount = state.history.size,
                                    pinnedCount = state.pinnedCount,
                                    searchQuery = state.historySearchQuery,
                                    activeFilter = state.historyFilter,
                                    formatTimestamp = controller::formatTimestamp,
                                    onSearchChanged = controller::onHistorySearchChanged,
                                    onFilterChanged = controller::onHistoryFilterChanged,
                                    onSelectEntry = controller::onHistorySelected,
                                    onOpenDirectly = { controller.openHistoryEntryDirectly(it, WhatsAppLaunchTarget.WEB) },
                                    supportsDesktopAction = controller.capabilities.supportsWebAndDesktopButtons,
                                    onOpenInDesktop = { controller.openHistoryEntryDirectly(it, WhatsAppLaunchTarget.DESKTOP) },
                                    onTogglePin = controller::toggleHistoryPin,
                                    onEditLabel = controller::startEditingHistoryLabel,
                                    onCopyLink = controller::copyHistoryLink,
                                    onDeleteEntry = controller::deleteHistoryEntry,
                                    onClearAll = controller::clearHistory
                                )
                            }
                        }

                        AppCreditFooter(platformName = controller.capabilities.platformName)
                    }
                } else {
                    // Single-column layout for Mobile & narrow windows
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 620.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "composer") {
                            ComposerCard(
                                phoneDigits = state.phoneDigits,
                                messageText = state.messageText,
                                selectedCountry = state.selectedCountry,
                                isPhoneValid = state.isPhoneValid,
                                validationHint = state.validationHint,
                                whatsAppPreference = state.whatsAppPreference,
                                capabilities = controller.capabilities,
                                quickMessages = state.quickMessages,
                                onCountryPickerClick = { controller.setShowCountryPicker(true) },
                                onPhoneDigitsChanged = controller::onPhoneDigitsChanged,
                                onClearPhone = controller::clearPhoneInput,
                                onMessageChanged = controller::onMessageChanged,
                                onPasteClick = controller::pasteFromClipboard,
                                onContactsClick = { controller.setShowContactPicker(true) },
                                onQrScannerClick = { controller.setShowQrScanner(true) },
                                onQuickMessagesClick = { controller.setShowQuickMessages(true) },
                                onSaveMessageTemplate = controller::saveCurrentMessageAsQuickTemplate,
                                onPreferenceChanged = controller::onWhatsAppPreferenceChanged,
                                onOpenConversation = { controller.openConversation() },
                                onOpenWhatsAppWeb = { controller.openConversation(WhatsAppLaunchTarget.WEB) },
                                onOpenWhatsAppDesktop = { controller.openConversation(WhatsAppLaunchTarget.DESKTOP) },
                                onCopyLink = controller::copyCurrentLink,
                                onShareLink = controller::shareCurrentLink,
                                onShowQrCode = { controller.setShowQrGenerator(true) }
                            )
                        }

                        item(key = "history") {
                            HistoryCard(
                                entries = state.filteredHistory,
                                totalCount = state.history.size,
                                pinnedCount = state.pinnedCount,
                                searchQuery = state.historySearchQuery,
                                activeFilter = state.historyFilter,
                                formatTimestamp = controller::formatTimestamp,
                                onSearchChanged = controller::onHistorySearchChanged,
                                onFilterChanged = controller::onHistoryFilterChanged,
                                onSelectEntry = controller::onHistorySelected,
                                onOpenDirectly = { controller.openHistoryEntryDirectly(it) },
                                supportsDesktopAction = controller.capabilities.supportsWebAndDesktopButtons,
                                onOpenInDesktop = { controller.openHistoryEntryDirectly(it, WhatsAppLaunchTarget.DESKTOP) },
                                onTogglePin = controller::toggleHistoryPin,
                                onEditLabel = controller::startEditingHistoryLabel,
                                onCopyLink = controller::copyHistoryLink,
                                onDeleteEntry = controller::deleteHistoryEntry,
                                onClearAll = controller::clearHistory
                            )
                        }

                        item(key = "footer") {
                            AppCreditFooter(platformName = controller.capabilities.platformName)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCreditFooter(platformName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "WhatsDireto • $platformName • Paulo Simplicio",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
