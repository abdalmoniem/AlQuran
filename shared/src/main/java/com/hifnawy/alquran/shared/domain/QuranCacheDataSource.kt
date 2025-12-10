package com.hifnawy.alquran.shared.domain

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheDataSourceFactory
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheFactoryMap
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheInfo
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheLock
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.cacheMap
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.releaseCache
import com.hifnawy.alquran.shared.domain.QuranCacheDataSource.simpleCache
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import com.hifnawy.alquran.shared.utils.LongEx.asHumanReadableSize
import com.hifnawy.alquran.shared.utils.NumberExt.MB
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * A singleton object responsible for managing media caching for ExoPlayer/Media3.
 *
 * This object provides a centralized way to create and manage a [SimpleCache] instance and
 * a [CacheDataSource.Factory]. It ensures that only one instance of the cache and data source
 * factory is created throughout the application's lifecycle. The cache is configured to have
 * a maximum size of `300MB` per item and uses a [LeastRecentlyUsedCacheEvictor] policy.
 *
 * It also offers utility functions to check the cache status for a specific media item and
 * to release the cache resources when no longer needed.
 *
 * Usage:
 * - Access the [CacheDataSource.Factory] via the [cacheDataSourceFactory] extension property.
 * - Release the cache using [releaseCache].
 * - Get information about a cached item using [cacheInfo].
 *
 * All methods are marked with @[UnstableApi] as they rely on Media3 components that are not yet
 * considered stable.
 *
 * @author AbdElMoniem ElHifnawy
 */
@UnstableApi
object QuranCacheDataSource {

    /**
     * A type-safe identifier for a [SimpleCache].
     *
     * This value class wraps a primitive [String] to prevent accidental misuse of raw strings
     * where a specific [SimpleCache] ID is expected, enhancing compile-time safety.
     *
     * @property value [Int] The raw integer ID of the reciter.
     *
     * @author AbdElMoniem ElHifnawy
     */
    @JvmInline
    value class CacheKey(val value: String) {

        companion object {

            /**
             * An extension property that converts a [String] into a type-safe [CacheKey].
             *
             * This utility provides a convenient and readable way to create a [CacheKey] instance
             * from a raw string, promoting better type safety throughout the codebase.
             *
             * Example:
             * ```
             * val url = "http://example.com/audio.mp3"
             * val cacheKey = url.asCacheKey // Creates a CacheKey("http://example.com/audio.mp3")
             * ```
             *
             * @return [CacheKey] A type-safe cache key wrapping the original string.
             */
            val String.asCacheKey get() = CacheKey(this)
        }
    }

    /**
     * A data class that encapsulates information about a cached media item.
     *
     * This class provides a structured way to represent the status of a specific item
     * within the [SimpleCache].
     *
     * @property cacheKey [CacheKey] The unique key identifying the cached content.
     * @property isCached [Boolean] A boolean flag indicating whether the content associated with the [cacheKey] is present in the cache.
     * @property cacheSize [Long] The size of the cached content in bytes. Will be `0` if not cached.
     * @property cacheSizeHumanReadable [String] A user-friendly string representation of the [cacheSize] (e.g., `1.23 MB`).
     */
    data class CacheInfo(val cacheKey: CacheKey, val isCached: Boolean, val cacheSize: Long, val cacheSizeHumanReadable: String)

    /**
     * A map to store and manage multiple [SimpleCache] instances, keyed by a type-safe [CacheKey].
     *
     * This allows for separate caches for different media sources or categories (e.g., one cache
     * per reciter per moshaf per surah). Each [SimpleCache] instance manages its own cache directory and eviction policy,
     * providing isolation and independent lifecycle management.
     *
     * Access to this map is synchronized via [cacheLock] to ensure thread safety when creating
     * or retrieving cache instances.
     *
     * @return [MutableMap<CacheKey, SimpleCache>][MutableMap] A map of [SimpleCache] instances, keyed by [CacheKey].
     *
     * @see CacheKey.simpleCache
     */
    private val cacheMap = mutableMapOf<CacheKey, SimpleCache>()

    /**
     * A map to store and manage multiple [CacheDataSource.Factory] instances, keyed by a type-safe [CacheKey].
     *
     * This mirrors the structure of [cacheMap], ensuring that each [SimpleCache] instance has a corresponding
     * [CacheDataSource.Factory]. This allows different media sources to use factories configured with their
     * specific cache instances.
     *
     * Access to this map is synchronized via [cacheLock] to ensure thread-safe creation and retrieval
     * of data source factories.
     *
     * @return [MutableMap<CacheKey, CacheDataSource.Factory>][MutableMap] A map of [CacheDataSource.Factory] instances, keyed by [CacheKey].
     *
     * @see CacheKey.cacheDataSourceFactory
     */
    private val cacheFactoryMap = mutableMapOf<CacheKey, CacheDataSource.Factory>()

