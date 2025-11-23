package com.hifnawy.alquran.shared.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Represents a specific recitation version (moshaf) by a reciter.
 *
 * This data class holds information about a particular version of the Quran recitation,
 * such as the one by "Abdul Basit" in "Murattal" style.
 *
 * @property id [Int] The unique identifier for the moshaf.
 * @property name [String] The name of the moshaf (e.g., "Murattal", "Mujawwad").
 * @property server [String] The base URL for the server hosting the audio files for this moshaf.
 * @property surahsCount [String] The total number of Surahs available in this moshaf.
 * @property moshafType [String] The type identifier for the moshaf.
 * @property surahIdsStr [String] A private, comma-separated string of Surah IDs available in this moshaf.
 *                       This is the raw value received from the data source.
 * @property surahIds [List< Int >][List] A computed property that parses [surahIdsStr] into a list of integers,
 *                    representing the IDs of the available Surahs.
 *
 * @author AbdElMoniem ElHifnawy
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
        private val surahIdsStr: String
) : Serializable {

    /**
     * A computed property that parses [surahIdsStr] into a list of integers.
     *
     * Each integer in the list represents the ID of a Surah available in this moshaf.
     * For example, if [surahIdsStr] is "1,2,5", this property will return `listOf(1, 2, 5)`.
     *
     * @return [List< Int >][List] A list of integers representing the IDs of the available Surahs.
     */
    val surahIds: List<Int> get() = surahIdsStr.split(",").map { surahIdStr -> surahIdStr.toInt() }
}
