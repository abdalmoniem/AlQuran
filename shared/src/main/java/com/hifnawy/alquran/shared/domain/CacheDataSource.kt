package com.hifnawy.alquran.shared.domain

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.hifnawy.alquran.shared.domain.CacheDataSource.cacheDataSourceFactory
import com.hifnawy.alquran.shared.domain.CacheDataSource.releaseCache
import com.hifnawy.alquran.shared.domain.CacheDataSource.simpleCache
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import timber.log.Timber
import java.io.File

/**
 * A singleton object responsible for managing media caching for ExoPlayer/Media3.
 *
 * This object provides a centralized way to create and manage a [SimpleCache] instance and
 * a [CacheDataSource.Factory]. It ensures that only one instance of the cache and data source
 * factory is created throughout the application's lifecycle. The cache is configured to have
 * a maximum size of 3GB and uses a [LeastRecentlyUsedCacheEvictor] policy.
 *
 * It also offers utility functions to check the cache status for a specific media item and
 * to release the cache resources when no longer needed.
 *
 * Usage:
 * - Access the [CacheDataSource.Factory] via the [Context.cacheDataSourceFactory] extension property.
 * - Release the cache using [releaseCache()].
 * - Get information about a cached item using [Context.getCacheInfo(cacheKey)].
 *
 * All methods are marked with @[UnstableApi] as they rely on Media3 components that are not yet
 * considered stable.
 *
 * @author AbdElMoniem ElHifnawy
 */
@UnstableApi
object CacheDataSource {

    /**
     * A data class that encapsulates information about a cached media item.
     *
     * This class provides a structured way to represent the status of a specific item
     * within the [SimpleCache].
     *
     * @property cacheKey [String] The unique key identifying the cached content. This is typically the URL of the media.
     * @property isCached [Boolean] A boolean flag indicating whether the content associated with the [cacheKey] is present in the cache.
     * @property cacheSize [Long] The size of the cached content in bytes. Will be 0 if not cached.
     * @property cacheSizeHumanReadable [String] A user-friendly string representation of the [cacheSize] (e.g., "1.23 MB").
     */
    data class CacheInfo(val cacheKey: String, val isCached: Boolean, val cacheSize: Long, val cacheSizeHumanReadable: String)

    /**
     * A singleton instance of [SimpleCache] used for caching media data.
     * This property is lazily initialized by the [simpleCache] extension property on [Context]
     * to ensure a single cache instance is used throughout the application. It is set to `null`
     * when [releaseCache] is called.
     *
     * @return [SimpleCache?][SimpleCache] a [SimpleCache] object to be used for caching media data
     */
    private var mediaCache: SimpleCache? = null

    /**
     * A singleton instance of [CacheDataSource.Factory] used to create [CacheDataSource] instances.
     * This property is lazily initialized by the [cacheDataSourceFactory] extension property on [Context]
     * to ensure a single factory instance is used throughout the application. It is set to `null`
     * when [releaseCache] is called.
     *
     * @return [CacheDataSource.Factory?][CacheDataSource.Factory] a factory for creating [CacheDataSource] instances.
     */
    private var mediaCacheDataSourceFactory: CacheDataSource.Factory? = null

