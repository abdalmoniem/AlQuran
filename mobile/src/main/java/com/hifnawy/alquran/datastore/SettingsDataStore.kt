package com.hifnawy.alquran.datastore

import androidx.compose.runtime.DisposableEffectScope
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.hifnawy.alquran.datastore.SettingsDataStore.SettingsObserver
import com.hifnawy.alquran.datastore.SettingsDataStore.Theme.DARK
import com.hifnawy.alquran.datastore.SettingsDataStore.Theme.LIGHT
import com.hifnawy.alquran.datastore.SettingsDataStore.Theme.SYSTEM
import com.hifnawy.alquran.datastore.SettingsDataStore.lastSettingsData
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

/**
 * A singleton object that manages application settings using Jetpack DataStore.
 *
 * It provides methods to get and set user preferences such as `locale`, `theme`, and `dynamic colors`.
 * The settings are stored persistently using [preferencesDataStore].
 *
 * This object also implements an observer pattern, allowing different parts of the application
 * to listen for changes to the settings. This is particularly useful for updating the UI
 * in real-time when a setting is changed. The [SettingsObserver] Composable function
 * provides a convenient way to subscribe to these updates within a Compose UI.
 *
 * Settings are read asynchronously upon initialization to populate the [lastSettingsData] cache,
 * ensuring that the most recent settings are readily available.
 *
 * @author AbdElMoniem ElHifnawy
 */
object SettingsDataStore {

    /**
     * Represents the user's application settings.
     * This data class aggregates various user-configurable preferences.
     *
     * @property locale [LocaleInfo] The current language and locale settings for the application, represented by [LocaleInfo].
     * @property theme [Theme] The selected application theme (Light, Dark, or System default), represented by [Theme].
     * @property isDynamicColorsEnabled [Boolean] A boolean flag indicating whether dynamic theming (Material You) is enabled.
     *
     * @author AbdElMoniem ElHifnawy
     */
    data class SettingsData(val locale: LocaleInfo, val theme: Theme, val isDynamicColorsEnabled: Boolean)

    /**
     * Represents the available application themes.
     * Each theme is associated with a unique integer [code] for persistent storage.
     *
     * @param code [Int] The integer code used to store the theme preference in DataStore.
     *
     * @property LIGHT [Theme] The light theme, associated with code 0.
     * @property SYSTEM [Theme] The system default theme, associated with code 1.
     * @property DARK [Theme] The dark theme, associated with code 2.
     *
     * @author AbdElMoniem ElHifnawy
     */
    enum class Theme(val code: Int) {

        /**
         * The light theme, associated with code 0.
         */
        LIGHT(code = 0),

        /**
         * The system default theme, associated with code 1.
         */
        SYSTEM(code = 1),

        /**
         * The dark theme, associated with code 2.
         */
        DARK(code = 2);

        /**
         * Provides a utility method for the [Theme] enum.
         *
         * @author AbdElMoniem ElHifnawy
         */
        companion object {

            /**
             * Retrieves a [Theme] enum constant from its integer [code].
             *
             * This function is used to convert the integer value stored in DataStore
             * back into its corresponding [Theme] object.
             *
             * @param code The integer code of the theme.
             * @return The matching [Theme] enum constant.
             * @throws NoSuchElementException if no theme with the given code is found.
             */
            fun fromCode(code: Int) = entries.first { it.code == code }
        }
    }

    /**
     * A sealed class that defines the keys for storing settings in DataStore.
     *
     * This class ensures type safety and provides a centralized definition for all preference keys.
     * Each key is associated with a specific setting and its corresponding data type.
     * The key's string name is automatically generated from the simple name of the data object
     * (e.g., [Locale], [Theme]), ensuring uniqueness and maintainability.
     *
     * @receiver T The data type of the preference value.
     * @param key [Preferences.Key] The actual key object used by DataStore.
     *
     * @property Locale The key for the user's selected locale, stored as a JSON string.
     * @property Theme The key for the application theme, stored as an integer code.
     * @property DynamicColors The key for enabling/disabling dynamic colors, stored as a boolean.
     *
     * @author AbdElMoniem ElHifnawy
     */
    private sealed class SettingsKey<T>(val key: Preferences.Key<T>) {

