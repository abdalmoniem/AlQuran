package com.hifnawy.alquran.viewModel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.domain.QuranMediaService
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.domain.ServiceStatusObserver
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.asSystemTimestamp
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursLong
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toFormattedTime
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel responsible for managing the state and interactions of the media player.
 *
 * This class acts as a bridge between the UI (Composables) and the [QuranMediaService],
 * which handles the actual media playback. It holds the player's state in a [PlayerState] object,
 * sends commands to the service (e.g., play, pause, skip), and receives status updates from the
 * service to keep the UI in sync.
 *
 * It implements [ServiceStatusObserver] to listen for playback status changes broadcast
 * by the [QuranMediaService].
 *
 * @param application [Application] The application instance, used to access the application context and
 * start the [QuranMediaService].
 *
 * @author AbdElMoniem ElHifnawy
 */
class MediaViewModel(application: Application) : AndroidViewModel(application), ServiceStatusObserver {

    /**
     * A lazily-initialized property that provides a type-casted instance of the [Application]
     * as a [QuranApplication]. This allows convenient access to application-specific properties
     * and methods, such as the list of service observers, without repeatedly casting.
     *
     * @return [QuranApplication] A [QuranApplication] instance
     */
    val quranApplication by lazy { application as QuranApplication }

    /**
     * Represents the current state of the media player UI.
     *
     * This property holds a [PlayerState] object, which is a data class containing all the
     * necessary information to render the media player controls and display track information.
     * It is a Compose `State` object (`mutableStateOf`), so any changes to its value
     * will automatically trigger recomposition of UI components that read from it.
     *
     * This state is updated in response to both user actions (e.g., calling [playMedia], [togglePlayback])
     * and status updates received from the [QuranMediaService] via the [onServiceStatusUpdated] callback.
     *
     * @return [PlayerState] The current state of the media player UI
     */
    var playerState by mutableStateOf(PlayerState())
        private set

    /**
     * Initializes the [MediaViewModel].
     *
     * This block is called when a new instance of the [MediaViewModel] is created and is
     * responsible for registering the view model as an observer of the [QuranMediaService]
     * status updates.
     */
    init {
        if (this !in quranApplication.quranServiceObservers) quranApplication.quranServiceObservers.add(this)
    }

    /**
     * Starts media playback for a specific surah, reciter, and moshaf.
     *
     * This function initiates the playback process by sending an `Intent` to the [QuranMediaService].
     * The intent contains the `ACTION_START_PLAYBACK` action and includes the necessary playback
     * information (reciter, moshaf, surah) as extras.
     *
     * It also immediately updates the local [playerState] to reflect the new playback session,
     * making the player UI visible and showing the selected track's information. This provides
     * instant feedback to the user while the service prepares the media in the background.
     *
     * @param selectedReciter [Reciter] The [Reciter] object for the selected recitation.
     * @param selectedMoshaf [Moshaf] The [Moshaf] object associated with the reciter.
     * @param selectedMoshafServer [String] The base URL for the server hosting the audio files for the given moshaf.
     * @param selectedSurah [Surah] The specific [Surah] to be played.
     */
    fun playMedia(selectedReciter: Reciter, selectedMoshaf: Moshaf, selectedMoshafServer: String, selectedSurah: Surah): Unit =
            Intent(quranApplication, QuranMediaService::class.java).run {
                action = QuranMediaService.Actions.ACTION_START_PLAYBACK.name
                putExtra(QuranMediaService.Extras.EXTRA_RECITER.name, selectedReciter)
                putExtra(QuranMediaService.Extras.EXTRA_MOSHAF.name, selectedMoshaf)
                putExtra(QuranMediaService.Extras.EXTRA_SURAH.name, selectedSurah)

                quranApplication.startService(this)
                updateState {
                    isVisible = true
                    isBuffering = true
                    isPlaying = false
                    isExpanded = true
                    reciter = selectedReciter
                    moshaf = selectedMoshaf
                    surahsServer = selectedMoshafServer
                    surah = selectedSurah
                    surahSelectionTimeStamp = System.currentTimeMillis()
                }
            }