    /**
     * A lock object used to synchronize access to the [cacheMap] and [cacheFactoryMap].
     *
     * This ensures thread safety when multiple threads attempt to create or retrieve cache instances
     * or data source factories concurrently. By synchronizing on this single object, it prevents race
     * conditions that could lead to creating duplicate instances or other inconsistent states.
     *
     * @return [Any] The lock object used to synchronize access to the [cacheMap] and [cacheFactoryMap].
     */
    private val cacheLock = Any()

    /**
     * An extension property on [CacheKey] that provides a managed [SimpleCache] instance.
     *
     * This property lazily initializes and returns a [SimpleCache] instance specific to the given
     * [CacheKey]. It ensures that each key gets its own isolated cache, preventing data collisions
     * and allowing for independent management.
     *
     * The cache for each key is configured as follows:
     * - **Location**: A subdirectory within the application's standard cache directory, named
     *   `exoplayer_cache/[CacheKey.value]`. This isolates caches for different reciters, for example.
     * - **Eviction Policy**: A [LeastRecentlyUsedCacheEvictor] is used, meaning the oldest, least-used
     *   files are removed first when the cache size limit is reached.
     * - **Maximum Size**: The cache is configured with a maximum size of `300 MB`.
     * - **Database**: A [StandaloneDatabaseProvider] is used to manage the cache's metadata.
     *
     * The initialization is thread-safe, using a [synchronized] block to manage access to the
     * shared [cacheMap]. If a cache for the given key already exists, it is returned; otherwise,
     * a new one is created, stored, and returned.
     *
     * This property is `private` as it's an internal implementation detail used by other public-facing
     * properties like [cacheDataSourceFactory] and [cacheInfo].
     *
     * @receiver [CacheKey] The key for which to get or create a cache instance.
     *
     * @return [SimpleCache] The singleton [SimpleCache] instance for the specific [CacheKey].
     *
     * @see CacheKey
     * @see SimpleCache
     */
    context(context: Context)
    private val CacheKey.simpleCache
        get() = synchronized(cacheLock) {
            cacheMap.getOrPut(this@simpleCache) {
                val cacheDir = File(context.cacheDir, "exoplayer_cache/$value")

                if (!cacheDir.exists()) cacheDir.mkdirs()

                Timber.debug("Cache directory: ${cacheDir.absolutePath}, Cache exists: ${cacheDir.exists()}")

                val cacheEvictor = LeastRecentlyUsedCacheEvictor(300L.MB)
                val databaseProvider = StandaloneDatabaseProvider(context)

                SimpleCache(cacheDir, cacheEvictor, databaseProvider).also {
                    Timber.debug("Cache initialized for key '$value'. Cached bytes: ${it.cacheSpace.asHumanReadableSize}")
                }
            }
        }

