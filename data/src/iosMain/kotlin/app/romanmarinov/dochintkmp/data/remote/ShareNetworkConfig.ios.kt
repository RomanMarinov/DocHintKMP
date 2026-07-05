package app.romanmarinov.dochintkmp.data.remote

import platform.Foundation.NSProcessInfo

actual object ShareNetworkConfig {
    actual val baseUrl: String =
        NSProcessInfo.processInfo.environment["DOCHINT_SHARE_BASE_URL"] as? String
            ?: "http://94.183.184.60:8081"


    // "http://192.168.1.143:8081" для локально теста
}
