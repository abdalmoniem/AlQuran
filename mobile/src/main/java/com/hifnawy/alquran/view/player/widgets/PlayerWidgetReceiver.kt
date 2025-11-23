package com.hifnawy.alquran.view.player.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import timber.log.Timber

/**
 * A [GlanceAppWidgetReceiver] for the [PlayerWidget].
 *
 * This class handles the lifecycle events of the app widget, such as updates, enabling,
 * and disabling. It serves as the entry point for the system to interact with the widget.
 * It's responsible for creating and managing instances of [PlayerWidget].
 *
 * @property glanceAppWidget [GlanceAppWidget] The [GlanceAppWidget] instance to be used by the receiver.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see PlayerWidget
 * @see GlanceAppWidgetReceiver
 */
class PlayerWidgetReceiver(override val glanceAppWidget: GlanceAppWidget = PlayerWidget()) : GlanceAppWidgetReceiver() {

    /**
     * Called when an instance of the AppWidget is added to the home screen for the first time.
     * This is a good place to perform one-time setup.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Timber.debug("Widget added to home screen")
    }

    /**
     * Called by the AppWidgetHost to update the AppWidget in response to a broadcast.
     * This method is also called when a new instance of the widget is added to the home screen.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     * @param appWidgetManager [AppWidgetManager] A manager for updating AppWidget views.
     * @param appWidgetIds [IntArray] The IDs of the app widgets that need to be updated.
     */
    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Timber.debug("onUpdate called for widget IDs: ${appWidgetIds.contentToString()}")
    }

    /**
     * Called when the last instance of the AppWidget is removed from the AppWidgetHost.
     * This method is the counterpart to [onEnabled] and is triggered when the user
     * removes the last widget from their home screen.
     *
     * @param context [Context] The [Context] in which this receiver is running.
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Timber.debug("Last widget instance removed from home screen")
    }
}
