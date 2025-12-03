package com.hifnawy.alquran.shared.utils

import com.hifnawy.alquran.shared.QuranApplication
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

/**
 * Provides extension functions for [Duration] that provide human-readable representations of the duration.
 *
 * @author AbdAlMoniem AlHifnawy
 */
object DurationExtensionFunctions {

    val Duration.hoursLong: Long get() = toComponents { hours, _, _, _ -> hours }
    val Duration.minutesInt: Int get() = toComponents { _, minutes, _, _ -> minutes }
    val Duration.secondsInt: Int get() = toComponents { _, _, seconds, _ -> seconds }
    val Duration.millisecondsInt: Int get() = toComponents { _, _, _, _, nanoseconds -> nanoseconds / 1_000_000 }
    val Duration.microsecondsInt: Int get() = toComponents { _, _, _, _, nanoseconds -> nanoseconds / 1_000 }
    val Duration.nanosecondsInt: Int get() = toComponents { _, _, _, _, nanoseconds -> nanoseconds }

    /**
     * Converts the duration to a formatted string suitable for display to the user.
     *
     * The format of the string is determined by the current locale. If the locale is a right-to-left locale, the format will be `HH:mm:ss`.
     * Otherwise, the format will be `HH MM SS`.
     *
     * @param hideLegend [Boolean] if `true`, the string will not include the `hour`, `minute`, or `second` labels.
     * @param showHours [Boolean] if `true`, the string will include the `hour` part of the time.
     *
     * @return [String] the formatted string.
     */
    fun Duration.toFormattedTime(hideLegend: Boolean = false, showHours: Boolean = true): String = toComponents { hours, minutes, seconds, _ ->
        val isHoursShown = showHours || hours > 0L

        when {
            isInfinite() -> "Ꝏ"

            else         -> {
                val format = when {
                    hideLegend -> if (!isHoursShown) "%02d:%02d" else "%02d:%02d:%02d"
                    else       -> if (!isHoursShown) "%02dm %02ds" else "%02dh %02dm %02ds"
                }

                when {
                    isHoursShown -> String.format(Locale.ENGLISH, format, hours, minutes, seconds)
                    else         -> String.format(Locale.ENGLISH, format, minutes, seconds)
                }
            }
        }
    }

    /**
     * Formats the duration in a localized string that is suitable for display to the user.
     *
     * The format of the string is determined by the current locale. If the locale is a right-to-left locale, the format will be `HH:mm:ss`.
     * Otherwise, the format will be `HH MM SS`.
     *
     * @param showHours [Boolean] if `true`, the string will include the `hour` part of the time.
     *
     * @return [String] the formatted string.
     */
    fun Duration.toLocalizedFormattedTime(showHours: Boolean = true): String = toComponents { hours, minutes, seconds, _ ->
        val isHoursShown = showHours || hours > 0L
        val locale = QuranApplication.currentLocale

        when {
            isInfinite() -> "∞"

            else         -> {
                val format = when {
                    !isHoursShown -> "%02d:%02d"
                    else          -> "%02d:%02d:%02d"
                }

                when {
                    isHoursShown -> String.format(locale, format, hours, minutes, seconds)
                    else         -> String.format(locale, format, minutes, seconds)
                }
            }
        }
    }

    /**
     * Returns a [String] representing the duration as a timestamp in the system's default time zone.
     *
     * This property treats the duration as the number of milliseconds since the Unix epoch (January 1, 1970, 00:00:00 UTC)
     * and converts it into a human-readable date and time string based on the user's local time zone.
     *
     * The format of the timestamp is "EEE, dd MMM yyyy, hh:mm:ss.SSS a" (e.g., "Thu, 27 Nov 2025, 03:23:30.026 PM").
     *
     * Example:
     * ```
     * // The number of milliseconds from the epoch to "Thu, 27 Nov 2025, 03:23:30.026 PM" UTC
     * val durationSinceEpoch = 1764209010026.milliseconds
     *
     * // Convert it to a human-readable timestamp in the local time zone
     * val timestamp = durationSinceEpoch.asSystemTimestamp
     *
     * // The output will be adjusted for the system's time zone.
     * // For example, in a UTC-5 time zone, the output would be "Thu, 27 Nov 2025, 10:23:30.026 AM".
     * println(timestamp)
     * ```
     *
     * @return [String] The formatted timestamp string.
     */
    val Duration.asSystemTimestamp: String
        get() {
            val resultInstant = Instant.ofEpochMilli(this.inWholeMilliseconds)
            val zonedDateTime = resultInstant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy, hh:mm:ss.SSS a", Locale.ENGLISH)

            return zonedDateTime.format(formatter)
        }
}