        /**
         * A companion object for utility functions related to [SettingsKey].
         * It provides helper methods to create type-safe [Preferences.Key] instances
         * based on the class simple name, ensuring consistent and maintainable key generation.
         *
         * @author AbdElMoniem ElHifnawy
         */
        private companion object {

            /**
             * Creates a [Preferences.Key] for a `String` preference.
             *
             * The key's name is derived from the simple name of the provided [KClass].
             * For example, if `MySetting::class` is passed, the key name will be `MySetting`.
             *
             * @param kClass [KClass] The [KClass] of the setting, used to generate a unique key name.
             *
             * @return [Preferences.Key] A [Preferences.Key] for storing a `String` value in DataStore.
             */
            fun stringKey(kClass: KClass<*>) = stringPreferencesKey(kClass.java.simpleName)

            /**
             * Creates a [Preferences.Key] for an integer preference.
             *
             * The key's name is derived from the simple name of the provided [KClass].
             * For example, `intKey(Theme::class)` would create a key with the name `Theme`.
             *
             * @param kClass [KClass] The [KClass] of the object this key represents.
             *
             * @return [Preferences.Key] A [Preferences.Key] for storing an [Int] value.
             */
            fun intKey(kClass: KClass<*>) = intPreferencesKey(kClass.java.simpleName)

            /**
             * Creates a [Preferences.Key] for a [Boolean] value.
             *
             * The key's name is derived from the simple name of the provided [KClass].
             *
             * @param kClass [KClass] The [KClass] of the object this key represents.
             *
             * @return [Preferences.Key] A [Preferences.Key] for storing a [Boolean].
             */
            fun boolKey(kClass: KClass<*>) = booleanPreferencesKey(kClass.java.simpleName)
        }

        /**
         * The key for storing the user's selected locale information.
         *
         * This key is used to save and retrieve the [LocaleInfo] object, which is serialized
         * into a JSON string for persistence in DataStore.
         *
         * @author AbdElMoniem ElHifnawy
         */
        data object Locale : SettingsKey<String>(stringKey(Locale::class))

        /**
         * The key for storing the application theme preference.
         * The value is stored as an [Int] code, which corresponds to one of the [Theme] enum values.
         *
         * @author AbdElMoniem ElHifnawy
         */
        data object Theme : SettingsKey<Int>(intKey(Theme::class))

        /**
         * A [SettingsKey] for the dynamic colors preference.
         *
         * This key is used to store and retrieve the boolean value that determines
         * whether the application should use dynamic, wallpaper-based theming (Material You).
         * The value is stored as a [Boolean].
         *
         * @author AbdElMoniem ElHifnawy
         */
        data object DynamicColors : SettingsKey<Boolean>(boolKey(DynamicColors::class))
    }

    /**
     * A dedicated [CoroutineScope] for handling background tasks related to DataStore operations.
     * It uses the [Dispatchers.IO] dispatcher, which is optimized for disk and network I/O.
     * A [Job] is included in the scope's context to allow for structured concurrency,
     * enabling cancellation of all coroutines launched within this scope.
     *
     * @return [CoroutineScope] A [CoroutineScope] for handling background tasks related to DataStore operations
     */
    private val coroutineScope = CoroutineScope(Dispatchers.IO) + Job()

    /**
     * A delegate property to create and retrieve a single instance of [DataStore]
     * for the application's preferences.
     *
     * The [preferencesDataStore] delegate ensures that there is only one instance of DataStore
     * with a specific name in the application. The name of the DataStore file is dynamically
     * generated from the simple name of the [SettingsDataStore] singleton, converted to `snake_case`.
     *
     * @return [DataStore< Preferences >][DataStore] A [DataStore] instance for the application's preferences.
     */
    private val Context.dataStore by preferencesDataStore(name = this::class.java.simpleName.snakeCase)

    /**
     * A list of observers that subscribe to changes in the application's settings.
     *
     * This list holds references to all registered [SettingsDataObserver] instances.
     * When a setting is updated (e.g., via [setTheme], [setLocale], etc...), the [notifySettingsDataObservers]
     * function iterates through this list to inform each observer about the new [SettingsData].
     * Observers are typically added and removed using the [SettingsObserver] Composable,
     * which manages their lifecycle automatically.
     *
     * @return [MutableList<SettingsDataStore.SettingsDataObserver>][MutableList] A [MutableList] of [SettingsDataObserver]s
     *
     * @see SettingsDataObserver
     * @see notifySettingsDataObservers
     * @see SettingsObserver
     */
    private val settingsDataObservers = mutableListOf<SettingsDataObserver>()

    /**
     * A cached copy of the most recently fetched or updated application settings.
     *
     * This property serves as an in-memory cache to provide synchronous access to the current
     * settings, avoiding the need for repeated asynchronous calls to DataStore for every read.
     * It is initialized asynchronously in the `init` block and is updated whenever a setting is
     * successfully written to DataStore via the `set...` methods.
     *
     * This ensures that the application has immediate access to the latest settings data,
     * which is crucial for responsive UI updates and behavior changes.
     *
     * @return [SettingsData] The most recent settings data.
     */
    private lateinit var lastSettingsData: SettingsData

