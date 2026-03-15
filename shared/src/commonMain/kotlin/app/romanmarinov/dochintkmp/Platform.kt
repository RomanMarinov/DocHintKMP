package app.romanmarinov.dochintkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform