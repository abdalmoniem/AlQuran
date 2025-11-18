package com.hifnawy.alquran.shared.domain

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.QuranRepository
import com.hifnawy.alquran.shared.repository.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MediaManager(private val context: Context) : LifecycleOwner {

    var onMediaReady: ((reciter: Reciter, surah: Surah, surahUri: Uri, surahDrawable: Drawable?) -> Unit)? = null

    private var reciters: List<Reciter> = emptyList()
    private var surahs: List<Surah> = emptyList()
    private var surahsUri: List<Uri> = emptyList()
    private var currentReciter: Reciter? = null
    private var currentSurah: Surah? = null
    private val quranRepository by lazy { QuranRepository(context) }
    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

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
            reciters.isEmpty() -> lifecycleScope.launch(Dispatchers.IO) { onReady(async { quranRepository.getRecitersList() }.await()) }

            else               -> onReady(Result.Success(reciters))
        }
    }

    fun whenSurahsReady(onReady: (result: Result<List<Surah>, DataError>) -> Unit) {
        when {
            surahs.isEmpty() -> lifecycleScope.launch(Dispatchers.IO) { onReady(async { quranRepository.getSurahs() }.await()) }

            else             -> onReady(Result.Success(surahs))
        }
    }

    fun whenReady(reciterID: Int, onReady: (reciters: List<Reciter>, surahs: List<Surah>, surahsUri: List<Uri>) -> Unit) = when {
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

                    onReady(reciters, surahs, surahsUri)
                }
            }
        }

        else                                   -> {
            surahsUri = getReciterSurahUris(reciterID, surahs)

            onReady(reciters, surahs, surahsUri)
        }
    }

    fun processSurah(context: Context, reciter: Reciter, surah: Surah) {
        @SuppressLint("DiscouragedApi")
        val drawableId = context.resources.getIdentifier("surah_${surah.id.toString().padStart(3, '0')}", "drawable", context.packageName)
        val moshaf = reciter.moshaf.first()
        val surahNum = surah.id.toString().padStart(3, '0')
        val surahUri = "${moshaf.server}$surahNum.mp3"

        currentReciter = reciter
        currentSurah = surah

        onMediaReady?.invoke(
                reciter,
                surah,
                surahUri.toUri(),
                AppCompatResources.getDrawable(context, drawableId)
        )
    }

    fun processNextSurah(context: Context) {
        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        currentSurah = surahs.find {
            it.id == when (surah.id) {
                surahs.last().id -> 1
                else             -> surah.id + 1
            }
        }?.also { newSurah ->
            processSurah(context, reciter, newSurah)
        }
    }

    fun processPreviousSurah(context: Context) {
        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        currentSurah = surahs.find {
            it.id == when (surah.id) {
                surahs.first().id -> surahs.last().id
                else              -> surah.id - 1
            }
        }?.also { newSurah ->
            processSurah(context, reciter, newSurah)
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
