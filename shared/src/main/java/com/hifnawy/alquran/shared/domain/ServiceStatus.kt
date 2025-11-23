package com.hifnawy.alquran.shared.domain

import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursLong
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toFormattedTime
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the various states of the media playback service.
 *
 * This sealed class is used to communicate the current status of the media player,
 * such as whether it's playing, paused, stopped, or has ended playback.
 * The [MediaInfo] subclasses also carry detailed information about the currently playing media.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see MediaInfo
 * @see Playing
 * @see Paused
 * @see Ended
 * @see Stopped
 * @see QuranMediaService
 */
sealed class ServiceStatus {

    /**
     * A sealed class that represents detailed information about the media being played.
     * This class serves as a base for states that carry media-specific data,
     * such as [Playing] and [Paused]. It holds references to the [reciter], [moshaf], and [surah],
     * as well as timing information like the total [durationMs], [currentPositionMs], and [bufferedPositionMs].
     *
     * It overrides [toString] to provide a human-readable summary of the media state.
     *
     * @author AbdElMoniem ElHifnawy
     * @see IMediaInfo
     */
    sealed class MediaInfo : ServiceStatus(), IMediaInfo {

        /**
         * Provides a string representation of the [MediaInfo] object.
         *
         * This function is useful for logging and debugging purposes. It formats the output to be human-readable,
         * including the class name (e.g., Playing, Paused), and details about the media such as the reciter, moshaf,
         * and surah, along with their respective IDs and names. It also includes the current playback time,
         * buffered position, and total duration, formatted as `HH:MM:SS` or `MM:SS`.
         *
         * Example output:
         * ```kotlin
         * Playing(reciter=(1, AbdulBaset AbdulSamad), moshaf=(1, Al-Musshaf Al-Murattal), surah=(1, Al-Fatihah), time: 00:15 (00:35) / 01:05)
         *```
         * @return [String] A formatted string detailing the current state and media information.
         */
        final override fun toString(): String {
            val showHours = durationMs.milliseconds.hoursLong > 0

            return "${this::class.simpleName}(" +
                   "reciter=(${reciter.id.value}, ${reciter.name}), " +
                   "moshaf=(${moshaf.id}, ${moshaf.name}), " +
                   "surah=(${surah.id}, ${surah.name}), " +
                   "time: ${currentPositionMs.milliseconds.toFormattedTime(showHours = showHours)} " +
                   "(${bufferedPositionMs.milliseconds.toFormattedTime(showHours = showHours)}) / " +
                   "${durationMs.milliseconds.toFormattedTime(showHours = showHours)})"
        }
    }

    /**
     * Represents the state where media is actively playing.
     *
     * This data class is a specific implementation of [MediaInfo] and indicates that the
     * media player is currently playing audio. It holds all the necessary details about the
     * currently playing track, including the [reciter], [moshaf], and [surah], as well as
     * the total [durationMs], the [currentPositionMs] of playback, and the [bufferedPositionMs].
     *
     * @param reciter [Reciter] The [Reciter] of the currently playing surah.
     * @param moshaf [Moshaf] The [Moshaf] (recitation style) of the currently playing surah.
     * @param surah [Surah] The [Surah] that is currently playing.
     * @param durationMs [Long] The total duration of the media in milliseconds.
     * @param currentPositionMs [Long] The current playback position in milliseconds.
     * @param bufferedPositionMs [Long] The position up to which the media has been buffered, in milliseconds.
     *
     * @see ServiceStatus
     * @see MediaInfo
     * @see Paused
     * @see Ended
     * @see Stopped
     */
    data class Playing(
            override val reciter: Reciter,
            override val moshaf: Moshaf,
            override val surah: Surah,
            override val durationMs: Long,
            override val currentPositionMs: Long,
            override val bufferedPositionMs: Long
    ) : MediaInfo()

