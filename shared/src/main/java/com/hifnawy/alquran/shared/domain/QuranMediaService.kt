package com.hifnawy.alquran.shared.domain

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.utils.MediaConstants
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.shared.utils.ExoPlayerEx
import com.hifnawy.alquran.shared.utils.ExoPlayerEx.asString
import com.hifnawy.alquran.shared.utils.ImageUtil.drawTextOn
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.NumberExt.sp
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.getTypedSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class QuranMediaService : MediaBrowserServiceCompat() {

    enum class Actions {
        ACTION_START_PLAYBACK,
        ACTION_STOP_PLAYBACK,
        ACTION_SKIP_TO_NEXT,
        ACTION_SKIP_TO_PREVIOUS,
        ACTION_SEEK_PLAYBACK_TO,
        ACTION_RESTART_PLAYBACK,
        ACTION_TOGGLE_PLAY_PAUSE
    }

    enum class Extras {
        EXTRA_RECITER,
        EXTRA_SURAH,
        EXTRA_SEEK_POSITION
    }

    private enum class MediaSessionState(val state: Int) {
        PLAYING(PlaybackStateCompat.STATE_PLAYING),
        PAUSED(PlaybackStateCompat.STATE_PAUSED),
        SKIPPING_TO_NEXT(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT),
        SKIPPING_TO_PREVIOUS(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS),
        BUFFERING(PlaybackStateCompat.STATE_BUFFERING),
        CONNECTING(PlaybackStateCompat.STATE_CONNECTING),
        STOPPED(PlaybackStateCompat.STATE_STOPPED)
    }

    private enum class MediaState {
        ROOT,
        RECITER_BROWSE,
        SURAH_BROWSE
    }

    private companion object {

        private val NOTIFICATION_ID = Random.nextInt()
        private const val CHANNEL_ID = "Quran Playback"
    }

    private val serviceJob = Job()
    private val serviceScope by lazy { CoroutineScope(Dispatchers.Main + serviceJob) }
    private val quranApplication by lazy { application as QuranApplication }
    private val mediaSession by lazy { MediaSessionCompat(this, this::class.java.simpleName) }
    private val mediaManager by lazy { MediaManager(this) }
    private val audioAttributes by lazy { AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).setUsage(C.USAGE_MEDIA).build() }
    private val player by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(this@QuranMediaService.audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
        }
    }

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            setMediaSessionState(if (isPlayingNow) MediaSessionState.PLAYING else MediaSessionState.BUFFERING)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)

            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    Timber.debug("Player.STATE_BUFFERING")

                    setMediaSessionState(MediaSessionState.BUFFERING)
                }

                Player.STATE_IDLE      -> {
                    Timber.debug("Player.STATE_IDLE")

                    if (quranApplication.lastStatusUpdate == ServiceStatus.Stopped) return
                    if (mediaSession.controller?.playbackState?.state == PlaybackStateCompat.STATE_STOPPED) return

                    val reciter = currentReciter ?: return
                    val surah = currentSurah ?: return

                    isMediaReady = false
                    prepareMedia(reciter, surah, currentSurahPosition)
                }

                Player.STATE_READY     -> {
                    Timber.debug("Player.STATE_READY")
                    val reciter = currentReciter ?: return
                    val surah = currentSurah ?: return

                    updateMediaSession(reciter, surah)
                    listenForPlayerPosition()
                }

                Player.STATE_ENDED     -> {
                    Timber.debug("Player.STATE_ENDED")

                    setMediaSessionState(MediaSessionState.STOPPED)
                    quranApplication.lastStatusUpdate = ServiceStatus.Ended
                }
            }
        }
    }

    private var positionUpdateJob: Job? = null
    private var reciterDrawables = listOf<Bitmap>()
    private var currentReciter: Reciter? = null
    private var currentSurah: Surah? = null
    private var currentSurahPosition: Long = -1L
    private var isMediaReady = false

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        mediaSession.apply {
            this@QuranMediaService.sessionToken = sessionToken
            setCallback(MediaSessionCallback(this@QuranMediaService))
            isActive = true
        }

        mediaManager.apply {
            onMediaReady = ::updateMediaSession
        }

        player.addListener(playerListener)

        val initialNotification = buildNotification(null, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Timber.debug("service removed from task manager! stopping...")

        Intent(quranApplication, QuranMediaService::class.java).apply {
            action = Actions.ACTION_STOP_PLAYBACK.name
            startService(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mediaSession.apply {
            isActive = false
            release()
        }

        // release exoplayer
        player.apply {
            removeListener(playerListener)
            release()
        }

        // stop mediaManager
        mediaManager.apply {
            stopLifecycle()
        }

        setMediaSessionState(MediaSessionState.STOPPED)
        quranApplication.lastStatusUpdate = ServiceStatus.Stopped
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            Actions.ACTION_START_PLAYBACK.name    -> intent.apply {
                val reciter = getTypedSerializable<Reciter>(Extras.EXTRA_RECITER.name) ?: return START_NOT_STICKY
                val surah = getTypedSerializable<Surah>(Extras.EXTRA_SURAH.name) ?: return START_NOT_STICKY

                if (reciter.id == currentReciter?.id && surah.id == currentSurah?.id) return START_NOT_STICKY

                processSurah(reciter, surah)
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

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot = BrowserRoot(MediaState.ROOT.name, null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()

        when (parentId) {
            MediaState.ROOT.name -> result.sendResult(listOf(createBrowsableMediaItem(MediaState.RECITER_BROWSE.name, -1)).toMutableList())

            else                 -> {
                var mediaItems: MutableList<MediaBrowserCompat.MediaItem>
                val mediaState = when {
                    parentId == MediaState.RECITER_BROWSE.name -> MediaState.RECITER_BROWSE
                    parentId.startsWith("reciter_")            -> MediaState.SURAH_BROWSE
                    else                                       -> MediaState.RECITER_BROWSE
                }

                when (mediaState) {
                    MediaState.RECITER_BROWSE -> {
                        mediaManager.whenRecitersReady(this@QuranMediaService) { recitersResult ->
                            when (recitersResult) {
                                is Result.Success -> {
                                    val reciters = recitersResult.data
                                    reciterDrawables.ifEmpty {
                                        reciterDrawables = reciters.mapIndexed { index, reciter ->
                                            (if ((index % 2) == 0) R.drawable.reciter_background_2
                                            else R.drawable.reciter_background_3).drawTextOn(
                                                    context = this@QuranMediaService,
                                                    text = reciter.name,
                                                    // subText = if (reciter.recitationStyle != null) "(${reciter.recitationStyle.style})" else "",
                                                    fontFace = R.font.aref_ruqaa,
                                                    fontSize = 60.sp.toFloat(),
                                                    fontMargin = 0
                                            )
                                        }
                                    }

                                    mediaItems = reciters.mapIndexed { reciterIndex, reciter ->
                                        createBrowsableMediaItem(
                                                "reciter_${reciter.id}",
                                                reciterIndex,
                                                reciter.name
                                        )
                                    }.toMutableList()

                                    result.sendResult(mediaItems)
                                }

                                is Result.Error   -> when (val error = recitersResult.error) {
                                    is DataError.LocalError   -> Timber.error(error.errorMessage)
                                    is DataError.NetworkError -> Timber.error("${error.errorCode} ${error.errorMessage}")
                                    is DataError.ParseError   -> Timber.error(error.errorMessage)
                                }
                            }
                        }
                    }

                    MediaState.SURAH_BROWSE   -> {
                        val reciterID = parentId.replace("reciter_", "").toInt()
                        mediaManager.whenReady(this@QuranMediaService, reciterID) { reciters, surahs, surahsUri ->
                            reciters.find { reciter -> reciter.id == reciterID }?.let { reciter ->
                                val surahsUriMap = surahsUri.mapIndexed { index, uri ->
                                    Pair(surahs[index].id, uri)
                                }.toMap()

                                mediaItems = surahs.map { surah ->
                                    createMediaItem(
                                            mediaId = "surah_${surah.id}",
                                            reciter = reciter,
                                            surah = surah,
                                            surahUri = surahsUriMap[surah.id],
                                    )
                                }.toMutableList()

                                result.sendResult(mediaItems)
                            }
                        }
                    }

                    else                      -> return
                }
            }
        }
    }

    private fun createBrowsableMediaItem(
            mediaId: String,
            reciterIndex: Int,
            reciterName: String? = null
    ): MediaBrowserCompat.MediaItem {
        val extras = Bundle()
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        reciterName?.let { mediaDescriptionBuilder.setTitle(it) }

        if (reciterIndex in reciterDrawables.indices) mediaDescriptionBuilder.setIconBitmap(reciterDrawables[reciterIndex])

        extras.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        extras.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        extras.putInt(
                MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
        )
        mediaDescriptionBuilder.setExtras(extras)
        return MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private fun createMediaItem(
            mediaId: String,
            reciter: Reciter,
            surah: Surah,
            surahUri: Uri? = null
    ): MediaBrowserCompat.MediaItem {
        val mediaDescriptionBuilder = MediaDescriptionCompat.Builder()
        mediaDescriptionBuilder.setMediaId(mediaId)
        mediaDescriptionBuilder.setTitle(surah.name)

        surahUri?.let { mediaDescriptionBuilder.setMediaUri(it) }

        @SuppressLint("DiscouragedApi")
        val drawableId = resources.getIdentifier("surah_${surah.id.toString().padStart(3, '0')}", "drawable", packageName)

        mediaDescriptionBuilder.setIconBitmap((AppCompatResources.getDrawable(this@QuranMediaService, drawableId) as BitmapDrawable).bitmap)

        mediaDescriptionBuilder.setExtras(Bundle().apply {
            putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            )
            putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            )
            putInt(
                    MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                    MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
            )

            putSerializable(Extras.EXTRA_RECITER.name, reciter)
            putSerializable(Extras.EXTRA_SURAH.name, surah)
        })

        return MediaBrowserCompat.MediaItem(mediaDescriptionBuilder.build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    private fun createNotificationChannel() {
        val name = "Quran Player"
        val descriptionText = "Shows controls for current Quran playback"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply { description = descriptionText }
        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(reciter: Reciter?, surah: Surah?): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.quran))
            .setContentText(surah?.name)
            .setSubText(reciter?.name)
            .setSmallIcon(R.drawable.play_arrow_24px)
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0))
            .build()
    }

    private fun updateNotification(reciter: Reciter?, surah: Surah?) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(reciter, surah)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateMediaSession(
            reciter: Reciter,
            surah: Surah,
            surahUri: Uri? = null,
            surahDrawable: Drawable? = null
    ) {
        currentReciter = reciter
        currentSurah = surah

        updateNotification(reciter, surah)
        updateMetadata(surah, surahDrawable)

        if (surahUri != null) playMedia(surah, surahUri)
    }

    @SuppressLint("DiscouragedApi")
    private fun updateMetadata(surah: Surah, surahDrawable: Drawable? = null) {
        val surahDrawableId = resources.getIdentifier("surah_${currentSurah!!.id.toString().padStart(3, '0')}", "drawable", packageName)
        val surahDrawableBitmap = when (surahDrawable) {
            null -> (AppCompatResources.getDrawable(this, surahDrawableId) as BitmapDrawable).bitmap
            else -> (surahDrawable as BitmapDrawable).bitmap
        }

        val metadata = MediaMetadataCompat.Builder()
            .putText(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    when (player.playbackState) {
                        Player.STATE_IDLE,
                        Player.STATE_BUFFERING -> getString(R.string.loading_surah, surah.name)

                        else                   -> surah.name
                    }
            )
            .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, currentReciter?.name)
            .putText(MediaMetadataCompat.METADATA_KEY_GENRE, getString(R.string.quran))
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, surahDrawableBitmap)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration)
            .build()

        mediaSession.setMetadata(metadata)
    }

    private fun setMediaSessionState(mediaSessionState: MediaSessionState) {
        if (mediaSessionState == MediaSessionState.BUFFERING &&
            (mediaSession.controller?.playbackState?.state == PlaybackStateCompat.STATE_BUFFERING ||
             mediaSession.controller?.playbackState?.state == PlaybackStateCompat.STATE_STOPPED)
        ) return

        Timber.debug("mediaSessionState: ${mediaSessionState.name}")

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

        if (mediaSessionState != MediaSessionState.STOPPED) playbackStateBuilder.setBufferedPosition(player.bufferedPosition)

        playbackStateBuilder.setState(
                mediaSessionState.state,
                when (mediaSessionState) {
                    MediaSessionState.PLAYING,
                    MediaSessionState.PAUSED -> player.currentPosition

                    else                     -> 0
                },
                1f
        )

        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    private fun processSurah(reciter: Reciter, surah: Surah) {
        if (player.isPlaying) player.stop()

        mediaManager.processSurah(this@QuranMediaService, reciter, surah)
    }

    private fun playMedia(surah: Surah, surahUri: Uri) {
        Timber.debug("playing ${surah.id}: ${surah.name}")
        updateNotification(currentReciter, surah)

        if (player.isPlaying || (currentSurahPosition == 0L)) player.stop()

        player.apply {
            isMediaReady = true
            playWhenReady = true

            setMediaItem(MediaItem.fromUri(surahUri))

            prepare()

            seekTo(currentSurahPosition)


            if (!isPlaying) {
                play()

                setMediaSessionState(
                        when (player.playbackState) {
                            ExoPlayerEx.PlayerState.BUFFERING.state -> MediaSessionState.BUFFERING
                            else                                    -> MediaSessionState.PLAYING
                        }
                )

                if (player.playbackState != ExoPlayerEx.PlayerState.BUFFERING.state) {
                    val reciter = currentReciter ?: return

                    updateMediaSession(reciter, surah)
                    quranApplication.lastStatusUpdate = ServiceStatus.Playing(
                            reciter = reciter,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )
                }
            }
        }

        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        if (player.playbackState == Player.STATE_READY) {
            setMediaSessionState(MediaSessionState.PLAYING)
            quranApplication.lastStatusUpdate = ServiceStatus.Playing(
                    reciter = reciter,
                    surah = surah,
                    durationMs = player.duration,
                    currentPositionMs = player.currentPosition,
                    bufferedPositionMs = player.bufferedPosition
            )
        }
    }

    private fun toggleMedia() = when {
        player.isPlaying -> pauseMedia()
        else             -> resumeMedia()
    }

    private fun restartMedia() {
        player.stop()
        player.seekTo(0)

        setMediaSessionState(MediaSessionState.STOPPED)
        quranApplication.lastStatusUpdate = ServiceStatus.Stopped

        player.prepare()
        player.play()

        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        updateMediaSession(reciter, surah)
        setMediaSessionState(MediaSessionState.PLAYING)
        quranApplication.lastStatusUpdate = ServiceStatus.Playing(
                reciter = reciter,
                surah = surah,
                durationMs = player.duration,
                currentPositionMs = player.currentPosition,
                bufferedPositionMs = player.bufferedPosition
        )
    }

    fun prepareMedia(reciter: Reciter? = null, surah: Surah? = null, surahPosition: Long = 0L) {
        Timber.debug("preparing (${reciter?.id}: ${reciter?.name})[${surah?.id}: ${surah?.name}]")
        if ((reciter == null) || (surah == null)) return

        currentSurahPosition = surahPosition

        if ((currentReciter == null) || (currentSurah == null)) {
            Timber.debug("processing new media (${reciter.id}: ${reciter.name})[${surah.id}: ${surah.name}]")
            currentReciter = reciter
            currentSurah = surah

            processSurah(reciter, surah)
            return
        }

        if ((reciter.id != currentReciter?.id) || (surah.id != currentSurah?.id)) {
            Timber.debug("processing updated media (${reciter.id}: ${reciter.name})[${surah.id}: ${surah.name}]")
            currentReciter = reciter
            currentSurah = surah

            processSurah(reciter, surah)
            return
        }

        if (!isMediaReady) {
            Timber.debug("media not ready, processing (${reciter.id}: ${reciter.name})[${surah.id}: ${surah.name}]")
            processSurah(reciter, surah)
            return
        }
    }

    fun pauseMedia() {
        player.pause()
        setMediaSessionState(MediaSessionState.PAUSED)

        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        updateMediaSession(reciter, surah)
        setMediaSessionState(MediaSessionState.PAUSED)
        quranApplication.lastStatusUpdate = ServiceStatus.Paused(
                reciter = reciter,
                surah = surah,
                durationMs = player.duration,
                currentPositionMs = player.currentPosition,
                bufferedPositionMs = player.bufferedPosition
        )
    }

    fun resumeMedia() = when {
        isMediaReady -> {
            player.play()

            val reciter = currentReciter ?: return
            val surah = currentSurah ?: return

            updateMediaSession(reciter, surah)
            setMediaSessionState(MediaSessionState.PLAYING)
            quranApplication.lastStatusUpdate = ServiceStatus.Playing(
                    reciter = reciter,
                    surah = surah,
                    durationMs = player.duration,
                    currentPositionMs = player.currentPosition,
                    bufferedPositionMs = player.bufferedPosition
            )
        }

        else         -> prepareMedia(currentReciter, currentSurah, currentSurahPosition)
    }

    fun seekTo(position: Long) {
        Timber.debug("seekTo: $position")

        if (position != -1L) {
            player.seekTo(position)
            currentSurahPosition = position

            setMediaSessionState(
                    when {
                        player.isPlaying -> MediaSessionState.PLAYING
                        else             -> MediaSessionState.PAUSED
                    }
            )

            val reciter = currentReciter ?: return
            val surah = currentSurah ?: return

            quranApplication.lastStatusUpdate = when {
                player.isPlaying -> ServiceStatus.Playing(
                        reciter = reciter,
                        surah = surah,
                        durationMs = player.duration,
                        currentPositionMs = player.currentPosition,
                        bufferedPositionMs = player.bufferedPosition
                )

                else             -> ServiceStatus.Paused(
                        reciter = reciter,
                        surah = surah,
                        durationMs = player.duration,
                        currentPositionMs = player.currentPosition,
                        bufferedPositionMs = player.bufferedPosition
                )
            }
        }
    }

    fun skipToNextSurah() {
        player.stop()
        currentSurahPosition = -1L
        mediaManager.processNextSurah(this@QuranMediaService)
    }

    fun skipToPreviousSurah() {
        player.stop()
        currentSurahPosition = -1L
        mediaManager.processPreviousSurah(this@QuranMediaService)
    }

    fun stopMedia() {
        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        setMediaSessionState(MediaSessionState.STOPPED)
        quranApplication.lastStatusUpdate = ServiceStatus.Stopped

        if (player.isPlaying) player.stop()
        isMediaReady = false

        if (player.playbackState != Player.STATE_BUFFERING) positionUpdateJob?.cancel()

        updateMediaSession(reciter, surah)
        setMediaSessionState(MediaSessionState.STOPPED)

        Timber.debug("playback stopped")
    }

    private fun listenForPlayerPosition() {
        positionUpdateJob?.cancel()

        positionUpdateJob = serviceScope.launch {
            while (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
                Timber.debug(player.asString)

                val reciter = currentReciter ?: break
                val surah = currentSurah ?: break
                val mediaSessionState = when {
                    player.isPlaying -> MediaSessionState.PLAYING
                    else             -> when (player.playbackState) {
                        Player.STATE_BUFFERING -> MediaSessionState.BUFFERING
                        Player.STATE_READY     -> MediaSessionState.PAUSED
                        else                   -> MediaSessionState.STOPPED
                    }
                }

                setMediaSessionState(mediaSessionState)

                quranApplication.lastStatusUpdate = when (mediaSessionState) {
                    MediaSessionState.PLAYING -> ServiceStatus.Playing(
                            reciter = reciter,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )

                    MediaSessionState.PAUSED  -> ServiceStatus.Paused(
                            reciter = reciter,
                            surah = surah,
                            durationMs = player.duration,
                            currentPositionMs = player.currentPosition,
                            bufferedPositionMs = player.bufferedPosition
                    )

                    MediaSessionState.STOPPED -> ServiceStatus.Stopped

                    else                      -> quranApplication.lastStatusUpdate
                }

                delay(1_000.milliseconds)
            }
        }
    }
}
