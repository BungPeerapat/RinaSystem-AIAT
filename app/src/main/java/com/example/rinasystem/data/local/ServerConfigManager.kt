package com.example.rinasystem.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.serverConfigStore: DataStore<Preferences> by preferencesDataStore(name = "aria_server_config")

@Singleton
class ServerConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVER_IP = stringPreferencesKey("server_ip")
        private val SERVER_PORT = intPreferencesKey("server_port")

        const val DEFAULT_IP = "systemdeveloper2.ddns.net"
        const val DEFAULT_PORT = 8000
    }

    val serverUrl: Flow<String> = context.serverConfigStore.data.map { prefs ->
        val ip = prefs[SERVER_IP] ?: DEFAULT_IP
        val port = prefs[SERVER_PORT] ?: DEFAULT_PORT
        "http://$ip:$port"
    }

    suspend fun getServerUrl(): String {
        val prefs = context.serverConfigStore.data.first()
        val ip = prefs[SERVER_IP] ?: DEFAULT_IP
        val port = prefs[SERVER_PORT] ?: DEFAULT_PORT
        return "http://$ip:$port"
    }

    suspend fun getServerIp(): String {
        return context.serverConfigStore.data.first()[SERVER_IP] ?: DEFAULT_IP
    }

    suspend fun getServerPort(): Int {
        return context.serverConfigStore.data.first()[SERVER_PORT] ?: DEFAULT_PORT
    }

    suspend fun saveConfig(ip: String, port: Int) {
        context.serverConfigStore.edit {
            it[SERVER_IP] = ip
            it[SERVER_PORT] = port
        }
    }
}
