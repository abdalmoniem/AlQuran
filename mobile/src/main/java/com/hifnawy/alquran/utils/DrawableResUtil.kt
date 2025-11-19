package com.hifnawy.alquran.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes

/**
 * Util class for all things related to drawable resources.
 *
 * @author AbdElMoniem ElHifnawy
 */
object DrawableResUtil {
    /**
     * Returns the drawable resource ID for the given surah.
     *
     * @param context [Context] The context to use for getting the resource ID.
     * @param surahId [Int]The ID of the surah to get the drawable resource ID for.
     * @return [@DrawableRes][DrawableRes] [Int] The drawable resource ID for the given surah.
     */
    @DrawableRes
    fun getSurahDrawableId(context: Context, surahId: Int? = null): Int {
        val surahNum = surahId.toString().padStart(3, '0')
        val resourceName = surahId?.let { "surah_$surahNum" } ?: "surah_name"

        @SuppressLint("DiscouragedApi")
        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
}
