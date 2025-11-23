package com.hifnawy.alquran.shared.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * A type-safe identifier for a [Reciter].
 *
 * This value class wraps a primitive [Int] to prevent accidental misuse of raw integers
 * where a specific reciter ID is expected, enhancing compile-time safety.
 *
 * @property value [Int] The raw integer ID of the reciter.
 *
 * @author AbdElMoniem ElHifnawy
 */
@JvmInline
value class ReciterId(val value: Int)

/**
 * A convenience extension property to convert an [Int] to a [ReciterId].
 * This allows for more type-safe and readable code when dealing with reciter IDs.
 *
 * **Caution**: Make sure that the integer value is a valid reciter ID.
 *
 * Example:
 * ```
 * val reciterId = 5.asReciterId
 * ```
 */
val Int.asReciterId: ReciterId get() = ReciterId(this)

/**
 * Represents a single Quran reciter.
 *
 * This data class holds information about a specific reciter, including their ID,
 * name, and a list of their available narrations (Moshaf).
 *
 * @property id [ReciterId] The unique identifier for the reciter, wrapped in a [ReciterId] value class.
 * @property name [String] The name of the reciter.
 * @property letter [String] A letter, possibly used for indexing or categorization of the reciter.
 * @property date [String] A string representing a date associated with the reciter's record / last updated time, format may vary.
 * @property moshafList [List< Moshaf >][List] A list of [Moshaf] objects, representing the different Quran narrations (e.g., Hafs, Warsh) available for this reciter.
 *
 * @author AbdElMoniem ElHifnawy
 */
data class Reciter(
        val id: ReciterId,
        val name: String,
        val letter: String,
        val date: String,
        @SerializedName("moshaf")
        val moshafList: List<Moshaf>
) : Serializable
