package com.hifnawy.alquran.shared.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_RESTART_PLAYBACK
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_SEEK_PLAYBACK_TO
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_SKIP_TO_NEXT
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_START_PLAYBACK
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_STOP_PLAYBACK
import com.hifnawy.alquran.shared.domain.QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE
import com.hifnawy.alquran.shared.domain.QuranMediaService.Companion.CHANNEL_ID
import com.hifnawy.alquran.shared.domain.QuranMediaService.Companion.NOTIFICATION_ID
import com.hifnawy.alquran.shared.domain.QuranMediaService.Extras.EXTRA_MOSHAF
import com.hifnawy.alquran.shared.domain.QuranMediaService.Extras.EXTRA_RECITER
import com.hifnawy.alquran.shared.domain.QuranMediaService.Extras.EXTRA_SEEK_POSITION
import com.hifnawy.alquran.shared.domain.QuranMediaService.Extras.EXTRA_SURAH
import com.hifnawy.alquran.shared.domain.QuranMediaService.MediaSessionState.Companion.fromState
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import com.hifnawy.alquran.shared.utils.ExoPlayerEx
import com.hifnawy.alquran.shared.utils.ExoPlayerEx.asString
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.getTypedSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A foreground [android.app.Service] responsible for managing the playback of Quran recitations.
 *
 * This service handles media playback using [ExoPlayer], manages media sessions via [MediaSessionCompat]
 * for integration with system UI and other media controllers (like Android Auto, Bluetooth devices, etc.),
 * and displays a persistent notification with playback controls.
 *
 * It is designed to run in the foreground to ensure continuous playback even when the app is in the
 * background or the device is locked. The service is controlled through [Intent]s with specific [Actions].
 *
 * Key responsibilities include:
 * - Initializing and managing the [ExoPlayer] instance for audio playback.
 * - Creating and updating a [MediaSessionCompat] to report playback state and handle media commands.
 * - Displaying and updating a foreground service notification with playback controls.
 * - Handling playback lifecycle events, such as starting, pausing, stopping, skipping, and seeking.
 * - Responding to player state changes (e.g., buffering, errors, completion).
 * - Implementing a retry mechanism for playback errors.
 * - Communicating playback status updates to the rest of the application via [ServiceStatus].
 * - Interacting with [MediaManager] to fetch and process media data (Surah URLs).
 * - Inheriting from [AndroidAutoMediaBrowser] to provide support for Android Auto.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see AndroidAutoMediaBrowser
 * @see MediaManager.MediaReadyObservable
 * @see Player.Listener
 */
