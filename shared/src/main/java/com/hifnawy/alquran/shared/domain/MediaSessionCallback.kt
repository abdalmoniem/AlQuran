package com.hifnawy.alquran.shared.domain

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.getTypedSerializable
import timber.log.Timber

class MediaSessionCallback(private val mediaService: QuranMediaService) : MediaSessionCompat.Callback() {

    override fun onPlay() {
        Timber.debug("Playing...")

        mediaService.resumeMedia()
    }

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        extras?.apply {
            val reciter = getTypedSerializable<Reciter>(QuranMediaService.Extras.EXTRA_RECITER.name)
            val moshaf = getTypedSerializable<Moshaf>(QuranMediaService.Extras.EXTRA_MOSHAF.name)
            val surah = getTypedSerializable<Surah>(QuranMediaService.Extras.EXTRA_SURAH.name)

            Timber.debug("Playing ${reciter?.name} - ${moshaf?.name} / ${surah?.name}...")
            mediaService.prepareMedia(reciter, moshaf, surah)
        }
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        Timber.debug("Playing $query from search...")
    }

    override fun onPause() {
        Timber.debug("Pausing...")

        mediaService.pauseMedia()
    }

    override fun onStop() {
        mediaService.stopMedia()
    }

    override fun onSkipToQueueItem(queueId: Long) {
        Timber.debug("Skipped to $queueId")
    }

    override fun onSeekTo(position: Long) {
        Timber.debug("Seeking to $position...")

        mediaService.seekTo(position)
    }

    override fun onSkipToNext() {
        Timber.debug("Skipping to next audio track...")

        mediaService.skipToNextSurah()
    }

    override fun onSkipToPrevious() {
        Timber.debug("Skipping to previous audio track...")

        mediaService.skipToPreviousSurah()
    }

    override fun onFastForward() {
        Timber.debug("Fast forwarding...")
    }

    override fun onRewind() {
        Timber.debug("Rewinding...")
    }

    override fun onCustomAction(action: String?, extras: Bundle?) {
        Timber.debug("Custom action...")
    }
}