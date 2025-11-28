package com.hifnawy.alquran.view.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.hifnawy.alquran.view.NavGraph
import com.hifnawy.alquran.view.theme.AppTheme
import com.hifnawy.alquran.view.player.widgets.PlayerWidget
import kotlinx.coroutines.launch

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

        updateGlanceWidgets()

        enableEdgeToEdge()

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NavGraph()
                }
            }
        }
    }

    /**
     * Update all glance widgets.
     *
     * @see [GlanceAppWidgetManager]
     */
    private fun updateGlanceWidgets() {
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)

            val glanceIds = manager.getGlanceIds(PlayerWidget::class.java)

            glanceIds.forEach { glanceId -> PlayerWidget().update(this@MainActivity, glanceId) }
        }
    }
}
