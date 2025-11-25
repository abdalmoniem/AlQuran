package com.hifnawy.alquran.utils

import androidx.window.core.layout.WindowSizeClass
import com.hifnawy.alquran.utils.DeviceConfiguration.COMPACT
import com.hifnawy.alquran.utils.DeviceConfiguration.Companion.deviceConfiguration
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.Companion.toHeightSize
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.HEIGHT_COMPACT
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.HEIGHT_EXPANDED
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.HEIGHT_MEDIUM
import com.hifnawy.alquran.utils.DeviceConfiguration.PHONE_LANDSCAPE
import com.hifnawy.alquran.utils.DeviceConfiguration.PHONE_PORTRAIT
import com.hifnawy.alquran.utils.DeviceConfiguration.TABLET_LANDSCAPE
import com.hifnawy.alquran.utils.DeviceConfiguration.TABLET_PORTRAIT
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.Companion.toWidthSize
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.WIDTH_COMPACT
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.WIDTH_EXPANDED
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.WIDTH_EXTRA_LARGE
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.WIDTH_LARGE
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.WIDTH_MEDIUM

/**
 * Represents different device configurations based on screen size classes.
 *
 * This enum helps in categorizing the device's screen into predefined types like phone (portrait/landscape)
 * and tablet (portrait/landscape), based on the width and height breakpoints provided by
 * [WindowSizeClass]. This allows for creating adaptive UIs that respond
 * to different screen sizes.
 *
 * The configurations are determined by the `deviceConfiguration` extension property on [WindowSizeClass].
 *
 * @property COMPACT Represents a compact screen configuration, typically for small phones or when the app is in a
 * multi-window mode with very limited space. This is the smallest size class, where both
 * width and height are in the compact range.
 * @property PHONE_PORTRAIT Represents a phone in portrait orientation.
 * @property PHONE_LANDSCAPE Represents a phone in landscape orientation.
 * @property TABLET_PORTRAIT Represents a tablet in portrait orientation.
 * @property TABLET_LANDSCAPE Represents a tablet in landscape orientation.
 *
 * @author AbdElMoniem ElHifnawy
 *
 * @see WindowSizeClass
 * @see deviceConfiguration
 */
enum class DeviceConfiguration {

    /**
     * Represents a compact screen configuration, typically for small phones or when the app is in a
     * multi-window mode with very limited space. This is the smallest size class, where both
     * width and height are in the compact range.
     */
    COMPACT,

    /**
     * Represents a phone in portrait orientation.
     */
    PHONE_PORTRAIT,

    /**
     * Represents a phone in landscape orientation.
     */
    PHONE_LANDSCAPE,

    /**
     * Represents a tablet in portrait orientation.
     */
    TABLET_PORTRAIT,

    /**
     * Represents a tablet in landscape orientation.
     */
    TABLET_LANDSCAPE;

    /**
     * Represents the width size of the screen.
     *
     * @param dp [Int] The width size in dp.
     *
     * @property WIDTH_COMPACT Represents a compact width size.
     * @property WIDTH_MEDIUM Represents a medium width size.
     * @property WIDTH_EXPANDED Represents an expanded width size.
     * @property WIDTH_LARGE Represents a large width size.
     * @property WIDTH_EXTRA_LARGE Represents an extra large width size.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see WindowSizeClass
     * @see toWidthSize
     */
    enum class WidthSize(val dp: Int) {

        /**
         * Represents a compact width size.
         *
         * @param dp [Int] The width size in dp.
         */
        WIDTH_COMPACT(dp = 0),

        /**
         * Represents a medium width size.
         *
         * @param dp [Int] The width size in dp.
         */
        WIDTH_MEDIUM(dp = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),

        /**
         * Represents an expanded width size.
         *
         * @param dp [Int] The width size in dp.
         */
        WIDTH_EXPANDED(dp = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),

        /**
         * Represents a large width size.
         *
         * @param dp [Int] The width size in dp.
         */
        WIDTH_LARGE(dp = WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND),

        /**
         * Represents an extra large width size.
         *
         * @param dp [Int] The width size in dp.
         */
        WIDTH_EXTRA_LARGE(dp = WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND);

        /**
         * Returns a string representation of the [WidthSize] enum constant, including its name
         * and the corresponding lower bound `dp` value.
         *
         * For example, `WidthSize.MEDIUM.toString()` will return `"MEDIUM(600)"`.
         *
         * @return [String] A [String] in the format "NAME(dp)".
         */
        override fun toString() = "$name($dp)"

        /**
         * A companion object for the [DeviceConfiguration] enum.
         *
         * This object holds extension properties and utility functions related to `DeviceConfiguration`.
         * The primary member is the [deviceConfiguration] extension property, which provides a convenient way
         * to determine the device configuration directly from a [WindowSizeClass] instance.
         *
         * @author AbdElMoniem ElHifnawy
         */
        companion object {

            /**
             * Converts an integer value to a [WidthSize] enum value.
             *
             * @param dp [Int] The integer value to convert.
             * @return [WidthSize] The corresponding [WidthSize] enum value.
             *
             * @author AbdElMoniem ElHifnawy
             *
             * @see WidthSize
             */
            val Int.toWidthSize get() = entries.first { it.dp >= this }
        }
    }

