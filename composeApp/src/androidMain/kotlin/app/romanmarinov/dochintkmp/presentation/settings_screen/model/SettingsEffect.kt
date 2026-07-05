package app.romanmarinov.dochintkmp.presentation.settings_screen.model

sealed interface SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect
}
