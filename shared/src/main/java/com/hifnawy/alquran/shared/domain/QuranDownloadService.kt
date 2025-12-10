package com.hifnawy.alquran.shared.domain

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.google.gson.Gson
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey.Companion.asCacheKey
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheDataSourceFactory
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.deleteKey
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.keyContents
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.asJsonString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.Serializable
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
class QuranDownloadService : DownloadService(
        FOREGROUND_NOTIFICATION_ID,
        UPDATE_PROGRESS_INTERVAL,
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        R.string.notification_channel_name,
        R.string.notification_channel_name
), DownloadManager.Listener {

    companion object {

        val downloadServiceObservers = mutableListOf<DownloadStatusObserver>()

        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "Quran Downloads"
        private const val UPDATE_PROGRESS_INTERVAL = 100L

        private val downloadsCacheKey = CacheKey("downloads")
        private val completedDownloads = mutableMapOf<DownloadRequestId, MutableList<Download>>()
        private var downloadManagerInstance: DownloadManager? = null

        private val ByteArray.asUTF8String
            get() = String(this, Charsets.UTF_8)

        private val DownloadRequest.serializedData
            get() = Gson().fromJson(data.asUTF8String, DownloadRequestData::class.java)

        data class DownloadRequestData(val reciter: Reciter, val moshaf: Moshaf, val surah: Surah) : Serializable {

            override fun toString(): String {
                return "${this::class.java.simpleName}(" +
                       "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                       "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                       "${::surah.name}=(${surah.id}: ${surah.name})" +
                       ")"
            }
        }

        private data class DownloadRequestId(val reciter: Reciter, val moshaf: Moshaf) : Serializable {

            override fun toString(): String {
                return "${this::class.java.simpleName}(" +
                       "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                       "${::moshaf.name}=(${moshaf.id}: ${moshaf.name})" +
                       ")"
            }
        }

        fun queueDownloads(context: Context, reciter: Reciter, moshaf: Moshaf, surahs: List<Surah>) = surahs.forEach { surah ->
            val downloadRequest = getDownloadRequest(reciter, moshaf, surah) ?: return@forEach
            val downloadRequestId = DownloadRequestId(reciter, moshaf)

            val surahPath = getSurahPath(context, downloadRequest)

            when {
                surahPath.exists() -> {
                    Timber.debug("Surah (${surah.id}: ${surah.name}) already downloaded.")

                    val download = Download(
                            downloadRequest,
                            DownloadState.State.COMPLETED.code,
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            surahPath.length(),
                            DownloadState.State.STOPPED.code,
                            Download.FAILURE_REASON_NONE,
                            DownloadProgress().apply {
                                bytesDownloaded = surahPath.length()
                                percentDownloaded = 100f
                            }
                    )

                    completedDownloads[downloadRequestId]?.add(download) ?: run {
                        completedDownloads[downloadRequestId] = mutableListOf(download)
                    }

                    updateDownloadState(download)
                }

                else               -> {
                    Timber.debug("Queueing ${downloadRequest.serializedData}...")

                    addDownload(context, downloadRequest)
                }
            }
        }

        fun addDownload(context: Context, downloadRequest: DownloadRequest) {
            sendAddDownload(
                    context,
                    QuranDownloadService::class.java,
                    downloadRequest,
                    true
            )
        }

        fun pauseDownloads(context: Context, reciter: Reciter, moshaf: Moshaf, surahs: List<Surah>) = surahs.forEach { surah ->
            val downloadRequest = getDownloadRequest(reciter, moshaf, surah) ?: return@forEach

            pauseDownload(context, downloadRequest)
        }

        fun pauseDownload(context: Context, downloadRequest: DownloadRequest) {
            sendSetStopReason(
                    context,
                    QuranDownloadService::class.java,
                    downloadRequest.id,
                    Download.STOP_REASON_NONE + 1, // Any non-zero value pauses the download
                    true
            )
        }

        fun resumeDownloads(context: Context, reciter: Reciter, moshaf: Moshaf, surahs: List<Surah>) = surahs.forEach { surah ->
            val downloadRequest = getDownloadRequest(reciter, moshaf, surah) ?: return@forEach
            val downloadRequestId = DownloadRequestId(reciter, moshaf)

            Timber.debug("Resuming ${downloadRequest.id}...")
            val surahDownload = completedDownloads[downloadRequestId]?.find { it.request.serializedData.surah.id == surah.id }

            surahDownload?.let {
                Timber.debug("Download ${it.request.id} already completed, updating state...")
                updateDownloadState(it)
            }

            resumeDownload(context, downloadRequest)
        }

        fun resumeDownload(context: Context, downloadRequest: DownloadRequest) {
            sendSetStopReason(
                    context,
                    QuranDownloadService::class.java,
                    downloadRequest.id,
                    Download.STOP_REASON_NONE, // Zero value resumes the download
                    true
            )
        }

        fun removeDownloads(context: Context) {
            completedDownloads.clear()

            sendRemoveAllDownloads(
                    context,
                    QuranDownloadService::class.java,
                    true
            )
        }

        private fun getCacheKey(reciter: Reciter?, moshaf: Moshaf?, surah: Surah?) =
                "reciter_#${reciter?.id?.value}_moshaf_#${moshaf?.id}_surah_#${surah?.id.toString().padStart(3, '0')}".asCacheKey

        private fun getSurahPath(context: Context, downloadRequest: DownloadRequest): File {
            val (reciter, moshaf, surah) = downloadRequest.serializedData
            val surahNum = surah.id.toString().padStart(3, '0')
            val baseDestination = File(context.filesDir, "downloads")
            val surahPath = File(baseDestination, "reciter_#${reciter.id.value}/moshaf_#${moshaf.id}/surah_#$surahNum.mp3")
            return surahPath
        }

        private fun getDownloadRequest(reciter: Reciter, moshaf: Moshaf, surah: Surah): DownloadRequest? {
            val uri = surah.url?.toUri() ?: return null
            val cacheKey = getCacheKey(reciter, moshaf, surah)

            val downloadRequestData = DownloadRequestData(reciter, moshaf, surah)
            val downloadRequest = DownloadRequest.Builder(cacheKey.value, uri).run {
                setCustomCacheKey(cacheKey.value)
                setData(downloadRequestData.asJsonString.toByteArray(Charsets.UTF_8))
                build()
            }

            return downloadRequest
        }

        private fun updateDownloadState(download: Download) = DownloadState(
                data = download.request.serializedData,
                state = DownloadState.State.fromCode(download.state),
                percentage = download.percentDownloaded.coerceIn(0f, 100f),
                downloaded = download.bytesDownloaded.coerceIn(0L, Long.MAX_VALUE),
                total = download.contentLength.coerceIn(0L, Long.MAX_VALUE),
                failureReason = DownloadState.FailureReason.fromCode(download.failureReason)
        ).run {
            notifyDownloadServiceObservers(this)

            Timber.debug("$this")
        }

        private fun notifyDownloadServiceObservers(state: DownloadState) {
            Timber.debug("notifying observers with status $state...")

            downloadServiceObservers.forEach { observer ->
                val iObservableClassName = IObservable::class.simpleName
                val observerClassName = observer::class.simpleName
                val observerClassHashCode = observer.hashCode().toString(16).uppercase()

                Timber.debug("notifying $iObservableClassName<$observerClassName@$observerClassHashCode>...")

                observer.onDownloadStateChanged(state)

                Timber.debug("$iObservableClassName<$observerClassName@$observerClassHashCode> notified!")
            }

            Timber.debug("observers notified!")
        }
    }

    data class DownloadState(
            val data: DownloadRequestData,
            val state: State,
            val percentage: Float,
            val downloaded: Long,
            val total: Long,
            val failureReason: FailureReason = FailureReason.fromCode(Download.FAILURE_REASON_NONE)
    ) : Serializable {

        enum class State(val code: Int) : Serializable {
            DOWNLOADING(Download.STATE_DOWNLOADING),
            COMPLETED(Download.STATE_COMPLETED),
            FAILED(Download.STATE_FAILED),
            QUEUED(Download.STATE_QUEUED),
            REMOVING(Download.STATE_REMOVING),
            STOPPED(Download.STATE_STOPPED);

            companion object {

                fun fromCode(code: Int) = entries.first { it.code == code }
            }
        }

        enum class FailureReason(val code: Int) : Serializable {
            NONE(Download.FAILURE_REASON_NONE),
            UNKNOWN(Download.FAILURE_REASON_UNKNOWN);

            companion object {

                fun fromCode(code: Int) = entries.first { it.code == code }
            }
        }

        override fun toString(): String {
            return "${this::class.java.simpleName}(" +
                   "${::data.name}=${data}, " +
                   "${::state.name}=${state.name}, " +
                   "${::percentage.name}=${String.format(Locale.ENGLISH, "%06.2f", percentage)}%, " +
                   "${::downloaded.name}=${downloaded.asHumanReadableSize}, " +
                   "${::total.name}=${total.asHumanReadableSize}, " +
                   "${::failureReason.name}=${failureReason.name}" +
                   ")"
        }
    }

    private val serviceJob = Job()

    private val serviceScope by lazy { CoroutineScope(Dispatchers.Main + serviceJob) }

    private var downloadMonitorJob: Job? = null

    private lateinit var notificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = DownloadNotificationHelper(this, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        downloadManager.addListener(this)
        startDownloadMonitor()
    }

    override fun onDestroy() {
        downloadMonitorJob?.cancel()
        downloadManager.removeListener(this)
        super.onDestroy()
    }

    override fun getDownloadManager() = downloadManagerInstance ?: run {
        val downloadIndex = DefaultDownloadIndex(StandaloneDatabaseProvider(this))

        DownloadManager(
                this,
                downloadIndex,
                DefaultDownloaderFactory(downloadsCacheKey.cacheDataSourceFactory, Runnable::run)
        ).apply {
            maxParallelDownloads = 1
            minRetryCount = 3
        }.also {
            downloadManagerInstance = it
        }
    }

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int) = notificationHelper.buildProgressNotification(
            this,
            R.drawable.quran_icon_monochrome_white_64,
            null,
            null,
            downloads,
            notMetRequirements
    )

    private fun startDownloadMonitor() {
        downloadMonitorJob?.cancel()

        downloadMonitorJob = serviceScope.launch {
            do {
                downloadManager.currentDownloads.forEach { download ->
                    if (download.state == DownloadState.State.QUEUED.code || download.state == DownloadState.State.REMOVING.code) return@forEach
                    updateDownloadState(download)
                }

                delay(UPDATE_PROGRESS_INTERVAL.milliseconds)
            } while (true)
        }
    }

    override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
        when (finalException) {
            null if (download.state == Download.STATE_COMPLETED) -> {
                val downloadRequestData = download.request.serializedData
                val downloadRequestId = DownloadRequestId(downloadRequestData.reciter, downloadRequestData.moshaf)

                updateDownloadState(download)
                moveCompletedDownload(download)

                completedDownloads[downloadRequestId]?.add(download) ?: run {
                    completedDownloads[downloadRequestId] = mutableListOf(download)
                }
            }

            null                                                 -> Unit
            else                                                 -> Timber.error("Download failed for ${download.request.id}: ${finalException.message}")
        }
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
        val downloadRequestData = download.request.serializedData
        val downloadRequestId = DownloadRequestId(downloadRequestData.reciter, downloadRequestData.moshaf)

        completedDownloads[downloadRequestId]?.remove(download)

        Timber.debug("Download removed: ${download.request.id}")
    }

    private fun moveCompletedDownload(download: Download) {
        val cacheContents = downloadsCacheKey.keyContents(download.request.id)

        if (cacheContents == null) return

        val surahPath = getSurahPath(this@QuranDownloadService, download.request)
        if (surahPath.parentFile?.exists() != true) surahPath.parentFile?.mkdirs()

        surahPath.writeBytes(cacheContents)

        Timber.debug("Surah downloaded. Path: $surahPath, Size: ${surahPath.length().asHumanReadableSize}")

        downloadsCacheKey.deleteKey(download.request.id)
        downloadManager.removeDownload(download.request.id)
    }

    fun interface DownloadStatusObserver : IObservable {

        fun onDownloadStateChanged(downloadState: DownloadState)
    }
}
