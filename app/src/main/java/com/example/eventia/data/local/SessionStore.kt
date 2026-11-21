package com.example.eventia.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "session")

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.sessionDataStore
    private val token = stringPreferencesKey("token")

    suspend fun readToken(): String? = store.data.first()[token]

    suspend fun saveToken(value: String) {
        store.edit { it[token] = value }
    }

    suspend fun clearToken() {
        store.edit { it.remove(token) }
    }
}
