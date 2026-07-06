package com.minipos.core.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One-shot navigation request from outside the compose tree (Phase 36): a notification tap sets a
 * route here via MainActivity; MainScaffold navigates to it once and clears it. Survives the
 * splash/license gate because the value stays until consumed.
 */
object PendingNav {

    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route

    fun request(route: String) {
        _route.value = route
    }

    fun consume() {
        _route.value = null
    }
}