    /**
     * Toggles the playback state between playing and paused.
     *
     * This function sends an [Intent] with the [QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE]
     * action to the [QuranMediaService]. This signals the service to either pause the playback
     * if it's currently playing, or resume it if it's paused.
     *
     * It also optimistically updates the local [playerState] by inverting the `isPlaying` boolean.
     * This provides immediate visual feedback to the user, although the definitive state will be
     * confirmed shortly after by an update from the service via [onServiceStatusUpdated].
     */
    fun togglePlayback(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        if (playerState.isBuffering) return

        action = QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE.name
        quranApplication.startService(this)

        updateState { isPlaying = !isPlaying }
    }

    /**
     * Sends a command to the [QuranMediaService] to skip to the next surah in the playlist.
     *
     * This function creates an [Intent] with the action [QuranMediaService.Actions.ACTION_SKIP_TO_NEXT]
     * and starts the service. The service will then handle the logic of advancing to the
     * next surah and updating the playback state accordingly.
     */
    fun skipToNextSurah(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_SKIP_TO_NEXT.name

        quranApplication.startService(this)
    }

    /**
     * Sends a command to the [QuranMediaService] to skip to the previous Surah in the playlist.
     *
     * This function creates an [Intent] with the action [QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS]
     * and starts the service with it. The service will then handle the logic of determining
     * and playing the previous track.
     *
     * The UI state (e.g., the currently displayed Surah) is not updated here directly. Instead,
     * the service will broadcast the new status, which will be received by [onServiceStatusUpdated]
     * to update the [playerState] accordingly.
     */
    fun skipToPreviousSurah(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS.name

        quranApplication.startService(this)
    }

    /**
     * Seeks the current media playback to a specified position.
     *
     * This function sends an intent to the [QuranMediaService] with the [QuranMediaService.Actions.ACTION_SEEK_PLAYBACK_TO]
     * action and the target position. It also optimistically updates the local [playerState] with the new
     * position to provide immediate UI feedback. The service will then perform the actual seek operation on the media player.
     *
     * @param position [Long] The position in milliseconds to seek to.
     */
    fun seekTo(position: Long): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_SEEK_PLAYBACK_TO.name
        putExtra(QuranMediaService.Extras.EXTRA_SEEK_POSITION.name, position)

        updateState { currentPositionMs = position }
    }

    /**
     * Stops the media playback and hides the player UI.
     *
     * This function sends an [Intent] with the [QuranMediaService.Actions.ACTION_STOP_PLAYBACK]
     * action to the [QuranMediaService], instructing it to stop playback and release its resources.
     *
     * It also updates the [playerState] to reset playback-related values (durations, positions)
     * and sets `isVisible` to `false`, effectively hiding the player from the UI. The player
     * is set back to an expanded state by default for its next appearance.
     */
    fun closePlayer(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_STOP_PLAYBACK.name
        quranApplication.startService(this)

        updateState {
            durationMs = 0
            currentPositionMs = 0
            bufferedPositionMs = 0
            isVisible = false
            isPlaying = false
            isExpanded = true
        }
    }

    /**
     * Updates the [playerState] in a safe and immutable way.
     *
     * This function takes a lambda with a [PlayerStateBuilder] as its receiver. This allows for
     * concisely modifying the properties of the current player state. A new [PlayerState]
     * object is then created from the builder and assigned to [playerState].
     *
     * Using this builder pattern ensures that the `playerState` (a Compose `State` object) is
     * assigned a completely new instance, which is necessary to correctly trigger UI recomposition
     * in components that depend on it.
     *
     * Example usage:
     * ```
     * updateState {
     *     isPlaying = true
     *     isBuffering = false
     * }
     * ```
     *
     * @param block [PlayerStateBuilder.() -> Unit][block] A lambda function that receives a [PlayerStateBuilder] and defines the
     * changes to be applied to the current state.
     */
    fun updateState(block: PlayerStateBuilder.() -> Unit) = PlayerStateBuilder(playerState)
        .apply(block).build().also { playerState = it }

    /**
     * Callback method invoked when the status of the [QuranMediaService] changes.
     *
     * This function is the implementation of the [ServiceStatusObserver] interface. It receives
     * a [ServiceStatus] object from the service, which encapsulates the current state of the
     * media player (e.g., Playing, Paused, Stopped).
     *
     * Based on the received status, this function updates the [playerState] property. This update
     * triggers a recomposition of the UI, ensuring that the media player controls and information
     * displayed to the user are always in sync with the actual playback state of the service.
     * For example, it updates playback progress, the current surah, and the play/pause state.
     *
     * If the status is [ServiceStatus.Ended], it automatically triggers a call to [skipToNextSurah].
     *
     * @param status [ServiceStatus] The [ServiceStatus] object representing the latest state from the
     * [QuranMediaService].
     */
    override fun onServiceStatusUpdated(status: ServiceStatus) {
        Timber.debug("status: $status")

        when (status) {
            is ServiceStatus.Paused,
            is ServiceStatus.Playing   -> updateState {
                isBuffering = false
                isPlaying = status is ServiceStatus.Playing
                surah = status.surah
                durationMs = status.durationMs
                currentPositionMs = status.currentPositionMs
                bufferedPositionMs = status.bufferedPositionMs
            }

            is ServiceStatus.Buffering -> updateState {
                isBuffering = true
                isPlaying = false
            }

            is ServiceStatus.Stopped   -> updateState {
                isBuffering = false
                isPlaying = false
            }

            is ServiceStatus.Ended     -> skipToNextSurah()
        }

        Timber.debug("surah: ${playerState.surah}")
    }
}

