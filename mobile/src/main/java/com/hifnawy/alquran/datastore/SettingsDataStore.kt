package com.hifnawy.alquran.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.QuranApplication.Companion.LocaleInfo
import com.hifnawy.alquran.shared.domain.IObservable
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.asJsonString
import com.hifnawy.alquran.utils.StringEx.snakeCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.reflect.KClass

object SettingsDataStore {
    data class SettingsData(val locale: LocaleInfo, val theme: Theme, val dynamicColors: Boolean)

    enum class Theme(val code: Int) {
        LIGHT(code = 0),
        SYSTEM(code = 1),
        DARK(code = 2);

        companion object {

            fun fromCode(code: Int) = entries.first { it.code == code }
        }
    }

    private sealed class SettingsKey<T>(val key: Preferences.Key<T>) {

        private companion object {

            fun stringKey(kClass: KClass<*>) = stringPreferencesKey(kClass.java.simpleName)
            fun intKey(kClass: KClass<*>) = intPreferencesKey(kClass.java.simpleName)
            fun boolKey(kClass: KClass<*>) = booleanPreferencesKey(kClass.java.simpleName)
        }

        data object Locale : SettingsKey<String>(stringKey(Locale::class))
        data object Theme : SettingsKey<Int>(intKey(Theme::class))
        data object DynamicColors : SettingsKey<Boolean>(boolKey(DynamicColors::class))
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()

    private val Context.dataStore by preferencesDataStore(name = this::class.java.simpleName.snakeCase)

    private val settingsDataObservers = mutableListOf<SettingsDataObserver>()

    private lateinit var lastSettingsData: SettingsData

    suspend fun getLocale(context: Context) = context.dataStore.data.map { preferences ->
        Gson().fromJson(preferences[SettingsKey.Locale.key], LocaleInfo::class.java)
    }.first() ?: QuranApplication.currentLocaleInfo

    suspend fun setLocale(context: Context, locale: LocaleInfo) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKey.Locale.key] = locale.asJsonString

            lastSettingsData = lastSettingsData.copy(locale = locale)

            notifySettingsDataObservers(lastSettingsData)
        }
    }

    suspend fun getTheme(context: Context) = context.dataStore.data.map { preferences ->
        preferences[SettingsKey.Theme.key]?.let { Theme.fromCode(it) }
    }.first() ?: Theme.SYSTEM

    suspend fun setTheme(context: Context, theme: Theme) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKey.Theme.key] = theme.code

            lastSettingsData = lastSettingsData.copy(theme = theme)

            notifySettingsDataObservers(lastSettingsData)
        }
    }

    suspend fun getDynamicColors(context: Context) = context.dataStore.data.map { preferences ->
        preferences[SettingsKey.DynamicColors.key]
    }.first() ?: true

    suspend fun setDynamicColors(context: Context, dynamicColors: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKey.DynamicColors.key] = dynamicColors

            lastSettingsData = lastSettingsData.copy(dynamicColors = dynamicColors)

            notifySettingsDataObservers(lastSettingsData)
        }
    }

    private fun notifySettingsDataObservers(data: SettingsData) {
        settingsDataObservers.forEach { observer -> observer.onSettingsDataUpdated(data) }
    }

    fun interface SettingsDataObserver : IObservable {

        fun onSettingsDataUpdated(data: SettingsData)
    }

    init {
        coroutineScope.launch {
            lastSettingsData = SettingsData(
                    locale = getLocale(QuranApplication.applicationContext),
                    theme = getTheme(QuranApplication.applicationContext),
                    dynamicColors = getDynamicColors(QuranApplication.applicationContext)
            )
        }
    }

    @Composable
    fun SettingsObserver(observer: SettingsDataObserver) {
        val settingsDataStore = remember { SettingsDataStore }

        val observer = remember {
            object : SettingsDataObserver {
                override fun onSettingsDataUpdated(data: SettingsData) {
                    observer.onSettingsDataUpdated(data)
                }
            }
        }

        DisposableEffect(settingsDataStore) {
            settingsDataStore.settingsDataObservers.add(observer)

            onDispose {
                settingsDataStore.settingsDataObservers.remove(observer)
            }
        }
    }
}