    /**
     * An extension property on [Context] that provides a singleton instance of [CacheDataSource.Factory].
     *
     * This factory is responsible for creating [CacheDataSource] instances that read from and write to
     * the application's shared cache. It is lazily initialized to ensure that the underlying
     * [SimpleCache] and data source factories are created only once.
     *
     * The factory is configured with the following:
     * - A [SimpleCache] instance provided by [simpleCache].
     * - An upstream data source factory that handles network requests ([DefaultHttpDataSource]).
     * - A data sink for writing data to the cache.
     * - A data source for reading data directly from the cache ([FileDataSource]).
     * - Flags to control behavior, such as ignoring the cache on error and blocking reads until data is cached.
     *
     * This property ensures that all media playback components in the app use the same caching mechanism,
     * promoting efficient resource usage.
     *
     * @return [CacheDataSource.Factory] a pre-configured factory for creating cache-aware data sources.
     *
     * @see simpleCache
     */
    val Context.cacheDataSourceFactory
        get() = mediaCacheDataSourceFactory ?: run {
            val cache = simpleCache
            val upstreamFactory = DefaultDataSource.Factory(this, DefaultHttpDataSource.Factory())

            CacheDataSource.Factory().apply {
                setCache(cache)
                setUpstreamDataSourceFactory(upstreamFactory)
                setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
                setCacheReadDataSourceFactory(FileDataSource.Factory())
                setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or CacheDataSource.FLAG_BLOCK_ON_CACHE)
            }.also {
                mediaCacheDataSourceFactory = it
            }
        }

    /**
     * An extension property on [Context] that provides a singleton instance of [SimpleCache].
     *
     * This property lazily initializes and returns a single [SimpleCache] instance for the entire
     * application. It ensures that all parts of the app use the same cache, preventing redundant
     * data storage and management.
     *
     * The cache is configured as follows:
     * - **Location**: A subdirectory named `exoplayer_cache` within the application's standard cache directory.
     * - **Eviction Policy**: A [LeastRecentlyUsedCacheEvictor] is used, meaning the oldest, least-used
     *   files are removed first when the cache size limit is reached.
     * - **Maximum Size**: The cache is configured with a maximum size of `300 MB`.
     * - **Database**: A [StandaloneDatabaseProvider] is used to manage the cache's metadata.
     *
     * The initialization is synchronized to ensure thread safety during the first access.
     *
     * @return [SimpleCache] The singleton [SimpleCache] instance for the application.
     */
    private val Context.simpleCache
        @Synchronized
        get() = mediaCache ?: run {
            val cacheDir = File(cacheDir, "exoplayer_cache")

            if (!cacheDir.exists()) cacheDir.mkdirs()

            Timber.debug("Cache directory: ${cacheDir.absolutePath}, Cache exists: ${cacheDir.exists()}")

            val cacheEvictor = LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024) // 300 MB
            val databaseProvider = StandaloneDatabaseProvider(this)

            SimpleCache(cacheDir, cacheEvictor, databaseProvider).also {
                mediaCache = it
                Timber.debug("Cache initialized. Cached bytes: ${it.cacheSpace.asHumanReadableSize}")
            }
        }

    /**
     * Retrieves caching information for a specific media item identified by its cache key.
     *
     * This function queries the [SimpleCache] to determine if a media item (usually identified by its URL)
     * is cached, and if so, how much of it is stored. It accesses the singleton cache instance
     * via the [simpleCache] property.
     *
     * It calculates the total size of all cached segments ([CacheSpan][androidx.media3.datasource.cache.CacheSpan]) for the given key.
     * The item is considered cached if there is at least one cached segment with a length greater than zero.
     *
     * In case of any error during the cache lookup (e.g., I/O exceptions), it logs the error
     * and returns a [CacheInfo] object indicating that the item is not cached.
     *
     * @receiver [Context] The context used to access the application's cache.
     *
     * @param cacheKey [String] The unique key for the content to check.
     *
     * @return [CacheInfo] A [CacheInfo] data class instance containing:
     * - [CacheInfo.cacheKey]: The original [cacheKey].
     * - [CacheInfo.isCached]: `true` if any part of the content is cached, `false` otherwise.
     * - [CacheInfo.cacheSize]: The total number of bytes cached for this key.
     * - [CacheInfo.cacheSizeHumanReadable]: A user-friendly string representation of the cache size.
     *
     * or a default "not cached" [CacheInfo] object on error.
     */
    fun Context.getCacheInfo(cacheKey: String) = try {
        val cache = simpleCache
        val cachedSpans = cache.getCachedSpans(cacheKey)
        val totalBytes = cachedSpans.sumOf { it.length }
        val isCached = cachedSpans.isNotEmpty() && totalBytes > 0

        val cacheInfo = CacheInfo(cacheKey = cacheKey, isCached = isCached, cacheSize = totalBytes, cacheSizeHumanReadable = totalBytes.asHumanReadableSize)
        Timber.debug("$cacheInfo")

        cacheInfo
    } catch (ex: Exception) {
        Timber.error("Error checking cache: ${ex.message}")
        CacheInfo(cacheKey, false, -1, (-1L).asHumanReadableSize)
    }

    /**
     * Releases the resources held by the [SimpleCache] and resets the singleton instances.
     *
     * This function should be called when the cache is no longer needed, typically when the
     * application is shutting down or during a process that requires clearing all media-related
     * resources.
     *
     * It performs the following actions:
     * - Logs the current size of the cache before release.
     * - Calls the [SimpleCache.release] method to close the cache and release any file locks
     *   and other resources.
     * - Sets the internal `mediaCache` and `mediaCacheDataSourceFactory` properties to `null`.
     *   This ensures that any subsequent access to [cacheDataSourceFactory] or [simpleCache]
     *   will re-initialize the cache from scratch.
     *
     * Any exceptions thrown during the release process are caught and logged to prevent
     * the application from crashing.
     */
    fun releaseCache() = try {
        val totalBytes = mediaCache?.cacheSpace ?: 0

        Timber.debug("Releasing cache. Current size: ${totalBytes.asHumanReadableSize}")
        mediaCache?.release()
        mediaCache = null
        mediaCacheDataSourceFactory = null
    } catch (ex: Exception) {
        Timber.error("Error releasing cache, ${ex.message}")
    }
}
