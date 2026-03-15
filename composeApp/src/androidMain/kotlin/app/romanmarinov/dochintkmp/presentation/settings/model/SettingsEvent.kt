package app.romanmarinov.dochintkmp.presentation.settings.model

sealed interface SettingsEvent {
    data class UpdateKeyDraft(val value: String) : SettingsEvent
    data object SaveApiKey : SettingsEvent
    data object ClearSavedKey : SettingsEvent
    data object LoadKeyInfo : SettingsEvent
}
