package com.hifnawy.alquran.shared.utils

import androidx.annotation.DrawableRes
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.DrawableResUtil.defaultSurahDrawableId
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawablesMap
import java.util.Locale

/**
 * Util class for all things related to drawable resources.
 *
 * @author AbdElMoniem ElHifnawy
 */
object DrawableResUtil {

    /**
     * A map of surah numbers to their corresponding drawable resource IDs.
     *
     * The keys are surah ids (1..114), and the values are the corresponding drawable resource IDs.
     *
     * @return [Map<Int, Int>][Map] A map of surah ids to their corresponding drawable resource IDs.
     */
    private val surahDrawablesMap = (1..114).associateWith {
        R.drawable::class.java.getField("surah_%03d".format(locale = Locale.ENGLISH, it)).getInt(defaultSurahDrawableId)
    }

    /**
     * A list of drawable resource IDs corresponding to each surah number (1..114).
     *
     * @return [@DrawableRes][DrawableRes] [List] A list of drawable resource IDs corresponding to each surah number (1..114).
     */
    @get:DrawableRes
    val surahDrawables = (1..114).map {
        R.drawable::class.java.getField("surah_%03d".format(locale = Locale.ENGLISH, it)).getInt(defaultSurahDrawableId)
    }

    /**
     * The drawable resource ID for the default surah.
     *
     * @return [@DrawableRes][DrawableRes] [Int] The drawable resource ID for the default surah.
     */
    @get:DrawableRes
    val defaultSurahDrawableId get() = R.drawable.surah_name

    /**
     * The drawable resource ID for a given [Surah].
     *
     * @return [@DrawableRes][DrawableRes] [Int] The drawable resource ID for the [Surah], or [defaultSurahDrawableId] if the [Surah] is not found in [surahDrawablesMap].
     */
    @get:DrawableRes
    val Surah.surahDrawableId get() = surahDrawablesMap[id] ?: defaultSurahDrawableId
}
