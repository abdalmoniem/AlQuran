package com.hifnawy.alquran.shared.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Represents a specific recitation version (moshaf) by a reciter.
 */
data class Moshaf(
        val id: Int,
        val name: String,
        val server: String,
        @SerializedName("surah_total")
        val surahsCount: Int,
        @SerializedName("moshaf_type")
        val moshafType: Int,
        @SerializedName("surah_list")
        private val surahList: String
) : Serializable {
    val surahIds: List<Int> get() = surahList.split(",").map { surahIdStr -> surahIdStr.toInt() }
}
