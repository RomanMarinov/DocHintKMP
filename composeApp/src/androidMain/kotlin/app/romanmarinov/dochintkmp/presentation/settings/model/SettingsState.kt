package app.romanmarinov.dochintkmp.presentation.settings.model

data class SettingsState(
    val savedKey: String = "",
    val hasSavedKey: Boolean = false,
    val keyDraft: String = "",
    val isKeyModified: Boolean = false,
    val keyInfoLoading: Boolean = false,
    val keyInfoError: String? = null,
    val keyInfo: KeyInfo? = null
)
