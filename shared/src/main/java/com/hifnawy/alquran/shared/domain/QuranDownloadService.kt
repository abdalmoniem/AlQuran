package com.hifnawy.alquran.shared.domain

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadProgress
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.offline.DownloaderFactory
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import com.google.gson.Gson
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.CacheKey.Companion.asCacheKey
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheDataSourceFactory
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.moveContentsTo
import com.hifnawy.alquran.shared.domain.QuranDownloadService.QuranDownloadManager.DownloadState.State.Companion.isTerminal
import com.hifnawy.alquran.shared.model.Moshaf
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import com.hifnawy.alquran.shared.utils.SerializableExt.Companion.asJsonString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        R.string.download_channel_name,
        R.string.download_channel_description
), DownloadManager.Listener {

    private lateinit var notificationHelper: DownloadNotificationHelper

    override fun onCreate() {
        notificationHelper = DownloadNotificationHelper(this@QuranDownloadService, DOWNLOAD_NOTIFICATION_CHANNEL_ID)

        downloadManager.addListener(this@QuranDownloadService)
        startDownloadMonitor()

        super.onCreate()
    }

    override fun onDestroy() {
        downloadManager.removeListener(this@QuranDownloadService)
        downloadMonitorJob?.cancel()

        super.onDestroy()
    }

    override fun getDownloadManager() = downloadManagerInstance ?: run {
        val downloadIndex = DefaultDownloadIndex(StandaloneDatabaseProvider(this@QuranDownloadService))

        val downloaderFactory = DownloaderFactory { request ->
            val mediaItem = MediaItem.Builder().run {
                setUri(request.uri)
                setCustomCacheKey(request.customCacheKey)
                build()
            }
            val cacheDataSourceFactory = request.id.asCacheKey.cacheDataSourceFactory
            val executor = Runnable::run
            val position = request.byteRange?.offset ?: 0L
            val length = request.byteRange?.length ?: -1L

            ProgressiveDownloader(mediaItem, cacheDataSourceFactory, executor, position, length)
        }

        DownloadManager(this@QuranDownloadService, downloadIndex, downloaderFactory).apply {
            maxParallelDownloads = 1
            minRetryCount = 3

            downloadManagerInstance = this@apply
        }
    }

    override fun getScheduler() = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int) = notificationHelper.run {
        val isActive = downloads.firstOrNull { it.state == Download.STATE_DOWNLOADING }
        val isQueued = downloads.any { it.state == Download.STATE_QUEUED }

        val message = when {
            isActive != null -> {
                val data = isActive.request.serializedData
                val progress = isActive.percentDownloaded

                getString(R.string.downloading_surah_detailed, data.surah.id, data.surah.name, data.reciter.name, data.moshaf.name, progress)
            }

            isQueued         -> {
                val queuedCount = downloads.count { it.state == Download.STATE_QUEUED }
                val nextDownload = downloads.firstOrNull { it.state == Download.STATE_QUEUED }

                when (nextDownload) {
                    null -> getString(R.string.preparing_downloads_count, queuedCount)

                    else -> {
                        val data = nextDownload.request.serializedData
                        getString(R.string.preparing_surah, data.surah.id, data.surah.name, queuedCount)
                    }
                }
            }

            else             -> getString(R.string.preparing_downloads)
        }

        buildProgressNotification(
                this@QuranDownloadService,
                R.drawable.quran_icon_monochrome_white_64,
                null,
                message,
                downloads,
                notMetRequirements
        )
    }

    private fun startDownloadMonitor() = downloadMonitorJob.let { job ->
        job?.cancel()

        ioCoroutineScope.launch {
            do {
                downloadManager.currentDownloads.run {
                    val activeDownloads = filter { it.state == DownloadState.State.DOWNLOADING.code }
                    val terminalDownloads = filter { DownloadState.State.fromCode(it.state).isTerminal }
                    val downloads = activeDownloads + terminalDownloads

                    withContext(Dispatchers.Main) { downloads.forEach { updateDownloadState(it) } }
                }
            } while (true)
        }.also { downloadMonitorJob = it }
    }

    override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) = when (finalException) {
        null if (download.state == Download.STATE_COMPLETED) -> download.request.serializedData.let { serializedData ->
            val downloadRequestId = DownloadRequestId(serializedData.reciter, serializedData.moshaf)

            moveCompletedDownload(download)

            completedDownloads[downloadRequestId]?.run {
                add(size, download)
            } ?: run {
                completedDownloads[downloadRequestId] = mutableListOf(download)
            }
        }

        null                                                 -> Unit
        else                                                 -> Timber.error("Download failed for '${download.request.id}': ${finalException.message}")
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) = download.run {
        val serializedData = request.serializedData
        val downloadRequestId = DownloadRequestId(serializedData.reciter, serializedData.moshaf)

        completedDownloads[downloadRequestId]?.remove(this@run)

        Timber.debug("Download '${request.id}' removed!")
    }

    private fun moveCompletedDownload(download: Download) = download.run {
        val cacheKey = request.id.asCacheKey
        val bytesWritten = cacheKey.moveContentsTo(request.downloadPath)

        when {
            bytesWritten != contentLength -> Timber.error("Failed to move download '${request.id}' to '${request.downloadPath}'")
            else                          -> Timber.debug("Download '${request.id}' (${bytesWritten.asHumanReadableSize}) moved to '${request.downloadPath}'")
        }

        downloadManager.removeDownload(request.id)
    }

    companion object QuranDownloadManager {

        val downloadStatusObservers = mutableListOf<DownloadStatusObserver>()

        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "Quran Downloads"
        private const val UPDATE_PROGRESS_INTERVAL = 100L

        private val ioCoroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }
        private val mainCoroutineScope by lazy { CoroutineScope(Dispatchers.Main + Job()) }
        private val completedDownloads = mutableMapOf<DownloadRequestId, MutableList<Download>>()

        private val ByteArray.asUTF8String
            get() = String(this@asUTF8String, Charsets.UTF_8)

        private val DownloadRequestData.cacheKey
            get() = "reciter_#${reciter.id.value}_moshaf_#${moshaf.id}_surah_#${surah.id.toString().padStart(3, '0')}".asCacheKey

        private val DownloadRequestData.downloadRequest
            get() = surah.url?.toUri()?.let { uri ->
                DownloadRequest.Builder(cacheKey.value, uri).run {
                    setCustomCacheKey(cacheKey.value)
                    setData(asJsonString.toByteArray(Charsets.UTF_8))
                    build()
                }
            }

        private val DownloadRequest.serializedData
            get() = Gson().fromJson(data.asUTF8String, DownloadRequestData::class.java)

        context(downloadRequest: DownloadRequest)
        private val File.completedDownload
            get() = Download(
                    downloadRequest,
                    DownloadState.State.COMPLETED.code,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    length(),
                    DownloadState.State.STOPPED.code,
                    Download.FAILURE_REASON_NONE,
                    DownloadProgress().apply {
                        bytesDownloaded = length()
                        percentDownloaded = 100f
                    }
            )

        context(context: Context)
        private val DownloadRequest.downloadPath
            get() = serializedData.let { (reciter, moshaf, surah) ->
                val surahNum = surah.id.toString().padStart(3, '0')
                val baseDestination = File(context.filesDir, "downloads")

                File(baseDestination, "reciter_#${reciter.id.value}/moshaf_#${moshaf.id}/surah_#$surahNum.mp3")
            }

        private var downloadJob: Job? = null
        private var downloadMonitorJob: Job? = null
        private var downloadManagerInstance: DownloadManager? = null

        fun interface DownloadStatusObserver : IObservable {

            suspend fun onDownloadStateChanged(downloadState: DownloadState)
        }

        data class BulkDownloadRequest(val reciter: Reciter, val moshaf: Moshaf, val surahs: List<Surah>) : Serializable {

            override fun toString() = "${this::class.java.simpleName}(" +
                                      "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                      "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                                      "${::surahs.name}=<${surahs.size}>" +
                                      surahs.joinToString(prefix = "[", postfix = "]", separator = ", ") { "(${it.id}: ${it.name})" } +
                                      ")"
        }

        data class DownloadState(
                val data: DownloadRequestData? = null,
                val state: State = State.STOPPED,
                val percentage: Float = 0f,
                val downloaded: Long = 0L,
                val total: Long = 0L,
                val failureReason: FailureReason = FailureReason.fromCode(Download.FAILURE_REASON_NONE)
        ) : Serializable {

            enum class State(val code: Int) : Serializable {
                QUEUED(Download.STATE_QUEUED),
                STOPPED(Download.STATE_STOPPED),
                COMPLETED(Download.STATE_COMPLETED),
                FAILED(Download.STATE_FAILED),
                DOWNLOADING(Download.STATE_DOWNLOADING),
                REMOVING(Download.STATE_REMOVING);

                companion object {

                    val State.isTerminal get() = this == COMPLETED || this == FAILED

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

            override fun toString() = "${this::class.java.simpleName}(" +
                                      "${::data.name}=${data}, " +
                                      "${::state.name}=${state.name}, " +
                                      "${::percentage.name}=${String.format(Locale.ENGLISH, "%06.2f", percentage)}%, " +
                                      "${::downloaded.name}=${downloaded.asHumanReadableSize}, " +
                                      "${::total.name}=${total.asHumanReadableSize}, " +
                                      "${::failureReason.name}=${failureReason.name}" +
                                      ")"
        }

        data class DownloadRequestData(val reciter: Reciter, val moshaf: Moshaf, val surah: Surah) : Serializable {

            override fun toString() = "${this::class.java.simpleName}(" +
                                      "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                      "${::moshaf.name}=(${moshaf.id}: ${moshaf.name}), " +
                                      "${::surah.name}=(${surah.id}: ${surah.name})" +
                                      ")"
        }

        private data class DownloadRequestId(val reciter: Reciter, val moshaf: Moshaf) : Serializable {

            override fun toString() = "${this::class.java.simpleName}(" +
                                      "${::reciter.name}=(${reciter.id.value}: ${reciter.name}), " +
                                      "${::moshaf.name}=(${moshaf.id}: ${moshaf.name})" +
                                      ")"
        }

        fun queueDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.surahs.map { surah ->
            val downloadRequest = with(bulkDownloadRequest) { DownloadRequestData(reciter, moshaf, surah).downloadRequest } ?: return
            val downloadPath = with(context) { downloadRequest.downloadPath }

            when {
                downloadPath.exists() -> downloadRequest to downloadPath
                else                  -> downloadRequest to null
            }
        }.run {
            val queuedDownloads = filter { (_, downloadPath) -> downloadPath == null }

            @Suppress("UNCHECKED_CAST")
            val finishedDownloads = filter { (_, downloadPath) -> downloadPath != null } as List<Pair<DownloadRequest, File>>

            mainCoroutineScope.launch {
                finishedDownloads.forEach { (downloadRequest, downloadPath) ->
                    val surah = downloadRequest.serializedData.surah
                    val download = with(downloadRequest) { downloadPath.completedDownload }
                    val downloadRequestId = with(bulkDownloadRequest) { DownloadRequestId(reciter, moshaf) }

                    Timber.debug("Queued Download '${downloadRequest.id}': (${surah.id}: ${surah.name}) is already ${DownloadState.State.COMPLETED}!")

                    completedDownloads[downloadRequestId]?.run {
                        add(size, download)
                    } ?: run {
                        completedDownloads[downloadRequestId] = mutableListOf(download)
                    }

                    updateDownloadState(download)
                }

                queuedDownloads.forEach { (downloadRequest, _) -> queueDownload(context, downloadRequest) }

                Timber.debug("${queuedDownloads.size} Downloads Queued!")
            }.also { downloadJob = it }.run { Timber.debug("Queueing ${queuedDownloads.size} Downloads...") }
        }

        fun resumeDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.surahs.map { surah ->
            val downloadRequest = with(bulkDownloadRequest) { DownloadRequestData(reciter, moshaf, surah).downloadRequest } ?: return
            val downloadPath = with(context) { downloadRequest.downloadPath }

            when {
                downloadPath.exists() -> downloadRequest to downloadPath
                else                  -> downloadRequest to null
            }
        }.run {
            val queuedDownloads = filter { (_, downloadPath) -> downloadPath == null }

            @Suppress("UNCHECKED_CAST")
            val finishedDownloads = filter { (_, downloadPath) -> downloadPath != null } as List<Pair<DownloadRequest, File>>

            mainCoroutineScope.launch {
                finishedDownloads.forEach { (downloadRequest, downloadPath) ->
                    val surah = downloadRequest.serializedData.surah
                    val download = with(downloadRequest) { downloadPath.completedDownload }
                    val downloadRequestId = with(bulkDownloadRequest) { DownloadRequestId(reciter, moshaf) }

                    Timber.debug("Resumed Download '$downloadRequestId': (${surah.id}: ${surah.name}) is already ${DownloadState.State.COMPLETED}!")

                    updateDownloadState(download)
                }

                queuedDownloads.forEach { (downloadRequest, _) -> resumeDownload(context, downloadRequest) }

                Timber.debug("${queuedDownloads.size} Downloads Resumed!")
            }.also { downloadJob = it }.run { Timber.debug("Resuming ${queuedDownloads.size} Downloads...") }
        }

        fun pauseDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.run {
            downloadJob?.cancel()
            downloadMonitorJob?.cancel()

            val downloadRequestId = DownloadRequestId(reciter, moshaf)

            ioCoroutineScope.launch {
                surahs.forEach { surah ->
                    val download = downloadManagerInstance?.currentDownloads?.find { it.request.serializedData.surah.id == surah.id }

                    download?.let {
                        Timber.debug("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is ${DownloadState.State.fromCode(it.state)}!")

                        pauseDownload(context, it.request)
                    } ?: Timber.warn("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is non-existent!!!")
                }
            }
        }

        fun removeDownloads(context: Context, bulkDownloadRequest: BulkDownloadRequest) = bulkDownloadRequest.run {
            downloadJob?.cancel()
            downloadMonitorJob?.cancel()

            val downloadRequestId = DownloadRequestId(reciter, moshaf)

            ioCoroutineScope.launch {
                surahs.forEach { surah ->
                    val download = downloadManagerInstance?.currentDownloads?.find { it.request.serializedData.surah.id == surah.id }

                    download?.let {
                        Timber.debug("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is ${DownloadState.State.fromCode(it.state)}!")

                        completedDownloads[downloadRequestId]?.remove(it)

                        removeDownload(context, it.request)
                    } ?: Timber.warn("Download '$downloadRequestId': (${surah.id}: ${surah.name}) is non-existent!!!")
                }
            }
        }

        fun queueDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
            val serializedData = downloadRequest.serializedData
            Timber.debug("Queueing download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

            sendAddDownload(
                    context,
                    QuranDownloadService::class.java,
                    this@run,
                    true
            )
        }

        fun resumeDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
            val serializedData = downloadRequest.serializedData
            Timber.debug("Resuming download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

            sendSetStopReason(
                    context,
                    QuranDownloadService::class.java,
                    downloadRequest.id,
                    Download.STOP_REASON_NONE, // Zero value resumes the download
                    true
            )
        }

        fun pauseDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
            val serializedData = downloadRequest.serializedData
            Timber.debug("Pausing download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

            sendSetStopReason(
                    context,
                    QuranDownloadService::class.java,
                    downloadRequest.id,
                    Download.STOP_REASON_NONE + 1, // Any non-zero value pauses the download
                    true
            )
        }

        fun removeDownload(context: Context, downloadRequest: DownloadRequest) = downloadRequest.run {
            val serializedData = downloadRequest.serializedData
            Timber.debug("Removing download '$id': (${serializedData.surah.id}: ${serializedData.surah.name})...")

            sendRemoveDownload(
                    context,
                    QuranDownloadService::class.java,
                    downloadRequest.id,
                    true
            )
        }

        private suspend fun updateDownloadState(download: Download) = download.run {
            val state = DownloadState(
                    data = request.serializedData,
                    state = DownloadState.State.fromCode(state),
                    percentage = percentDownloaded.coerceIn(0f, 100f),
                    downloaded = bytesDownloaded.coerceIn(0L, Long.MAX_VALUE),
                    total = contentLength.coerceIn(0L, Long.MAX_VALUE),
                    failureReason = DownloadState.FailureReason.fromCode(failureReason)
            )

            Timber.debug("$state")

            notifyDownloadStatusObservers(state = state)
            delay(UPDATE_PROGRESS_INTERVAL.milliseconds)
        }

        private suspend fun notifyDownloadStatusObservers(state: DownloadState) = downloadStatusObservers.forEach { observer -> observer.onDownloadStateChanged(state) }
    }
}
