package com.minipos.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates are Long epoch millis (CONVENTIONS §4). java.time via desugaring. */
object DateUtil {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH)
    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd MMM yy, h:mm a", Locale.ENGLISH)

    fun formatDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(dateFmt)

    fun formatDateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(dateTimeFmt)

    fun startOfDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(millis: Long): Long =
        startOfDay(millis) + DAY_MILLIS - 1

    fun startOfMonth(millis: Long): Long {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().withDayOfMonth(1)
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfMonth(millis: Long): Long {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val firstNext = date.withDayOfMonth(1).plusMonths(1)
        return firstNext.atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }

    fun startOfYear(millis: Long): Long {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().withDayOfYear(1)
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfYear(millis: Long): Long {
        val year = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().year
        return LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }

    /** Inclusive [start, end] millis for a ledger/report filter. */
    fun rangeFor(
        filter: DateFilter,
        now: Long = System.currentTimeMillis(),
        customStart: Long? = null,
        customEnd: Long? = null,
    ): Pair<Long, Long> = when (filter) {
        DateFilter.DAY -> startOfDay(now) to endOfDay(now)
        DateFilter.MONTH -> startOfMonth(now) to endOfMonth(now)
        DateFilter.YEAR -> startOfYear(now) to endOfYear(now)
        DateFilter.ALL -> 0L to Long.MAX_VALUE
        DateFilter.CUSTOM ->
            (customStart?.let { startOfDay(it) } ?: 0L) to (customEnd?.let { endOfDay(it) } ?: Long.MAX_VALUE)
    }

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
