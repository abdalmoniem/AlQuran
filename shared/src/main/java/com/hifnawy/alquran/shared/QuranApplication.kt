package com.hifnawy.alquran.shared

import android.app.Application
import android.content.Context
import com.hifnawy.alquran.shared.QuranApplication.Companion.applicationContext
import com.hifnawy.alquran.shared.domain.IObservable
import com.hifnawy.alquran.shared.domain.QuranMediaService
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.domain.ServiceStatusObserver
import com.hifnawy.alquran.shared.utils.LogDebugTree
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import timber.log.Timber

/**
 * A custom [Application] class for the Al-Quran app.
 *
 * This class serves as the central point for initializing application-wide components
 * and managing global state, such as service status observers and the application context.
 * It extends the Android [Application] class to provide a singleton instance that lives
 * for the entire duration of the app's lifecycle.
 *
 * Key Responsibilities:
 * - Initializes [Timber] for logging in debug builds.
 * - Holds a static reference to the application [Context].
 * - Manages a list of [ServiceStatusObserver]s to broadcast updates about the Quran service's state.
 * - Tracks and updates the last known [ServiceStatus] of the Quran service.
 *
 * This class is designed to be extended by specific application variants if needed.
 *
 * @author AbdElMoniem ElHifnawy
 */
open class QuranApplication : Application() {

    /**
     * A list of observers that are interested in receiving updates about the Quran recitation service's status.
     * Components (like Activities or Fragments) can register themselves here to be notified
     * when the service starts, stops, plays, pauses, etc.
     *
     * @return [MutableList] of [ServiceStatusObserver]
     *
     * @see ServiceStatusObserver
     * @see notifyQuranServiceObservers
     */
    val quranServiceObservers = mutableListOf<ServiceStatusObserver>()

    /**
     * Holds the most recent status of the [QuranMediaService].
     *
     * When this property is updated, it automatically triggers the [notifyQuranServiceObservers]
     * method to inform all registered observers about the new [ServiceStatus].
     *
     * @return [ServiceStatus]
     *
     * @see ServiceStatus.Playing
     * @see ServiceStatus.Paused
     * @see ServiceStatus.MediaInfo
     * @see ServiceStatus.Ended
     * @see ServiceStatus.Stopped
     */
    var lastStatusUpdate: ServiceStatus = ServiceStatus.Stopped
        set(status) {
            field = status

            notifyQuranServiceObservers(status)
        }

    /**
     * Called when the application is starting, before any other application objects have been
     * created.
     *
     * This implementation initializes the application context for global access and sets up
     * a custom Timber logging tree for debug builds.
     */
    override fun onCreate() {
        super.onCreate()

        QuranApplication.applicationContext = applicationContext

        if (BuildConfig.DEBUG) Timber.plant(LogDebugTree())

        // val arEg = LocaleListCompat.forLanguageTags("ar-EG")
        //
        // // Only set default if user has not chosen a locale
        // if (AppCompatDelegate.getApplicationLocales().isEmpty) {
        //     AppCompatDelegate.setApplicationLocales(arEg)
        // }
    }

    /**
     * Notifies all registered observers about a change in the Quran service's status.
     *
     * This function iterates through the `quranServiceObservers` list and calls the
     * [ServiceStatusObserver.onServiceStatusUpdated] method on each observer, passing the new status. It also logs
     * the notification process for debugging purposes.
     *
     * @param status [ServiceStatus] The new [ServiceStatus] to be sent to the observers.
     */
    open fun notifyQuranServiceObservers(status: ServiceStatus) {

        Timber.debug("notifying observers with status $status...")

        quranServiceObservers.forEach { observer ->
            val iObservableClassName = IObservable::class.simpleName
            val observerClassName = observer::class.simpleName
            val observerClassHashCode = observer.hashCode().toString(16).uppercase()

            Timber.debug("notifying $iObservableClassName<$observerClassName@$observerClassHashCode>...")

            observer.onServiceStatusUpdated(status)

            Timber.debug("$iObservableClassName<$observerClassName@$observerClassHashCode> notified!")
        }

        Timber.debug("observers notified!")
    }

    /**
     * Provides static access to application-level properties.
     *
     * This companion object holds a static reference to the application [Context],
     * making it globally accessible throughout the app without needing to pass it
     * between components. This is a common pattern for accessing resources, SharedPreferences,
     * or other context-dependent services from anywhere in the application.
     *
     * @property applicationContext [Context] The global application [Context]. It is initialized in [QuranApplication.onCreate]
     * and is read-only from outside the class.
     */
    companion object {

        /**
         * Provides a global, static reference to the application's [Context].
         *
         * This property is initialized in the [onCreate] method of the [QuranApplication] class,
         * making the application context accessible from anywhere in the app without needing to
         * pass it down through various classes. This is useful for components that need a
         * context but don't have direct access to one (e.g., utility classes, view models).
         *
         * The `private set` modifier ensures that the context can only be assigned within this
         * class, preventing accidental reassignment from other parts of the application.
         *
         * @return [Context] The application's [Context].
         */
        lateinit var applicationContext: Context
            private set
    }
}
