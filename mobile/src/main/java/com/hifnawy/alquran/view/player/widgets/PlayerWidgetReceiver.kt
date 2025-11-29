package com.hifnawy.alquran.view.player.widgets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.session.Session
import androidx.glance.session.SessionManagerScope
import androidx.work.Operation
import androidx.work.WorkManager
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import com.hifnawy.alquran.utils.sampleReciters
import com.hifnawy.alquran.utils.sampleSurahs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
     * Called when the system sends a broadcast to the receiver.
     *
     * @param context [Context] The [Context] in which the receiver is running.
     * @param intent [Intent] The [Intent] that was received.
     */
    @SuppressLint("RestrictedApi")
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

        Timber.debug("onReceive called with action: ${intent.action} on appWidgetId: $appWidgetId")

        if (appWidgetId == -1) return
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> updateWidget(context = context, appWidgetId = appWidgetId)
            else                                              -> Unit
        }
    }
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
     * Called by the [AppWidgetHost] to update the AppWidget in response to a broadcast.
     * This method is also called when a new instance of the widget is added to the home screen.
     *
     * During testing I found out that when a widget is first placed on the home screen, onUpdate is called
     * with appWidgetIds containing only one element, which is the ID of the newly added widget. Glance then
     * takes over and tries to update the widget with the new state. However, it is extremely slow and takes
     * a long time to update the widget. As a result, the widget remains blank until the user restarts the app.
     *
     * I also noticed in the logcat that the widget is updated several multiple times per second which I think
     * could be the cause of the blank widget since the [AppWidgetManager] won't have time to update the widget
     * before Glance tries to update it again. I think it is a bug in the Glance library.
     *
     * In logcat when the widget is added you'll find:
     *
     * ```
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * - updateAppWidgetIds, mPackageName: com.hifnawy.alquran.debug, appWidgetIds: [633], views:android.widget.RemoteViews@7fe892d
     * ...
     * ```
     *
     * which is an indicator that the widget is being updated multiple times per second.
     *
     * To mitigate this issue, we need to call [PlayerWidget.updateGlanceWidgets] in the onUpdate method.
     * This will update the widget with the a default state and prevent it from remaining blank.
     *
     * But before we do that we have to stop the Glance library from spamming the [AppWidgetManager]
     * with update requests. To do that we need to close the session that hosts the update worker used by the Glance library
     * to update the widget.
     * Poking around the [GlanceAppWidget.deleted] method, I found:
     *
     * ```
     * val glanceId = AppWidgetId(appWidgetId)
     * getSessionManager(context).runWithLock { closeSession(glanceId.toSessionKey()) }
     *
     * // toSessionKey is defined in androidx.glance.appwidget.AppWidgetUtils which is simply
     * // defined as "appWidget-$appWidgetId"
     *
     * internal fun createUniqueRemoteUiName(appWidgetId: Int) = "appWidget-$appWidgetId"
     * internal fun AppWidgetId.toSessionKey() = createUniqueRemoteUiName(appWidgetId)
     * ```
     * so we use that key and pass it to [SessionManagerScope.closeSession] to terminate the session.
     * and then start our custom update function [PlayerWidget.updateGlanceWidgets].
     *
     * @param context [Context] The [Context] in which this receiver is running.
     * @param appWidgetManager [AppWidgetManager] A manager for updating AppWidget views.
     * @param appWidgetIds [IntArray] The IDs of the app widgets that need to be updated.
     */
    @OptIn(ExperimentalGlanceApi::class)
    @SuppressLint("RestrictedApi")
    override fun onUpdate(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Timber.debug("onUpdate called for widget IDs: ${appWidgetIds.contentToString()}")

        val coroutineScope = CoroutineScope(Dispatchers.Default)
        lateinit var observer: (Operation.State) -> Unit

        coroutineScope.launch {
            appWidgetIds.forEach { appWidgetId ->
                val sessionKey = "appWidget-$appWidgetId"
                val sessionManager = PlayerWidget().getSessionManager(context)
                var session: Session?

                do {
                    session = sessionManager.runWithLock { getSession(sessionKey) }
                    Timber.warn("Waiting for session to be created for key: $sessionKey")
                    delay(50)
                } while (session == null)

                Timber.warn("Closing session for key: $sessionKey...")
                sessionManager.runWithLock { closeSession(sessionKey) }

                Timber.warn("Canceling all WorkManager work for key: $sessionKey...")
                val operation = WorkManager.getInstance(context).cancelUniqueWork(sessionKey)

                observer = { state ->
                    Timber.warn("WorkManager state: $state")

                    if (state is Operation.State.SUCCESS) {
                        Timber.warn("WorkManager work canceled successfully for key: $sessionKey!")
                        operation.state.removeObserver(observer)

                        updateWidget(context = context, appWidgetId = appWidgetId)
                    }
                }

                withContext(Dispatchers.Main) { operation.state.observeForever(observer) }
            }
        }
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

    /**
     * Updates a specific widget instance with either the last known state or a default sample state.
     *
     * This function fetches the current state of the widget. If the widget has a previously saved
     * status, it forces an update using that status. If the widget has no saved status (e.g., it's a
     * new instance or the data was cleared), it updates the widget with a random sample state to
     * provide a default view.
     *
     * The update process is executed within a coroutine on the [Dispatchers.Default] dispatcher.
     *
     * @param context [Context] The [Context] used to access widget state and perform updates.
     * @param appWidgetId [Int] The ID of the specific app widget instance to update.
     */
    @SuppressLint("RestrictedApi")
    private fun updateWidget(context: Context, appWidgetId: Int) = CoroutineScope(Dispatchers.Default).launch {
        Timber.warn("Updating glance widget #$appWidgetId...")
        val reciter = sampleReciters.random()
        val moshaf = reciter.moshafList.first()
        val surah = sampleSurahs.random()
        val status = ServiceStatus.Paused(
                reciter = sampleReciters.random(),
                moshaf = moshaf,
                surah = surah,
                durationMs = 0,
                currentPositionMs = 0,
                bufferedPositionMs = 0
        )

        // TODO: Load the last status from the data store
        val widgetState = PlayerWidget().getAppWidgetState<PlayerWidgetState>(context = context, glanceId = AppWidgetId(appWidgetId))
        when {
            widgetState.status == null -> PlayerWidget.updateGlanceWidgets(context = context, status = status)

            else                       -> PlayerWidget.updateGlanceWidgets(context = context, status = widgetState.status, forceUpdate = true)
        }
    }
}
