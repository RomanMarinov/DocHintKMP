package app.romanmarinov.dochintkmp.presentation.settings

data class KeyInfo(
    val usage: Double = 0.0,
    val isFreeTier: Boolean = false
)

data class SettingsState(
    val savedKey: String = "",
    val hasSavedKey: Boolean = false,
    val keyDraft: String = "",
    val isKeyModified: Boolean = false,
    val keyInfoLoading: Boolean = false,
    val keyInfoError: String? = null,
    val keyInfo: KeyInfo? = null
)

sealed interface SettingsEvent {
    data class UpdateKeyDraft(val value: String) : SettingsEvent
    data object SaveApiKey : SettingsEvent
    data object ClearSavedKey : SettingsEvent
    data object LoadKeyInfo : SettingsEvent
}

sealed interface SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect
}
