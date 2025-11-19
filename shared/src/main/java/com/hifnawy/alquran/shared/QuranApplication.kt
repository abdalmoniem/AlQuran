package com.hifnawy.alquran.shared

import android.app.Application
import android.content.Context
import com.hifnawy.alquran.shared.domain.IObserver
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.domain.ServiceStatusObserver
import com.hifnawy.alquran.shared.utils.LogDebugTree
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import timber.log.Timber

class QuranApplication : Application() {

    var quranServiceObservers = mutableListOf<ServiceStatusObserver>()

    var lastStatusUpdate: ServiceStatus = ServiceStatus.Stopped
        set(status) {
            field = status

            notifyKeepAwakeServiceObservers(status)
        }

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

    private fun notifyKeepAwakeServiceObservers(status: ServiceStatus) {

        Timber.debug("notifying observers with status $status...")

        quranServiceObservers.forEach { observer ->
            val iObserverClassName = IObserver::class.simpleName
            val observerClassName = observer::class.simpleName
            val observerClassHashCode = observer.hashCode().toString(16).uppercase()

            Timber.debug("notifying $iObserverClassName<$observerClassName@$observerClassHashCode>...")

            observer.onServiceStatusUpdated(status)

            Timber.debug("$iObserverClassName<$observerClassName@$observerClassHashCode> notified!")
        }

        Timber.debug("observers notified!")
    }

    companion object {

        lateinit var applicationContext: Context
            private set
    }
}
