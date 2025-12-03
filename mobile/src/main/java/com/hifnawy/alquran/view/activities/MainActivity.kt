package com.hifnawy.alquran.view.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hifnawy.alquran.datastore.SettingsDataStore
import com.hifnawy.alquran.datastore.SettingsDataStore.SettingsObserver
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.view.navigation.NavGraph
import com.hifnawy.alquran.view.theme.AppTheme
import timber.log.Timber

/**
 * The main activity of the app.
 *
 * @author AbdElMoniem ElHifnawy
 */
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState [Bundle] If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     *     Note: Otherwise it is null.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val settingsDataStore = remember { SettingsDataStore }

            val localeDirection = remember {
                when {
                    QuranApplication.currentLocaleInfo.isRTL -> LayoutDirection.Rtl
                    else                                     -> LayoutDirection.Ltr
                }
            }

            var layoutDirection by rememberSaveable { mutableStateOf(localeDirection) }
            var theme by rememberSaveable { mutableStateOf(SettingsDataStore.Theme.SYSTEM) }
            var dynamicColors by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                val locale = settingsDataStore.getLocale(context)

                layoutDirection = when {
                    locale.isRTL -> LayoutDirection.Rtl
                    else         -> LayoutDirection.Ltr
                }

                theme = settingsDataStore.getTheme(context)
                dynamicColors = settingsDataStore.getDynamicColors(context)
            }

            SettingsObserver { data ->
                Timber.debug("data: $data")

                layoutDirection = when {
                    data.locale.isRTL -> LayoutDirection.Rtl
                    else              -> LayoutDirection.Ltr
                }

                theme = data.theme
                dynamicColors = data.dynamicColors
            }

            AppTheme(
                    darkTheme = when (theme) {
                        SettingsDataStore.Theme.LIGHT  -> false
                        SettingsDataStore.Theme.SYSTEM -> isSystemInDarkTheme()
                        SettingsDataStore.Theme.DARK   -> true
                    },
                    dynamicColor = dynamicColors
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    NavGraph()
                }
            }
        }
    }
}
