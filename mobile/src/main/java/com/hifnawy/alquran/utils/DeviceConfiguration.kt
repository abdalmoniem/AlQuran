package com.hifnawy.alquran.utils

import androidx.window.core.layout.WindowSizeClass
import com.hifnawy.alquran.utils.DeviceConfiguration.COMPACT
import com.hifnawy.alquran.utils.DeviceConfiguration.Companion.deviceConfiguration
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.COMPACT
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.Companion.toHeightSize
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.EXPANDED
import com.hifnawy.alquran.utils.DeviceConfiguration.HeightSize.MEDIUM
import com.hifnawy.alquran.utils.DeviceConfiguration.PHONE_LANDSCAPE
import com.hifnawy.alquran.utils.DeviceConfiguration.PHONE_PORTRAIT
import com.hifnawy.alquran.utils.DeviceConfiguration.TABLET_LANDSCAPE
import com.hifnawy.alquran.utils.DeviceConfiguration.TABLET_PORTRAIT
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.COMPACT
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.Companion.toWidthSize
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.EXPANDED
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.EXTRA_LARGE
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.LARGE
import com.hifnawy.alquran.utils.DeviceConfiguration.WidthSize.MEDIUM

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
     * @property COMPACT Represents a compact width size.
     * @property MEDIUM Represents a medium width size.
     * @property EXPANDED Represents an expanded width size.
     * @property LARGE Represents a large width size.
     * @property EXTRA_LARGE Represents an extra large width size.
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
        COMPACT(dp = 0),

        /**
         * Represents a medium width size.
         *
         * @param dp [Int] The width size in dp.
         */
        MEDIUM(dp = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),

        /**
         * Represents an expanded width size.
         *
         * @param dp [Int] The width size in dp.
         */
        EXPANDED(dp = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND),

        /**
         * Represents a large width size.
         *
         * @param dp [Int] The width size in dp.
         */
        LARGE(dp = WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND),

        /**
         * Represents an extra large width size.
         *
         * @param dp [Int] The width size in dp.
         */
        EXTRA_LARGE(dp = WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND);

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
     * @property COMPACT Represents a compact height size.
     * @property MEDIUM Represents a medium height size.
     * @property EXPANDED Represents an expanded height size.
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
        COMPACT(dp = 0),

        /**
         * Represents a medium height size.
         *
         * @param dp [Int] The height size in dp.
         */
        MEDIUM(dp = WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND),

        /**
         * Represents an expanded height size.
         *
         * @param dp The height size in dp.
         */
        EXPANDED(dp = WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND);

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
        @Suppress("RemoveRedundantQualifierName")
        val WindowSizeClass.deviceConfiguration: DeviceConfiguration
            get() = when {
                // Extra Large Width
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.EXTRA_LARGE.dp, heightDpBreakpoint = HeightSize.EXPANDED.dp) -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.EXTRA_LARGE.dp, heightDpBreakpoint = HeightSize.MEDIUM.dp)   -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.EXTRA_LARGE.dp, heightDpBreakpoint = HeightSize.COMPACT.dp)  -> PHONE_LANDSCAPE

                // Large Width
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.LARGE.dp, heightDpBreakpoint = HeightSize.EXPANDED.dp)       -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.LARGE.dp, heightDpBreakpoint = HeightSize.MEDIUM.dp)         -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.LARGE.dp, heightDpBreakpoint = HeightSize.COMPACT.dp)        -> PHONE_LANDSCAPE

                // Expanded Width
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.EXPANDED.dp, heightDpBreakpoint = HeightSize.EXPANDED.dp)    -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.EXPANDED.dp, heightDpBreakpoint = HeightSize.MEDIUM.dp)      -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.EXPANDED.dp, heightDpBreakpoint = HeightSize.COMPACT.dp)     -> PHONE_LANDSCAPE

                // Medium Width
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.MEDIUM.dp, heightDpBreakpoint = HeightSize.EXPANDED.dp)      -> TABLET_PORTRAIT
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.MEDIUM.dp, heightDpBreakpoint = HeightSize.MEDIUM.dp)        -> TABLET_LANDSCAPE
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.MEDIUM.dp, heightDpBreakpoint = HeightSize.COMPACT.dp)       -> PHONE_LANDSCAPE

                // Compact Width
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.COMPACT.dp, heightDpBreakpoint = HeightSize.EXPANDED.dp)     -> PHONE_PORTRAIT
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.COMPACT.dp, heightDpBreakpoint = HeightSize.MEDIUM.dp)       -> PHONE_PORTRAIT
                isAtLeastBreakpoint(widthDpBreakpoint = WidthSize.COMPACT.dp, heightDpBreakpoint = HeightSize.COMPACT.dp)      -> COMPACT

                else                                                                                                           -> throw IllegalArgumentException("Unknown Device Configuration")
            }
    }
}
