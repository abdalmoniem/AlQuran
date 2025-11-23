package com.hifnawy.alquran

import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.view.player.widgets.PlayerWidget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Custom [QuranApplication] class for the mobile platform.
 *
 * This class extends the shared [QuranApplication] and adds mobile-specific functionality,
 * such as updating Glance App Widgets. It ensures that widget updates are performed
 * sequentially on a dedicated background thread to prevent race conditions and ensure
 * consistent state.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see QuranApplication
 * @see PlayerWidget
 */
class MobileApplication : QuranApplication() {

    /**
     * A single-threaded [CoroutineDispatcher] for updating glance widgets.
     *
     * This dispatcher ensures that widget updates are performed sequentially on a dedicated
     * background thread to prevent race conditions and ensure consistent state.
     *
     * @see CoroutineScope
     * @see CoroutineDispatcher
     * @see Executors.newSingleThreadExecutor
     * @see asCoroutineDispatcher
     */
    private val singleThreadDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    /**
     * The [CoroutineScope] responsible for updating glance widgets.
     *
     * This scope ensures that widget updates are performed sequentially on the
     * [singleThreadDispatcher] to prevent race conditions and ensure consistent state.
     *
     * @see CoroutineScope
     * @see CoroutineContext
     * @see singleThreadDispatcher
     */
    private val widgetUpdateScope = CoroutineScope(singleThreadDispatcher)

    /**
     * Called when the application is starting, before any other application objects have been created.
     *
     * This override initializes the application and performs initial setup, such as updating the
     * player widget to its default stopped state. The widget update is launched in a dedicated
     * coroutine scope to ensure it runs on a background thread.
     */
    override fun onCreate() {
        super.onCreate()

        // TODO: Load the last status from the data store
        widgetUpdateScope.launch {
            async { PlayerWidget.updateGlanceWidgets(this@MobileApplication, status = ServiceStatus.Stopped) }.await()
        }
    }

    /**
     * Notifies observers about changes in the Quran service status and updates mobile-specific components.
     *
     * This method overrides the base implementation to add functionality specific to the mobile
     * platform. In addition to notifying the base class observers, it launches a coroutine on a
     * dedicated single-threaded scope [widgetUpdateScope] to update all [PlayerWidget]
     * Glance widgets with the new [status]. This ensures that widget updates are handled
     * sequentially and off the main thread, preventing race conditions and UI freezes.
     *
     * @param status [ServiceStatus] The new [ServiceStatus] of the Quran service.
     */
    override fun notifyQuranServiceObservers(status: ServiceStatus) {
        super.notifyQuranServiceObservers(status)

        widgetUpdateScope.launch {
            Timber.debug("${System.currentTimeMillis().milliseconds}: Updating glance widgets...")
            async { PlayerWidget.updateGlanceWidgets(this@MobileApplication, status) }.await()
        }

        // TODO: Store the status in a data store for later use
    }
}
