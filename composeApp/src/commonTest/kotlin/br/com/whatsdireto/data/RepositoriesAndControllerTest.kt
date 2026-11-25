package br.com.whatsdireto.data

import br.com.whatsdireto.domain.Countries
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference
import br.com.whatsdireto.platform.PlatformBridge
import br.com.whatsdireto.ui.TUTORIAL_TOTAL_STEPS
import br.com.whatsdireto.ui.WhatsDiretoController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakePlatformBridge(
    override val storage: KeyValueStorage = InMemoryKeyValueStorage(),
    var clipboardText: String? = null
) : PlatformBridge {
    var now: Long = 1_700_000_000_000L
    var lastOpenedPhone: String? = null
    var lastOpenedMessage: String? = null

    override val capabilities = PlatformCapabilities(
        platformName = "Test",
        supportsContactsPicker = false,
        supportsQrScanner = false,
        supportsWhatsAppVariantPicker = true,
        supportsNativeShare = true
    )

    override fun openWhatsApp(
        normalizedPhone: String,
        message: String,
        preference: WhatsAppAppPreference
    ): LaunchResult {
        lastOpenedPhone = normalizedPhone
        lastOpenedMessage = message
        return LaunchResult.Success
    }

    override fun readClipboard(): String? = clipboardText
    override fun copyToClipboard(text: String): Boolean {
        clipboardText = text
        return true
    }

    override fun shareText(text: String): Boolean = true
    override fun currentTimeMillis(): Long = now
    override fun formatTimestamp(timestamp: Long): String = "Agora"
}

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoriesAndControllerTest {

    @Test
    fun quickMessageStorageHandlesPipesAndNewlinesSafely() {
        val storage = InMemoryKeyValueStorage()
        val repo = QuickMessageRepository(storage)
        val tricky = "Opção A | Opção B\nSegunda linha\tTab\\Barra"

        repo.addOrPromote(tricky)
        val reloaded = QuickMessageRepository(storage)
        assertEquals(tricky, reloaded.messages.value.first())
    }

    @Test
    fun historySupportsLabelsPinsDeleteAndUndo() {
        val storage = InMemoryKeyValueStorage()
        var clock = 1000L
        val repo = HistoryRepository(storage) { clock++ }

        repo.save("5511999990001")
        repo.save("5511999990002")
        repo.updateLabel("5511999990001", "Clínica | Sala 2")
        repo.togglePin("5511999990001")

        // Pinned entry must stay first even with older timestamp
        assertEquals("5511999990001", repo.history.value.first().phone)
        assertEquals("Clínica | Sala 2", repo.history.value.first().label)
        assertTrue(repo.history.value.first().isPinned)

        // Delete & restore
        val removed = repo.delete("5511999990001")!!
        assertEquals(1, repo.history.value.size)
        repo.restore(removed)
        assertEquals(2, repo.history.value.size)
        assertEquals("5511999990001", repo.history.value.first().phone)
    }

    @Test
    fun tutorialPersistenceAndManualNavigationWorkCorrectly() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bridge = FakePlatformBridge()

        val controller1 = WhatsDiretoController(
            platform = bridge,
            historyRepository = HistoryRepository(bridge.storage, bridge::currentTimeMillis),
            quickMessageRepository = QuickMessageRepository(bridge.storage),
            settingsRepository = SettingsRepository(bridge.storage),
            scope = TestScope(dispatcher)
        )

        assertTrue(controller1.uiState.value.showTutorial)
        assertEquals(0, controller1.uiState.value.tutorialStep)

        repeat(TUTORIAL_TOTAL_STEPS - 1) { index ->
            controller1.nextTutorialStep()
            assertEquals(index + 1, controller1.uiState.value.tutorialStep)
        }
        controller1.nextTutorialStep()
        assertFalse(controller1.uiState.value.showTutorial)

        // New session must remember tutorial was seen
        val controller2 = WhatsDiretoController(
            platform = bridge,
            historyRepository = HistoryRepository(bridge.storage, bridge::currentTimeMillis),
            quickMessageRepository = QuickMessageRepository(bridge.storage),
            settingsRepository = SettingsRepository(bridge.storage),
            scope = TestScope(dispatcher)
        )
        assertFalse(controller2.uiState.value.showTutorial)

        // Reopening manually starts at step 0 and advances properly
        controller2.openTutorial()
        assertTrue(controller2.uiState.value.showTutorial)
        assertEquals(0, controller2.uiState.value.tutorialStep)
        controller2.nextTutorialStep()
        assertEquals(1, controller2.uiState.value.tutorialStep)
    }

    @Test
    fun pasteAndOpenConversationSavesToHistory() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val bridge = FakePlatformBridge(clipboardText = "+351 912 345 678")
        val controller = WhatsDiretoController(
            platform = bridge,
            historyRepository = HistoryRepository(bridge.storage, bridge::currentTimeMillis),
            quickMessageRepository = QuickMessageRepository(bridge.storage),
            settingsRepository = SettingsRepository(bridge.storage),
            scope = TestScope(dispatcher)
        )

        controller.pasteFromClipboard()
        assertEquals(Countries.findByIso("PT"), controller.uiState.value.selectedCountry)
        assertEquals("912345678", controller.uiState.value.phoneDigits)
        assertTrue(controller.uiState.value.isPhoneValid)

        controller.onMessageChanged("Bom dia!")
        controller.openConversation()
        advanceUntilIdle()

        assertEquals("351912345678", bridge.lastOpenedPhone)
        assertEquals("Bom dia!", bridge.lastOpenedMessage)
        assertEquals(1, controller.uiState.value.history.size)
        assertEquals("351912345678", controller.uiState.value.history.first().phone)
    }
}
