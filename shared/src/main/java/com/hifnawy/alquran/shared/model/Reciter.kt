package com.hifnawy.alquran.shared.model

import java.io.Serializable

@JvmInline
value class ReciterId(val value: Int)

val Int.asReciterId: ReciterId get() = ReciterId(this)

/**
 * Represents a single Quran reciter.
 */
data class Reciter(
        val id: ReciterId,
        val name: String,
        val letter: String,
        val date: String,
        val moshaf: List<Moshaf>
) : Serializable
