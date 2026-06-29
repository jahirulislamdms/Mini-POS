package com.minipos.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "minipos_prefs")

/**
 * Stores the selected shopId in DataStore and exposes it as a Flow (CONVENTIONS §3).
 * Screens observe [currentShopId] and re-query when it changes.
 */
class CurrentShopManager(private val context: Context) {

    private val keyShopId = longPreferencesKey("current_shop_id")

    /** Emits the active shopId, or null when no shop has been selected yet. */
    val currentShopId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[keyShopId]
    }

    suspend fun setCurrentShop(shopId: Long) {
        context.dataStore.edit { prefs -> prefs[keyShopId] = shopId }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs -> prefs.remove(keyShopId) }
    }
}
