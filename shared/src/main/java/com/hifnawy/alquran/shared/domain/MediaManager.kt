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
import com.hifnawy.alquran.shared.model.ReciterId
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
        lifecycleScope.launch(Dispatchers.IO) {
            val result = async { QuranRepository.getReciters() }.await()

            onReady(result)

            if (result !is Result.Success) return@launch
            reciters = result.data
        }
    }

    fun whenSurahsReady(onReady: (result: Result<List<Surah>, DataError>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = async { QuranRepository.getSurahs() }.await()

            onReady(result)

            if (result !is Result.Success) return@launch
            surahs = result.data
        }
    }

    fun whenReady(reciterID: ReciterId, onReady: (reciter: Reciter, moshaf: Moshaf, surahs: List<Surah>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val recitersResult = async { QuranRepository.getReciters() }.await()
            val surahsResult = async { QuranRepository.getSurahs() }.await()

            if (recitersResult !is Result.Success) return@launch
            if (surahsResult !is Result.Success) return@launch

            val (moshaf, moshafSurahIds) = reciterID.moshafSurahs

            reciters = recitersResult.data
            surahs = surahsResult.data.filter { it.id in moshafSurahIds }.sortedBy { surah -> surah.id }

            reciters.find { reciter -> reciter.id == reciterID }?.let { reciter -> onReady(reciter, moshaf, surahs) }
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

    private fun Moshaf.getMoshafSurahs(surahs: List<Surah>): List<Surah> = surahs.map { surah ->
        val surahNum = surah.id.toString().padStart(3, '0')

        surah.copy().apply { uri = "$server$surahNum.mp3".toUri() }
    }

    private val ReciterId.moshafSurahs: MoshafSurahs
        get() {
            val moshaf = reciters.first { it.id == this }.moshaf.first()
            return MoshafSurahs(moshaf, moshaf.surahIds)
        }

    private data class MoshafSurahs(val moshaf: Moshaf, val surahIds: List<Int>)
}
