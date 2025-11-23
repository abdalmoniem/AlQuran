package com.hifnawy.alquran.view.player.widgets

import androidx.datastore.core.Serializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import com.hifnawy.alquran.shared.domain.ServiceStatus
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.error
import com.hifnawy.alquran.utils.RuntimeTypeAdapterFactoryEx
import com.hifnawy.alquran.utils.RuntimeTypeAdapterFactoryEx.registerSealedSubtypes
import com.hifnawy.alquran.utils.RuntimeTypeAdapterFactoryEx.registeredSubtypes
import com.hifnawy.alquran.view.player.widgets.PlayerWidgetStateSerializer.defaultValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

/**
 * A [Serializer] for the [PlayerWidgetState] class, used with Proto DataStore.
 *
 * This object handles the serialization and deserialization of the [PlayerWidgetState]
 * to and from a JSON string. It uses [Gson] for the JSON processing and is specifically
 * configured to handle the polymorphic nature of the [ServiceStatus] sealed class
 * by using a custom [RuntimeTypeAdapterFactoryEx].
 *
 * If an error occurs during deserialization (e.g., due to corrupted data or a `JsonSyntaxException`),
 * it logs the error and returns the `defaultValue` to ensure the app remains stable.
 *
 * @author AbdElMoniem ElHifnawy
 */
object PlayerWidgetStateSerializer : Serializer<PlayerWidgetState> {

    /**
     * A [RuntimeTypeAdapterFactoryEx] specifically configured for the [ServiceStatus] sealed class.
     *
     * This adapter is essential for correctly serializing and deserializing the different
     * subtypes of [ServiceStatus] (e.g., `Idle`, `Playing`, `Paused`) to and from JSON.
     * It uses the [registerSealedSubtypes] extension function to automatically discover and
     * register all sealed subtypes, enabling Gson to handle the polymorphism.
     *
     * @return [RuntimeTypeAdapterFactory< ServiceStatus >][RuntimeTypeAdapterFactory] the adapter for the [ServiceStatus] sealed class.
     */
    private val serviceStatusAdapter = ServiceStatus::class.registerSealedSubtypes

    /**
     * Gson instance configured to handle the serialization and deserialization of the [ServiceStatus] sealed class.
     * It uses a custom type adapter factory (`serviceStatusAdapter`) to correctly process the polymorphic nature
     * of [ServiceStatus] and its subtypes.
     *
     * @return [Gson] the Gson instance configured for the [PlayerWidgetState] class.
     */
    private val gson = GsonBuilder().registerTypeAdapterFactory(serviceStatusAdapter).create()

    /**
     * Initializes the object and logs the registered subtypes for the [ServiceStatus] sealed class.
     *
     * This block is called when the object is initialized and logs the registered subtypes of the
     * [ServiceStatus] sealed class. This information can be useful for debugging purposes and
     * verifying that all subtypes of [ServiceStatus] have been registered correctly.
     *
     * @see RuntimeTypeAdapterFactoryEx.registeredSubtypes
     */
    init {
        Timber.debug("Registered SubTypes:")
        serviceStatusAdapter.registeredSubtypes.lines().forEach { line -> Timber.debug(line) }
    }

    /**
     * The default value for the [PlayerWidgetState] data store.
     *
     * This value is used when the DataStore is first created or if there's an error
     * reading the existing data, such as a `JsonSyntaxException` during deserialization.
     * It ensures that the application always has a valid state to work with.
     *
     * @return [PlayerWidgetState] the default value for the [PlayerWidgetState] data store.
     */
    override val defaultValue = PlayerWidgetState()

    /**
     * Reads a [PlayerWidgetState] from the provided [InputStream].
     *
     * This function reads the byte stream, decodes it into a JSON string, and then uses Gson
     * to deserialize it into a [PlayerWidgetState] object. If a [JsonSyntaxException] occurs,
     * indicating corrupted or invalid data, it logs the error and returns the [defaultValue]
     * to prevent the application from crashing.
     *
     * @param input [InputStream] The [InputStream] to read the data from.
     *
     * @return [PlayerWidgetState] The deserialized [PlayerWidgetState] object, or the [defaultValue] if an error occurs.
     *
     * @throws SerializationException if an I/O error occurs during reading.
     */
    @Throws(SerializationException::class)
    override suspend fun readFrom(input: InputStream): PlayerWidgetState = withContext(Dispatchers.IO) {
        val jsonString = input.readBytes().decodeToString()

        try {
            gson.fromJson(jsonString, PlayerWidgetState::class.java)
        } catch (ex: JsonSyntaxException) {
            Timber.error("Error reading widget state: $ex")
            ex.printStackTrace()

            defaultValue
        }
    }

    /**
     * Serializes the given [PlayerWidgetState] `t` to a JSON string and writes it to the `output` stream.
     *
     * This method is executed on the [Dispatchers.IO] thread to avoid blocking the main thread.
     * It uses the configured [Gson] instance to convert the state object into a JSON representation,
     * which is then written to the provided [OutputStream] as a byte array.
     *
     * @param t [PlayerWidgetState] The [PlayerWidgetState] instance to serialize.
     * @param output [OutputStream] The [OutputStream] to write the serialized data to.
     */
    override suspend fun writeTo(t: PlayerWidgetState, output: OutputStream) = withContext(Dispatchers.IO) {
        gson.toJson(t).run { output.write(encodeToByteArray()) }
    }
}