@Suppress("RemoveRedundantQualifierName")
class QuranMediaService : AndroidAutoMediaBrowser(),
                          MediaManager.MediaReadyObservable,
                          Player.Listener {

    /**
     * Defines the set of actions that can be sent to [QuranMediaService] via an [Intent]
     * to control media playback. These actions are used to start, stop, pause, resume,
     * and navigate through the Quran recitations.
     *
     * @property ACTION_START_PLAYBACK start the media playback
     * @property ACTION_STOP_PLAYBACK stop the media playback
     * @property ACTION_SKIP_TO_NEXT skip to the next media item
     * @property ACTION_SKIP_TO_PREVIOUS skip to the previous media item
     * @property ACTION_SEEK_PLAYBACK_TO seek to a specific position in the media
     * @property ACTION_RESTART_PLAYBACK restart the media playback
     * @property ACTION_TOGGLE_PLAY_PAUSE toggle between play and pause
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see Extras
     * @see onStartCommand
     */
    enum class Actions {

        /**
         * Start the media playback.
         */
        ACTION_START_PLAYBACK,

        /**
         * Stop the media playback.
         */
        ACTION_STOP_PLAYBACK,

        /**
         * Skip to the next media item.
         */
        ACTION_SKIP_TO_NEXT,

        /**
         * Skip to the previous media item.
         */
        ACTION_SKIP_TO_PREVIOUS,

        /**
         * Seek to a specific position in the media.
         */
        ACTION_SEEK_PLAYBACK_TO,

        /**
         * Restart the media playback.
         */
        ACTION_RESTART_PLAYBACK,

        /**
         * Toggle between play and pause.
         */
        ACTION_TOGGLE_PLAY_PAUSE
    }

    /**
     * Defines the keys for extra data passed in an [Intent] to [QuranMediaService].
     * These keys are used to retrieve specific pieces of information required for playback,
     * such as the selected reciter, moshaf, surah, or a specific seek position.
     *
     * @property EXTRA_RECITER Key for the [Reciter] object.
     * @property EXTRA_MOSHAF Key for the [Moshaf] object.
     * @property EXTRA_SURAH Key for the [Surah] object.
     * @property EXTRA_SEEK_POSITION Key for the seek position (in milliseconds) as a [Long].
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see Actions
     * @see onStartCommand
     */
    enum class Extras {

        /**
         * Key for the [Reciter] object.
         */
        EXTRA_RECITER,

        /**
         * Key for the [Moshaf] object.
         */
        EXTRA_MOSHAF,

        /**
         * Key for the [Surah] object.
         */
        EXTRA_SURAH,

        /**
         * Key for the seek position (in milliseconds) as a [Long].
         */
        EXTRA_SEEK_POSITION
    }

    /**
     * Represents the various states of the [MediaSessionCompat], mirroring the constants
     * defined in [PlaybackStateCompat]. This enum provides a type-safe way to manage
     * and identify the current state of media playback.
     *
     * Each enum constant holds a corresponding integer value from [PlaybackStateCompat]
     * to ensure compatibility with the Android media framework.
     *
     * @property state The integer constant from [PlaybackStateCompat] representing the media state.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see PlaybackStateCompat
     */
    private enum class MediaSessionState(val state: Int) {

        /**
         * Represents the state of media playback when the media is actively playing.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        PLAYING(PlaybackStateCompat.STATE_PLAYING),

        /**
         * Represents the state of media playback when the media is paused.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        PAUSED(PlaybackStateCompat.STATE_PAUSED),

        /**
         * Represents the state of media playback when the media is skipped to the next item.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        SKIPPING_TO_NEXT(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT),

        /**
         * Represents the state of media playback when the media is skipped to the previous item.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        SKIPPING_TO_PREVIOUS(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS),

        /**
         * Represents the state of media playback when the media is buffering data before playback.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        BUFFERING(PlaybackStateCompat.STATE_BUFFERING),

        /**
         * Represents the state of media playback when the service is attempting to connect to the media source.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        CONNECTING(PlaybackStateCompat.STATE_CONNECTING),

        /**
         * Represents the state of media playback when the media playback has stopped.
         *
         * @param state [Int] The integer constant from [PlaybackStateCompat] representing the media state.
         *
         * @see PlaybackStateCompat
         */
        STOPPED(PlaybackStateCompat.STATE_STOPPED);

        /**
         * Companion object for the [MediaSessionState] enum.
         *
         * @property fromState [MediaSessionState] A utility function to convert an integer state from [PlaybackStateCompat]
         *           to its corresponding [MediaSessionState] enum constant.
         *
         * @author AbdElMoniem ElHifnawy
         *
         * @see MediaSessionState
         * @see PlaybackStateCompat
         */
        companion object {

            /**
             * Retrieves the [MediaSessionState] enum constant corresponding to a given
             * [PlaybackStateCompat] integer state value.
             *
             * This is a utility function to convert the integer-based state from Android's
             * `PlaybackStateCompat` into a more readable and type-safe [MediaSessionState] enum.
             *
             * @param state [Int] The integer state value from `PlaybackStateCompat`.
             *
             *  @return [MediaSessionState] The matching [MediaSessionState] enum constant.
             *
             * @throws NoSuchElementException if no enum constant matches the provided state integer.
             */
            @Suppress("unused")
            fun fromState(state: Int) = entries.first { it.state == state }
        }
    }

    /**
     * Companion object for [QuranMediaService] to hold constants and static properties.
     *
     * This object encapsulates private constants used throughout the service, ensuring they are
     * not exposed to other parts of the application.
     *
     * @property NOTIFICATION_ID [Int] A random integer used as the unique ID for the foreground service notification.
     * @property CHANNEL_ID [String] The string identifier for the notification channel used for media playback notifications.
     *
     * @author AbdElMoniem ElHifnawy
     */
    private companion object {

        /**
         * A random integer used as the unique ID for the foreground service notification.
         */
        private val NOTIFICATION_ID = Random.nextInt()

        /**
         * The string identifier for the notification channel used for media playback notifications.
         */
        private const val CHANNEL_ID = "Quran Playback"
    }

    /**
     * A [Job] for the scope of the [QuranMediaService].
     *
     * This job is used to control the lifecycle of coroutines launched by the service.
     */
    private val serviceJob = Job()

    /**
     * A [CoroutineScope] that is scoped to the lifecycle of the [QuranMediaService].
     *
     * This coroutine scope is used to launch coroutines that need to be tied to the lifecycle of the service.
     */
    private val serviceScope by lazy { CoroutineScope(Dispatchers.Main + serviceJob) }

    /**
     * A reference to the [QuranApplication] instance.
     *
     * This property is lazily initialized to ensure that it is only accessed when needed.
     */
    private val quranApplication by lazy { application as QuranApplication }

    /**
     * A [MediaSessionCompat] instance for the [QuranMediaService].
     *
     * This media session is used to control media playback in the service.
     */
    private val mediaSession by lazy { MediaSessionCompat(this, this::class.java.simpleName) }

    /**
     * Retrieves [PlaybackStateCompat] State the current playback state of the media session.
     *
     * This property lazily retrieves the playback state from the media session's controller.
     */
    private val mediaSessionState get() = mediaSession.controller?.playbackState?.state

    /**
     * The [AudioAttributes] used for media playback in the service.
     *
     * This property lazily initializes the audio attributes for media playback.
     */
    private val audioAttributes by lazy {
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    /**
     * An [ExoPlayer] instance for the [QuranMediaService].
     *
     * This player is used to play audio media in the service.
     */
    private val player by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(this@QuranMediaService.audioAttributes, true)
            setHandleAudioBecomingNoisy(true)

            @UnstableApi
            skipSilenceEnabled = true
        }
    }

    /**
     * Returns whether the retry mechanism should be running.
     *
     * This property returns `true` if the media session state is not [MediaSessionState.STOPPED] and the player's
     * playback state is either [Player.STATE_IDLE] or [Player.STATE_BUFFERING]. Otherwise, it returns `false`.
     */
    private val shouldRetry: Boolean
        get() {
            if (mediaSessionState == MediaSessionState.STOPPED.state) return false

            return (player.playbackState == Player.STATE_IDLE) || (player.playbackState == Player.STATE_BUFFERING)
        }

    /**
     * A [Job] for the retry mechanism of the [QuranMediaService].
     *
     * This job is used to control the lifecycle of the retry mechanism.
     */
    private var retryJob: Job? = null

    /**
     * A [Job] for the position update mechanism of the [QuranMediaService].
     *
     * This job is used to control the lifecycle of the position update mechanism.
     */
    private var positionUpdateJob: Job? = null

    /**
     * The currently active [Reciter] in the [QuranMediaService].
     *
     * This property is used to keep track of the currently active reciter.
     */
    private var currentReciter: Reciter? = null

    /**
     * The currently active [Moshaf] in the [QuranMediaService].
     *
     * This property is used to keep track of the currently active moshaf.
     */
    private var currentMoshaf: Moshaf? = null

    /**
     * The currently active [Surah] in the [QuranMediaService].
     *
     * This property is used to keep track of the currently active surah.
     */
    private var currentSurah: Surah? = null

    /**
     * The current position of the active [Surah] in the [QuranMediaService].
     *
     * This property is used to keep track of the current position of the active surah.
     */
    private var currentSurahPosition: Long = -1L

    /**
     * Indicates whether the media in the [QuranMediaService] is ready for playback.
     *
     * This property is used to keep track of the readiness state of the media.
     */
    private var isMediaReady = false

    /**
     * The last [MediaSessionState] of the [QuranMediaService].
     *
     * This property is used to keep track of the last known state of the media session.
     */
    private var lastMediaSessionState: MediaSessionState? = null

    /**
     * Called by the system when the service is first created.
     *
     * This method initializes the essential components of the service:
     * - Creates a [NotificationChannel] for the playback notification on Android Oreo and above.
     * - Initializes and activates the [MediaSessionCompat] to handle media controls and state.
     * - Sets a [MediaSessionCompat.Callback] to process media button events.
     * - Registers the service as a listener for media readiness events from [MediaManager].
     * - Adds a listener to the [ExoPlayer] instance to respond to player state changes.
     * - Starts the service in the foreground with an initial notification to prevent it from being
     *   killed by the system. This ensures continuous playback even when the app is in the background.
     */
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        mediaSession.apply {
            this@QuranMediaService.sessionToken = sessionToken
            setCallback(MediaSessionCallback(this@QuranMediaService))
            isActive = true
        }

        if (this !in MediaManager.mediaReadyListeners) MediaManager.mediaReadyListeners.add(this@QuranMediaService)

        player.addListener(this@QuranMediaService)

        val initialNotification = buildNotification(null, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    /**
     * Called by the system when the user removes the app's task from the recent apps list.
     *
     * This callback is triggered when the app is swiped away from the task manager. To ensure
     * a clean shutdown and release of resources, this method sends an explicit `ACTION_STOP_PLAYBACK`
     * intent to the service itself. This triggers the `stopMedia()` method, which handles stopping
     * the player, releasing the media session, and cleaning up other resources gracefully.
     *
     * @param rootIntent [Intent?][Intent] The original intent that was used to launch the task that is being removed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.debug("service removed from task manager! stopping...")

        Intent(quranApplication, QuranMediaService::class.java).apply {
            action = Actions.ACTION_STOP_PLAYBACK.name
            startService(this)
        }
    }

    /**
     * Called by the system when the service is no longer in use.
     *
     * This method is called when the service is no longer needed and is being removed by the system.
     * It is responsible for releasing all resources held by the service, including the media session
     * and player.
     *
     * This method first stops the player, deactivates the media session, and releases the player.
     * It then removes itself as a listener from the media session and player.
     *
     * Finally, it stops the lifecycle of [MediaManager] and removes itself from the list of media ready
     * listeners.
     */
    override fun onDestroy() {
        super.onDestroy()

        mediaSession.apply {
            isActive = false
            release()
        }

        player.apply {
            removeListener(this@QuranMediaService)
            release()
        }

        MediaManager.stopLifecycle()

        if (this in MediaManager.mediaReadyListeners) MediaManager.mediaReadyListeners.remove(this@QuranMediaService)

        setMediaSessionState(MediaSessionState.STOPPED)
        quranApplication.lastStatusUpdate = ServiceStatus.Stopped
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling
     * [android.content.Context.startService], providing the [Intent] that was used to
     * [android.content.Context.startService].
     *
     * This method is invoked on the main thread of the application.
     *
     * @param intent [Intent?][Intent] The [Intent] that was used to bind to this service, as given to
     * [android.content.Context.startService].
     * @param flags [Int] Additional data about this start request. Currently takes on values defined
     * by [android.app.Service.START_FLAG_REDELIVERY] and [android.app.Service.START_FLAG_RETRY]./
     * @param startId [Int] A unique integer representing this specific request to start. Use with
     * [android.app.Service.stopSelfResult] to tell the system to stop this service.
     *
     * @return [Int] The return value indicates what semantics the system should use for the
     * [android.app.Service.startService] call.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            Actions.ACTION_START_PLAYBACK.name    -> intent.apply {
                val reciter = getTypedSerializable<Reciter>(Extras.EXTRA_RECITER.name) ?: return START_NOT_STICKY
                val moshaf = getTypedSerializable<Moshaf>(Extras.EXTRA_MOSHAF.name) ?: return START_NOT_STICKY
                val surah = getTypedSerializable<Surah>(Extras.EXTRA_SURAH.name) ?: return START_NOT_STICKY

                if ((reciter.id == currentReciter?.id) && (moshaf.id == currentMoshaf?.id) && (surah.id == currentSurah?.id)) return START_NOT_STICKY

                currentSurahPosition = -1L
                processSurah(reciter, moshaf, surah)
            }

            Actions.ACTION_STOP_PLAYBACK.name     -> stopMedia()
            Actions.ACTION_SKIP_TO_NEXT.name      -> skipToNextSurah()
            Actions.ACTION_SKIP_TO_PREVIOUS.name  -> skipToPreviousSurah()
            Actions.ACTION_SEEK_PLAYBACK_TO.name  -> seekTo(intent.getLongExtra(Extras.EXTRA_SEEK_POSITION.name, 0L))
            Actions.ACTION_RESTART_PLAYBACK.name  -> restartMedia()
            Actions.ACTION_TOGGLE_PLAY_PAUSE.name -> toggleMedia()
        }

        return START_NOT_STICKY
    }

    /**
     * Called when the player encounters an error.
     *
     * @param error [PlaybackException] The error that occurred.
     */
    override fun onPlayerError(error: PlaybackException) {
        if (mediaSessionState == MediaSessionState.STOPPED.state) return
        Timber.error("Playback Error ${error.errorCode}: ${error.message}")

        setMediaSessionState(MediaSessionState.BUFFERING)

        val reciter = currentReciter ?: return
        val moshaf = currentMoshaf ?: return
        val surah = currentSurah ?: return

        quranApplication.lastStatusUpdate = ServiceStatus.Paused(
                reciter = reciter,
                moshaf = moshaf,
                surah = surah,
                durationMs = player.duration,
                currentPositionMs = player.currentPosition,
                bufferedPositionMs = player.bufferedPosition
        )

        startRetryMechanism()
    }

    /**
     * Called when the player's playback state changes.
     *
     * @param isPlaying [Boolean] Indicates whether the player is currently playing or not.
     */
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)

        Timber.d("isPlaying: $isPlaying")
        when {
            isPlaying -> setMediaSessionState(MediaSessionState.PLAYING)
            else      -> setMediaSessionState(MediaSessionState.STOPPED)
        }
    }

    /**
     * Called when the player's playback state changes.
     *
     * @param state [Int] The new playback state of the player.
     */
    override fun onPlaybackStateChanged(state: Int) {
        super.onPlaybackStateChanged(state)

        Timber.debug("Playback State: ${ExoPlayerEx.PlayerState.fromState(state)}")

        val reciter = currentReciter ?: return
        val moshaf = currentMoshaf ?: return
        val surah = currentSurah ?: return

        when (state) {
            Player.STATE_BUFFERING -> {
                setMediaSessionState(MediaSessionState.BUFFERING)
                quranApplication.lastStatusUpdate = ServiceStatus.Paused(
                        reciter = reciter,
                        moshaf = moshaf,
                        surah = surah,
                        durationMs = player.duration,
                        currentPositionMs = player.currentPosition,
                        bufferedPositionMs = player.bufferedPosition
                )
            }

            Player.STATE_READY     -> when {
                player.isPlaying -> {
                    setMediaSessionState(MediaSessionState.PLAYING)
                    quranApplication.lastStatusUpdate = ServiceStatus.Playing(
                            reciter = reciter,
                            moshaf = moshaf,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )

                    startPlaybackMonitoring()
                }

                else             -> {
                    setMediaSessionState(MediaSessionState.PAUSED)
                    quranApplication.lastStatusUpdate = ServiceStatus.Paused(
                            reciter = reciter,
                            moshaf = moshaf,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )
                }
            }

            Player.STATE_IDLE      -> Unit

            Player.STATE_ENDED     -> {
                setMediaSessionState(MediaSessionState.STOPPED)
                quranApplication.lastStatusUpdate = ServiceStatus.Stopped

                skipToNextSurah()
            }
        }

        updateNotification(reciter, surah)
        updateMetadata(surah)
    }

    /**
     * Callback invoked when the media is ready for playback.
     *
     * @param reciter [Reciter] The reciter for the media.
     * @param moshaf [Moshaf] The moshaf (moshaf) for the media.
     * @param surah [Surah] The surah for the media.
     * @param surahDrawable [Drawable?][Drawable] The drawable for the surah.
     */
    override fun onMediaReady(reciter: Reciter, moshaf: Moshaf, surah: Surah, surahDrawable: Drawable?) {
        currentReciter = reciter
        currentMoshaf = moshaf
        currentSurah = surah

        updateNotification(reciter, surah)
        updateMetadata(surah)

        val surahUri = surah.url?.toUri() ?: return
        if (player.playbackState == Player.STATE_BUFFERING) return

        playMedia(surah, surahUri)
    }

    /**
     * Creates a notification channel for the Quran player service.
     *
     * This function is called during the service's initialization and creates a notification channel with the
     * specified name, description, and importance level. The channel is used to display notifications for the Quran
     * player service.
     *
     * @see NotificationChannel
     * @see NotificationManager
     */
    private fun createNotificationChannel() {
        val name = "Quran Player"
        val descriptionText = "Shows controls for current Quran playback"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Builds a notification for the Quran player service.
     *
     * @param reciter [Reciter?][Reciter] The reciter for the media.
     * @param surah [Surah?][Surah] The surah for the media.
     *
     * @return [Notification] The notification for the Quran player service.
     */
    private fun buildNotification(reciter: Reciter?, surah: Surah?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.quran))
            .setContentText(surah?.name)
            .setSubText(reciter?.name)
            .setSmallIcon(R.drawable.play_arrow_24px)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0))
            .build()
    }

    /**
     * Updates the notification for the Quran player service.
     *
     * @param reciter [Reciter?][Reciter] The reciter for the media.
     * @param surah [Surah?][Surah] The surah for the media.
     */
    private fun updateNotification(reciter: Reciter?, surah: Surah?) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(reciter, surah)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Updates the metadata for the Quran player service.
     *
     * @param surah [Surah] The surah for the media.
     */
    private fun updateMetadata(surah: Surah) {
        val surahDrawableBitmap = (AppCompatResources.getDrawable(this@QuranMediaService, surah.surahDrawableId) as BitmapDrawable).bitmap

        val title = when (mediaSessionState) {
            MediaSessionState.STOPPED.state -> surah.name
            else                            -> when (player.playbackState) {
                Player.STATE_IDLE,
                Player.STATE_BUFFERING -> getString(R.string.loading_surah, surah.name)

                else                   -> surah.name
            }
        }

        val duration = when (player.playbackState) {
            Player.STATE_READY -> player.duration
            else               -> 0
        }

        Timber.debug("duration: $duration")

        val metadata = MediaMetadataCompat.Builder().run {
            putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            putText(MediaMetadataCompat.METADATA_KEY_ARTIST, currentReciter?.name)
            putText(MediaMetadataCompat.METADATA_KEY_GENRE, getString(R.string.quran))
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, surahDrawableBitmap)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

            build()
        }

        mediaSession.setMetadata(metadata)
    }

    /**
     * Sets the state of the media session.
     *
     * @param mediaSessionState [MediaSessionState] The new state of the media session.
     */
    private fun setMediaSessionState(mediaSessionState: MediaSessionState) {
        val previousState = lastMediaSessionState
        lastMediaSessionState = mediaSessionState

        val playbackStateBuilder = when (mediaSessionState) {
            MediaSessionState.PLAYING              -> PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
            )

            MediaSessionState.PAUSED               -> PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_SEEK_TO
            )

            MediaSessionState.SKIPPING_TO_NEXT,
            MediaSessionState.SKIPPING_TO_PREVIOUS -> PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_STOP)

            MediaSessionState.CONNECTING,
            MediaSessionState.BUFFERING            -> PlaybackStateCompat.Builder().setActions(
                    PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )

            MediaSessionState.STOPPED              -> PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)
        }

        val playBackSpeed = when (mediaSessionState) {
            MediaSessionState.BUFFERING -> 0f
            else                        -> 1f
        }

        if (mediaSessionState != MediaSessionState.STOPPED) playbackStateBuilder.setBufferedPosition(player.bufferedPosition)
        if ((mediaSessionState == MediaSessionState.BUFFERING) && (previousState == MediaSessionState.BUFFERING)) return

        playbackStateBuilder.setState(mediaSessionState.state, player.currentPosition, playBackSpeed)

        Timber.debug("newState: ${mediaSessionState.name}")
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    /**
     * Processes the surah for playback.
     *
     * @param reciter [Reciter] The reciter for the surah.
     * @param moshaf [Moshaf] The moshaf for the surah.
     * @param surah [Surah] The surah to be processed.
     */
    private fun processSurah(reciter: Reciter, moshaf: Moshaf, surah: Surah) {
        if (player.isPlaying) player.stop()

        MediaManager.processSurah(reciter, moshaf, surah)
    }

    /**
     * Plays the media for the given [surah] from the provided [surahUri].
     *
     * @param surah [Surah] The surah to play.
     * @param surahUri [Uri] The URI of the surah to play.
     */
    private fun playMedia(surah: Surah, surahUri: Uri) {
        Timber.debug("playing ${surah.id}: ${surah.name}")

        player.apply {
            isMediaReady = true
            playWhenReady = true

            stop()

            setMediaItem(MediaItem.fromUri(surahUri))
            prepare()
            seekTo(currentSurahPosition)

            play()
        }
    }

    /**
     * Toggles the media playback.
     *
     * If the media is currently playing, it will be paused.
     * If the media is currently paused or stopped, it will be resumed.
     */
    private fun toggleMedia() = when {
        player.isPlaying -> pauseMedia()
        else             -> resumeMedia()
    }

    /**
     * Restarts the media playback from the beginning.
     */
    private fun restartMedia() = player.run {
        stop()
        seekTo(0)

        prepare()
        play()
    }

    /**
     * Prepares the media for playback.
     *
     * @param reciter [Reciter?][Reciter] The reciter for the media.
     * @param moshaf [Moshaf?][Moshaf] The moshaf for the media.
     * @param surah [Surah?][Surah] The surah for the media.
     * @param surahPosition [Long] The position in the surah for the media (default is 0L).
     */
    fun prepareMedia(reciter: Reciter?, moshaf: Moshaf?, surah: Surah?, surahPosition: Long = 0L) {
        Timber.debug("preparing (${reciter?.id}: ${reciter?.name}) - (${moshaf?.id}: ${moshaf?.name})[${surah?.id}: ${surah?.name}]")
        reciter ?: return
        moshaf ?: return
        surah ?: return

        currentSurahPosition = surahPosition

        if ((currentReciter == null) || (currentSurah == null)) {
            Timber.debug("processing new media (${reciter.id}: ${reciter.name}) - (${moshaf.id}: ${moshaf.name})[${surah.id}: ${surah.name}]")
            currentReciter = reciter
            currentSurah = surah

            processSurah(reciter, moshaf, surah)
            return
        }

        if ((reciter.id != currentReciter?.id) || (surah.id != currentSurah?.id)) {
            Timber.debug("processing updated media (${reciter.id}: ${reciter.name})[${surah.id}: ${surah.name}]")
            currentReciter = reciter
            currentSurah = surah

            processSurah(reciter, moshaf, surah)
            return
        }

        if (!isMediaReady) {
            Timber.debug("media not ready, processing (${reciter.id}: ${reciter.name})[${surah.id}: ${surah.name}]")
            processSurah(reciter, moshaf, surah)
            return
        }
    }

    /**
     * Pauses the media playback.
     */
    fun pauseMedia() = player.pause()

    /**
     * Resumes the media playback.
     */
    fun resumeMedia() = when {
        isMediaReady -> player.play()
        else         -> prepareMedia(currentReciter, currentMoshaf, currentSurah, currentSurahPosition)
    }

    /**
     * Seeks to the specified position in the media.
     *
     * @param position [Long] The position to seek to.
     */
    fun seekTo(position: Long) {
        if (position <= 0) return

        Timber.debug("seekTo: $position")

        player.seekTo(position)
        currentSurahPosition = position
    }

    /**
     * Skips to the next surah.
     *
     * Stops the current playback and processes the next surah.
     */
    fun skipToNextSurah() {
        val reciter = currentReciter ?: return
        val moshaf = currentMoshaf ?: return

        player.stop()
        currentSurahPosition = -1L

        MediaManager.processNextSurah(reciter, moshaf)
    }

    /**
     * Skips to the previous surah.
     *
     * Stops the current playback and processes the previous surah.
     */
    fun skipToPreviousSurah() {
        val reciter = currentReciter ?: return
        val moshaf = currentMoshaf ?: return

        player.stop()
        currentSurahPosition = -1L
        MediaManager.processPreviousSurah(reciter, moshaf)
    }

    /**
     * Stops the media playback.
     *
     * This function stops the playback of the current media item and resets the
     * player to its initial state.
     */
    fun stopMedia() {
        isMediaReady = false

        retryJob?.cancel()
        positionUpdateJob?.cancel()

        player.apply {
            playWhenReady = false
            stop()
            clearMediaItems()
        }

        setMediaSessionState(MediaSessionState.STOPPED)
        quranApplication.lastStatusUpdate = ServiceStatus.Stopped

        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return
        updateNotification(reciter, surah)
        updateMetadata(surah)
    }

    /**
     * Starts monitoring the playback of the media.
     *
     * This function starts a coroutine that continuously monitors the playback state of the player
     * and updates the service status accordingly.
     */
    private fun startPlaybackMonitoring() {
        positionUpdateJob?.cancel()

        positionUpdateJob = serviceScope.launch {
            while ((player.playbackState == Player.STATE_READY) || (player.playbackState == Player.STATE_BUFFERING)) {
                Timber.debug(player.asString)

                val reciter = currentReciter ?: break
                val moshaf = currentMoshaf ?: break
                val surah = currentSurah ?: break

                val mediaSessionState = when {
                    player.isPlaying -> MediaSessionState.PLAYING
                    else             -> when (player.playbackState) {
                        Player.STATE_BUFFERING -> MediaSessionState.BUFFERING
                        else                   -> MediaSessionState.PAUSED
                    }
                }

                setMediaSessionState(mediaSessionState)
                quranApplication.lastStatusUpdate = when (mediaSessionState) {
                    MediaSessionState.PLAYING -> ServiceStatus.Playing(
                            reciter = reciter,
                            moshaf = moshaf,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )

                    MediaSessionState.PAUSED  -> ServiceStatus.Paused(
                            reciter = reciter,
                            moshaf = moshaf,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )

                    else                      -> quranApplication.lastStatusUpdate
                }

                // TODO: check this, the delay amount might be too low / too high
                delay(500.milliseconds)
            }
        }
    }

    /**
     * Starts the retry mechanism to attempt recovering from playback errors.
     *
     * This function launches a coroutine that repeatedly attempts to recover playback
     * by calling [attemptRecovery] until playback is successful or the retry mechanism is
     * manually stopped by calling [retryJob].[cancel][Job.cancel].
     *
     * If the retry mechanism is already running, this function does nothing.
     */
    private fun startRetryMechanism() {
        retryJob?.cancel()

        retryJob = serviceScope.launch {
            delay(2.seconds) // debounce

            currentReciter ?: return@launch
            currentMoshaf ?: return@launch
            currentSurah ?: return@launch

            while (shouldRetry) {
                attemptRecovery()
                delay(1.seconds)
            }

            Timber.debug("Retry Cancelled.")
        }
    }

    /**
     * Attempts to recover playback from a playback error.
     *
     * This function checks the current playback state and attempts to recover playback by either preparing the
     * player or playing the player depending on the current state.
     *
     * If playback is already in the [Player.STATE_READY] state and is playing, this function cancels the retry job
     * and returns. If playback is in any other state, this function prepares the player and plays it.
     */
    private fun attemptRecovery() {
        if ((player.playbackState == Player.STATE_READY) && (player.isPlaying)) {
            Timber.debug("Playback Recovered!")
            retryJob?.cancel()
            return
        }

        Timber.debug("Retrying Playback...")
        player.prepare()
        player.play()
    }
}
