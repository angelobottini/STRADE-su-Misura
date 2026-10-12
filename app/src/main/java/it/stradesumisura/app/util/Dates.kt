package it.stradesumisura.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date helpers shared by the UI and the PDF exporter. Dates are stored as ISO-8601 strings. */
object Dates {
    private val locale: Locale = Locale.ITALIAN
    private val longFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
    private val shortFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)
    private val timestampFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale)

    fun today(): LocalDate = LocalDate.now()

    fun parse(iso: String?): LocalDate? =
        iso?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    fun toIso(date: LocalDate): String = date.toString()

    fun formatLong(date: LocalDate?): String = date?.format(longFormat) ?: "—"

    fun formatShort(date: LocalDate?): String = date?.format(shortFormat) ?: "—"

    fun formatIsoLong(iso: String?): String = formatLong(parse(iso))

    fun formatIsoShort(iso: String?): String = formatShort(parse(iso))

    fun ageAt(birth: LocalDate?, at: LocalDate = today()): Int? =
        birth?.takeIf { !it.isAfter(at) }?.let { Period.between(it, at).years }

    fun toUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromUtcMillis(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun formatTimestamp(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timestampFormat)

    fun fileStamp(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", locale))
}
