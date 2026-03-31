package app.romanmarinov.dochintkmp.data.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object DatePatterns {
    const val DD_MM_YYYY = "dd.MM.yyyy"
    const val DD_MM_YYYY_HH_MM = "dd.MM.yyyy HH:mm"
}

fun Instant.formatDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String = toLocalDateTime(timeZone).formatDateTime()

fun LocalDateTime.formatDateTime(): String =
    "${date.formatDate()} ${hour.twoDigits()}:${minute.twoDigits()}"

fun LocalDate.formatDate(): String =
    "${dayOfMonth.twoDigits()}.${monthNumber.twoDigits()}.$year"

fun String.isoUtcToDateTimeOrSelf(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String = runCatching {
    Instant.parse(this).formatDateTime(timeZone)
}.getOrElse { this }

fun String.isoUtcToMoscowDateTimeOrSelf(): String =
    isoUtcToDateTimeOrSelf(TimeZone.of("Europe/Moscow"))

fun Long.toLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

fun Long.toLocalDate(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDate = toLocalDateTime(timeZone).date

fun LocalDate.startOfDayEpochMillis(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long = atStartOfDayIn(timeZone).toEpochMilliseconds()

fun LocalDateTime.toEpochMillis(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): Long = toInstant(timeZone).toEpochMilliseconds()

val nowLocalDateTime: LocalDateTime
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun Int.twoDigits(): String = toString().padStart(2, '0')