    /**
     * An extension property on [CacheKey] that provides a managed, singleton instance of [CacheDataSource.Factory].
     *
     * This factory is responsible for creating [QuranCacheDataSource] instances that read from and write to
     * the application's cache, specific to the receiver [CacheKey]. It is lazily initialized to ensure
     * that the underlying [SimpleCache] and data source factories are created only once per key.
     *
     * The factory is configured with the following:
     * - A [SimpleCache] instance provided by [simpleCache], ensuring each key has an isolated cache.
     * - An upstream data source factory ([DefaultHttpDataSource]) to handle network requests for uncached content.
     * - A data sink for writing downloaded data into the cache.
     * - A data source for reading data directly from the cache ([FileDataSource]).
     * - Flags to control behavior:
     *   - [CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR]: If an error occurs while reading from the cache,
     *     it will fall back to the upstream source.
     *   - [CacheDataSource.FLAG_BLOCK_ON_CACHE]: Playback will wait until the requested data is
     *     downloaded and cached.
     *
     * This property ensures that all media playback components in the app use a consistent and
     * isolated caching mechanism per key, promoting efficient resource usage and data separation.
     *
     * @receiver [CacheKey] The key for which to get or create a data source factory.
     *
     * @return [CacheDataSource.Factory] A pre-configured factory for creating cache-aware data sources for the specific [CacheKey].
     *
     * @see CacheKey.simpleCache
     * @see CacheDataSource.Factory
     */
    context(context: Context)
    val CacheKey.cacheDataSourceFactory
        get() = synchronized(cacheLock) {
            cacheFactoryMap.getOrPut(this@cacheDataSourceFactory) {
                val cache = simpleCache
                val upstreamFactory = DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory())

                CacheDataSource.Factory().apply {
                    setCache(cache)
                    setUpstreamDataSourceFactory(upstreamFactory)
                    setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
                    setCacheReadDataSourceFactory(FileDataSource.Factory())
                    setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR or CacheDataSource.FLAG_BLOCK_ON_CACHE)
                }
            }
        }

    /**
     * Retrieves caching information for a specific media item identified by its cache key.
     *
     * This function queries the [SimpleCache] to determine if a media item is cached, and if so,
     * how much of it is stored. It accesses the singleton cache instance via the [simpleCache] property.
     *
     * It calculates the total size of all cached segments ([CacheSpan]) for the given key.
     * The item is considered cached if there is at least one cached segment with a length greater than zero.
     *
     * In case of any error during the cache lookup (e.g. `I/O exceptions`), it logs the error
     * and returns a [CacheInfo] object indicating that the item is not cached.
     *
     * @receiver [CacheKey] The key for which to get the cache information.
     *
     * @return [CacheInfo] A data class instance containing:
     * - [CacheInfo.cacheKey]: The original [CacheKey].
     * - [CacheInfo.isCached]: `true` if any part of the content is cached, `false` otherwise.
     * - [CacheInfo.cacheSize]: The total number of bytes cached for this key.
     * - [CacheInfo.cacheSizeHumanReadable]: A user-friendly string representation of the cache size.
     *
     * or a default `not cached` [CacheInfo] object on error.
     */
    context(context: Context)
    val CacheKey.cacheInfo
        get() = synchronized(cacheLock) {
            cacheMap[this@cacheInfo]?.run {
                val cachedSpans = getCachedSpans(value)
                val totalBytes = cachedSpans.sumOf { it.length }
                val isCached = cachedSpans.isNotEmpty() && totalBytes > 0

                val cacheInfo = CacheInfo(cacheKey = this@cacheInfo, isCached = isCached, cacheSize = totalBytes, cacheSizeHumanReadable = totalBytes.asHumanReadableSize)
                Timber.debug("$cacheInfo")

                cacheInfo
            } ?: run {
                Timber.warn("Cache for key '${this@cacheInfo.value}' not found!")
                CacheInfo(this@cacheInfo, false, -1, (-1L).asHumanReadableSize)
            }
        }

    /**
     * Retrieves the complete byte content of a cached media item.
     *
     * This function reads all the cached segments ([CacheSpan]) associated with the receiver's [CacheKey]
     * and concatenates their raw byte data into a single [ByteArray]. It iterates through each segment,
     * reads its corresponding file from the cache, and appends the bytes to an in-memory stream.
     *
     * This is useful for accessing the full, downloaded content directly, for example, to save it to a
     * different location or to process it.
     *
     * **WARNING**: This operation can be memory-intensive for large files, as it loads the entire
     * cached content into a byte array in memory.
     *
     * @receiver [CacheKey] The [CacheKey] for which to retrieve the full content.
     *
     * @return [ByteArray] A [ByteArray] containing the complete data of the cached item, or `null` if the item
     *         is not found in the cache or has no content.
     */
    context(context: Context)
    val CacheKey.contents
        get() = synchronized(cacheLock) {
            cacheMap[this@contents]?.run {
                val spans = getCachedSpans(this@contents.value)
                val cacheKeyBytes = ByteArrayOutputStream()

                spans.forEach { span ->
                    val spanFile = span?.file
                    val spanFileBytes = spanFile?.readBytes() ?: return@forEach

                    when {
                        span.isCached -> cacheKeyBytes.write(spanFileBytes)
                        else          -> Timber.error("Span is not cached or span file is null")
                    }
                }

                cacheKeyBytes.toByteArray()
            } ?: run {
                Timber.warn("Cache for key '${this@contents.value}' not found!")
                null
            }
        }

    /**
     * Retrieves the complete byte content of a cached item for a specific key.
     *
     * This function reads all the cached segments ([CacheSpan]) associated with the provided [key]
     * and concatenates their raw byte data into a single [ByteArray]. It iterates through each segment,
     * reads its corresponding file from the cache, and appends the bytes to an in-memory stream.
     *
     * This is useful for accessing the full, downloaded content directly, for example, to save it to a
     * different location or to process it.
     *
     * If the cache for the receiver [CacheKey] doesn't exist, or if no cached content is found
     * for the specified [key], it returns `null`.
     *
     * **WARNING**: This operation can be memory-intensive for large files, as it loads the entire
     * cached content into a byte array in memory.
     *
     * @receiver [CacheKey] The context of the [CacheKey] instance to search within.
     *
     * @param key [String] The specific [String] key of the content to retrieve.
     *
     * @return [ByteArray] A [ByteArray] containing the complete cached content, or `null` if the content is not found or the cache does not exist.
     */
    context(context: Context)
    fun CacheKey.keyContents(key: String) = synchronized(cacheLock) {
        cacheMap[this@keyContents]?.run {
            val spans = getCachedSpans(key)
            val cacheKeyBytes = ByteArrayOutputStream()

            spans.forEach { span ->
                val spanFile = span?.file
                val spanFileBytes = spanFile?.readBytes() ?: return@forEach

                when {
                    span.isCached -> cacheKeyBytes.write(spanFileBytes)
                    else          -> Timber.error("Span is not cached or span file is null")
                }
            }

            if (cacheKeyBytes.size() == 0) return null

            cacheKeyBytes.toByteArray()
        } ?: run {
            Timber.warn("Cache for key '${this@keyContents.value}' not found!")
            null
        }
    }

    /**
     * Releases all managed cache instances and clears the internal maps.
     *
     * This function should be called when the caches are no longer needed, for example,
     * when the application is shutting down or when a full cache reset is required.
     *
     * It iterates through all active [SimpleCache] instances stored in [cacheMap],
     * logs their size, and calls the [SimpleCache.release] method on each one.
     * The [SimpleCache.release] method closes the cache, releases file locks, and frees up
     * associated resources. After releasing all individual caches, it clears both
     * the [cacheMap] and [cacheFactoryMap], ensuring that any subsequent requests
     * will create new instances.
     *
     * Access is synchronized via [cacheLock] to prevent race conditions during the
     * release process. Any exceptions thrown during the release of a specific cache
     * are caught and logged, allowing the function to continue attempting to release
     * the remaining caches.
     *
     * @see SimpleCache.release
     * @see cacheMap
     * @see cacheFactoryMap
     */
    fun releaseCache() = synchronized(cacheLock) {
        cacheMap.forEach { (key, cache) ->
            try {
                val totalBytes = cache.cacheSpace
                Timber.debug("Releasing cache for key '${key.value}'. Current size: ${totalBytes.asHumanReadableSize}")
                cache.release()
            } catch (ex: Exception) {
                Timber.error("Error releasing cache for key '${key.value}': ${ex.message}")
            }
        }

        cacheMap.clear()
        cacheFactoryMap.clear()
    }

    /**
     * Deletes the entire cache directory associated with this [CacheKey].
     *
     * This function locates the specific cache directory for the given key and deletes
     * it recursively, removing all cached files and metadata for this key.
     *
     * It first checks if the cache directory exists before attempting deletion.
     * If the cache instance for the key is not found in the internal map, or if the
     * directory does not exist, a warning is logged, and the function does nothing.
     *
     * Access is synchronized to ensure thread safety during the deletion process.
     *
     * **WARNING**: This is a destructive operation and will permanently remove all cached
     * content associated with this key.
     *
     * @receiver [CacheKey] The [CacheKey] for which to delete the entire cache.
     */
    context(context: Context)
    fun CacheKey.delete() = synchronized(cacheLock) {
        cacheMap[this@delete]?.run {
            val cacheDir = File(context.cacheDir, "exoplayer_cache/$value")

            if (!cacheDir.exists()) return@run

            cacheDir.deleteRecursively()
            Timber.debug("Cache for key '${this@delete.value}' deleted!")
        } ?: Timber.warn("Cache for key '${this@delete.value}' not found!")
    }

    /**
     * Deletes all cached content associated with a specific key string within this cache instance.
     *
     * This function targets and removes all cached files [CacheSpan] for a particular content key
     * inside the cache directory managed by the receiver [CacheKey]. It iterates through all the
     * [CacheSpan]s associated with the given [key], deletes their corresponding files, and then
     * proceeds to delete the entire cache directory for the receiver [CacheKey].
     *
     * **FIX**: fix this method to delete only the files for the specific [key] string, not the entire cache.
     *
     * This method appears to have a side effect of deleting the entire cache for the receiver [CacheKey],
     * not just the files for the specific [key] string, due to [cacheDir.deleteRecursively()][File.deleteRecursively].
     * Use with caution.
     *
     * If the cache for the receiver [CacheKey] is not found in the internal map, a warning is logged,
     * and the function does nothing.
     *
     * Access is synchronized to ensure thread safety during the deletion process.
     *
     * **WARNING**: This is a destructive operation that permanently removes cached data.
     * Due to its current implementation, it may delete more data than intended.
     *
     * @receiver [CacheKey] The context of the [CacheKey] instance to delete from.
     *
     * @param key [String] The specific [String] key of the content to be deleted.
     */
    context(context: Context)
    fun CacheKey.deleteKey(key: String) = synchronized(cacheLock) {
        cacheMap[this@deleteKey]?.run {
            val spans = getCachedSpans(key)
            val cacheDir = File(context.cacheDir, "exoplayer_cache/$value")

            if (!cacheDir.exists()) return@run

            spans.forEach { span -> span?.file?.delete() }
            cacheDir.deleteRecursively()
            Timber.debug("Cache for key '${key}' deleted!")
        } ?: Timber.warn("Cache for key '${this@deleteKey.value}' not found!")
    }
}
