package com.hifnawy.alquran.shared.domain

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.media.MediaBrowserServiceCompat
import androidx.media.utils.MediaConstants
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.domain.QuranMediaService.Extras
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.model.asReciterId
import com.hifnawy.alquran.shared.repository.DataError
import com.hifnawy.alquran.shared.repository.Result
import com.hifnawy.alquran.shared.utils.ImageUtil.drawTextOn
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.NumberExt.sp
import timber.log.Timber

open class AndroidAutoMediaBrowser : MediaBrowserServiceCompat() {

    private enum class Page {
        ROOT,
        RECITER_BROWSE,
        SURAH_BROWSE
    }

    private val List<Reciter>.drawables: List<Bitmap>
        get() = mapIndexed { index, reciter ->
            when {
                index % 2 == 0 -> R.drawable.reciter_background_2
                else           -> R.drawable.reciter_background_3
            }.drawTextOn(
                    context = this@AndroidAutoMediaBrowser,
                    text = reciter.name,
                    // subText = if (reciter.recitationStyle != null) "(${reciter.recitationStyle.style})" else "",
                    fontFace = R.font.aref_ruqaa,
                    fontSize = 60.sp.toFloat(),
                    fontMargin = 0
            )
        }

    private var reciterDrawables = listOf<Bitmap>()

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot = BrowserRoot(Page.ROOT.name, null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.detach()

        when (parentId) {
            Page.ROOT.name -> result.sendResult(listOf(createBrowsableMediaItem(Page.RECITER_BROWSE.name, -1)).toMutableList())

            else           -> {
                var mediaItems: MutableList<MediaBrowserCompat.MediaItem>
                val page = when {
                    parentId == Page.RECITER_BROWSE.name -> Page.RECITER_BROWSE
                    parentId.startsWith("reciter_")      -> Page.SURAH_BROWSE
                    else                                 -> Page.RECITER_BROWSE
                }

                when (page) {
                    Page.RECITER_BROWSE -> {
                        MediaManager.whenRecitersReady { recitersResult ->
                            when (recitersResult) {
                                is Result.Success -> {
                                    val reciters = recitersResult.data
                                    reciterDrawables = reciters.drawables

                                    mediaItems = reciters.mapIndexed { reciterIndex, reciter ->
                                        createBrowsableMediaItem(
                                                "reciter_${reciter.id.value}",
                                                reciterIndex,
                                                reciter.name
                                        )
                                    }.toMutableList()

                                    result.sendResult(mediaItems)
                                }

                                is Result.Error   -> when (val error = recitersResult.error) {
                                    is DataError.LocalError   -> Timber.error(error.errorMessage)
                                    is DataError.NetworkError -> Timber.error("${error.errorCode} ${error.errorMessage}")
                                    is DataError.ParseError   -> Timber.error(error.errorMessage)
                                }
                            }
                        }
                    }

                    Page.SURAH_BROWSE   -> {
                        val reciterID = parentId.replace("reciter_", "").toInt().asReciterId

                        MediaManager.whenReady(reciterID) { reciter, moshaf, surahs ->
                            mediaItems = surahs.map { surah ->
                                createMediaItem(
                                        mediaId = "surah_${surah.id}",
                                        reciter = reciter,
                                        moshaf = moshaf,
                                        surah = surah,
                                        surahUri = surah.uri,
                                )
                            }.toMutableList()

                            result.sendResult(mediaItems)
                        }
                    }

                    else                -> return
                }
            }
        }
    }

    private fun createBrowsableMediaItem(
            mediaId: String,
            reciterIndex: Int,
            reciterName: String? = null
    ) = MediaDescriptionCompat.Builder().run {
        val extras = Bundle().apply {
            putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
        }

        setExtras(extras)
        setMediaId(mediaId)

        reciterName?.let { setTitle(it) }
        reciterDrawables.indices.find { index -> index == reciterIndex }?.let { index -> setIconBitmap(reciterDrawables[index]) }

        MediaBrowserCompat.MediaItem(build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    private fun createMediaItem(
            mediaId: String,
            reciter: Reciter,
            moshaf: Moshaf,
            surah: Surah,
            surahUri: Uri? = null
    ) = MediaDescriptionCompat.Builder().run {
        val extras = Bundle().apply {
            putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)
            putInt(MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM)

            putSerializable(Extras.EXTRA_RECITER.name, reciter)
            putSerializable(Extras.EXTRA_MOSHAF.name, moshaf)
            putSerializable(Extras.EXTRA_SURAH.name, surah)
        }

        setExtras(extras)
        setMediaId(mediaId)
        setTitle(surah.name)

        surahUri?.let { setMediaUri(it) }

        @SuppressLint("DiscouragedApi")
        val drawableId = resources.getIdentifier("surah_${surah.id.toString().padStart(3, '0')}", "drawable", packageName)
        setIconBitmap((AppCompatResources.getDrawable(this@AndroidAutoMediaBrowser, drawableId) as BitmapDrawable).bitmap)

        MediaBrowserCompat.MediaItem(build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }
}