    /**
     * Represents the state where media playback is paused.
     *
     * This data class holds all the necessary information about the paused media,
     * allowing the UI to display the current state and for playback to be resumed
     * from the exact same position. It inherits from [MediaInfo] to provide
     * detailed information about the media item.
     *
     * @param reciter [Reciter] The [Reciter] of the currently playing surah.
     * @param moshaf [Moshaf] The [Moshaf] (recitation style) of the currently playing surah.
     * @param surah [Surah] The [Surah] that is currently playing.
     * @param durationMs [Long] The total duration of the media in milliseconds.
     * @param currentPositionMs [Long] The current playback position in milliseconds.
     * @param bufferedPositionMs [Long] The position up to which the media has been buffered, in milliseconds.
     *
     * @see ServiceStatus
     * @see MediaInfo
     * @see Playing
     * @see Ended
     * @see Stopped
     */
    data class Paused(
            override val reciter: Reciter,
            override val moshaf: Moshaf,
            override val surah: Surah,
            override val durationMs: Long,
            override val currentPositionMs: Long,
            override val bufferedPositionMs: Long
    ) : MediaInfo()

    /**
     * Represents the state where the media playback has finished.
     *
     * This state is triggered when the current media item has played to completion.
     * It signifies that the playback service is idle and ready for a new command,
     * such as playing the next track in a playlist or stopping the service.
     *
     * @see ServiceStatus
     * @see Playing
     * @see Paused
     * @see Stopped
     */
    data object Ended : ServiceStatus()

    /**
     * Represents the state where media playback has been explicitly stopped.
     *
     * This state indicates that the media player is idle and no media is loaded or ready to play.
     * This typically occurs when the user manually stops playback or when the service is
     * initialized. It is distinct from [Paused], where playback can be resumed, and [Ended],
     * which signifies the completion of a track.
     *
     * @see ServiceStatus
     * @see Playing
     * @see Paused
     * @see Ended
     */
    data object Stopped : ServiceStatus()
}

/**
 * Defines a contract for objects that hold detailed information about a media item being played.
 *
 * This interface is implemented by states that carry media-specific data, such as [ServiceStatus.Playing] and [ServiceStatus.Paused].
 * It ensures that any class representing an active media session provides access to the essential details of the playback,
 * including the [reciter], [moshaf], and [surah], as well as timing information like the total [durationMs],
 * [currentPositionMs], and [bufferedPositionMs].
 *
 * @param reciter [Reciter] The [Reciter] of the currently playing surah.
 * @param moshaf [Moshaf] The [Moshaf] (recitation style) of the currently playing surah.
 * @param surah [Surah] The [Surah] that is currently playing.
 * @param durationMs [Long] The total duration of the media in milliseconds.
 * @param currentPositionMs [Long] The current playback position in milliseconds.
 * @param bufferedPositionMs [Long] The position up to which the media has been buffered, in milliseconds.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see ServiceStatus.MediaInfo
 * @see ServiceStatus.Playing
 * @see ServiceStatus.Paused
 * @see ServiceStatus.Ended
 * @see ServiceStatus.Stopped
 */
interface IMediaInfo {

    val reciter: Reciter
    val moshaf: Moshaf
    val surah: Surah
    val durationMs: Long
    val currentPositionMs: Long
    val bufferedPositionMs: Long
}

/**
 * A functional interface for observing updates to the [ServiceStatus].
 *
 * This interface defines a single method, [onServiceStatusUpdated], which is called
 * whenever the state of the media playback service changes. Implementations of this
 * interface can be used to react to these changes, for example, by updating the UI
 * to reflect the current playback state (e.g., Playing, Paused, Stopped).
 *
 * Being a `fun interface`, it can be implemented using a lambda expression for concise syntax.
 *
 * Example Usage:
 * ```kotlin
 * val myObserver = ServiceStatusObserver { status ->
 *     when (status) {
 *         is ServiceStatus.Playing -> updateUiForPlaying(status)
 *         is ServiceStatus.Paused -> updateUiForPaused(status)
 *         ServiceStatus.Stopped -> updateUiForStopped()
 *         ServiceStatus.Ended -> updateUiForEnded()
 *     }
 * }
 * ```
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see ServiceStatus
 * @see IObservable
 */
fun interface ServiceStatusObserver : IObservable {

    /**
     * Callback function that is invoked whenever the status of the media playback service changes.
     *
     * This function receives a [ServiceStatus] object, which represents the new state of the service.
     * Implementations should handle the different possible statuses (e.g., [ServiceStatus.Playing],
     * [ServiceStatus.Paused], [ServiceStatus.Stopped], [ServiceStatus.Ended]) to update the UI
     * or perform other necessary actions accordingly.
     *
     * @param status [ServiceStatus] The new [ServiceStatus] of the media service.
     *
     * @see ServiceStatus
     * @see ServiceStatusObserver
     */
    fun onServiceStatusUpdated(status: ServiceStatus)
}
