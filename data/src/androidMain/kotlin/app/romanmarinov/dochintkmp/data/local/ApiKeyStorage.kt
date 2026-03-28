package app.romanmarinov.dochintkmp.data.local

/**
 * Minimal abstraction over secure API key storage.
 * Exists mainly to make Android ViewModel unit-testing possible.
 */
interface ApiKeyStorage {
    val apiKey: String
}