    /**
     * Represents the height size of the screen.
     *
     * @param dp [Int] The height size in dp.
     *
     * @property HEIGHT_COMPACT Represents a compact height size.
     * @property HEIGHT_MEDIUM Represents a medium height size.
     * @property HEIGHT_EXPANDED Represents an expanded height size.
     *
     * @author AbdElMoniem ElHifnawy
     *
     * @see WindowSizeClass
     * @see toHeightSize
     */
    enum class HeightSize(val dp: Int) {

        /**
         * Represents a compact height size.
         *
         * @param dp [Int] The height size in dp.
         */
        HEIGHT_COMPACT(dp = 0),

        /**
         * Represents a medium height size.
         *
         * @param dp [Int] The height size in dp.
         */
        HEIGHT_MEDIUM(dp = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND),

        /**
         * Represents an expanded height size.
         *
         * @param dp The height size in dp.
         */
        HEIGHT_EXPANDED(dp = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND);

        /**
         * Returns a string representation of the [WidthSize] enum constant.
         *
         * This representation includes the name of the constant and its associated dp value in parentheses,
         * which is useful for logging and debugging purposes.
         *
         * For example, `WidthSize.MEDIUM` would be represented as `"MEDIUM(600)"`.
         *
         * @return [String] A [String] in the format `"NAME(dp)"`.
         */
        override fun toString() = "$name($dp)"

        /**
         * A companion object for the [DeviceConfiguration] enum.
         *
         * This object holds extension properties and utility functions related to `DeviceConfiguration`.
         * The primary member is the [deviceConfiguration] extension property, which provides a convenient way
         * to determine the device configuration directly from a [WindowSizeClass] instance.
         *
         * @author AbdElMoniem ElHifnawy
         */
        companion object {

            /**
             * Converts an integer value to a [HeightSize] enum value.
             *
             * @param dp [Int] The integer value to convert.
             * @return [HeightSize] The corresponding [HeightSize] enum value.
             *
             * @author AbdElMoniem ElHifnawy
             *
             * @see HeightSize
             */
            val Int.toHeightSize: HeightSize get() = entries.first { it.dp >= this }
        }
    }

    /**
     * A companion object for the [DeviceConfiguration] enum.
     *
     * This object holds extension properties and utility functions related to `DeviceConfiguration`.
     * The primary member is the [deviceConfiguration] extension property, which provides a convenient way
     * to determine the device configuration directly from a [WindowSizeClass] instance.
     *
     * @author AbdElMoniem ElHifnawy
     */
    companion object {

        /**
         * Converts a [WindowSizeClass] to a [DeviceConfiguration] based on the information provided by Google on
         * [https://developer.android.com](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes)
         *
         * @receiver [WindowSizeClass] The window size class to convert.
         *
         * @return [DeviceConfiguration] The corresponding [DeviceConfiguration].
         *
         * @author AbdElMoniem ElHifnawy
         *
         * @see WindowSizeClass
         * @see DeviceConfiguration
         */
        val WindowSizeClass.deviceConfiguration
            get() = when {
                // Extra Large Width
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_EXTRA_LARGE.dp, heightDpBreakpoint = HEIGHT_EXPANDED.dp) -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_EXTRA_LARGE.dp, heightDpBreakpoint = HEIGHT_MEDIUM.dp)   -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_EXTRA_LARGE.dp, heightDpBreakpoint = HEIGHT_COMPACT.dp)  -> PHONE_LANDSCAPE

                // Large Width
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_LARGE.dp, heightDpBreakpoint = HEIGHT_EXPANDED.dp)       -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_LARGE.dp, heightDpBreakpoint = HEIGHT_MEDIUM.dp)         -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_LARGE.dp, heightDpBreakpoint = HEIGHT_COMPACT.dp)        -> PHONE_LANDSCAPE

                // Expanded Width
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_EXPANDED.dp, heightDpBreakpoint = HEIGHT_EXPANDED.dp)    -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_EXPANDED.dp, heightDpBreakpoint = HEIGHT_MEDIUM.dp)      -> PHONE_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_EXPANDED.dp, heightDpBreakpoint = HEIGHT_COMPACT.dp)     -> PHONE_LANDSCAPE

                // Medium Width
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_MEDIUM.dp, heightDpBreakpoint = HEIGHT_EXPANDED.dp)      -> TABLET_PORTRAIT
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_MEDIUM.dp, heightDpBreakpoint = HEIGHT_MEDIUM.dp)        -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_MEDIUM.dp, heightDpBreakpoint = HEIGHT_COMPACT.dp)       -> PHONE_LANDSCAPE

                // Compact Width
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_COMPACT.dp, heightDpBreakpoint = HEIGHT_EXPANDED.dp)     -> PHONE_PORTRAIT
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_COMPACT.dp, heightDpBreakpoint = HEIGHT_MEDIUM.dp)       -> PHONE_PORTRAIT
                isAtLeastBreakpoint(widthDpBreakpoint = WIDTH_COMPACT.dp, heightDpBreakpoint = HEIGHT_COMPACT.dp)      -> COMPACT

                else                                                                                                   -> throw IllegalArgumentException("Unknown Device Configuration")
            }
    }
}
