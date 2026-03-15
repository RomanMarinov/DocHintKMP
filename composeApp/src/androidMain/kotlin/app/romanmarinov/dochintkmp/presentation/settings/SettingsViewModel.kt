package app.romanmarinov.dochintkmp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.romanmarinov.dochintkmp.data.local.SecureStorage
import app.romanmarinov.dochintkmp.data.remote.OpenRouterClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class SettingsViewModel(
    private val secureStorage: SecureStorage,
    private val openRouterClient: OpenRouterClient
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsState(
            savedKey = secureStorage.apiKey,
            hasSavedKey = secureStorage.apiKey.isNotEmpty()
        )
    )
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _effect = Channel<SettingsEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        if (secureStorage.apiKey.isNotEmpty()) {
            loadKeyInfo()
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateKeyDraft -> {
                _state.update {
                    it.copy(
                        keyDraft = event.value,
                        isKeyModified = event.value.isNotEmpty() && event.value != it.savedKey
                    )
                }
            }
            is SettingsEvent.SaveApiKey -> saveApiKey()
            is SettingsEvent.ClearSavedKey -> clearSavedKey()
            is SettingsEvent.LoadKeyInfo -> loadKeyInfo()
        }
    }

    private fun loadKeyInfo() {
        val key = _state.value.savedKey.trim()
        if (key.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(keyInfoLoading = true, keyInfoError = null) }
            try {
                val data = openRouterClient.getKeyInfo(key)
                if (data != null) {
                    _state.update {
                        it.copy(keyInfo = KeyInfo(usage = data.usage, isFreeTier = data.isFreeTier))
                    }
                } else {
                    _state.update { it.copy(keyInfoError = "Не удалось загрузить данные ключа") }
                }
            } catch (e: UnknownHostException) {
                _state.update { it.copy(keyInfoError = "Нет сети") }
            } catch (e: Exception) {
                _state.update { it.copy(keyInfoError = e.message ?: "Ошибка проверки") }
            } finally {
                _state.update { it.copy(keyInfoLoading = false) }
            }
        }
    }

    private fun saveApiKey() {
        val key = _state.value.keyDraft.trim()
        if (key.isEmpty()) return
        secureStorage.apiKey = key
        _state.update {
            it.copy(savedKey = key, hasSavedKey = true, keyDraft = "", isKeyModified = false)
        }
        viewModelScope.launch { _effect.send(SettingsEffect.ShowToast("API ключ сохранён")) }
        loadKeyInfo()
    }

    private fun clearSavedKey() {
        secureStorage.apiKey = ""
        _state.update {
            it.copy(
                savedKey = "",
                hasSavedKey = false,
                keyDraft = "",
                isKeyModified = false,
                keyInfo = null,
                keyInfoError = null
            )
        }
        viewModelScope.launch { _effect.send(SettingsEffect.ShowToast("API ключ удалён")) }
    }
}
