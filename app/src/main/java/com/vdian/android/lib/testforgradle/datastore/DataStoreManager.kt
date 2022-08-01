package com.vdian.android.lib.testforgradle.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * @author yulun
 * @since 2022-08-01 17:45
 */
class DataStoreManager {
    companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        suspend fun read(key: String, context: Context): String {
            val exampleCounterFlow: Flow<String> = context.dataStore.data.map { preferences ->
                // No type safety.
                preferences[stringPreferencesKey(key)] ?: ""
            }
            return exampleCounterFlow.first()
        }

        suspend fun write(key: String, value: String, context: Context) {
            context.dataStore.edit { settings ->
                settings[stringPreferencesKey(key)] = value
            }
        }
    }
}