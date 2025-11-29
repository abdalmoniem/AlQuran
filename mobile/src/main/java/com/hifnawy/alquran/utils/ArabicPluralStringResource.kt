package com.hifnawy.alquran.utils

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource

object ArabicPluralStringResource {

    /**
     * A composable function that handles Arabic pluralization rules for string resources.
     *
     * Arabic has complex pluralization rules that are not fully supported by the standard Android
     * [pluralStringResource]. This function provides a workaround for a specific range of numbers
     * (103-110) where the standard implementation might select the wrong plural form.
     * For numbers between 103 and 110 (inclusive), it forces the use of a quantity that typically
     * corresponds to a different plural category (e.g., `many` or a specific number like 111)
     * to get the correct grammatical form. For all other numbers, it behaves like the standard
     * [pluralStringResource].
     *
     * **CLDR Plural Rules for Arabic in Android**:
     * Arabic has one of the most complex plural systems in CLDR (Common Locale Data Repository),
     * which Android uses for pluralization. Arabic has six plural categories, more than most
     * languages. The Six Plural Categories For Arabic are:
     *
     * - `zero` - exactly 0
     * - `one` - numbers ending in 01 (1, 101, 201, etc.)
     * - `two` - numbers ending in 02 (2, 102, 202, etc.)
     * - `few` - numbers ending in 03-10 (3-10, 103-110, 203-210, etc.)
     * - `many` - numbers ending in 11-99 (11-99, 111-199, 211-299, etc.)
     * - `other` - all other numbers (100, 1000, etc.)
     *
     * The Calculation Logic Given a number n, Android applies these rules in order:
     * - zero:  n = 0
     * - one:   n % 100 = 1
     * - two:   n % 100 = 2
     * - few:   n % 100 = 3..10
     * - many:  n % 100 = 11..99
     * - other: everything else
     *
     * Example `plurals.xml` for this use case:
     * ```xml
     * <plurals name="surah_count">
     *      <!-- exactly 0 -->
     *      <item quantity="zero">لا توجد سور</item>
     *
     *      <!-- exactly 1 -->
     *      <item quantity="one">ٌسورةٌ واحدة</item>
     *
     *      <!-- exactly 2 -->
     *      <item quantity="two">سورتان</item>
     *
     *      <!-- 3-10, 103-110, 203-210, etc. -->
     *      <!-- notice that this will also include 103-110 which is incorrect -->
     *      <item quantity="few">%d سور</item>
     *
     *      <!-- 11-99, 111-199, 211-299, etc. -->
     *      <item quantity="many">%d سورة</item>
     *
     *      <!-- 100, 200, 1000, etc. -->
     *      <item quantity="other">%d سورة</item>
     * </plurals>
     * ```
     * By mapping 103-110 to a different quantity (like `111`, which falls under "other" or "many"),
     * we ensure the correct plural form "سورة" is used instead of "سور".
     *
     * @param pluralStringResourceId [Int] The resource ID of the plurals string to use.
     * @param count [Int] The number to use to select the appropriate plural string.
     *
     * @return [String] The correctly pluralized string for the given count, formatted with the count.
     *
     * @see pluralStringResource
     */
    @Composable
    fun arabicPluralStringResource(
            @PluralsRes
            pluralStringResourceId: Int,
            count: Int
    ) = when {
        // 103-110, 203-210, etc... (all hundreds + 3-10)
        count >= 103 && count % 100 in 3..10 -> pluralStringResource(pluralStringResourceId, 111, count)
        else                                 -> pluralStringResource(pluralStringResourceId, count, count)
    }
}