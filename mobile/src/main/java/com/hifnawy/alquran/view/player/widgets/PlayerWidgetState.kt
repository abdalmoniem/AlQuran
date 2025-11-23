package com.hifnawy.alquran.view.player.widgets

import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import java.io.Serializable

/**
 * Represents the state of the media player widget.
 *
 * This data class holds all the necessary information to render the player widget UI,
 * including the current reciter, moshaf, surah, and the player's status.
 * It is designed to be serializable to allow it to be passed between components, such as in intents.
 *
 * The `equals` and `hashCode` methods are overridden to provide custom logic for determining state changes.
 * This is crucial for optimizing widget updates. For example, two states are considered equal if the player
 * status is `Stopped` or `Ended`, regardless of the media info, preventing unnecessary UI redraws
 * when the player is not active. It also compares media information (reciter, moshaf, surah) only
 * when the status is `MediaInfo` to ensure the widget updates when the playing track changes.
 *
 * @property reciter [Reciter?][Reciter] The currently selected reciter, or null if none is selected.
 * @property moshaf [Moshaf?][Moshaf] The currently selected moshaf, or null if none is selected.
 * @property surah [Surah?][Surah] The currently playing or selected surah, or null if none is selected.
 * @property status [ServiceStatus?][ServiceStatus] The current status of the media player service, represented by a [ServiceStatus] subclass.
 *
 * @author AbdElMoniem ElHifnawy
 */
data class PlayerWidgetState(
        val reciter: Reciter? = null,
        val moshaf: Moshaf? = null,
        val surah: Surah? = null,
        val status: ServiceStatus? = null
) : Serializable {

    /**
     * Compares this [PlayerWidgetState] to the specified object for equality.
     *
     * This implementation considers two states equal for the purpose of updating a widget view
     * if the changes between them are not visually relevant to the widget.
     *
     * The comparison logic is as follows:
     * 1.  Standard reference and type checks are performed first.
     * 2.  It checks if the `status` types are different. For example, a change from `Playing` to `Paused`
     *     is considered a significant difference.
     * 3.  If the current `status` is either [ServiceStatus.Stopped] or [ServiceStatus.Ended], the states are
     *     considered equal, as these states typically result in a common "stopped" or "idle" widget view,
     *     regardless of other properties.
     * 4.  If both states have a `status` of [ServiceStatus.MediaInfo], it performs a deep comparison
     *     of `reciter`, `moshaf`, and `surah` to detect changes in the currently playing media.
     * 5.  For all other status changes (e.g., progress updates within `Playing`), the states are considered
     *     equal to prevent unnecessary widget re-renders for minor updates.
     *
     * @param other [Any?][Any] The object to compare with this instance.
     * @return [Boolean] `true` if the states are considered equal for widget update purposes, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerWidgetState) return false

        if (status?.javaClass != other.status?.javaClass) return false

        if (status is ServiceStatus.Stopped || status is ServiceStatus.Ended) return true

        if (status is ServiceStatus.MediaInfo && other.status is ServiceStatus.MediaInfo) {
            if (reciter != other.reciter || moshaf != other.moshaf || surah != other.surah) return false
        }

        // If we reach here, no widget-relevant difference was found, so states are considered EQUAL.
        return true
    }

    /**
     * Generates a hash code for this [PlayerWidgetState].
     *
     * This implementation is consistent with the custom [equals] method. It calculates the hash code
     * based on the `reciter`, `moshaf`, `surah`, and the *class type* of the `status`, not the `status`
     * object itself. This ensures that two states considered equal by the `equals` method will
     * produce the same hash code.
     *
     * For example, two states with `ServiceStatus.Playing` will have the same status-related hash component,
     * even if the progress within `Playing` is different, mirroring the logic in `equals`.
     *
     * @return [Int] An integer hash code value for this object.
     */
    override fun hashCode(): Int {
        val reciterHashCode = reciter?.hashCode() ?: 0
        val moshafHashCode = moshaf?.hashCode() ?: 0
        val surahHashCode = surah?.hashCode() ?: 0
        val statusHashCode = status?.javaClass?.hashCode() ?: 0

        var result = reciterHashCode

        // Combine the hashes.
        // The multiplication by 31 (an odd prime) minimizes collisions.
        result = 31 * result + moshafHashCode
        result = 31 * result + surahHashCode
        result = 31 * result + statusHashCode

        return result
    }
}
