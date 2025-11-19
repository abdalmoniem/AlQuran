package com.hifnawy.alquran.shared.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Surah(
        val id: Int,
        val name: String,
        @SerializedName("start_page")
        val startPage: Int,
        @SerializedName("end_page")
        val endPage: Int,
        val makkia: Int,
        val type: Int,
        var url: String? = null
) : Serializable
