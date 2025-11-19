package com.hifnawy.alquran.shared.domain

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursLong
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toFormattedTime
import kotlin.time.Duration.Companion.milliseconds

sealed class ServiceStatus {
    sealed class MediaInfo : ServiceStatus(), IMediaInfo {

        final override fun toString(): String {
            val showHours = durationMs.milliseconds.hoursLong > 0

            return "${this::class.simpleName}(" +
                   "reciter=(${reciter.id}, ${reciter.name}), " +
                   "surah=(${surah.id}, ${surah.name}), " +
                   "time: ${currentPositionMs.milliseconds.toFormattedTime(showHours = showHours)} " +
                   "(${bufferedPositionMs.milliseconds.toFormattedTime(showHours = showHours)}) / " +
                   "${durationMs.milliseconds.toFormattedTime(showHours = showHours)})"
        }
    }

    data class Playing(
            override val reciter: Reciter,
            override val surah: Surah,
            override val durationMs: Long,
            override val currentPositionMs: Long,
            override val bufferedPositionMs: Long
    ) : MediaInfo()

    data class Paused(
            override val reciter: Reciter,
            override val surah: Surah,
            override val durationMs: Long,
            override val currentPositionMs: Long,
            override val bufferedPositionMs: Long
    ) : MediaInfo()

    data object Stopped : ServiceStatus()
    data object Ended : ServiceStatus()
}

interface IMediaInfo {

    val reciter: Reciter
    val surah: Surah
    val durationMs: Long
    val currentPositionMs: Long
    val bufferedPositionMs: Long
}

fun interface ServiceStatusObserver : IObserver {

    fun onServiceStatusUpdated(status: ServiceStatus)
}