/**
 * Represents the comprehensive state of the media player UI at any given moment.
 *
 * This data class encapsulates all the information needed to render the player controls,
 * display track details, and manage the player's visibility and layout. It is used within
 * the [MediaViewModel] as a Compose `State` object, ensuring that any changes to this
 * state automatically trigger UI recomposition.
 *
 * @property reciters [List< Reciter >][List] The list of available [Reciter]s.
 * @property surahs [List< Surah >][List] The list of available [Surah]s.
 * @property reciter [Reciter?][Reciter] The currently selected [Reciter], or `null` if none is selected.
 * @property moshaf [Moshaf?][Moshaf] The currently selected [Moshaf] (Quranic recitation style/version), or `null`.
 * @property surah [Surah?][Surah] The currently playing [Surah] (chapter of the Quran), or `null`.
 * @property surahsServer [String?][String] The base URL for the server hosting the audio files for the current [moshaf].
 * @property durationMs [Long] The total duration of the current track in milliseconds.
 * @property currentPositionMs [Long] The current playback position of the track in milliseconds.
 * @property bufferedPositionMs [Long] The position up to which the media has been buffered, in milliseconds.
 * @property isVisible [Boolean] Determines whether the player UI component should be visible.
 * @property isPlaying [Boolean] Indicates whether the media is currently playing `true` or paused / ended / stopped `false`.
 * @property isExpanded [Boolean] Controls the layout of the player UI, e.g., showing a minimized or fully expanded view.
 * @property isExpanding [Boolean] Indicates whether the player UI is currently expanding.
 * @property isMinimizing [Boolean] Indicates whether the player UI is currently minimizing..
 * @property surahSelectionTimeStamp [Long?][Long] The timestamp of the last surah selection.
 *
 * @author AbdElMoniem ElHifnawy
 */
data class PlayerState(
        val reciters: List<Reciter> = emptyList(),
        val surahs: List<Surah> = emptyList(),
        val reciter: Reciter? = null,
        val moshaf: Moshaf? = null,
        val surah: Surah? = null,
        val surahsServer: String? = null,
        val durationMs: Long = 0,
        val currentPositionMs: Long = 0,
        val bufferedPositionMs: Long = 0,
        val isVisible: Boolean = false,
        val isBuffering: Boolean = false,
        val isPlaying: Boolean = false,
        val isExpanded: Boolean = true,
        val isExpanding: Boolean = false,
        val isMinimizing: Boolean = false,
        val surahSelectionTimeStamp: Long? = null
) {

    private val showHours get() = durationMs.milliseconds.hoursLong > 0

    /**
     * Provides a custom string representation of the [PlayerState] object, useful for logging
     * and debugging.
     *
     * This implementation formats the key properties of the player's state into a readable
     * string, including the current reciter, moshaf, surah, playback time, and boolean flags.
     * The playback time is formatted as `HH:MM:SS` or `MM:SS` depending on the total duration.
     *
     * @return [String] A formatted string summarizing the current state of the player.
     */
    override fun toString() = "PlayerState(" +
                              "reciter=(${reciter?.id?.value}: ${reciter?.name}), " +
                              "moshaf=(${moshaf?.id}: ${moshaf?.name}), " +
                              "surah=(${surah?.id}: ${surah?.name}), " +
                              "surahsServer=$surahsServer, " +
                              "time: ${currentPositionMs.milliseconds.toFormattedTime(showHours = showHours)} " +
                              "(${bufferedPositionMs.milliseconds.toFormattedTime(showHours = showHours)}) / " +
                              "${durationMs.milliseconds.toFormattedTime(showHours = showHours)}), " +
                              "isVisible=$isVisible, " +
                              "isBuffering=$isBuffering, " +
                              "isPlaying=$isPlaying, " +
                              "isExpanded=$isExpanded, " +
                              "isExpanding=$isExpanding, " +
                              "isMinimizing=$isMinimizing, " +
                              "surahSelectionTimeStamp=${surahSelectionTimeStamp?.milliseconds?.asSystemTimestamp}" +
                              ")"
}

