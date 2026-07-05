package app.romanmarinov.dochintkmp.data.remote.share

object ShareApiEndpoint {
    const val SHARE = "share"

    fun consumeShare(code: String): String = "$SHARE/$code"
}
