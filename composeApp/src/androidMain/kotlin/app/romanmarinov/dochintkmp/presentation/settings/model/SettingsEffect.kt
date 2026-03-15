package app.romanmarinov.dochintkmp.presentation.settings.model

sealed interface SettingsEffect {
    data class ShowToast(val message: String) : SettingsEffect
}
