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
import com.hifnawy.alquran.view.NavigationStack
import com.hifnawy.alquran.view.theme.AppTheme
import com.hifnawy.alquran.view.player.widgets.PlayerWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateGlanceWidgets()

        enableEdgeToEdge()

        setContent {
            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NavigationStack()
                }
            }
        }
    }

    /**
     * Update the glance widgets with the current list of notifications.
     *
     * @see [GlanceAppWidgetManager]
     */
    private fun updateGlanceWidgets() {
        // Use the lifecycleScope to run the suspend function
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MainActivity)

            // Get all active GlanceIds for your specific widget class
            val glanceIds = manager.getGlanceIds(PlayerWidget::class.java)

            // Iterate over all instances and call update()
            glanceIds.forEach { glanceId ->
                PlayerWidget().update(this@MainActivity, glanceId)
            }
        }
    }
}
