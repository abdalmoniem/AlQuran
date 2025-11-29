package com.hifnawy.alquran.view.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hifnawy.alquran.view.navigation.NavGraph
import com.hifnawy.alquran.view.theme.AppTheme

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
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NavGraph()
                }
            }
        }
    }
}
