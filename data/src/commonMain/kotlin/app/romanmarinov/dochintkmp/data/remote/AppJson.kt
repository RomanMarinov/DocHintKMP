package app.romanmarinov.dochintkmp.data.remote

import kotlinx.serialization.json.Json

fun createAppJson(): Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}
