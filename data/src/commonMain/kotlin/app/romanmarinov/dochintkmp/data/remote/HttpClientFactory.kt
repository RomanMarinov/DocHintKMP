package app.romanmarinov.dochintkmp.data.remote

import io.ktor.client.HttpClient

/**
 * Платформенная фабрика HTTP-клиента для Ktor (Android — OkHttp, iOS — Darwin).
 */
expect fun createOpenRouterHttpClient(): HttpClient

