package com.hifnawy.alquran.view.player.widgets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetStateDefinition.getLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A [GlanceStateDefinition] for the [PlayerWidget].
 *
 * This object defines how the state for the player widget ([PlayerWidgetState]) is stored and retrieved.
 * It uses a [DataStoreFactory] to create a DataStore instance for each widget, serializing the state
 * with [PlayerWidgetStateSerializer]. The location of the DataStore file is determined by the
 * provided context and a unique file key for each widget instance.
 *
 * @author AbdElMoniem ElHifnawy
 */
object PlayerWidgetStateDefinition : GlanceStateDefinition<PlayerWidgetState> {

    /**
     * A [CoroutineScope] for managing the lifecycle of the DataStore.
     *
     * This scope is used by the [DataStoreFactory] to handle asynchronous operations,
     * such as reading from and writing to the state file. It uses a [SupervisorJob]
     * to ensure that the failure of one coroutine (e.g., for one widget instance)
     * does not cancel the entire scope, and it runs on the [Dispatchers.IO] thread pool,
     * which is optimized for disk and network I/O operations.
     *
     * @return [CoroutineScope] the [CoroutineScope] from which all datastore operations are performed.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A concurrent in-memory cache for [DataStore] instances.
     *
     * This map holds the created [DataStore] instances, keyed by their unique `fileKey`.
     * Using a [ConcurrentHashMap] ensures thread-safe access and retrieval, which is crucial
     * because [getDataStore] can be called from different threads. Caching these instances
     * prevents the overhead of repeatedly creating a new [DataStore] for the same widget,
     * improving performance by reusing the existing object.
     *
     * @return [ConcurrentHashMap <String, DataStore< PlayerWidgetState>>][ConcurrentHashMap]
     *  A concurrent in-memory cache for [DataStore < PlayerWidgetState>][DataStore] instances.
     */
    private val dataStoreMap = ConcurrentHashMap<String, DataStore<PlayerWidgetState>>()

    /**
     * Returns the file where the widget state is stored.
     *
     * This function defines the storage location for a specific widget's state data.
     * It uses the application context to create a file within the app's `datastore` directory.
     * The filename is constructed by prefixing "player_widget_state_" with a unique `fileKey`
     * to ensure that each widget instance has its own state file.
     *
     * @param context [Context] The application context, used to access the file system.
     * @param fileKey [String] A unique identifier for the widget instance, used to create a distinct filename.
     *
     * @return A [File] object pointing to the location of the datastore file.
     */
    override fun getLocation(context: Context, fileKey: String) =
            context.dataStoreFile("player_widget_state_$fileKey")

    /**
     * Creates and returns a [DataStore] instance for the widget's state.
     *
     * This function uses a [DataStoreFactory] to construct a DataStore for managing the
     * [PlayerWidgetState]. It configures the DataStore to use [PlayerWidgetStateSerializer] for
     * serializing and deserializing the state object. The actual file location for the DataStore
     * is determined by the [getLocation] function, ensuring each widget instance has its own
     * persistent storage.
     *
     * @param context [Context] The application context, which is passed to the factory to create the DataStore.
     * @param fileKey [String] A unique key for the widget instance, used to identify the correct storage file.
     *
     * @return [DataStore] A [DataStore] of [PlayerWidgetState] for the specified widget instance.
     */
    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<PlayerWidgetState> = dataStoreMap.getOrPut(fileKey) {
        DataStoreFactory.create(
                serializer = PlayerWidgetStateSerializer,
                produceFile = { getLocation(context, fileKey) },
                scope = scope
        )
    }

    /**
     * Releases the resources associated with a specific widget instance.
     *
     * This function performs cleanup for a widget that is being removed. It removes the cached
     * [DataStore] instance from the in-memory map and deletes the corresponding state file
     * from the device's storage. This ensures that no orphaned data or resources are left
     * behind when a widget is no longer in use.
     *
     * @param context [Context] The application context, used to locate the state file.
     * @param fileKey [String] The unique key identifying the widget instance to be released.
     */
    fun release(context: Context, fileKey: String) {
        dataStoreMap.remove(fileKey)
        getLocation(context, fileKey).delete()
    }
}
