package com.hifnawy.alquran.shared.utils

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.toFormattedTime
import kotlin.time.Duration.Companion.milliseconds

object ExoPlayerEx {

    enum class PlayerState(val state: Int) {
        BUFFERING(Player.STATE_BUFFERING),
        READY(Player.STATE_READY),
        ENDED(Player.STATE_ENDED),
        IDLE(Player.STATE_IDLE);

        companion object {

            fun fromState(value: Int) = PlayerState.entries.first { it.state == value }
        }
    }

    val ExoPlayer.asString
        get() = "ExoPlayer(" +
                "playbackState: ${PlayerState.fromState(playbackState)}, " +
                "isLoading: $isLoading, " +
                "isPlaying: $isPlaying, " +
                "durations: ${currentPosition.milliseconds.toFormattedTime()} " +
                "(${bufferedPosition.milliseconds.toFormattedTime()}) / " +
                duration.milliseconds.toFormattedTime() +
                ")"
}
