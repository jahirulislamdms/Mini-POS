package com.minipos.core.util

/** Standard ledger/report time filters (BUILD_PLAN §6). */
enum class DateFilter(val label: String) {
    DAY("Day"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All"),
    CUSTOM("Custom"),
}
