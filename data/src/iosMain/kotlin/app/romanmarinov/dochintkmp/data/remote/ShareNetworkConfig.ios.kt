package app.romanmarinov.dochintkmp.data.remote

import platform.Foundation.NSProcessInfo

actual object ShareNetworkConfig {
    actual val baseUrl: String =
        NSProcessInfo.processInfo.environment["DOCHINT_SHARE_BASE_URL"] as? String
            ?: "http://127.0.0.1:8080"
}
