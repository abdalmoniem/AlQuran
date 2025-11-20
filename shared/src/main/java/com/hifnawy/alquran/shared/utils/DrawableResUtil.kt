package com.hifnawy.alquran.shared.utils

import androidx.annotation.DrawableRes
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.model.Surah
import java.util.Locale

/**
 * Util class for all things related to drawable resources.
 *
 * @author AbdElMoniem ElHifnawy
 */
object DrawableResUtil {

    private val surahDrawables = (1..114).associateWith {
        R.drawable::class.java.getField("surah_%03d".format(locale = Locale.ENGLISH, it)).getInt(defaultSurahDrawableId)
    }

    /**
     * Returns the drawable resource ID for the given surah.
     *
     * @return [@DrawableRes][androidx.annotation.DrawableRes] [Int] The drawable resource ID for the given surah.
     */
    @get:DrawableRes
    val Surah.surahDrawableId get() = surahDrawables[id] ?: defaultSurahDrawableId

    @get:DrawableRes
    val defaultSurahDrawableId get() = R.drawable.surah_name
}
