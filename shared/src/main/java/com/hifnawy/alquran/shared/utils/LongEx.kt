package com.hifnawy.alquran.shared.utils

import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import com.hifnawy.alquran.shared.utils.NumberExt.GB
import com.hifnawy.alquran.shared.utils.NumberExt.KB
import com.hifnawy.alquran.shared.utils.NumberExt.MB
import java.util.Locale

/**
 * A utility object that provides convenient extension properties for the [Long] data type.
 *
 * This object centralizes common transformations and formatting operations on [Long] values,
 * making the code more readable and reusable. It's designed to be a singleton,
 * ensuring that its extensions are globally accessible within the project scope
 * where it's imported.
 *
 * Current extensions include:
 *  - [asHumanReadableSize]: Converts a [Long] value, typically representing bytes,
 *    into a human-readable string format (e.g., `KB`, `MB`, `GB`).
 *
 * By scoping extensions within this object, we avoid polluting the global namespace
 * for the [Long] class, requiring an explicit import of the desired extension.
 * This practice enhances code clarity and prevents potential extension function conflicts.
 *
 * @author AbdElMoniem ElHifnawy
 */
object LongEx {

    /**
     * Converts a [Long] value representing a size in bytes into a human-readable string.
     *
     * This extension property formats the byte count into a more understandable format,
     * using kilobytes `KB`, megabytes `MB`, or gigabytes `GB` as appropriate.
     * The formatting is done with two decimal places for precision. If the size is less than
     * `1024 bytes`, it is simply returned with ` bytes` appended.
     *
     * - Values >= `1 GB` are formatted as `X.XX GB`.
     * - Values >= `1 MB` are formatted as `X.XX MB`.
     * - Values >= `1 KB` are formatted as `X.XX KB`.
     * - Values < `1 KB` are formatted as `X bytes`.
     *
     * The formatting uses [Locale.ENGLISH] to ensure a consistent decimal point (`.`).
     *
     * Example usage:
     * ```kotlin
     * import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
     *
     * val fileSizeInBytes: Long = 10485760L // 10 MB
     * val formattedSize = fileSizeInBytes.asHumanReadableSize // "10.00 MB"
     * println(formattedSize)
     *
     * val smallFileSize: Long = 512L
     * val smallFormattedSize = smallFileSize.asHumanReadableSize // "512 bytes"
     * println(smallFormattedSize)
     * ```
     *
     * @receiver [Long] The [Long] value to be formatted.
     *
     * @return A [String] representing the size in a human-readable format.
     */
    val Long.asHumanReadableSize
        get() = when {
            this >= 1.0.GB -> String.format(Locale.ENGLISH, "%.2f GB", this / 1.0.GB)
            this >= 1.0.MB -> String.format(Locale.ENGLISH, "%.2f MB", this / 1.0.MB)
            this >= 1.0.KB -> String.format(Locale.ENGLISH, "%.2f KB", this / 1.0.KB)
            else           -> "$this bytes"
        }

    /**
     * Converts a [Long] value representing a size in bytes into a localized, human-readable string.
     *
     * This extension property is similar to [asHumanReadableSize] but utilizes Android string
     * resources to provide a localized representation of the size and its unit. It's suitable for
     * displaying file sizes to the user in a way that respects their device's language settings.
     *
     * The logic determines the appropriate unit (`GB`, `MB`, `KB`, or bytes) and then uses to combine the
     * numeric value and the unit. The units themselves are also fetched from string resources, allowing for
     * full internationalization.
     *
     * - Values >= `1 GB` are formatted using the [R.string.gb_unit].
     * - Values >= `1 MB` are formatted using the [R.string.mb_unit].
     * - Values >= `1 KB` are formatted using the [R.string.kb_unit].
     * - Values < `1 KB` are formatted using the [R.string.b_unit].
     *
     * The formatting uses [R.string.unit_format], to produce outputs such as `10.50 مب` or `1.25 GB` depending
     * on the locale.
     *
     * Example usage:
     * ```kotlin
     * import com.hifnawy.alquran.shared.utils.LongEx.asLocalizedHumanReadableSize
     *
     * val fileSizeInBytes: Long = 10485760L // 10 MB
     * val formattedSize = fileSizeInBytes.asLocalizedHumanReadableSize // "10.00 MB" // "10.00 مب"
     * println(formattedSize)
     *
     * val smallFileSize: Long = 512L
     * val smallFormattedSize = smallFileSize.asLocalizedHumanReadableSize // "512 bytes" // "512 ب"
     * println(smallFormattedSize)
     * ```
     *
     * @receiver [Long] The [Long] value to be formatted.
     *
     * @return A [String] representing the size in a localized, human-readable format.
     */
    val Long.asLocalizedHumanReadableSize
        get() = when {
            this >= 1.0.GB -> 1.0.GB to QuranApplication.applicationContext.getString(R.string.gb_unit)
            this >= 1.0.MB -> 1.0.MB to QuranApplication.applicationContext.getString(R.string.mb_unit)
            this >= 1.0.KB -> 1.0.KB to QuranApplication.applicationContext.getString(R.string.kb_unit)
            else           -> 1.0 to QuranApplication.applicationContext.getString(R.string.b_unit)
        }.let { (divisor, unit) ->

            QuranApplication.applicationContext.getString(R.string.unit_format, this@asLocalizedHumanReadableSize / divisor, unit)
        }
}
