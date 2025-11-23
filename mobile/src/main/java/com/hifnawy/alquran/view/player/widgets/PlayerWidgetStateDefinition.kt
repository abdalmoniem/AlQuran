package com.hifnawy.alquran.view.player.widgets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetStateDefinition.getLocation
import java.io.File

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
    override suspend fun getDataStore(context: Context, fileKey: String) =
            DataStoreFactory.create(serializer = PlayerWidgetStateSerializer, produceFile = { getLocation(context, fileKey) })
}
