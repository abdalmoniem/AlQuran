package com.hifnawy.alquran.shared.domain

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.QuranRepository
import com.hifnawy.alquran.shared.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object MediaManager : LifecycleOwner {

    var onMediaReady: ((reciter: Reciter, moshaf: Moshaf, surah: Surah, surahUri: Uri, surahDrawable: Drawable?) -> Unit)? = null

    private var reciters: List<Reciter> = emptyList()
    private var surahs: List<Surah> = emptyList()
    private var surahsUri: List<Uri> = emptyList()
    private var currentReciter: Reciter? = null
    private var currentMoshaf: Moshaf? = null
    private var currentSurah: Surah? = null
    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    private val applicationContext by lazy { QuranApplication.applicationContext }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun stopLifecycle() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun whenRecitersReady(onReady: (result: Result<List<Reciter>, DataError>) -> Unit) {
        when {
            reciters.isEmpty() -> lifecycleScope.launch(Dispatchers.IO) {
                val result = async { QuranRepository.getRecitersList() }.await()

                onReady(result)

                if (result !is Result.Success) return@launch
                reciters = result.data
            }

            else               -> onReady(Result.Success(reciters))
        }
    }

    fun whenSurahsReady(onReady: (result: Result<List<Surah>, DataError>) -> Unit) {
        when {
            surahs.isEmpty() -> lifecycleScope.launch(Dispatchers.IO) {
                val result = async { QuranRepository.getSurahs() }.await()

                onReady(result)

                if (result !is Result.Success) return@launch
                surahs = result.data
            }

            else             -> onReady(Result.Success(surahs))
        }
    }

    fun whenReady(reciterID: Int, onReady: (reciters: List<Reciter>, moshafs: Moshaf, surahs: List<Surah>, surahsUri: List<Uri>) -> Unit) = when {
        reciters.isEmpty() || surahs.isEmpty() -> {
            whenRecitersReady { result ->
                if (result is Result.Success) reciters = result.data

                whenSurahsReady { result ->
                    val reciter = reciters.first { it.id == reciterID }
                    val moshaf = reciter.moshaf.first()
                    val moshafSurahs = moshaf.surah_list.split(",").map { it.toInt() }

                    if (result !is Result.Success) return@whenSurahsReady
                    surahs = result.data.filter { it.id in moshafSurahs }.sortedBy { surah -> surah.id }

                    surahsUri = getReciterSurahUris(reciterID, surahs)

                    onReady(reciters, moshaf, surahs, surahsUri)
                }
            }
        }

        else                                   -> {
            surahsUri = getReciterSurahUris(reciterID, surahs)
            val reciter = reciters.first { it.id == reciterID }
            val moshaf = reciter.moshaf.first()

            onReady(reciters, moshaf, surahs, surahsUri)
        }
    }

    fun processSurah(reciter: Reciter, moshaf: Moshaf, surah: Surah) {
        @SuppressLint("DiscouragedApi")
        val drawableId = applicationContext.resources.getIdentifier("surah_${surah.id.toString().padStart(3, '0')}", "drawable", applicationContext.packageName)
        val surahNum = surah.id.toString().padStart(3, '0')
        val surahUri = "${moshaf.server}$surahNum.mp3"

        currentReciter = reciter
        currentMoshaf = moshaf
        currentSurah = surah

        onMediaReady?.invoke(
                reciter,
                moshaf,
                surah,
                surahUri.toUri(),
                AppCompatResources.getDrawable(applicationContext, drawableId)
        )
    }

    fun processNextSurah() {
        val reciter = currentReciter ?: return
        val moshaf = currentMoshaf ?: return
        val surah = currentSurah ?: return

        surahs.find {
            it.id == when (surah.id) {
                surahs.last().id -> 1
                else             -> surah.id + 1
            }
        }?.let { newSurah ->
            processSurah(reciter, moshaf, newSurah)
        }
    }

    fun processPreviousSurah() {
        val reciter = currentReciter ?: return
        val moshaf = currentMoshaf ?: return
        val surah = currentSurah ?: return

        surahs.find {
            it.id == when (surah.id) {
                surahs.first().id -> surahs.last().id
                else              -> surah.id - 1
            }
        }?.let { newSurah ->
            processSurah(reciter, moshaf, newSurah)
        }
    }

    private fun getReciterSurahUris(reciterID: Int, reciterSurahs: List<Surah>): List<Uri> {
        val reciter = reciters.first { it.id == reciterID }
        val moshaf = reciter.moshaf.first()
        val moshafServer = moshaf.server

        return reciterSurahs.map {
            val surahNum = it.id.toString().padStart(3, '0')
            "${moshafServer}$surahNum.mp3".toUri()
        }
    }
}