    /**
     * Asynchronously retrieves the saved [LocaleInfo] from DataStore.
     *
     * This function reads the locale setting, which is stored as a JSON string,
     * from the user's preferences. It then deserializes the JSON string back into a
     * [LocaleInfo] object.
     *
     * If no locale is found in DataStore (e.g., on the first app run), it returns the
     * application's default locale, [QuranApplication.currentLocaleInfo].
     *
     * @param context [Context] The [Context] used to access the DataStore instance.
     *
     * @return [LocaleInfo] The saved [LocaleInfo], or the default locale if none is found.
     */
    suspend fun getLocale(context: Context) = context.dataStore.data.map { preferences ->
        Gson().fromJson(preferences[SettingsKey.Locale.key], LocaleInfo::class.java)
    }.first() ?: QuranApplication.currentLocaleInfo

    /**
     * Asynchronously sets the application's locale and persists it to DataStore.
     *
     * This function takes a [LocaleInfo] object, serializes it to a JSON string,
     * and saves it under the [SettingsKey.Locale] key. After a successful write,
     * it updates the in-memory cache [lastSettingsData] and notifies all registered
     * [SettingsDataObserver] instances of the change.
     *
     * @param context [Context] The [Context] used to access the DataStore instance.
     * @param locale [LocaleInfo] The [LocaleInfo] object representing the new locale to be set.
     */
    suspend fun setLocale(context: Context, locale: LocaleInfo) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKey.Locale.key] = locale.asJsonString

            lastSettingsData = lastSettingsData.copy(locale = locale)

            notifySettingsDataObservers(lastSettingsData)
        }
    }

    /**
     * Asynchronously retrieves the user's selected application theme from DataStore.
     *
     * This function reads the integer code stored under the [SettingsKey.Theme] key and converts
     * it back into a [Theme] enum constant using [Theme.fromCode]. If no theme has been
     * previously set, it returns [Theme.SYSTEM] as the default value.
     *
     * @param context [Context] The [Context] used to access the application's DataStore instance.
     *
     * @return [Theme] The user's selected [Theme], or [Theme.SYSTEM] if not set.
     */
    suspend fun getTheme(context: Context) = context.dataStore.data.map { preferences ->
        preferences[SettingsKey.Theme.key]?.let { Theme.fromCode(it) }
    }.first() ?: SYSTEM

    /**
     * Asynchronously sets the application theme and persists it to DataStore.
     *
     * This function takes a [Theme] enum, retrieves its integer [Theme.code], and saves it
     * under the [SettingsKey.Theme] key. After a successful write, it updates the
     * in-memory cache [lastSettingsData] with the new theme and notifies all registered
     * [SettingsDataObserver] instances of the change, triggering UI updates where necessary.
     *
     * @param context [Context] The [Context] used to access the DataStore instance.
     * @param theme [Theme] The new [Theme] to be applied and saved.
     */
    suspend fun setTheme(context: Context, theme: Theme) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKey.Theme.key] = theme.code

            lastSettingsData = lastSettingsData.copy(theme = theme)

            notifySettingsDataObservers(lastSettingsData)
        }
    }

    /**
     * Asynchronously retrieves the "dynamic colors" preference from DataStore.
     *
     * This function reads the boolean value stored under the [SettingsKey.DynamicColors] key.
     * The value determines whether the application should use dynamic, wallpaper-based
     * theming (Material You).
     *
     * If no value is found in DataStore (e.g., on the first app run), it returns `true`
     * as the default, enabling dynamic colors.
     *
     * @param context [Context] The [Context] used to access the DataStore instance.
     *
     * @return [Boolean] `true` if dynamic colors are enabled, `false` otherwise. Returns `true` by default if not set.
     */
    suspend fun getDynamicColors(context: Context) = context.dataStore.data.map { preferences ->
        preferences[SettingsKey.DynamicColors.key]
    }.first() ?: true

    /**
     * Asynchronously sets the dynamic colors preference and persists it to DataStore.
     *
     * This function saves the provided boolean value under the [SettingsKey.DynamicColors] key.
     * After the value is successfully written to DataStore, it updates the in-memory cache
     * [lastSettingsData] and notifies all registered [SettingsDataObserver] instances about the
     * change, allowing the UI to react accordingly (e.g., enabling or disabling Material You theming).
     *
     * @param context [Context] The [Context] used to access the DataStore instance.
     * @param dynamicColors [Boolean] A boolean indicating whether to enable (`true`) or disable (`false`) dynamic colors.
     */
    suspend fun setDynamicColors(context: Context, dynamicColors: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKey.DynamicColors.key] = dynamicColors

            lastSettingsData = lastSettingsData.copy(isDynamicColorsEnabled = dynamicColors)

            notifySettingsDataObservers(lastSettingsData)
        }
    }

    /**
     * Notifies all registered observers about an update to the application settings.
     *
     * This function iterates through the [settingsDataObservers] list and calls the
     * [onSettingsDataUpdated][SettingsDataObserver.onSettingsDataUpdated] method on each [SettingsDataObserver], passing the new
     * [SettingsData]. This mechanism allows different parts of the application, such as UI
     * components, to react to settings changes in real-time.
     *
     * This method is called internally by the `set...` functions (e.g., [setLocale], [setTheme])
     * after the new setting has been successfully persisted to DataStore and the
     * [lastSettingsData] cache has been updated.
     *
     * @param data [SettingsData] The updated settings data to be dispatched to the observers.
     *
     * @see SettingsDataObserver
     * @see settingsDataObservers
     */
    private fun notifySettingsDataObservers(data: SettingsData) = settingsDataObservers.forEach { observer -> observer.onSettingsDataUpdated(data) }

    /**
     * A functional interface for observing changes to the application's settings.
     *
     * Implement this interface to create a component that needs to react when settings like
     * theme, locale, or dynamic colors are updated. The single abstract method,
     * [onSettingsDataUpdated], will be invoked with the new [SettingsData] whenever a change occurs.
     *
     * This interface extends [IObservable] as a marker to indicate its role in the observer pattern.
     *
     * Example Usage:
     * ```
     * val myObserver = SettingsDataObserver { newSettings ->
     *     // Update UI or logic with newSettings.theme, newSettings.locale, etc.
     * }
     * ```
     *
     * Registration and unregistration of observers are typically handled by the
     * [SettingsDataStore.SettingsObserver] Composable function, which manages the lifecycle automatically.
     *
     * @see SettingsDataStore.SettingsObserver
     * @see SettingsData
     *
     * @author AbdElMoniem ElHifnawy
     */
    fun interface SettingsDataObserver : IObservable {

        /**
         * A callback method that is invoked whenever the application's settings are updated.
         *
         * Implement this method to receive the new [SettingsData] and react to changes,
         * for example, by updating the UI or reloading content.
         *
         * @param data [SettingsData] The updated [SettingsData] object containing the new user preferences.
         */
        fun onSettingsDataUpdated(data: SettingsData)
    }

    /**
     * Initializes the [SettingsDataStore] and loads the current settings into memory.
     *
     * This function is called when the [SettingsDataStore] is first created. It loads the current
     * application settings into memory and sets up the observers for future changes.
     *
     * @see SettingsData
     */
    init {
        coroutineScope.launch {
            lastSettingsData = SettingsData(
                    locale = getLocale(QuranApplication.applicationContext),
                    theme = getTheme(QuranApplication.applicationContext),
                    isDynamicColorsEnabled = getDynamicColors(QuranApplication.applicationContext)
            )
        }
    }

    /**
     * A Composable function that registers a [SettingsDataObserver] to listen for application
     * setting changes and automatically unregisters it when the Composable leaves the composition.
     *
     * This function provides a declarative and lifecycle-aware way to subscribe to settings updates
     * within a Jetpack Compose UI. It uses a [DisposableEffect] to add the provided [observer]
     * to the central [settingsDataObservers] list when the Composable is first composed and removes
     * it [onDispose][DisposableEffectScope.onDispose], preventing memory leaks.
     *
     * When any setting is changed (e.g., via [setTheme], [setLocale], etc...), the [observer]'s
     * [onSettingsDataUpdated][SettingsDataObserver.onSettingsDataUpdated] callback will be invoked
     * with the new [SettingsData].
     *
     * **Example Usage:**
     * ```kotlin
     * @Composable
     * fun MyScreen() {
     *     var currentTheme by remember { mutableStateOf(SettingsDataStore.Theme.SYSTEM) }
     *
     *     SettingsObserver { newSettings ->
     *         // This block will run whenever settings change.
     *         currentTheme = newSettings.theme
     *     }
     *
     *     // UI that depends on currentTheme...
     * }
     * ```
     *
     * @param observer [SettingsDataObserver] A [SettingsDataObserver] lambda or object that will be notified of settings changes.
     *
     * @see SettingsDataObserver
     * @see DisposableEffect
     */
    @Composable
    fun SettingsObserver(observer: SettingsDataObserver) {
        val settingsDataStore = remember { SettingsDataStore }

        DisposableEffect(settingsDataStore) {
            settingsDataStore.settingsDataObservers.add(observer)

            onDispose { settingsDataStore.settingsDataObservers.remove(observer) }
        }
    }
}
