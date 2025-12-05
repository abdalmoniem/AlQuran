package com.hifnawy.alquran.shared.utils

import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.hoursComponent
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.microsecondsComponent
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.millisecondsComponent
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.minutesComponent
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.nanosecondsComponent
import com.hifnawy.alquran.shared.utils.DurationExtensionFunctions.secondsComponent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

/**
 * An object that groups together extension functions for the [Duration] class.
 *
 * These extensions provide convenient ways to format durations into human-readable strings,
 * extract time components (like `hours`, `minutes`, `seconds`, etc...), and convert `durations` to `timestamps`.
 * The functions are designed to handle different formatting requirements, such as localization
 * and the inclusion or exclusion of specific time parts.
 *
 * This object acts as a namespace, so its members are accessed on [Duration] instances
 *
 * @author AbdAlMoniem AlHifnawy
 */
object DurationExtensionFunctions {

    /**
     * The number of hours in the time component of this duration.
     *
     * This value is always in the range `0..23`.
     *
     * This is different from `inWholeHours` which returns the total number of full hours in the entire duration.
     * For example, for a duration of 37 hours, [hoursComponent] would be `13` (as in 1 day and 13 hours),
     * whereas `inWholeHours` would be `37`.
     *
     * @return [Long] the number of hours in the time component of this duration.
     *
     * @see toComponents
     * @see minutesComponent
     * @see secondsComponent
     * @see millisecondsComponent
     * @see microsecondsComponent
     * @see nanosecondsComponent
     */
    val Duration.hoursComponent get() = toComponents { hours, _, _, _ -> hours }

    /**
     * The number of minutes in the time component of this duration.
     *
     * This value is always in the range `0..59`.
     *
     * This is different from `inWholeMinutes` which returns the total number of full minutes in the entire duration.
     * For example, for a duration of 90 minutes, [minutesComponent] would be `30` (as in 1 hour and 30 minutes),
     * whereas `inWholeMinutes` would be `90`.
     *
     * @return [Int] the number of minutes in the time component of this duration.
     *
     * @see toComponents
     * @see hoursComponent
     * @see secondsComponent
     * @see millisecondsComponent
     * @see microsecondsComponent
     * @see nanosecondsComponent
     */
    val Duration.minutesComponent get() = toComponents { _, minutes, _, _ -> minutes }

    /**
     * The number of seconds in the time component of this duration.
     *
     * This value is always in the range `0..59`.
     *
     * This is different from `inWholeSeconds` which returns the total number of full seconds in the entire duration.
     * For example, for a duration of 90 seconds, [secondsComponent] would be `30` (as in 1 minute and 30 seconds),
     * whereas `inWholeSeconds` would be `90`.
     *
     * @return [Int] the number of seconds in the time component of this duration.
     *
     * @see toComponents
     * @see hoursComponent
     * @see minutesComponent
     * @see millisecondsComponent
     * @see microsecondsComponent
     * @see nanosecondsComponent
     */
    val Duration.secondsComponent get() = toComponents { _, _, seconds, _ -> seconds }


    /**
     * The number of milliseconds in the time component of this duration.
     *
     * This value is always in the range `0..999`.
     *
     * This is different from `inWholeMilliseconds` which returns the total number of full milliseconds in the entire duration.
     * For example, for a duration of `1234` milliseconds, [millisecondsComponent] would be `234` (as in 1 second and 234 milliseconds),
     * whereas `inWholeMilliseconds` would be `1234`.
     *
     * @return [Int] the number of milliseconds in the time component of this duration.
     *
     * @see toComponents
     * @see hoursComponent
     * @see minutesComponent
     * @see secondsComponent
     * @see microsecondsComponent
     * @see nanosecondsComponent
     */
    val Duration.millisecondsComponent get() = toComponents { _, _, _, nanoseconds -> nanoseconds / 1_000_000 }

    /**
     * The number of microseconds in the time component of this duration.
     *
     * This value is always in the range `0..999`.
     *
     * This is different from `inWholeMicroseconds` which returns the total number of full microseconds in the entire duration.
     *
     * @return [Int] the number of microseconds in the time component of this duration.
     *
     * @see toComponents
     * @see hoursComponent
     * @see minutesComponent
     * @see secondsComponent
     * @see millisecondsComponent
     * @see nanosecondsComponent
     */
    val Duration.microsecondsComponent get() = toComponents { _, _, _, nanoseconds -> nanoseconds / 1_000 }

    /**
     * The number of nanoseconds in the time component of this duration.
     *
     * This value is always in the range `0..999_999_999`.
     *
     * This is different from `inWholeNanoseconds` which returns the total number of nanoseconds in the entire duration.
     * For example, for a duration of 1 second and 123 nanoseconds, [nanosecondsComponent] would be `123`,
     * whereas `inWholeNanoseconds` would be `1_000_000_123`.
     *
     * @return [Int] the number of nanoseconds in the time component of this duration.
     *
     * @see toComponents
     * @see hoursComponent
     * @see minutesComponent
     * @see secondsComponent
     * @see millisecondsComponent
     * @see microsecondsComponent
     */
    val Duration.nanosecondsComponent get() = toComponents { _, _, _, nanoseconds -> nanoseconds }

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
            isInfinite() -> "--"

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
            isInfinite() -> "--"

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
