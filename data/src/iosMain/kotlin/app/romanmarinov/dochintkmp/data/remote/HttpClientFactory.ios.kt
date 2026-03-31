package app.romanmarinov.dochintkmp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json

actual fun createOpenRouterHttpClient(): HttpClient = HttpClient(Darwin) {
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("KTOR LOGGING httpClient: $message")
            }
        }
        level = LogLevel.BODY
    }

    install(ContentNegotiation) {
        json(createAppJson())
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
    }

    install(ResponseObserver) {
        onResponse {
            println("KTOR protocol=${it.version}")
        }
    }

    defaultRequest {
        url {
            protocol = URLProtocol.HTTPS
            host = "openrouter.ai"
        }
    }
}