/**
 * A builder class for creating new instances of [PlayerState] in an immutable way.
 *
 * This class facilitates updating the [PlayerState] by providing a mutable copy of an
 * existing state. It is initialized with a `PlayerState` object, and its properties
 * can then be modified. The [build] method is used to construct a new, immutable
 * [PlayerState] object with the updated values.
 *
 * This pattern is utilized in the [MediaViewModel.updateState] function to ensure that the
 * Compose `State` is updated with a completely new object instance, which is crucial for
 * correctly triggering recomposition in the UI.
 *
 * Example usage:
 * ```
 * val newState = PlayerStateBuilder(currentState).apply {
 *     isPlaying = true
 *     currentPositionMs = 10000L
 * }.build()
 * ```
 *
 * @param current The initial [PlayerState] to build upon. All properties of the builder
 * are initialized with the values from this object.
 *
 * @property reciters Mutable list of available [Reciter]s.
 * @property surahs Mutable list of available [Surah]s.
 * @property reciter Mutable nullable reference to the current [Reciter].
 * @property moshaf Mutable nullable reference to the current [Moshaf].
 * @property surah Mutable nullable reference to the current [Surah].
 * @property surahsServer Mutable nullable string for the surahs' server URL.
 * @property durationMs Mutable total duration of the track in milliseconds.
 * @property currentPositionMs Mutable current playback position in milliseconds.
 * @property bufferedPositionMs Mutable buffered position in milliseconds.
 * @property isVisible Mutable flag for player UI visibility.
 * @property isBuffering Mutable flag indicating if the media is buffering.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see PlayerState
 */
class PlayerStateBuilder(current: PlayerState) {

    var reciters = current.reciters
    var surahs = current.surahs
    var reciter = current.reciter
    var moshaf = current.moshaf
    var surah = current.surah
    var surahsServer = current.surahsServer
    var durationMs = current.durationMs
    var currentPositionMs = current.currentPositionMs
    var bufferedPositionMs = current.bufferedPositionMs
    var isVisible = current.isVisible
    var isBuffering = current.isBuffering
    var isPlaying = current.isPlaying
    var isExpanded = current.isExpanded
    var isExpanding = current.isExpanding
    var isMinimizing = current.isMinimizing
    var surahSelectionTimeStamp = current.surahSelectionTimeStamp

    /**
     * Constructs and returns a new, immutable [PlayerState] object.
     *
     * This method is called after modifying the properties of the [PlayerStateBuilder].
     * It takes the current values of the builder's properties and uses them to create
     * a new `PlayerState` instance. This ensures that the state remains immutable, a key
     * principle for working with Compose's state management, as creating a new object
     * instance guarantees that state changes are correctly detected.
     *
     * @return [PlayerState] A new, immutable instance of [PlayerState] with the values
     * configured in the builder.
     */
    fun build() = PlayerState(
            reciters = reciters,
            surahs = surahs,
            reciter = reciter,
            moshaf = moshaf,
            surah = surah,
            surahsServer = surahsServer,
            durationMs = durationMs,
            currentPositionMs = currentPositionMs,
            bufferedPositionMs = bufferedPositionMs,
            isVisible = isVisible,
            isBuffering = isBuffering,
            isPlaying = isPlaying,
            isExpanded = isExpanded,
            isExpanding = isExpanding,
            isMinimizing = isMinimizing,
            surahSelectionTimeStamp = surahSelectionTimeStamp
    )
}
