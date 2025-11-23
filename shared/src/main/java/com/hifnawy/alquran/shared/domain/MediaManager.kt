package com.hifnawy.alquran.shared.domain

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
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
import com.hifnawy.alquran.shared.utils.DrawableResUtil.surahDrawableId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MediaManager : LifecycleOwner {

    interface MediaReadyObservable : IObservable {

        fun onMediaReady(reciter: Reciter, moshaf: Moshaf, surah: Surah, surahDrawable: Drawable? = null)
    }

    val mediaReadyListeners = mutableListOf<MediaReadyObservable>()

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
        mediaReadyListeners.clear()
    }

    fun whenRecitersReady(onReady: (result: Result<List<Reciter>, DataError>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = QuranRepository.getReciters()

            onReady(result)

            if (result !is Result.Success) return@launch
            reciters = result.data
        }
    }

    fun whenSurahsReady(onReady: (result: Result<List<Surah>, DataError>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = QuranRepository.getSurahs()

            onReady(result)

            if (result !is Result.Success) return@launch
            surahs = result.data
        }
    }

    fun whenReady(reciterID: ReciterId, onReady: (reciter: Reciter, moshaf: Moshaf, surahs: List<Surah>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val recitersResult = QuranRepository.getReciters()
            val surahsResult = QuranRepository.getSurahs()

            if (recitersResult !is Result.Success) return@launch
            if (surahsResult !is Result.Success) return@launch

            reciters = recitersResult.data
            val (moshaf, moshafSurahIds) = reciterID.moshafSurahs
            surahs = surahsResult.data.filter { it.id in moshafSurahIds }.sortedBy { surah -> surah.id }
            surahs = moshaf.getMoshafSurahs(surahs)

            reciters.find { reciter -> reciter.id == reciterID }?.let { reciter -> onReady(reciter, moshaf, surahs) }
        }
    }

    fun processSurah(reciter: Reciter, moshaf: Moshaf, surah: Surah) {
        currentReciter = reciter
        currentMoshaf = moshaf
        currentSurah = surah

        mediaReadyListeners.forEach { listener ->
            listener.onMediaReady(
                    reciter = reciter,
                    moshaf = moshaf,
                    surah = surah.toSurahWithUri(moshaf),
                    surahDrawable = AppCompatResources.getDrawable(applicationContext, surah.surahDrawableId)
            )
        }
    }

    fun processNextSurah(reciter: Reciter, moshaf: Moshaf) {
        val surah = currentSurah ?: return

        surahs = surahs.filter { it.id in moshaf.surahIds }.sortedBy { surah -> surah.id }
        surahs = moshaf.getMoshafSurahs(surahs)

        val currentSurahIndex = surahs.indexOfFirst { it.id == surah.id }
        val nextSurahIndex = (currentSurahIndex + 1) % surahs.size
        val newSurah = surahs[nextSurahIndex]

        processSurah(reciter, moshaf, newSurah)
    }

    fun processPreviousSurah(reciter: Reciter, moshaf: Moshaf) {
        val surah = currentSurah ?: return

        surahs = surahs.filter { it.id in moshaf.surahIds }.sortedBy { surah -> surah.id }
        surahs = moshaf.getMoshafSurahs(surahs)

        val currentSurahIndex = surahs.indexOfFirst { it.id == surah.id }
        val previousSurahIndex = (currentSurahIndex - 1 + surahs.size) % surahs.size
        val newSurah = surahs[previousSurahIndex]

        processSurah(reciter, moshaf, newSurah)
    }

    private fun Moshaf.getMoshafSurahs(surahs: List<Surah>): List<Surah> = surahs.map { surah ->
        val surahNum = surah.id.toString().padStart(3, '0')

        surah.copy().apply { url = "$server$surahNum.mp3" }
    }

    private fun Surah.toSurahWithUri(moshaf: Moshaf): Surah = copy().apply {
        val surahNum = id.toString().padStart(3, '0')
        url = "${moshaf.server}$surahNum.mp3"
    }

    private val ReciterId.moshafSurahs: MoshafSurahs
        get() {
            val moshaf = reciters.first { it.id == this }.moshafList.first()
            return MoshafSurahs(moshaf, moshaf.surahIds)
        }

    private data class MoshafSurahs(val moshaf: Moshaf, val surahIds: List<Int>)
}
