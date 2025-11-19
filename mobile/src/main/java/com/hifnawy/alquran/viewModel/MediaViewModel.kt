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
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import timber.log.Timber

class MediaViewModel(application: Application) : AndroidViewModel(application), ServiceStatusObserver {

    val quranApplication by lazy { application as QuranApplication }
    var playerState by mutableStateOf(PlayerState())

    init {
        if (this !in quranApplication.quranServiceObservers) quranApplication.quranServiceObservers.add(this)
    }

    fun playMedia(surah: Surah): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_START_PLAYBACK.name
        putExtra(QuranMediaService.Extras.EXTRA_RECITER.name, playerState.reciter)
        putExtra(QuranMediaService.Extras.EXTRA_MOSHAF.name, playerState.moshaf)
        putExtra(QuranMediaService.Extras.EXTRA_SURAH.name, surah)

        quranApplication.startService(this)
        playerState = playerState.copy(
                isVisible = true,
                isPlaying = true,
                isExpanded = true,
                surah = surah
        )
    }

    fun togglePlayback(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_TOGGLE_PLAY_PAUSE.name
        quranApplication.startService(this)

        playerState = playerState.copy(isPlaying = !playerState.isPlaying)
    }

    fun skipToNextSurah(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_SKIP_TO_NEXT.name

        quranApplication.startService(this)
    }

    fun skipToPreviousSurah(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_SKIP_TO_PREVIOUS.name

        quranApplication.startService(this)
    }

    fun seekTo(position: Long): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_SEEK_PLAYBACK_TO.name
        putExtra(QuranMediaService.Extras.EXTRA_SEEK_POSITION.name, position)

        playerState = playerState.copy(currentPositionMs = position)
        quranApplication.startService(this)
    }

    fun closePlayer(): Unit = Intent(quranApplication, QuranMediaService::class.java).run {
        action = QuranMediaService.Actions.ACTION_STOP_PLAYBACK.name
        quranApplication.startService(this)

        playerState = playerState.copy(
                durationMs = 0,
                currentPositionMs = 0,
                bufferedPositionMs = 0,
                isVisible = false,
                isPlaying = false,
                isExpanded = true
        )
    }

    override fun onServiceStatusUpdated(status: ServiceStatus) {
        Timber.debug("status: $status")

        when (status) {
            is ServiceStatus.Paused,
            is ServiceStatus.Playing -> playerState = playerState.copy(
                    isPlaying = status is ServiceStatus.Playing,
                    surah = status.surah,
                    durationMs = status.durationMs,
                    currentPositionMs = status.currentPositionMs,
                    bufferedPositionMs = status.bufferedPositionMs
            )

            is ServiceStatus.Stopped -> playerState = playerState.copy(isPlaying = false)
            is ServiceStatus.Ended   -> skipToNextSurah()
        }

        Timber.debug("surah: ${playerState.surah}")
    }
}

data class PlayerState(
        val reciter: Reciter? = null,
        val moshaf: Moshaf? = null,
        val surah: Surah? = null,
        val surahsServer: String? = null,
        val durationMs: Long = 0,
        val currentPositionMs: Long = 0,
        val bufferedPositionMs: Long = 0,
        val isVisible: Boolean = false,
        val isPlaying: Boolean = false,
        val isExpanded: Boolean = true
)
