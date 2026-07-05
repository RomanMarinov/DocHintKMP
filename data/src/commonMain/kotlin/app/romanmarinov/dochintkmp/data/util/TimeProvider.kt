package app.romanmarinov.dochintkmp.data.util

import kotlinx.datetime.Clock

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
